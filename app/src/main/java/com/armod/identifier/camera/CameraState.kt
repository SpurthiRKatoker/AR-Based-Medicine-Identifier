package com.armod.identifier.camera

/**
 * State representing camera permissions and capture readiness.
 */
sealed interface CameraState {
    data object PermissionRequired : CameraState
    data class PermissionDenied(val isPermanentlyDenied: Boolean = false) : CameraState
    data object Initializing : CameraState
    data object Ready : CameraState
    data object Capturing : CameraState
    data class Captured(
        val imageWidth: Int,
        val imageHeight: Int,
        val rotationDegrees: Int,
        val timestamp: Long = System.currentTimeMillis()
    ) : CameraState
    data class Error(val message: String, val canRetry: Boolean = true) : CameraState
}
