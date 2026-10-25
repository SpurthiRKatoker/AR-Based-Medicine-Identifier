package com.armod.identifier.ar

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.ar.core.ArCoreApk
import com.google.ar.core.CameraConfig
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException

/**
 * Manages the lifecycle, configuration, and exception handling of the ARCore [Session].
 *
 * Designed to guarantee:
 * 1. Safe lifecycle cleanup (no camera or GL leaks).
 * 2. Graceful exception catching for unsupported hardware (Galaxy F13) or uninstalled services.
 * 3. Continuous autofocus for close-up medicine package scanning.
 * 4. Automatic fallback state notification.
 */
class ArSessionManager(
    private val context: Context,
    private val onStateChange: (ArSessionState) -> Unit
) {
    companion object {
        private const val TAG = "ArSessionManager"
    }

    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    var session: Session? = null
        private set
    var currentState: ArSessionState = ArSessionState.Uninitialized
        private set

    /**
     * Initializes and configures the ARCore session.
     * Returns true if session was created successfully, false if fallback is triggered.
     */
    fun createSession(activity: Activity? = null): Boolean {
        updateState(ArSessionState.Initializing)

        // 1. Verify availability first
        val availability = ArAvailabilityChecker.checkAvailability(context)
        if (availability is ArAvailabilityStatus.UnsupportedDevice) {
            updateState(ArSessionState.Unsupported(availability.reason))
            return false
        }

        try {
            // 2. Instantiate Session
            val newSession = Session(context)

            // 3. Configure Session: autofocus and plane finding
            val config = Config(newSession).apply {
                focusMode = Config.FocusMode.AUTO
                updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
            }
            newSession.configure(config)

            this.session = newSession
            updateState(ArSessionState.Active(ArTrackingState.SearchingPlanes))
            Log.i(TAG, "ARCore Session created and configured successfully.")
            return true
        } catch (e: UnavailableArcoreNotInstalledException) {
            Log.w(TAG, "ARCore not installed: ${e.message}")
            updateState(ArSessionState.Error("Google Play Services for AR is not installed.", canFallbackTo2D = true))
            return false
        } catch (e: UnavailableDeviceNotCompatibleException) {
            Log.w(TAG, "Device not compatible with ARCore: ${e.message}")
            updateState(ArSessionState.Unsupported("Device hardware is not certified for ARCore. 2D fallback active."))
            return false
        } catch (e: UnavailableApkTooOldException) {
            Log.w(TAG, "ARCore APK too old: ${e.message}")
            updateState(ArSessionState.Error("ARCore requires an update from Play Store.", canFallbackTo2D = true))
            return false
        } catch (e: UnavailableSdkTooOldException) {
            Log.w(TAG, "ARCore SDK too old: ${e.message}")
            updateState(ArSessionState.Error("App ARCore SDK requires an update.", canFallbackTo2D = true))
            return false
        } catch (e: UnavailableUserDeclinedInstallationException) {
            Log.w(TAG, "User declined ARCore installation: ${e.message}")
            updateState(ArSessionState.Unsupported("ARCore installation was declined. 2D fallback active."))
            return false
        } catch (e: SecurityException) {
            Log.e(TAG, "Camera permission not granted for AR: ${e.message}")
            updateState(ArSessionState.Error("Camera permission is required for AR view.", canFallbackTo2D = true))
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create ARCore session: ${e.message}", e)
            updateState(ArSessionState.Error("Failed to initialize AR: ${e.message}", canFallbackTo2D = true))
            return false
        }
    }

    /**
     * Resumes the ARCore session. Must be called in onResume or active composition.
     */
    fun resume() {
        val s = session ?: return
        try {
            s.resume()
            updateState(ArSessionState.Active(ArTrackingState.SearchingPlanes))
            Log.i(TAG, "ARCore Session resumed.")
        } catch (e: CameraNotAvailableException) {
            Log.e(TAG, "Camera not available for ARCore: ${e.message}", e)
            updateState(ArSessionState.Error("Camera is currently in use or unavailable.", canFallbackTo2D = true))
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming ARCore session: ${e.message}", e)
            updateState(ArSessionState.Error("Error resuming AR: ${e.message}", canFallbackTo2D = true))
        }
    }

    /**
     * Pauses the ARCore session and releases camera access.
     */
    fun pause() {
        try {
            session?.pause()
            Log.i(TAG, "ARCore Session paused.")
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing ARCore session: ${e.message}", e)
        }
    }

    /**
     * Updates tracking state based on the current AR frame.
     */
    fun updateFromFrame(frame: Frame) {
        val camera = frame.camera
        val trackingState = when (camera.trackingState) {
            TrackingState.TRACKING -> ArTrackingState.Tracking
            TrackingState.PAUSED -> {
                when (camera.trackingFailureReason) {
                    TrackingFailureReason.EXCESSIVE_MOTION -> {
                        ArTrackingState.Limited("Excessive motion: hold device steady")
                    }
                    TrackingFailureReason.INSUFFICIENT_LIGHT -> {
                        ArTrackingState.Limited("Low lighting: improve illumination")
                    }
                    TrackingFailureReason.INSUFFICIENT_FEATURES -> {
                        ArTrackingState.Limited("Searching for package texture…")
                    }
                    TrackingFailureReason.CAMERA_UNAVAILABLE -> {
                        ArTrackingState.Limited("Camera unavailable")
                    }
                    else -> ArTrackingState.SearchingPlanes
                }
            }
            TrackingState.STOPPED -> ArTrackingState.Lost
        }
        updateState(ArSessionState.Active(trackingState))
    }

    /**
     * Releases all native resources held by the ARCore session.
     */
    fun destroy() {
        try {
            session?.pause()
            session?.close()
            session = null
            updateState(ArSessionState.Uninitialized)
            Log.i(TAG, "ARCore Session destroyed.")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing ARCore session: ${e.message}", e)
        }
    }

    private fun updateState(newState: ArSessionState) {
        currentState = newState
        mainHandler.post {
            onStateChange(newState)
        }
    }
}
