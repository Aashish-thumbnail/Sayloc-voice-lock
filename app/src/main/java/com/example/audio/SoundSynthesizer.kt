package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

enum class SoundEffect {
    TAP,
    LISTENING_START,
    COMMAND_DETECTED,
    LOCK,
    ERROR
}

class SoundSynthesizer {
    private val sampleRate = 24000
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    var enabled: Boolean = true

    fun play(effect: SoundEffect) {
        if (!enabled) return
        scope.launch {
            runCatching {
                val samples = when (effect) {
                    SoundEffect.TAP -> generateToneSequence(
                        listOf(ToneSpec(freqStart = 760.0, freqEnd = 520.0, durationMs = 38, volume = 0.28))
                    )
                    SoundEffect.LISTENING_START -> generateToneSequence(
                        listOf(
                            ToneSpec(freqStart = 440.0, freqEnd = 587.3, durationMs = 85, volume = 0.34),
                            ToneSpec(freqStart = 659.2, freqEnd = 880.0, durationMs = 115, volume = 0.36)
                        )
                    )
                    SoundEffect.COMMAND_DETECTED -> generateToneSequence(
                        listOf(
                            ToneSpec(freqStart = 587.3, freqEnd = 587.3, durationMs = 70, volume = 0.38),
                            ToneSpec(freqStart = 880.0, freqEnd = 1174.6, durationMs = 135, volume = 0.42)
                        )
                    )
                    SoundEffect.LOCK -> generateToneSequence(
                        listOf(
                            ToneSpec(freqStart = 660.0, freqEnd = 330.0, durationMs = 90, volume = 0.40),
                            ToneSpec(freqStart = 280.0, freqEnd = 160.0, durationMs = 120, volume = 0.36)
                        )
                    )
                    SoundEffect.ERROR -> generateToneSequence(
                        listOf(
                            ToneSpec(freqStart = 240.0, freqEnd = 210.0, durationMs = 110, volume = 0.36),
                            ToneSpec(freqStart = 185.0, freqEnd = 155.0, durationMs = 140, volume = 0.34)
                        )
                    )
                }
                playPcm(samples, AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            }
        }
    }

    fun playAlarmPulse() {
        runCatching {
            val samples = generateToneSequence(
                listOf(
                    ToneSpec(freqStart = 880.0, freqEnd = 1320.0, durationMs = 240, volume = 0.88),
                    ToneSpec(freqStart = 1320.0, freqEnd = 880.0, durationMs = 240, volume = 0.88)
                )
            )
            playPcm(samples, AudioAttributes.USAGE_ALARM)
        }
    }

    private data class ToneSpec(
        val freqStart: Double,
        val freqEnd: Double,
        val durationMs: Int,
        val volume: Double
    )

    private fun generateToneSequence(specs: List<ToneSpec>): ShortArray {
        val totalSamples = specs.sumOf { (sampleRate * it.durationMs) / 1000 }
        val buffer = ShortArray(totalSamples)
        var offset = 0
        var phase = 0.0

        for (spec in specs) {
            val count = (sampleRate * spec.durationMs) / 1000
            val attackSamples = min(count / 4, (sampleRate * 10) / 1000).coerceAtLeast(1)
            val releaseSamples = min(count / 3, (sampleRate * 25) / 1000).coerceAtLeast(1)

            for (i in 0 until count) {
                val progress = i.toDouble() / count.coerceAtLeast(1)
                val freq = spec.freqStart + (spec.freqEnd - spec.freqStart) * progress
                phase += 2.0 * PI * freq / sampleRate

                val env = when {
                    i < attackSamples -> i.toDouble() / attackSamples
                    i > count - releaseSamples -> (count - i).toDouble() / releaseSamples
                    else -> 1.0
                }.coerceIn(0.0, 1.0)

                // Fundamental + soft 2nd harmonic for glossy glass-like tone
                val wave = 0.82 * sin(phase) + 0.18 * sin(phase * 2.0)
                val sample = (wave * env * spec.volume * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                buffer[offset + i] = sample.toShort()
            }
            offset += count
        }
        return buffer
    }

    private fun playPcm(samples: ShortArray, usage: Int) {
        if (samples.isEmpty()) return
        val byteCount = samples.size * 2
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(byteCount)

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(usage)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBuf)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        try {
            audioTrack.write(samples, 0, samples.size)
            audioTrack.play()
            val sleepMs = (samples.size * 1000L) / sampleRate + 40L
            Thread.sleep(sleepMs)
        } finally {
            runCatching {
                audioTrack.stop()
                audioTrack.release()
            }
        }
    }
}
