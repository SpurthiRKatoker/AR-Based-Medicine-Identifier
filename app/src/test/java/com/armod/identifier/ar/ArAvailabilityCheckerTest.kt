package com.armod.identifier.ar

import com.google.ar.core.ArCoreApk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArAvailabilityCheckerTest {

    @Test
    fun mapAvailability_supportedInstalled_returnsSupportedAndInstalled() {
        val result = ArAvailabilityChecker.mapAvailability(ArCoreApk.Availability.SUPPORTED_INSTALLED)
        assertEquals(ArAvailabilityStatus.SupportedAndInstalled, result)
    }

    @Test
    fun mapAvailability_supportedNotInstalled_returnsSupportedNeedsInstall() {
        val result = ArAvailabilityChecker.mapAvailability(ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED)
        assertEquals(ArAvailabilityStatus.SupportedNeedsInstall, result)
    }

    @Test
    fun mapAvailability_supportedApkTooOld_returnsSupportedNeedsInstall() {
        val result = ArAvailabilityChecker.mapAvailability(ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD)
        assertEquals(ArAvailabilityStatus.SupportedNeedsInstall, result)
    }

    @Test
    fun mapAvailability_unsupportedDevice_returnsUnsupportedDeviceWithExplanation() {
        val result = ArAvailabilityChecker.mapAvailability(ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE)
        assertTrue(result is ArAvailabilityStatus.UnsupportedDevice)
        val unsupported = result as ArAvailabilityStatus.UnsupportedDevice
        assertTrue(unsupported.reason.contains("not certified", ignoreCase = true))
    }

    @Test
    fun mapAvailability_unknownStates_returnsExpectedStatus() {
        val checking = ArAvailabilityChecker.mapAvailability(ArCoreApk.Availability.UNKNOWN_CHECKING)
        assertTrue(checking is ArAvailabilityStatus.Unknown)

        val timedOut = ArAvailabilityChecker.mapAvailability(ArCoreApk.Availability.UNKNOWN_TIMED_OUT)
        assertTrue(timedOut is ArAvailabilityStatus.UnsupportedDevice)

        val error = ArAvailabilityChecker.mapAvailability(ArCoreApk.Availability.UNKNOWN_ERROR)
        assertTrue(error is ArAvailabilityStatus.UnsupportedDevice)
    }

    @Test
    fun arSessionState_statesAndTransitions() {
        val uninitialized: ArSessionState = ArSessionState.Uninitialized
        assertTrue(uninitialized is ArSessionState.Uninitialized)

        val active = ArSessionState.Active(ArTrackingState.Tracking)
        assertEquals(ArTrackingState.Tracking, active.trackingState)

        val searching = ArSessionState.Active(ArTrackingState.SearchingPlanes)
        assertEquals(ArTrackingState.SearchingPlanes, searching.trackingState)

        val limited = ArSessionState.Active(ArTrackingState.Limited("Low light"))
        assertEquals("Low light", (limited.trackingState as ArTrackingState.Limited).reason)

        val error = ArSessionState.Error("Camera busy", canFallbackTo2D = true)
        assertEquals("Camera busy", error.message)
        assertTrue(error.canFallbackTo2D)

        val unsupported = ArSessionState.Unsupported("Not capable")
        assertEquals("Not capable", unsupported.reason)
    }
}
