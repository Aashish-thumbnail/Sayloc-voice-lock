package com.example.system

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.SaylocApplication
import com.example.service.VoiceListeningService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles BOOT_COMPLETED and MY_PACKAGE_REPLACED when "Start on Boot" is enabled.
 * On Android 14+ (API 34+), starting a microphone FGS directly from BOOT_COMPLETED is
 * restricted by OS privacy rules, so we post a one-tap resume notification instead of crashing.
 */
class SaylocBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as? SaylocApplication ?: return@launch
                val settings = app.settingsDataStore.getSnapshot()
                if (!settings.startOnBoot) return@launch
                if (!PermissionHelper.hasMicrophonePermission(context)) return@launch

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    postBootResumeNotification(context)
                } else {
                    runCatching {
                        VoiceListeningService.start(context)
                    }.onFailure {
                        postBootResumeNotification(context)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun postBootResumeNotification(context: Context) {
        if (!PermissionHelper.hasNotificationPermission(context)) return
        VoiceListeningService.ensureNotificationChannels(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_AUTO_START_LISTENING, true)
        }
        val pi = PendingIntent.getActivity(
            context,
            401,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, VoiceListeningService.CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_tile_lock)
            .setContentTitle("Sayloc is ready after restart")
            .setContentText("Tap to resume voice listening (Android 14+ requires one tap after boot).")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        val nm = context.getSystemService(NotificationManager::class.java)
        nm?.notify(401, notification)
    }
}
