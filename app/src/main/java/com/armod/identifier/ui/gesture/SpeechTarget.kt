package com.armod.identifier.ui.gesture

/**
 * Designated audio playback target triggered by an accessible touch gesture.
 */
enum class SpeechTarget {
    /**
     * Single Tap: Triggers playback of basic item name and dosage.
     */
    BasicNameAndDosage,

    /**
     * Double Tap: Triggers playback of warnings and side effects.
     */
    WarningsAndSideEffects,

    /**
     * Long Press: Triggers playback of the complete profile summary.
     */
    FullProfile
}
