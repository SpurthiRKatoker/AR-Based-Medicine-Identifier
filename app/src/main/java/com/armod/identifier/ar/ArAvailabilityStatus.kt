package com.armod.identifier.ar

/**
 * Represents the device-level ARCore availability status.
 */
sealed interface ArAvailabilityStatus {
    /**
     * ARCore is officially supported by the device hardware and Google Play Services for AR is installed.
     */
    data object SupportedAndInstalled : ArAvailabilityStatus

    /**
     * ARCore is supported by the device hardware, but Google Play Services for AR requires
     * installation or an update from the Play Store.
     */
    data object SupportedNeedsInstall : ArAvailabilityStatus

    /**
     * The device hardware or chipset is not certified for ARCore (e.g. Samsung Galaxy F13).
     * The application must automatically transition to the 2D accessible fallback.
     */
    data class UnsupportedDevice(val reason: String) : ArAvailabilityStatus

    /**
     * Device capability check is currently running or encountered a transient state.
     */
    data class Unknown(val message: String = "Checking AR readiness…") : ArAvailabilityStatus
}
