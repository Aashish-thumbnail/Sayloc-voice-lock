package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.PermissionHealthStatus
import com.example.ui.components.Glass3DCard
import com.example.ui.components.Glass3DChip
import com.example.ui.components.TiltOffset
import com.example.ui.i18n.LocalSaylocStrings
import com.example.ui.theme.DetectedEmerald
import com.example.ui.theme.LocalSaylocPalette
import com.example.ui.theme.LockCrimson
import com.example.ui.theme.WarningAmber

@Composable
fun PermissionHealthScreen(
    health: PermissionHealthStatus,
    tilt: TiltOffset,
    onRequestMic: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestDeviceAdmin: () -> Unit,
    onRequestBattery: () -> Unit,
    onRequestDnd: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val strings = LocalSaylocStrings.current
    val palette = LocalSaylocPalette.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .verticalScroll(rememberScrollState())
                .padding(top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("health_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = palette.textPrimary
                    )
                }
                Spacer(Modifier.width(6.dp))
                Column {
                    Text(
                        text = strings.healthTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "${health.grantedCount}/4 • ${strings.healthSubtitle}",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
            }

            HealthItemCard(
                icon = Icons.Rounded.Mic,
                title = strings.healthMic,
                isOk = health.hasMicrophone,
                isOptional = false,
                tilt = tilt,
                onFix = onRequestMic,
                testTag = "health_fix_mic"
            )
            HealthItemCard(
                icon = Icons.Rounded.NotificationsActive,
                title = strings.healthNotif,
                isOk = health.hasNotifications,
                isOptional = false,
                tilt = tilt,
                onFix = onRequestNotifications,
                testTag = "health_fix_notif"
            )
            HealthItemCard(
                icon = Icons.Rounded.AdminPanelSettings,
                title = strings.healthAdmin,
                isOk = health.isDeviceAdminActive,
                isOptional = false,
                tilt = tilt,
                onFix = onRequestDeviceAdmin,
                testTag = "health_fix_admin"
            )
            HealthItemCard(
                icon = Icons.Rounded.BatterySaver,
                title = strings.healthBattery,
                isOk = health.isBatteryOptimizationIgnored,
                isOptional = false,
                tilt = tilt,
                onFix = onRequestBattery,
                testTag = "health_fix_battery"
            )
            HealthItemCard(
                icon = Icons.Rounded.DoNotDisturbOn,
                title = strings.healthDnd,
                isOk = health.hasDndPolicyAccess,
                isOptional = true,
                tilt = tilt,
                onFix = onRequestDnd,
                testTag = "health_fix_dnd"
            )

            // Transparent OS Lock Security diagnostic card
            Glass3DCard(modifier = Modifier.fillMaxWidth(), tilt = tilt) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = strings.healthLockType,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (health.isKeyguardSecure) {
                        strings.healthSecureLockDetected
                    } else {
                        strings.healthSwipeLockDetected
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary
                )
            }
        }
    }
}

@Composable
private fun HealthItemCard(
    icon: ImageVector,
    title: String,
    isOk: Boolean,
    isOptional: Boolean,
    tilt: TiltOffset,
    onFix: () -> Unit,
    testTag: String
) {
    val strings = LocalSaylocStrings.current
    val palette = LocalSaylocPalette.current
    val statusColor = when {
        isOk -> DetectedEmerald
        isOptional -> WarningAmber
        else -> LockCrimson
    }
    val statusText = when {
        isOk -> strings.readyBadge
        isOptional -> strings.optionalBadge
        else -> strings.missingBadge
    }

    Glass3DCard(
        modifier = Modifier.fillMaxWidth(),
        tilt = tilt,
        accentBorder = isOk
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(statusColor.copy(alpha = 0.16f))
                        .border(1.dp, statusColor.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textPrimary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isOk) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor
                        )
                    }
                }
            }

            if (!isOk) {
                Spacer(Modifier.width(8.dp))
                Glass3DChip(
                    label = strings.fixBtn,
                    selected = true,
                    onClick = onFix,
                    testTag = testTag
                )
            }
        }
    }
}
