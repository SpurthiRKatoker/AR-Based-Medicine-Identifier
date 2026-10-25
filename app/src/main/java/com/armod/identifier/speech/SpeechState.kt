package com.armod.identifier.speech

/**
 * State representing TextToSpeech lifecycle and utterance status.
 */
sealed interface SpeechState {
    data object Uninitialized : SpeechState
    data object Ready : SpeechState
    data class Speaking(val utterance: String) : SpeechState
    data object Unavailable : SpeechState
}
