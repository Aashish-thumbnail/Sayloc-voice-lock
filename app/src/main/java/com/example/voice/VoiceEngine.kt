package com.example.voice

import com.example.data.VoiceLanguage

data class VoiceEngineConfig(
    val language: VoiceLanguage = VoiceLanguage.HINGLISH,
    val preferOffline: Boolean = true,
    val suppressRecognizerBeep: Boolean = true
)

interface VoiceEngineCallback {
    fun onReadyForSpeech()
    fun onRmsChanged(normalizedLevel: Float)
    fun onPartialTranscript(candidates: List<String>)
    fun onFinalTranscript(candidates: List<String>)
    fun onEngineError(errorCode: Int, message: String, recoverable: Boolean)
    fun onEngineStopped()
}

/**
 * Pluggable voice recognition interface.
 * Default implementation uses Android's built-in SpeechRecognizer (prefer offline).
 * An offline Vosk engine can implement this same interface later without changing the service or UI.
 */
interface VoiceEngine {
    val engineName: String
    fun isAvailable(): Boolean
    fun start(config: VoiceEngineConfig, callback: VoiceEngineCallback)
    fun stop()
    fun destroy()
}
