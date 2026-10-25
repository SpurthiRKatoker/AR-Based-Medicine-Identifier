package com.armod.identifier.ar

/**
 * Live spatial tracking state for the AR overlay.
 */
sealed interface ArTrackingState {
    /**
     * Camera pose and spatial tracking are active with good quality.
     */
    data object Tracking : ArTrackingState

    /**
     * ARCore is actively searching for planes or surfaces to anchor the medicine overlay.
     */
    data object SearchingPlanes : ArTrackingState

    /**
     * Tracking is limited due to environmental conditions (low light, excessive motion).
     */
    data class Limited(val reason: String) : ArTrackingState

    /**
     * Tracking is lost.
     */
    data object Lost : ArTrackingState
}

/**
 * High-level state of the AR session and lifecycle.
 */
sealed interface ArSessionState {
    data object Uninitialized : ArSessionState
    data object Initializing : ArSessionState
    data class Active(val trackingState: ArTrackingState = ArTrackingState.SearchingPlanes) : ArSessionState
    data class Error(val message: String, val canFallbackTo2D: Boolean = true) : ArSessionState
    data class Unsupported(val reason: String) : ArSessionState
}
