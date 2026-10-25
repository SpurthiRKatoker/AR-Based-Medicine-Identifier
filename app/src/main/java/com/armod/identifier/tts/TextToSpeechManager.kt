package com.armod.identifier.tts

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID

/**
 * Observable lifecycle states of the on-device TextToSpeech engine.
 */
sealed interface TtsState {
    data object Initializing : TtsState
    data class Ready(val isSpeaking: Boolean = false) : TtsState
    data class Error(val message: String) : TtsState
    data class LanguageUnavailable(val reason: String) : TtsState
}

/**
 * Lifecycle-aware wrapper around Android's native [TextToSpeech] engine.
 *
 * Rules:
 * - 100% on-device; no cloud API or network permissions.
 * - Posts state callbacks to Android Main Looper for safe Compose state consumption.
 * - Tracks last spoken text for accessible 'Repeat' functionality.
 * - Releases resources on dispose to prevent audio leaks or background playback.
 */
class TextToSpeechManager(
    context: Context,
    private val onStateChange: (TtsState) -> Unit = {}
) : TextToSpeech.OnInitListener {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var lastSpokenText: String? = null
    private var pendingSpeechOnReady: String? = null

    var currentState: TtsState = TtsState.Initializing
        private set

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val ttsEngine = tts
            if (ttsEngine != null) {
                // Prioritize system default locale, fallback to US English
                var langResult = ttsEngine.setLanguage(Locale.getDefault())
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    langResult = ttsEngine.setLanguage(Locale.US)
                }

                if (langResult == TextToSpeech.LANG_MISSING_DATA) {
                    updateState(TtsState.LanguageUnavailable("Language voice data is missing on this device."))
                    return
                } else if (langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    updateState(TtsState.LanguageUnavailable("English language is not supported by device TTS engine."))
                    return
                }

                ttsEngine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        updateState(TtsState.Ready(isSpeaking = true))
                    }

                    override fun onDone(utteranceId: String?) {
                        updateState(TtsState.Ready(isSpeaking = false))
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        updateState(TtsState.Ready(isSpeaking = false))
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        updateState(TtsState.Ready(isSpeaking = false))
                    }
                })

                updateState(TtsState.Ready(isSpeaking = false))

                // If speech was queued while waiting for init, deliver it now
                val queued = pendingSpeechOnReady
                pendingSpeechOnReady = null
                if (!queued.isNullOrBlank()) {
                    speak(queued)
                }
            } else {
                updateState(TtsState.Error("TextToSpeech engine instance is null."))
            }
        } else {
            updateState(TtsState.Error("TextToSpeech engine failed to initialize (status $status)."))
        }
    }

    /**
     * Speaks the provided text using QUEUE_FLUSH to cancel any prior speech.
     */
    fun speak(text: String) {
        if (text.isBlank()) return
        lastSpokenText = text

        val ttsEngine = tts
        when (val state = currentState) {
            is TtsState.Initializing -> {
                pendingSpeechOnReady = text
            }
            is TtsState.Ready -> {
                val utteranceId = UUID.randomUUID().toString()
                ttsEngine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            }
            is TtsState.Error, is TtsState.LanguageUnavailable -> {
                // Voice guidance unavailable; visual fallback handles display
            }
        }
    }

    /**
     * Immediately halts any active speech.
     */
    fun stop() {
        pendingSpeechOnReady = null
        tts?.stop()
        if (currentState is TtsState.Ready) {
            updateState(TtsState.Ready(isSpeaking = false))
        }
    }

    /**
     * Replays the last spoken text from the beginning.
     */
    fun repeat() {
        val text = lastSpokenText
        if (!text.isNullOrBlank()) {
            speak(text)
        }
    }

    /**
     * Shuts down and releases the TTS engine. Must be called when disposing of the screen/activity.
     */
    fun release() {
        pendingSpeechOnReady = null
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {
        } finally {
            tts = null
        }
    }

    private fun updateState(newState: TtsState) {
        currentState = newState
        mainHandler.post {
            onStateChange(newState)
        }
    }
}
