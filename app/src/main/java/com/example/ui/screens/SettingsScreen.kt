package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.AccentTheme
import com.example.data.AppSettings
import com.example.data.BatterySaverMode
import com.example.data.ThemeMode
import com.example.data.UiLanguage
import com.example.data.VoiceLanguage
import com.example.ui.components.Glass3DButton
import com.example.ui.components.Glass3DCard
import com.example.ui.components.Glass3DChip
import com.example.ui.components.Glass3DSlider
import com.example.ui.components.Glass3DToggle
import com.example.ui.components.GlassButtonStyle
import com.example.ui.components.TiltOffset
import com.example.ui.i18n.LocalSaylocStrings
import com.example.ui.i18n.batteryModeLabel
import com.example.ui.theme.LocalSaylocPalette
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.paletteFor
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    tilt: TiltOffset,
    onUiLanguageChange: (UiLanguage) -> Unit,
    onVoiceLanguageChange: (VoiceLanguage) -> Unit,
    onSensitivityChange: (Float) -> Unit,
    onBatteryModeChange: (BatterySaverMode) -> Unit,
    onTimeWindowChange: (Int, Int) -> Unit,
    onAutoLockChange: (Int) -> Unit,
    onShakeEnabledChange: (Boolean) -> Unit,
    onShakeSensitivityChange: (Float) -> Unit,
    onStartOnBootChange: (Boolean) -> Unit,
    onAccentChange: (AccentTheme) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onReduceEffectsChange: (Boolean) -> Unit,
    onSoundEnabledChange: (Boolean) -> Unit,
    onHapticsEnabledChange: (Boolean) -> Unit,
    onExperimentalUnlockChange: (Boolean) -> Unit,
    onOpenHealthCheck: () -> Unit,
    onOpenHistory: () -> Unit,
    onBackupJson: () -> Unit,
    onRestoreJson: () -> Unit,
    onRerunOnboarding: () -> Unit,
    onTapFeedback: () -> Unit
) {
    val strings = LocalSaylocStrings.current
    val palette = LocalSaylocPalette.current
    var showExperimentalWarning by remember { mutableStateOf(false) }

    if (showExperimentalWarning) {
        AlertDialog(
            onDismissRequest = { showExperimentalWarning = false },
            containerColor = palette.pillBg,
            titleContentColor = WarningAmber,
            textContentColor = palette.textPrimary,
            icon = {
                Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = WarningAmber)
            },
            title = { Text(strings.experimentalDialogTitle) },
            text = { Text(strings.experimentalDialogBody) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExperimentalWarning = false
                        onExperimentalUnlockChange(true)
                    }
                ) {
                    Text(strings.enableAnywayBtn, color = palette.rimGlow)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExperimentalWarning = false }) {
                    Text(strings.cancelBtn, color = palette.textSecondary)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = strings.settingsTitle,
                style = MaterialTheme.typography.headlineMedium,
                color = palette.textPrimary
            )

            // 1. Voice & Language
            Glass3DCard(modifier = Modifier.fillMaxWidth(), tilt = tilt) {
                SectionHeading(strings.sectionVoiceLanguage)
                Spacer(Modifier.height(10.dp))

                Text(strings.uiLanguageLabel, style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Glass3DChip(
                        label = "English",
                        selected = settings.uiLanguage == UiLanguage.EN,
                        onClick = { onTapFeedback(); onUiLanguageChange(UiLanguage.EN) },
                        testTag = "settings_ui_lang_en"
                    )
                    Glass3DChip(
                        label = "हिंदी (Hindi)",
                        selected = settings.uiLanguage == UiLanguage.HI,
                        onClick = { onTapFeedback(); onUiLanguageChange(UiLanguage.HI) },
                        testTag = "settings_ui_lang_hi"
                    )
                }

                Spacer(Modifier.height(14.dp))
                Text(strings.voiceLanguageLabel, style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VoiceLanguage.entries.forEach { lang ->
                        val label = when (lang) {
                            VoiceLanguage.HINGLISH -> "Hinglish"
                            VoiceLanguage.HINDI -> "Hindi"
                            VoiceLanguage.ENGLISH -> "English"
                        }
                        Glass3DChip(
                            label = label,
                            selected = settings.voiceLanguage == lang,
                            onClick = { onTapFeedback(); onVoiceLanguageChange(lang) },
                            testTag = "settings_voice_lang_${lang.key}"
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                val sensitivityTag = when {
                    settings.sensitivity < 0.45f -> strings.sensitivityStrict
                    settings.sensitivity > 0.78f -> strings.sensitivityRelaxed
                    else -> strings.sensitivityBalanced
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(strings.sensitivityLabel, style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
                    Text(
                        text = "$sensitivityTag (${(settings.sensitivity * 100).toInt()}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.rimGlow
                    )
                }
                Glass3DSlider(
                    value = settings.sensitivity,
                    onValueChange = onSensitivityChange,
                    valueRange = 0.2f..0.95f,
                    testTag = "settings_sensitivity_slider"
                )
            }

            // 2. Battery & Smart Triggers
            Glass3DCard(modifier = Modifier.fillMaxWidth(), tilt = tilt) {
                SectionHeading(strings.sectionBatteryTriggers)
                Spacer(Modifier.height(10.dp))

                Text(strings.batterySaverLabel, style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BatterySaverMode.entries.forEach { mode ->
                        Glass3DChip(
                            label = batteryModeLabel(mode, settings.uiLanguage),
                            selected = settings.batterySaverMode == mode,
                            onClick = { onTapFeedback(); onBatteryModeChange(mode) },
                            testTag = "settings_battery_${mode.key}"
                        )
                    }
                }

                if (settings.batterySaverMode == BatterySaverMode.TIME_WINDOW) {
                    Spacer(Modifier.height(12.dp))
                    Text(strings.activeWindowLabel, style = MaterialTheme.typography.labelMedium, color = palette.rimGlow)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Glass3DChip(
                            label = "Start: ${formatMinutes(settings.windowStartMinutes)} (+1h)",
                            selected = false,
                            onClick = {
                                onTapFeedback()
                                val next = (settings.windowStartMinutes + 60) % 1440
                                onTimeWindowChange(next, settings.windowEndMinutes)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        Glass3DChip(
                            label = "End: ${formatMinutes(settings.windowEndMinutes)} (+1h)",
                            selected = false,
                            onClick = {
                                onTapFeedback()
                                val next = (settings.windowEndMinutes + 60) % 1440
                                onTimeWindowChange(settings.windowStartMinutes, next)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text(strings.autoLockLabel, style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
                Spacer(Modifier.height(8.dp))
                val autoOptions = listOf(0, 1, 5, 10, 30)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    autoOptions.forEach { mins ->
                        Glass3DChip(
                            label = if (mins == 0) strings.autoLockOff else "${mins}m",
                            selected = settings.autoLockMinutes == mins,
                            onClick = { onTapFeedback(); onAutoLockChange(mins) }
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                ToggleRow(
                    title = strings.shakeToLockLabel,
                    checked = settings.shakeToLockEnabled,
                    onCheckedChange = { onTapFeedback(); onShakeEnabledChange(it) },
                    testTag = "settings_shake_toggle"
                )
                if (settings.shakeToLockEnabled) {
                    Spacer(Modifier.height(6.dp))
                    Text(strings.shakeSensitivityLabel, style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                    Glass3DSlider(
                        value = settings.shakeSensitivity,
                        onValueChange = onShakeSensitivityChange,
                        valueRange = 0.2f..1.0f
                    )
                }

                Spacer(Modifier.height(10.dp))
                ToggleRow(
                    title = strings.startOnBootLabel,
                    subtitle = strings.startOnBootHint,
                    checked = settings.startOnBoot,
                    onCheckedChange = { onTapFeedback(); onStartOnBootChange(it) },
                    testTag = "settings_boot_toggle"
                )
            }

            // 3. 3D Glass Appearance & Performance
            Glass3DCard(modifier = Modifier.fillMaxWidth(), tilt = tilt) {
                SectionHeading(strings.sectionAppearance)
                Spacer(Modifier.height(12.dp))

                Text(strings.accentColorLabel, style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    AccentTheme.entries.forEach { accent ->
                        val preview = paletteFor(accent, isDark = true)
                        val isSelected = settings.accentTheme == accent
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("settings_accent_${accent.key}")
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(preview.accentStart, preview.accentEnd)))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.35f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    onTapFeedback()
                                    onAccentChange(accent)
                                }
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text(strings.themeModeLabel, style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        Glass3DChip(
                            label = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                            selected = settings.themeMode == mode,
                            onClick = { onTapFeedback(); onThemeModeChange(mode) },
                            testTag = "settings_theme_${mode.key}"
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                ToggleRow(
                    title = strings.reduceEffectsLabel,
                    subtitle = strings.reduceEffectsHint,
                    checked = settings.reduceEffects,
                    onCheckedChange = { onTapFeedback(); onReduceEffectsChange(it) },
                    testTag = "settings_reduce_effects_toggle"
                )
            }

            // 4. Sound, Haptics & Tools (Health-Check, History, Backup/Restore)
            Glass3DCard(modifier = Modifier.fillMaxWidth(), tilt = tilt) {
                SectionHeading(strings.sectionFeedbackTools)
                Spacer(Modifier.height(10.dp))

                ToggleRow(
                    title = strings.soundEffectsLabel,
                    checked = settings.soundEnabled,
                    onCheckedChange = { onSoundEnabledChange(it) },
                    testTag = "settings_sound_toggle"
                )
                Spacer(Modifier.height(8.dp))
                ToggleRow(
                    title = strings.hapticsLabel,
                    checked = settings.hapticsEnabled,
                    onCheckedChange = { onHapticsEnabledChange(it) },
                    testTag = "settings_haptics_toggle"
                )

                Spacer(Modifier.height(14.dp))
                Glass3DButton(
                    text = strings.permissionHealthBtn,
                    icon = Icons.Rounded.HealthAndSafety,
                    style = GlassButtonStyle.SECONDARY,
                    onClick = onOpenHealthCheck,
                    onTapFeedback = onTapFeedback,
                    testTag = "settings_health_btn"
                )
                Spacer(Modifier.height(10.dp))
                Glass3DButton(
                    text = strings.commandHistoryBtn,
                    icon = Icons.Rounded.History,
                    style = GlassButtonStyle.SECONDARY,
                    onClick = onOpenHistory,
                    onTapFeedback = onTapFeedback,
                    testTag = "settings_history_btn"
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Glass3DButton(
                        text = strings.backupJsonBtn,
                        icon = Icons.Rounded.Upload,
                        style = GlassButtonStyle.SECONDARY,
                        onClick = onBackupJson,
                        onTapFeedback = onTapFeedback,
                        modifier = Modifier.weight(1f),
                        testTag = "settings_backup_btn"
                    )
                    Glass3DButton(
                        text = strings.restoreJsonBtn,
                        icon = Icons.Rounded.Download,
                        style = GlassButtonStyle.SECONDARY,
                        onClick = onRestoreJson,
                        onTapFeedback = onTapFeedback,
                        modifier = Modifier.weight(1f),
                        testTag = "settings_restore_btn"
                    )
                }
                Spacer(Modifier.height(10.dp))
                Glass3DButton(
                    text = strings.rerunOnboardingBtn,
                    icon = Icons.Rounded.RestartAlt,
                    style = GlassButtonStyle.SECONDARY,
                    onClick = onRerunOnboarding,
                    onTapFeedback = onTapFeedback,
                    testTag = "settings_onboarding_btn"
                )
            }

            // 5. Experimental & Honest Security Section
            Glass3DCard(
                modifier = Modifier.fillMaxWidth(),
                tilt = tilt,
                accentBorder = settings.experimentalUnlockEnabled
            ) {
                SectionHeading(strings.sectionExperimental, color = WarningAmber)
                Spacer(Modifier.height(8.dp))
                ToggleRow(
                    title = strings.experimentalUnlockLabel,
                    subtitle = strings.experimentalUnlockHint,
                    checked = settings.experimentalUnlockEnabled,
                    onCheckedChange = { wantsOn ->
                        onTapFeedback()
                        if (wantsOn) {
                            showExperimentalWarning = true
                        } else {
                            onExperimentalUnlockChange(false)
                        }
                    },
                    testTag = "settings_experimental_unlock_toggle"
                )
            }
        }
    }
}

@Composable
private fun SectionHeading(text: String, color: Color? = null) {
    val palette = LocalSaylocPalette.current
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = color ?: palette.rimGlow
    )
}

@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
    testTag: String? = null
) {
    val palette = LocalSaylocPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = palette.textPrimary
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Glass3DToggle(
            checked = checked,
            onCheckedChange = onCheckedChange,
            testTag = testTag
        )
    }
}

private fun formatMinutes(totalMinutes: Int): String {
    val hours = (totalMinutes / 60) % 24
    val mins = totalMinutes % 60
    return String.format(Locale.US, "%02d:%02d", hours, mins)
}
