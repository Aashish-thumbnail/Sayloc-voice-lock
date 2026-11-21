package com.example.data

enum class ActionType(val key: String) {
    LOCK("LOCK"),
    FLASHLIGHT_TOGGLE("FLASHLIGHT_TOGGLE"),
    FLASHLIGHT_ON("FLASHLIGHT_ON"),
    FLASHLIGHT_OFF("FLASHLIGHT_OFF"),
    OPEN_APP("OPEN_APP"),
    FIND_MY_PHONE("FIND_MY_PHONE"),
    ROUTINE("ROUTINE"),
    EXPERIMENTAL_UNLOCK("EXPERIMENTAL_UNLOCK");

    companion object {
        fun fromKey(key: String): ActionType =
            entries.firstOrNull { it.key == key } ?: LOCK
    }
}

enum class RoutineStep(val key: String) {
    ENABLE_DND("ENABLE_DND"),
    DISABLE_DND("DISABLE_DND"),
    FLASHLIGHT_ON("FLASHLIGHT_ON"),
    FLASHLIGHT_OFF("FLASHLIGHT_OFF"),
    LOCK_DEVICE("LOCK_DEVICE"),
    FIND_MY_PHONE("FIND_MY_PHONE");

    companion object {
        fun fromKey(key: String): RoutineStep? =
            entries.firstOrNull { it.key == key }
    }
}

enum class VoiceLanguage(val key: String, val localeTag: String) {
    HINGLISH("HINGLISH", "en-IN"),
    HINDI("HINDI", "hi-IN"),
    ENGLISH("ENGLISH", "en-US");

    companion object {
        fun fromKey(key: String): VoiceLanguage =
            entries.firstOrNull { it.key == key } ?: HINGLISH
    }
}

enum class UiLanguage(val key: String) {
    EN("EN"),
    HI("HI");

    companion object {
        fun fromKey(key: String): UiLanguage =
            entries.firstOrNull { it.key == key } ?: EN
    }
}

enum class BatterySaverMode(val key: String) {
    ALWAYS("ALWAYS"),
    CHARGING_ONLY("CHARGING_ONLY"),
    TIME_WINDOW("TIME_WINDOW");

    companion object {
        fun fromKey(key: String): BatterySaverMode =
            entries.firstOrNull { it.key == key } ?: ALWAYS
    }
}

enum class AccentTheme(val key: String) {
    ELECTRIC_VIOLET("ELECTRIC_VIOLET"),
    CYAN_PULSE("CYAN_PULSE"),
    NEON_MAGENTA("NEON_MAGENTA"),
    AURORA_INDIGO("AURORA_INDIGO");

    companion object {
        fun fromKey(key: String): AccentTheme =
            entries.firstOrNull { it.key == key } ?: ELECTRIC_VIOLET
    }
}

enum class ThemeMode(val key: String) {
    DARK("DARK"),
    LIGHT("LIGHT"),
    AUTO("AUTO");

    companion object {
        fun fromKey(key: String): ThemeMode =
            entries.firstOrNull { it.key == key } ?: DARK
    }
}

enum class LogSource(val key: String) {
    VOICE("VOICE"),
    TEST("TEST"),
    SHAKE("SHAKE"),
    QUICK_TILE("QUICK_TILE"),
    AUTO_LOCK("AUTO_LOCK"),
    MANUAL("MANUAL");

    companion object {
        fun fromKey(key: String): LogSource =
            entries.firstOrNull { it.key == key } ?: VOICE
    }
}

enum class ListeningState {
    IDLE,
    LISTENING,
    DETECTED,
    PAUSED_BATTERY,
    ERROR
}

data class VoiceCommand(
    val id: Long = 0L,
    val name: String,
    val aliases: List<String> = emptyList(),
    val actionType: ActionType = ActionType.LOCK,
    val targetPackage: String? = null,
    val targetAppLabel: String? = null,
    val routineSteps: List<RoutineStep> = emptyList(),
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

data class CommandLog(
    val id: Long = 0L,
    val commandName: String,
    val heardText: String,
    val actionSummary: String,
    val source: LogSource,
    val succeeded: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class AppSettings(
    val onboardingCompleted: Boolean = false,
    val uiLanguage: UiLanguage = UiLanguage.EN,
    val voiceLanguage: VoiceLanguage = VoiceLanguage.HINGLISH,
    val sensitivity: Float = 0.65f,
    val batterySaverMode: BatterySaverMode = BatterySaverMode.ALWAYS,
    val windowStartMinutes: Int = 7 * 60, // 07:00
    val windowEndMinutes: Int = 23 * 60, // 23:00
    val autoLockMinutes: Int = 0, // 0 = disabled
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val accentTheme: AccentTheme = AccentTheme.ELECTRIC_VIOLET,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val reduceEffects: Boolean = false,
    val startOnBoot: Boolean = false,
    val shakeToLockEnabled: Boolean = false,
    val shakeSensitivity: Float = 0.6f,
    val experimentalUnlockEnabled: Boolean = false
)

data class InstalledAppInfo(
    val packageName: String,
    val label: String
)

data class PermissionHealthStatus(
    val hasMicrophone: Boolean = false,
    val hasNotifications: Boolean = true,
    val isDeviceAdminActive: Boolean = false,
    val isBatteryOptimizationIgnored: Boolean = false,
    val hasDndPolicyAccess: Boolean = false,
    val isKeyguardSecure: Boolean = true,
    val isSpeechRecognizerAvailable: Boolean = true
) {
    val allCoreGranted: Boolean
        get() = hasMicrophone && hasNotifications && isDeviceAdminActive && isBatteryOptimizationIgnored

    val grantedCount: Int
        get() = listOf(
            hasMicrophone,
            hasNotifications,
            isDeviceAdminActive,
            isBatteryOptimizationIgnored
        ).count { it }
}
