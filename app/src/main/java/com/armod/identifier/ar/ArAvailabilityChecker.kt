package com.armod.identifier.ar

import android.content.Context
import com.google.ar.core.ArCoreApk

/**
 * Checks ARCore support and Google Play Services for AR availability on the device.
 *
 * Designed to fail gracefully on unsupported devices (such as Samsung Galaxy F13)
 * without blocking the application or forcing installation.
 */
object ArAvailabilityChecker {

    /**
     * Checks device ARCore availability synchronously using cached status or checks asynchronously.
     */
    fun checkAvailability(context: Context): ArAvailabilityStatus {
        return try {
            val availability = ArCoreApk.getInstance().checkAvailability(context)
            mapAvailability(availability)
        } catch (e: Exception) {
            ArAvailabilityStatus.UnsupportedDevice("ARCore availability check failed: ${e.message}")
        }
    }

    /**
     * Maps an [ArCoreApk.Availability] instance to our typed [ArAvailabilityStatus].
     */
    fun mapAvailability(availability: ArCoreApk.Availability): ArAvailabilityStatus {
        return when (availability) {
            ArCoreApk.Availability.SUPPORTED_INSTALLED -> {
                ArAvailabilityStatus.SupportedAndInstalled
            }

            ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD,
            ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> {
                ArAvailabilityStatus.SupportedNeedsInstall
            }

            ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> {
                ArAvailabilityStatus.UnsupportedDevice(
                    "This device hardware is not certified for Google ARCore. 2D fallback mode will be used."
                )
            }

            ArCoreApk.Availability.UNKNOWN_CHECKING -> {
                ArAvailabilityStatus.Unknown("Checking ARCore compatibility…")
            }

            ArCoreApk.Availability.UNKNOWN_ERROR -> {
                ArAvailabilityStatus.UnsupportedDevice("ARCore availability check returned unknown error.")
            }

            ArCoreApk.Availability.UNKNOWN_TIMED_OUT -> {
                ArAvailabilityStatus.UnsupportedDevice("ARCore availability check timed out.")
            }
        }
    }
}
