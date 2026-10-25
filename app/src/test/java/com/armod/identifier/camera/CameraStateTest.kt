package com.armod.identifier.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraStateTest {

    @Test
    fun cameraState_permissionDenied_retainsPermanenceFlag() {
        val normalDenial = CameraState.PermissionDenied(isPermanentlyDenied = false)
        assertFalse(normalDenial.isPermanentlyDenied)

        val permanentDenial = CameraState.PermissionDenied(isPermanentlyDenied = true)
        assertTrue(permanentDenial.isPermanentlyDenied)
    }

    @Test
    fun cameraState_captured_retainsMetadata() {
        val captured = CameraState.Captured(
            imageWidth = 1920,
            imageHeight = 1080,
            rotationDegrees = 90
        )
        assertEquals(1920, captured.imageWidth)
        assertEquals(1080, captured.imageHeight)
        assertEquals(90, captured.rotationDegrees)
        assertTrue(captured.timestamp > 0)
    }

    @Test
    fun cameraState_error_retainsMessageAndRetry() {
        val recoverableError = CameraState.Error(message = "Camera in use by another app", canRetry = true)
        assertEquals("Camera in use by another app", recoverableError.message)
        assertTrue(recoverableError.canRetry)

        val fatalError = CameraState.Error(message = "Hardware unavailable", canRetry = false)
        assertFalse(fatalError.canRetry)
    }
}
