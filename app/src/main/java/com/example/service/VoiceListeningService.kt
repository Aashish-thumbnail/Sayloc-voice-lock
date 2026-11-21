package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.SaylocApplication
import com.example.audio.HapticStyle
import com.example.audio.SoundEffect
import com.example.data.AppSettings
import com.example.data.BatterySaverMode
import com.example.data.ListeningState
import com.example.data.LogSource
import com.example.data.VoiceCommand
import com.example.system.PermissionHelper
import com.example.system.ServiceStateBus
import com.example.system.ShakeToLockDetector
import com.example.voice.AndroidSpeechVoiceEngine
import com.example.voice.FuzzyCommandMatcher
import com.example.voice.VoiceEngine
import com.example.voice.VoiceEngineCallback
import com.example.voice.VoiceEngineConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Foreground microphone service keeping on-device voice recognition, battery schedule checks,
 * Shake-to-Lock, and Auto-Lock inactivity timer running reliably in the background.
 */
class VoiceListeningService : Service() {

    companion object {
        const val CHANNEL_LISTENING = "sayloc_listening"
        const val CHANNEL_ALERTS = "sayloc_alerts"
        const val FOREGROUND_NOTIFICATION_ID = 101

        const val ACTION_START = "com.example.sayloc.ACTION_START"
        const val ACTION_STOP = "com.example.sayloc.ACTION_STOP"
        const val ACTION_LOCK_NOW = "com.example.sayloc.ACTION_LOCK_NOW"
        const val ACTION_STOP_FIND_MY_PHONE = "com.example.sayloc.ACTION_STOP_FIND_MY_PHONE"

        fun start(context: Context) {
            val intent = Intent(context, VoiceListeningService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, VoiceListeningService::class.java).apply {
                action = ACTION_STOP
            }
            runCatching { context.startService(intent) }
        }

        fun ensureNotificationChannels(context: Context) {
            val nm = context.getSystemService(NotificationManager::class.java) ?: return
            val listeningChannel = NotificationChannel(
                CHANNEL_LISTENING,
                context.getString(R.string.notif_channel_listening),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notif_channel_listening_desc)
                setShowBadge(false)
            }
            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS,
                context.getString(R.string.notif_channel_alerts),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_channel_alerts_desc)
            }
            nm.createNotificationChannel(listeningChannel)
            nm.createNotificationChannel(alertsChannel)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var app: SaylocApplication
    private lateinit var voiceEngine: VoiceEngine
    private lateinit var shakeDetector: ShakeToLockDetector

    private var currentSettings: AppSettings = AppSettings()
    private var cachedCommands: List<VoiceCommand> = emptyList()
    private var engineActive: Boolean = false
    private var lastTriggerTimeMs: Long = 0L
    private var lastActivityMs: Long = System.currentTimeMillis()
    private var schedulerJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        app = applicationContext as SaylocApplication
        ensureNotificationChannels(this)
        voiceEngine = AndroidSpeechVoiceEngine(this)
        shakeDetector = ShakeToLockDetector(this) {
            serviceScope.launch {
                app.actionExecutor.lockNow(LogSource.SHAKE)
            }
        }

        // Observe commands & settings live so edits take effect without restarting the service
        serviceScope.launch {
            app.repository.commands.collectLatest { list ->
                cachedCommands = list
            }
        }
        serviceScope.launch {
            app.settingsDataStore.settingsFlow.collectLatest { settings ->
                val languageChanged = settings.voiceLanguage != currentSettings.voiceLanguage
                currentSettings = settings
                app.soundSynthesizer.enabled = settings.soundEnabled
                app.hapticManager.enabled = settings.hapticsEnabled

                shakeDetector.sensitivity = settings.shakeSensitivity
                if (settings.shakeToLockEnabled) shakeDetector.start() else shakeDetector.stop()

                if (ServiceStateBus.state.value.isServiceRunning) {
                    evaluateScheduleAndEngine(forceRestart = languageChanged)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopListeningAndShutdown()
                return START_NOT_STICKY
            }
            ACTION_LOCK_NOW -> {
                serviceScope.launch { app.actionExecutor.lockNow(LogSource.MANUAL) }
                return START_STICKY
            }
            ACTION_STOP_FIND_MY_PHONE -> {
                app.findMyPhoneController.stop()
                if (!ServiceStateBus.state.value.isServiceRunning) {
                    stopSelf()
                }
                return START_NOT_STICKY
            }
            else -> {
                if (!PermissionHelper.hasMicrophonePermission(this)) {
                    ServiceStateBus.setError("Microphone permission is required to listen.")
                    stopSelf()
                    return START_NOT_STICKY
                }
                startAsMicrophoneForeground("Listening for voice commands…")
                lastActivityMs = System.currentTimeMillis()
                ServiceStateBus.setServiceRunning(true, ListeningState.LISTENING)
                app.soundSynthesizer.play(SoundEffect.LISTENING_START)
                app.hapticManager.trigger(HapticStyle.TOGGLE)
                startSchedulerLoop()
                evaluateScheduleAndEngine(forceRestart = true)
                return START_STICKY
            }
        }
    }

    private fun startAsMicrophoneForeground(statusText: String) {
        val notification = buildForegroundNotification(statusText)
        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else {
            0
        }
        ServiceCompat.startForeground(this, FOREGROUND_NOTIFICATION_ID, notification, serviceType)
    }

    private fun updateNotification(statusText: String) {
        if (!PermissionHelper.hasNotificationPermission(this)) return
        val nm = getSystemService(NotificationManager::class.java)
        nm?.notify(FOREGROUND_NOTIFICATION_ID, buildForegroundNotification(statusText))
    }

    private fun buildForegroundNotification(statusText: String): Notification {
        val openPi = PendingIntent.getActivity(
            this,
            101,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val lockPi = PendingIntent.getService(
            this,
            102,
            Intent(this, VoiceListeningService::class.java).apply { action = ACTION_LOCK_NOW },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopPi = PendingIntent.getService(
            this,
            103,
            Intent(this, VoiceListeningService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_LISTENING)
            .setSmallIcon(R.drawable.ic_tile_lock)
            .setContentTitle("Sayloc • Say it. Lock it.")
            .setContentText(statusText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(openPi)
            .addAction(R.drawable.ic_tile_lock, "Lock Now", lockPi)
            .addAction(R.drawable.ic_tile_lock, "Stop", stopPi)
            .build()
    }

    private fun startSchedulerLoop() {
        schedulerJob?.cancel()
        schedulerJob = serviceScope.launch {
            while (isActive) {
                delay(20_000L)
                evaluateScheduleAndEngine(forceRestart = false)

                // Check Auto-Lock inactivity timer
                val autoMinutes = currentSettings.autoLockMinutes
                if (autoMinutes > 0) {
                    val elapsedMs = System.currentTimeMillis() - lastActivityMs
                    if (elapsedMs >= autoMinutes * 60_000L) {
                        lastActivityMs = System.currentTimeMillis()
                        app.actionExecutor.lockNow(LogSource.AUTO_LOCK)
                    }
                }
            }
        }
    }

    private fun evaluateScheduleAndEngine(forceRestart: Boolean) {
        val allowed = isAllowedByBatterySaver(currentSettings)
        if (!allowed) {
            if (engineActive) {
                voiceEngine.stop()
                engineActive = false
            }
            val reason = when (currentSettings.batterySaverMode) {
                BatterySaverMode.CHARGING_ONLY -> "Paused (Battery Saver: charging only)"
                BatterySaverMode.TIME_WINDOW -> "Paused (Outside active time window)"
                BatterySaverMode.ALWAYS -> "Paused"
            }
            ServiceStateBus.setServiceRunning(true, ListeningState.PAUSED_BATTERY)
            ServiceStateBus.updateStatusDetail(reason)
            updateNotification(reason)
            return
        }

        if (!engineActive || forceRestart) {
            if (engineActive) voiceEngine.stop()
            engineActive = true
            ServiceStateBus.setServiceRunning(true, ListeningState.LISTENING)
            ServiceStateBus.updateStatusDetail("On-device • ${currentSettings.voiceLanguage.name}")
            updateNotification("Listening (${currentSettings.voiceLanguage.name}) — All audio stays on phone")
            voiceEngine.start(
                config = VoiceEngineConfig(
                    language = currentSettings.voiceLanguage,
                    preferOffline = true
                ),
                callback = engineCallback
            )
        }
    }

    private fun isAllowedByBatterySaver(settings: AppSettings): Boolean {
        return when (settings.batterySaverMode) {
            BatterySaverMode.ALWAYS -> true
            BatterySaverMode.CHARGING_ONLY -> isDeviceCharging()
            BatterySaverMode.TIME_WINDOW -> isWithinTimeWindow(
                settings.windowStartMinutes,
                settings.windowEndMinutes
            )
        }
    }

    private fun isDeviceCharging(): Boolean {
        val batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun isWithinTimeWindow(startMin: Int, endMin: Int): Boolean {
        val cal = Calendar.getInstance()
        val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        return if (startMin <= endMin) {
            nowMin in startMin..endMin
        } else {
            // Overnight window (e.g. 22:00 -> 07:00)
            nowMin >= startMin || nowMin <= endMin
        }
    }

    private val engineCallback = object : VoiceEngineCallback {
        override fun onReadyForSpeech() {
            if (ServiceStateBus.state.value.listeningState != ListeningState.DETECTED) {
                ServiceStateBus.setServiceRunning(true, ListeningState.LISTENING)
            }
        }

        override fun onRmsChanged(normalizedLevel: Float) {
            ServiceStateBus.updateRms(normalizedLevel)
        }

        override fun onPartialTranscript(candidates: List<String>) {
            candidates.firstOrNull()?.let { ServiceStateBus.updateHeardText(it) }
            checkAndTriggerCommand(candidates)
        }

        override fun onFinalTranscript(candidates: List<String>) {
            candidates.firstOrNull()?.let { ServiceStateBus.updateHeardText(it) }
            checkAndTriggerCommand(candidates)
        }

        override fun onEngineError(errorCode: Int, message: String, recoverable: Boolean) {
            ServiceStateBus.updateStatusDetail(message)
            if (!recoverable) {
                ServiceStateBus.setError(message)
                stopListeningAndShutdown()
            }
        }

        override fun onEngineStopped() {
            ServiceStateBus.updateRms(0f)
        }
    }

    private fun checkAndTriggerCommand(candidates: List<String>) {
        val now = System.currentTimeMillis()
        if (now - lastTriggerTimeMs < 2200L) return

        val match = FuzzyCommandMatcher.findBestMatch(
            candidates = candidates,
            commands = cachedCommands,
            sensitivity = currentSettings.sensitivity
        ) ?: return

        lastTriggerTimeMs = now
        lastActivityMs = now

        serviceScope.launch {
            val outcome = app.actionExecutor.executeCommand(
                command = match.command,
                heardText = match.matchedCandidate,
                source = LogSource.VOICE
            )
            updateNotification("Matched \"${match.command.name}\" • ${outcome.message}")
            delay(1800L)
            if (ServiceStateBus.state.value.isServiceRunning && engineActive) {
                ServiceStateBus.setServiceRunning(true, ListeningState.LISTENING)
            }
        }
    }

    private fun stopListeningAndShutdown() {
        schedulerJob?.cancel()
        engineActive = false
        voiceEngine.stop()
        shakeDetector.stop()
        ServiceStateBus.setServiceRunning(false, ListeningState.IDLE)
        ServiceStateBus.updateStatusDetail("")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        schedulerJob?.cancel()
        shakeDetector.stop()
        voiceEngine.destroy()
        ServiceStateBus.setServiceRunning(false, ListeningState.IDLE)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
