package com.example.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

enum class HapticStyle {
    TAP,
    TOGGLE,
    DETECTED,
    LOCK,
    ERROR
}

class HapticManager(context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    @Volatile
    var enabled: Boolean = true

    fun trigger(style: HapticStyle) {
        if (!enabled) return
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return

        runCatching {
            val effect = when (style) {
                HapticStyle.TAP -> VibrationEffect.createOneShot(18L, 85)
                HapticStyle.TOGGLE -> VibrationEffect.createOneShot(24L, 120)
                HapticStyle.DETECTED -> VibrationEffect.createWaveform(
                    longArrayOf(0L, 35L, 45L, 55L),
                    intArrayOf(0, 140, 0, 220),
                    -1
                )
                HapticStyle.LOCK -> VibrationEffect.createOneShot(65L, 230)
                HapticStyle.ERROR -> VibrationEffect.createWaveform(
                    longArrayOf(0L, 40L, 40L, 40L),
                    intArrayOf(0, 180, 0, 180),
                    -1
                )
            }
            vib.vibrate(effect)
        }
    }
}
