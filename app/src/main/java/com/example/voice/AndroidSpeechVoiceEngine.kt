package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * On-device VoiceEngine backed by Android's built-in SpeechRecognizer.
 * Prefers offline recognition (EXTRA_PREFER_OFFLINE) and gracefully falls back
 * if an offline language pack is not yet downloaded on the phone.
 * Manages clean restart pacing to avoid tight battery-draining error loops.
 */
class AndroidSpeechVoiceEngine(
    private val context: Context
) : VoiceEngine {

    override val engineName: String = "Android SpeechRecognizer (Offline-First)"

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var callback: VoiceEngineCallback? = null
    private var currentConfig: VoiceEngineConfig = VoiceEngineConfig()

    @Volatile
    private var isRunning: Boolean = false
    private var consecutiveErrors: Int = 0
    private var fallbackOnlineOnce: Boolean = false

    private val restartRunnable = Runnable {
        if (isRunning) {
            beginListeningSession()
        }
    }

    override fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    override fun start(config: VoiceEngineConfig, callback: VoiceEngineCallback) {
        this.currentConfig = config
        this.callback = callback
        this.isRunning = true
        this.consecutiveErrors = 0
        this.fallbackOnlineOnce = false

        mainHandler.post {
            if (!isAvailable()) {
                isRunning = false
                callback.onEngineError(
                    SpeechRecognizer.ERROR_CLIENT,
                    "No Android speech recognition service is installed on this phone.",
                    recoverable = false
                )
                return@post
            }
            recreateRecognizer()
            beginListeningSession()
        }
    }

    override fun stop() {
        isRunning = false
        mainHandler.removeCallbacks(restartRunnable)
        mainHandler.post {
            runCatching {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
            }
            callback?.onEngineStopped()
        }
    }

    override fun destroy() {
        isRunning = false
        mainHandler.removeCallbacks(restartRunnable)
        mainHandler.post {
            runCatching {
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            }
            speechRecognizer = null
            callback = null
        }
    }

    private fun recreateRecognizer() {
        runCatching {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext).apply {
            setRecognitionListener(listener)
        }
    }

    private fun beginListeningSession() {
        if (!isRunning) return
        val recognizer = speechRecognizer ?: run {
            recreateRecognizer()
            speechRecognizer
        } ?: return

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentConfig.language.localeTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, currentConfig.language.localeTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2000L)
            if (currentConfig.preferOffline && !fallbackOnlineOnce) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
        }

        runCatching {
            recognizer.startListening(intent)
        }.onFailure { err ->
            scheduleRestart(delayMs = 1200L, recreate = true)
            callback?.onEngineError(
                SpeechRecognizer.ERROR_CLIENT,
                err.message ?: "Failed to start recognizer",
                recoverable = true
            )
        }
    }

    private fun scheduleRestart(delayMs: Long, recreate: Boolean = false) {
        if (!isRunning) return
        mainHandler.removeCallbacks(restartRunnable)
        mainHandler.postDelayed({
            if (!isRunning) return@postDelayed
            if (recreate) {
                recreateRecognizer()
            }
            beginListeningSession()
        }, delayMs)
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            consecutiveErrors = 0
            callback?.onReadyForSpeech()
        }

        override fun onBeginningOfSpeech() = Unit

        override fun onRmsChanged(rmsdB: Float) {
            // Typical SpeechRecognizer rmsdB range is -2f..10f
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            callback?.onRmsChanged(normalized)
        }

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() {
            callback?.onRmsChanged(0f)
        }

        override fun onError(error: Int) {
            callback?.onRmsChanged(0f)
            if (!isRunning) return

            when (error) {
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                    // Normal quiet cycle — gentle pause saves battery and reduces rapid chimes
                    scheduleRestart(delayMs = 450L, recreate = false)
                }

                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                    consecutiveErrors++
                    scheduleRestart(delayMs = 900L, recreate = true)
                }

                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                    isRunning = false
                    callback?.onEngineError(
                        error,
                        "Microphone permission is required.",
                        recoverable = false
                    )
                }

                SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE,
                SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                    consecutiveErrors++
                    if (currentConfig.preferOffline && !fallbackOnlineOnce) {
                        // Offline pack missing on phone — relax EXTRA_PREFER_OFFLINE and notify cleanly
                        fallbackOnlineOnce = true
                        callback?.onEngineError(
                            error,
                            "Offline voice pack unavailable; switching to default recognizer.",
                            recoverable = true
                        )
                        scheduleRestart(delayMs = 700L, recreate = true)
                    } else {
                        val backoff = (1500L * consecutiveErrors.coerceAtMost(5))
                        callback?.onEngineError(
                            error,
                            "Speech engine temporarily unavailable (code $error). Retrying…",
                            recoverable = true
                        )
                        scheduleRestart(delayMs = backoff, recreate = true)
                    }
                }

                else -> {
                    consecutiveErrors++
                    val backoff = (800L * consecutiveErrors.coerceAtMost(6))
                    callback?.onEngineError(
                        error,
                        "Recognizer paused (code $error). Restarting…",
                        recoverable = true
                    )
                    scheduleRestart(delayMs = backoff, recreate = true)
                }
            }
        }

        override fun onResults(results: Bundle?) {
            callback?.onRmsChanged(0f)
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
            if (matches.isNotEmpty()) {
                callback?.onFinalTranscript(matches)
            }
            if (isRunning) {
                scheduleRestart(delayMs = 420L, recreate = false)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val partials = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
            if (partials.isNotEmpty()) {
                callback?.onPartialTranscript(partials)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }
}
