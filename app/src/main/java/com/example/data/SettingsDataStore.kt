package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.saylocDataStore: DataStore<Preferences> by preferencesDataStore(name = "sayloc_settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val UI_LANGUAGE = stringPreferencesKey("ui_language")
        val VOICE_LANGUAGE = stringPreferencesKey("voice_language")
        val SENSITIVITY = floatPreferencesKey("sensitivity")
        val BATTERY_SAVER_MODE = stringPreferencesKey("battery_saver_mode")
        val WINDOW_START_MINUTES = intPreferencesKey("window_start_minutes")
        val WINDOW_END_MINUTES = intPreferencesKey("window_end_minutes")
        val AUTO_LOCK_MINUTES = intPreferencesKey("auto_lock_minutes")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val ACCENT_THEME = stringPreferencesKey("accent_theme")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val REDUCE_EFFECTS = booleanPreferencesKey("reduce_effects")
        val START_ON_BOOT = booleanPreferencesKey("start_on_boot")
        val SHAKE_TO_LOCK = booleanPreferencesKey("shake_to_lock")
        val SHAKE_SENSITIVITY = floatPreferencesKey("shake_sensitivity")
        val EXPERIMENTAL_UNLOCK = booleanPreferencesKey("experimental_unlock")
    }

    val settingsFlow: Flow<AppSettings> = context.saylocDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            AppSettings(
                onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
                uiLanguage = UiLanguage.fromKey(prefs[Keys.UI_LANGUAGE] ?: UiLanguage.EN.key),
                voiceLanguage = VoiceLanguage.fromKey(prefs[Keys.VOICE_LANGUAGE] ?: VoiceLanguage.HINGLISH.key),
                sensitivity = (prefs[Keys.SENSITIVITY] ?: 0.65f).coerceIn(0.2f, 0.95f),
                batterySaverMode = BatterySaverMode.fromKey(prefs[Keys.BATTERY_SAVER_MODE] ?: BatterySaverMode.ALWAYS.key),
                windowStartMinutes = prefs[Keys.WINDOW_START_MINUTES] ?: (7 * 60),
                windowEndMinutes = prefs[Keys.WINDOW_END_MINUTES] ?: (23 * 60),
                autoLockMinutes = prefs[Keys.AUTO_LOCK_MINUTES] ?: 0,
                soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
                hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
                accentTheme = AccentTheme.fromKey(prefs[Keys.ACCENT_THEME] ?: AccentTheme.ELECTRIC_VIOLET.key),
                themeMode = ThemeMode.fromKey(prefs[Keys.THEME_MODE] ?: ThemeMode.DARK.key),
                reduceEffects = prefs[Keys.REDUCE_EFFECTS] ?: false,
                startOnBoot = prefs[Keys.START_ON_BOOT] ?: false,
                shakeToLockEnabled = prefs[Keys.SHAKE_TO_LOCK] ?: false,
                shakeSensitivity = (prefs[Keys.SHAKE_SENSITIVITY] ?: 0.6f).coerceIn(0.2f, 1.0f),
                experimentalUnlockEnabled = prefs[Keys.EXPERIMENTAL_UNLOCK] ?: false
            )
        }

    suspend fun getSnapshot(): AppSettings = settingsFlow.first()

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.saylocDataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setUiLanguage(language: UiLanguage) {
        context.saylocDataStore.edit { it[Keys.UI_LANGUAGE] = language.key }
    }

    suspend fun setVoiceLanguage(language: VoiceLanguage) {
        context.saylocDataStore.edit { it[Keys.VOICE_LANGUAGE] = language.key }
    }

    suspend fun setSensitivity(value: Float) {
        context.saylocDataStore.edit { it[Keys.SENSITIVITY] = value.coerceIn(0.2f, 0.95f) }
    }

    suspend fun setBatterySaverMode(mode: BatterySaverMode) {
        context.saylocDataStore.edit { it[Keys.BATTERY_SAVER_MODE] = mode.key }
    }

    suspend fun setTimeWindow(startMinutes: Int, endMinutes: Int) {
        context.saylocDataStore.edit {
            it[Keys.WINDOW_START_MINUTES] = startMinutes.coerceIn(0, 1439)
            it[Keys.WINDOW_END_MINUTES] = endMinutes.coerceIn(0, 1439)
        }
    }

    suspend fun setAutoLockMinutes(minutes: Int) {
        context.saylocDataStore.edit { it[Keys.AUTO_LOCK_MINUTES] = minutes.coerceAtLeast(0) }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.saylocDataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.saylocDataStore.edit { it[Keys.HAPTICS_ENABLED] = enabled }
    }

    suspend fun setAccentTheme(accent: AccentTheme) {
        context.saylocDataStore.edit { it[Keys.ACCENT_THEME] = accent.key }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.saylocDataStore.edit { it[Keys.THEME_MODE] = mode.key }
    }

    suspend fun setReduceEffects(reduce: Boolean) {
        context.saylocDataStore.edit { it[Keys.REDUCE_EFFECTS] = reduce }
    }

    suspend fun setStartOnBoot(enabled: Boolean) {
        context.saylocDataStore.edit { it[Keys.START_ON_BOOT] = enabled }
    }

    suspend fun setShakeToLockEnabled(enabled: Boolean) {
        context.saylocDataStore.edit { it[Keys.SHAKE_TO_LOCK] = enabled }
    }

    suspend fun setShakeSensitivity(value: Float) {
        context.saylocDataStore.edit { it[Keys.SHAKE_SENSITIVITY] = value.coerceIn(0.2f, 1.0f) }
    }

    suspend fun setExperimentalUnlockEnabled(enabled: Boolean) {
        context.saylocDataStore.edit { it[Keys.EXPERIMENTAL_UNLOCK] = enabled }
    }
}
