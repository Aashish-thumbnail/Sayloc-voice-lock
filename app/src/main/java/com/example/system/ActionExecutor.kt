package com.example.system

import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.PowerManager
import com.example.MainActivity
import com.example.audio.HapticManager
import com.example.audio.HapticStyle
import com.example.audio.SoundEffect
import com.example.audio.SoundSynthesizer
import com.example.data.ActionType
import com.example.data.LogSource
import com.example.data.RoutineStep
import com.example.data.SaylocRepository
import com.example.data.SettingsDataStore
import com.example.data.VoiceCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ExecutionOutcome(
    val succeeded: Boolean,
    val message: String,
    val needsDeviceAdmin: Boolean = false,
    val needsDndAccess: Boolean = false
)

class ActionExecutor(
    private val context: Context,
    private val repository: SaylocRepository,
    private val settingsDataStore: SettingsDataStore,
    private val soundSynthesizer: SoundSynthesizer,
    private val hapticManager: HapticManager,
    val findMyPhoneController: FindMyPhoneController
) {
    private val devicePolicyManager = context.getSystemService(DevicePolicyManager::class.java)
    private val cameraManager = context.getSystemService(CameraManager::class.java)
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    suspend fun executeCommand(
        command: VoiceCommand,
        heardText: String,
        source: LogSource
    ): ExecutionOutcome {
        ServiceStateBus.reportCommandDetected(command.name, heardText)
        soundSynthesizer.play(SoundEffect.COMMAND_DETECTED)
        hapticManager.trigger(HapticStyle.DETECTED)

        val outcome = when (command.actionType) {
            ActionType.LOCK -> performLockInternal()
            ActionType.FLASHLIGHT_TOGGLE -> toggleFlashlight(null)
            ActionType.FLASHLIGHT_ON -> toggleFlashlight(true)
            ActionType.FLASHLIGHT_OFF -> toggleFlashlight(false)
            ActionType.OPEN_APP -> openApp(command.targetPackage, command.targetAppLabel)
            ActionType.FIND_MY_PHONE -> {
                findMyPhoneController.start()
                ExecutionOutcome(true, "Find My Phone alert started")
            }
            ActionType.ROUTINE -> executeRoutine(command.routineSteps)
            ActionType.EXPERIMENTAL_UNLOCK -> executeExperimentalUnlock()
        }

        if (!outcome.succeeded) {
            soundSynthesizer.play(SoundEffect.ERROR)
            hapticManager.trigger(HapticStyle.ERROR)
        }

        repository.logExecution(
            commandName = command.name,
            heardText = heardText,
            actionSummary = outcome.message,
            source = source,
            succeeded = outcome.succeeded
        )

        return outcome
    }

    suspend fun lockNow(source: LogSource): ExecutionOutcome {
        val outcome = performLockInternal()
        repository.logExecution(
            commandName = "Lock Now",
            heardText = source.key,
            actionSummary = outcome.message,
            source = source,
            succeeded = outcome.succeeded
        )
        return outcome
    }

    private fun performLockInternal(): ExecutionOutcome {
        if (!PermissionHelper.isDeviceAdminActive(context)) {
            soundSynthesizer.play(SoundEffect.ERROR)
            hapticManager.trigger(HapticStyle.ERROR)
            return ExecutionOutcome(
                succeeded = false,
                message = "Device Admin is not enabled yet",
                needsDeviceAdmin = true
            )
        }

        return runCatching {
            soundSynthesizer.play(SoundEffect.LOCK)
            hapticManager.trigger(HapticStyle.LOCK)
            devicePolicyManager?.lockNow()
            ExecutionOutcome(true, "Phone locked")
        }.getOrElse { err ->
            ExecutionOutcome(false, err.message ?: "Lock failed")
        }
    }

    fun toggleFlashlight(explicitState: Boolean?): ExecutionOutcome {
        val cm = cameraManager ?: return ExecutionOutcome(false, "Camera service unavailable")
        val cameraId = runCatching {
            cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull() ?: return ExecutionOutcome(false, "No flashlight hardware found")

        val desired = explicitState ?: !ServiceStateBus.state.value.isFlashlightOn
        return runCatching {
            cm.setTorchMode(cameraId, desired)
            ServiceStateBus.setFlashlightOn(desired)
            ExecutionOutcome(true, if (desired) "Flashlight turned ON" else "Flashlight turned OFF")
        }.getOrElse { err ->
            ExecutionOutcome(false, err.message ?: "Could not switch flashlight")
        }
    }

    private fun openApp(packageName: String?, label: String?): ExecutionOutcome {
        if (packageName.isNullOrBlank()) {
            return ExecutionOutcome(false, "No target app chosen")
        }
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: return ExecutionOutcome(false, "App not installed: ${label ?: packageName}")

        return runCatching {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            ExecutionOutcome(true, "Opened ${label ?: packageName}")
        }.getOrElse { err ->
            ExecutionOutcome(false, err.message ?: "Could not open app")
        }
    }

    private suspend fun executeRoutine(steps: List<RoutineStep>): ExecutionOutcome {
        if (steps.isEmpty()) {
            return ExecutionOutcome(false, "Routine has no actions")
        }
        val notes = mutableListOf<String>()
        var allOk = true
        var needsAdmin = false
        var needsDnd = false

        // Execute non-lock steps before locking so they all complete cleanly
        val ordered = steps.sortedBy { if (it == RoutineStep.LOCK_DEVICE) 1 else 0 }

        for (step in ordered) {
            val res = when (step) {
                RoutineStep.ENABLE_DND -> setDnd(true)
                RoutineStep.DISABLE_DND -> setDnd(false)
                RoutineStep.FLASHLIGHT_ON -> toggleFlashlight(true)
                RoutineStep.FLASHLIGHT_OFF -> toggleFlashlight(false)
                RoutineStep.FIND_MY_PHONE -> {
                    findMyPhoneController.start()
                    ExecutionOutcome(true, "Find My Phone ON")
                }
                RoutineStep.LOCK_DEVICE -> performLockInternal()
            }
            notes.add(res.message)
            if (!res.succeeded) allOk = false
            if (res.needsDeviceAdmin) needsAdmin = true
            if (res.needsDndAccess) needsDnd = true
        }
        return ExecutionOutcome(
            succeeded = allOk,
            message = notes.joinToString(" • "),
            needsDeviceAdmin = needsAdmin,
            needsDndAccess = needsDnd
        )
    }

    private fun setDnd(enable: Boolean): ExecutionOutcome {
        val nm = notificationManager ?: return ExecutionOutcome(false, "Notification service unavailable")
        if (!nm.isNotificationPolicyAccessGranted) {
            return ExecutionOutcome(
                succeeded = false,
                message = "Do Not Disturb access not granted",
                needsDndAccess = true
            )
        }
        return runCatching {
            nm.setInterruptionFilter(
                if (enable) NotificationManager.INTERRUPTION_FILTER_PRIORITY
                else NotificationManager.INTERRUPTION_FILTER_ALL
            )
            ExecutionOutcome(true, if (enable) "Do Not Disturb ON" else "Do Not Disturb OFF")
        }.getOrElse { err ->
            ExecutionOutcome(false, err.message ?: "DND change failed")
        }
    }

    /**
     * Experimental Voice Unlock:
     * Honestly respects Android OS security — CANNOT bypass PIN, pattern, password, or fingerprint.
     * Only wakes the screen and requests dismissing a non-secure swipe lock screen.
     */
    private suspend fun executeExperimentalUnlock(): ExecutionOutcome = withContext(Dispatchers.Main) {
        val settings = settingsDataStore.getSnapshot()
        if (!settings.experimentalUnlockEnabled) {
            return@withContext ExecutionOutcome(
                false,
                "Enable Experimental Voice Unlock in Settings first"
            )
        }
        if (PermissionHelper.isKeyguardSecure(context)) {
            return@withContext ExecutionOutcome(
                false,
                "Phone has PIN/Pattern/Biometric lock. Android blocks third-party unlock."
            )
        }

        runCatching {
            val pm = context.getSystemService(PowerManager::class.java)
            @Suppress("DEPRECATION")
            val wakeLock = pm?.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "Sayloc:ExperimentalWake"
            )
            wakeLock?.acquire(4_000L)

            val unlockIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(MainActivity.EXTRA_DISMISS_NON_SECURE_KEYGUARD, true)
            }
            context.startActivity(unlockIntent)
            ExecutionOutcome(true, "Screen woken (non-secure swipe lock only)")
        }.getOrElse { err ->
            ExecutionOutcome(false, err.message ?: "Could not wake screen")
        }
    }
}
