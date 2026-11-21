package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AppSettings
import com.example.data.ListeningState
import com.example.data.PermissionHealthStatus
import com.example.data.VoiceCommand
import com.example.system.LiveRuntimeState
import com.example.ui.components.Glass3DButton
import com.example.ui.components.Glass3DCard
import com.example.ui.components.Glass3DChip
import com.example.ui.components.GlassButtonStyle
import com.example.ui.components.Sayloc3DOrb
import com.example.ui.components.TiltOffset
import com.example.ui.i18n.LocalSaylocStrings
import com.example.ui.i18n.listeningStateLabel
import com.example.ui.theme.DetectedEmerald
import com.example.ui.theme.ListeningCyan
import com.example.ui.theme.LocalSaylocPalette
import com.example.ui.theme.LockCrimson
import com.example.ui.theme.WarningAmber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    runtime: LiveRuntimeState,
    settings: AppSettings,
    commands: List<VoiceCommand>,
    health: PermissionHealthStatus,
    tilt: TiltOffset,
    onToggleListening: () -> Unit,
    onLockNow: () -> Unit,
    onToggleFlashlight: () -> Unit,
    onToggleShakeLock: () -> Unit,
    onOpenHealthCheck: () -> Unit,
    onTestCommand: (VoiceCommand) -> Unit,
    onTapFeedback: () -> Unit
) {
    val strings = LocalSaylocStrings.current
    val palette = LocalSaylocPalette.current
    val isListening = runtime.isServiceRunning

    val statusColor = when (runtime.listeningState) {
        ListeningState.LISTENING -> ListeningCyan
        ListeningState.DETECTED -> DetectedEmerald
        ListeningState.PAUSED_BATTERY -> WarningAmber
        ListeningState.ERROR -> LockCrimson
        ListeningState.IDLE -> palette.textSecondary
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Brand & Privacy Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = strings.appName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = palette.textPrimary
                    )
                    Text(
                        text = strings.tagline,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.rimGlow
                    )
                }

                Glass3DChip(
                    label = "${health.grantedCount}/4",
                    selected = !health.allCoreGranted,
                    icon = Icons.Rounded.HealthAndSafety,
                    onClick = onOpenHealthCheck,
                    testTag = "home_health_chip"
                )
            }

            // Missing permission callout banner if incomplete
            if (!health.allCoreGranted) {
                Spacer(Modifier.height(12.dp))
                Glass3DCard(
                    modifier = Modifier.fillMaxWidth(),
                    tilt = tilt,
                    contentPadding = PaddingValues(14.dp),
                    onClick = onOpenHealthCheck
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = WarningAmber,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.permissionAttentionTitle,
                                style = MaterialTheme.typography.titleSmall,
                                color = WarningAmber
                            )
                            Text(
                                text = strings.permissionAttentionBody,
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = strings.checkPermissionsBtn,
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.rimGlow
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Live Status Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.14f))
                    .border(1.dp, statusColor.copy(alpha = 0.45f), CircleShape)
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = strings.listeningStateLabel(runtime.listeningState),
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor
                )
            }

            // Big 3D Glass Orb Centerpiece
            Sayloc3DOrb(
                listeningState = runtime.listeningState,
                rmsLevel = runtime.rmsLevel,
                lastDetectionTimestamp = runtime.lastDetectionTimestamp,
                tilt = tilt,
                onClick = {
                    onTapFeedback()
                    onToggleListening()
                }
            )

            Text(
                text = runtime.errorMessage
                    ?: runtime.statusDetail.takeIf { it.isNotBlank() }
                    ?: strings.tapOrbHint,
                style = MaterialTheme.typography.bodySmall,
                color = if (runtime.errorMessage != null) LockCrimson else palette.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(16.dp))

            // Primary Controls: Start/Stop Listening + Big Lock Now
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Glass3DButton(
                    text = if (isListening) strings.stopListening else strings.startListening,
                    icon = if (isListening) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                    style = if (isListening) GlassButtonStyle.SECONDARY else GlassButtonStyle.PRIMARY,
                    onClick = onToggleListening,
                    onTapFeedback = onTapFeedback,
                    modifier = Modifier.weight(1f),
                    testTag = "home_toggle_listening_btn"
                )
            }

            Spacer(Modifier.height(12.dp))

            Glass3DButton(
                text = strings.lockNow,
                icon = Icons.Rounded.Lock,
                style = GlassButtonStyle.DANGER,
                onClick = onLockNow,
                onTapFeedback = onTapFeedback,
                modifier = Modifier.fillMaxWidth(),
                testTag = "home_lock_now_btn"
            )

            Spacer(Modifier.height(16.dp))

            // Live Voice Telemetry Card (Last Heard & Matched)
            Glass3DCard(
                modifier = Modifier.fillMaxWidth(),
                tilt = tilt,
                accentBorder = runtime.listeningState == ListeningState.DETECTED
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.lastHeardLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.rimGlow
                    )
                    if (runtime.lastMatchedCommand.isNotBlank()) {
                        Text(
                            text = "${strings.lastMatchedLabel}: ${runtime.lastMatchedCommand}",
                            style = MaterialTheme.typography.labelSmall,
                            color = DetectedEmerald
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = runtime.lastHeardText.ifBlank { strings.waitingForVoice },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    color = palette.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(12.dp))

                // Quick Toggles Row (Flashlight + Shake-to-Lock)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Glass3DChip(
                        label = strings.flashlightQuick,
                        selected = runtime.isFlashlightOn,
                        icon = if (runtime.isFlashlightOn) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff,
                        onClick = {
                            onTapFeedback()
                            onToggleFlashlight()
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "home_quick_flashlight"
                    )
                    Glass3DChip(
                        label = strings.shakeToLockQuick,
                        selected = settings.shakeToLockEnabled,
                        icon = Icons.Rounded.Vibration,
                        onClick = {
                            onTapFeedback()
                            onToggleShakeLock()
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "home_quick_shake"
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Active Command Quick-Test Chips
            val enabledCommands = commands.filter { it.isEnabled }
            if (enabledCommands.isNotEmpty()) {
                Glass3DCard(
                    modifier = Modifier.fillMaxWidth(),
                    tilt = tilt
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.activeCommandsTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = palette.textPrimary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = DetectedEmerald,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = settings.voiceLanguage.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = DetectedEmerald
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        enabledCommands.take(6).forEach { cmd ->
                            Glass3DChip(
                                label = "\"${cmd.name}\"",
                                selected = runtime.lastMatchedCommand.equals(cmd.name, ignoreCase = true),
                                onClick = {
                                    onTapFeedback()
                                    onTestCommand(cmd)
                                },
                                testTag = "home_cmd_chip_${cmd.id}"
                            )
                        }
                    }
                }
            }
        }
    }
}
