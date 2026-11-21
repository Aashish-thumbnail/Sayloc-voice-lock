package com.example.system

import android.Manifest
import android.app.KeyguardManager
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import com.example.data.PermissionHealthStatus

object PermissionHelper {

    fun deviceAdminComponent(context: Context): ComponentName =
        ComponentName(context, SaylocDeviceAdminReceiver::class.java)

    fun hasMicrophonePermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun isDeviceAdminActive(context: Context): Boolean {
        val dpm = context.getSystemService(DevicePolicyManager::class.java) ?: return false
        return dpm.isAdminActive(deviceAdminComponent(context))
    }

    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        val pm = context.getSystemService(PowerManager::class.java) ?: return false
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun hasDndPolicyAccess(context: Context): Boolean {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return false
        return nm.isNotificationPolicyAccessGranted
    }

    fun isKeyguardSecure(context: Context): Boolean {
        val km = context.getSystemService(KeyguardManager::class.java) ?: return true
        return km.isKeyguardSecure
    }

    fun checkHealth(context: Context): PermissionHealthStatus {
        return PermissionHealthStatus(
            hasMicrophone = hasMicrophonePermission(context),
            hasNotifications = hasNotificationPermission(context),
            isDeviceAdminActive = isDeviceAdminActive(context),
            isBatteryOptimizationIgnored = isBatteryOptimizationIgnored(context),
            hasDndPolicyAccess = hasDndPolicyAccess(context),
            isKeyguardSecure = isKeyguardSecure(context),
            isSpeechRecognizerAvailable = SpeechRecognizer.isRecognitionAvailable(context)
        )
    }

    fun createDeviceAdminIntent(context: Context): Intent {
        return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdminComponent(context))
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Sayloc uses force-lock permission only so you can lock your phone by voice, shake, or one tap."
            )
        }
    }

    fun createBatteryOptimizationIntent(context: Context): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }

    fun createAppDetailsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }

    fun createDndSettingsIntent(): Intent {
        return Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
    }
}
