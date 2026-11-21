package com.example.ui.components

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.ui.theme.LocalReduceEffects

data class TiltOffset(
    val pitchDeg: Float = 0f,
    val rollDeg: Float = 0f
)

/**
 * Low-pass filtered accelerometer parallax helper for 2.5D orb and card tilt.
 * Automatically unregisters and returns zero tilt when "Reduce effects" is enabled.
 */
@Composable
fun rememberParallaxTilt(maxDegrees: Float = 6.5f): State<TiltOffset> {
    val reduceEffects = LocalReduceEffects.current
    val context = LocalContext.current
    val state = remember { mutableStateOf(TiltOffset()) }

    DisposableEffect(reduceEffects) {
        if (reduceEffects) {
            state.value = TiltOffset(0f, 0f)
            return@DisposableEffect onDispose { }
        }

        val sm = context.getSystemService(SensorManager::class.java)
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sm == null || sensor == null) {
            return@DisposableEffect onDispose { }
        }

        var smoothX = 0f
        var smoothY = 0f
        val alpha = 0.10f

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val v = event?.values ?: return
                // Tilt relative to natural portrait hold
                val rawRoll = (-v[0] / SensorManager.GRAVITY_EARTH).coerceIn(-1f, 1f)
                val rawPitch = ((v[1] - 4.5f) / SensorManager.GRAVITY_EARTH).coerceIn(-1f, 1f)
                smoothX += alpha * (rawRoll * maxDegrees - smoothX)
                smoothY += alpha * (rawPitch * maxDegrees - smoothY)
                state.value = TiltOffset(
                    pitchDeg = smoothY.coerceIn(-maxDegrees, maxDegrees),
                    rollDeg = smoothX.coerceIn(-maxDegrees, maxDegrees)
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose {
            sm.unregisterListener(listener)
        }
    }

    return state
}
