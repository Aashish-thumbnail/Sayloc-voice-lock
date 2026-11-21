package com.example.system

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Low-overhead accelerometer shake detector with configurable sensitivity and debounce.
 */
class ShakeToLockDetector(
    context: Context,
    private val onShakeDetected: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val accelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    @Volatile
    var sensitivity: Float = 0.6f // 0.2 (hard shake) .. 1.0 (light shake)

    private var isListening = false
    private var lastTriggerMs = 0L
    private var shakeCount = 0
    private var lastPeakMs = 0L

    fun start() {
        if (isListening || accelerometer == null) return
        isListening = sensorManager?.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_UI
        ) == true
    }

    fun stop() {
        if (!isListening) return
        sensorManager?.unregisterListener(this)
        isListening = false
        shakeCount = 0
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val values = event?.values ?: return
        val gX = values[0] / SensorManager.GRAVITY_EARTH
        val gY = values[1] / SensorManager.GRAVITY_EARTH
        val gZ = values[2] / SensorManager.GRAVITY_EARTH
        val gForce = sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()

        // sensitivity 0.2 -> threshold 3.2G, sensitivity 1.0 -> threshold 1.85G
        val thresholdG = 3.55f - (sensitivity.coerceIn(0.2f, 1.0f) * 1.70f)
        val now = System.currentTimeMillis()

        if (gForce >= thresholdG) {
            if (now - lastPeakMs > 600L) {
                shakeCount = 0
            }
            lastPeakMs = now
            shakeCount++

            if (shakeCount >= 2 && (now - lastTriggerMs) > 2500L) {
                lastTriggerMs = now
                shakeCount = 0
                onShakeDetected()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
