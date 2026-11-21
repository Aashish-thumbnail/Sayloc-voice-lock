package com.example.system

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.audio.SoundSynthesizer
import com.example.service.VoiceListeningService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Runs Find My Phone alert: boosts alarm volume to max, plays continuous siren pulse,
 * blinks camera flashlight, and shows a high-priority dismiss notification until stopped.
 */
class FindMyPhoneController(
    private val context: Context,
    private val soundSynthesizer: SoundSynthesizer
) {
    companion object {
        const val NOTIFICATION_ID = 309
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val cameraManager = context.getSystemService(CameraManager::class.java)

    private var alertJob: Job? = null
    private var previousAlarmVolume: Int? = null
    private var torchCameraId: String? = null

    fun start() {
        if (alertJob?.isActive == true) return
        ServiceStateBus.setFindMyPhoneActive(true)
        torchCameraId = findTorchCameraId()

        // Boost alarm volume to maximum while remembering user's original level
        audioManager?.let { am ->
            runCatching {
                previousAlarmVolume = am.getStreamVolume(AudioManager.STREAM_ALARM)
                val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                am.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, 0)
            }
        }

        showAlertNotification()

        alertJob = scope.launch {
            var torchOn = false
            val startedAt = System.currentTimeMillis()
            // Auto-stop after 2 minutes as a safety guard if unattended
            while (isActive && System.currentTimeMillis() - startedAt < 120_000L) {
                torchOn = !torchOn
                setTorchSafe(torchOn)
                soundSynthesizer.playAlarmPulse()
                delay(180L)
            }
            stop()
        }
    }

    fun stop() {
        alertJob?.cancel()
        alertJob = null
        setTorchSafe(false)
        ServiceStateBus.setFindMyPhoneActive(false)
        ServiceStateBus.setFlashlightOn(false)

        previousAlarmVolume?.let { vol ->
            runCatching {
                audioManager?.setStreamVolume(AudioManager.STREAM_ALARM, vol, 0)
            }
            previousAlarmVolume = null
        }

        val nm = context.getSystemService(NotificationManager::class.java)
        nm?.cancel(NOTIFICATION_ID)
    }

    private fun showAlertNotification() {
        if (!PermissionHelper.hasNotificationPermission(context)) return
        VoiceListeningService.ensureNotificationChannels(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPi = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, VoiceListeningService::class.java).apply {
            action = VoiceListeningService.ACTION_STOP_FIND_MY_PHONE
        }
        val stopPi = PendingIntent.getService(
            context,
            NOTIFICATION_ID + 1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, VoiceListeningService.CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_tile_lock)
            .setContentTitle("Find My Phone Active")
            .setContentText("Ringing at max volume & blinking flashlight. Tap Stop when found.")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setContentIntent(openPi)
            .addAction(R.drawable.ic_tile_lock, "I Found It — Stop", stopPi)
            .build()

        val nm = context.getSystemService(NotificationManager::class.java)
        nm?.notify(NOTIFICATION_ID, notification)
    }

    private fun findTorchCameraId(): String? = runCatching {
        val cm = cameraManager ?: return null
        cm.cameraIdList.firstOrNull { id ->
            val chars = cm.getCameraCharacteristics(id)
            chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    }.getOrNull()

    private fun setTorchSafe(enabled: Boolean) {
        val id = torchCameraId ?: return
        runCatching {
            cameraManager?.setTorchMode(id, enabled)
            ServiceStateBus.setFlashlightOn(enabled)
        }
    }
}
