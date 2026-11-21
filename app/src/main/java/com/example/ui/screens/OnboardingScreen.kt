package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.PermissionHealthStatus
import com.example.data.UiLanguage
import com.example.ui.components.Glass3DButton
import com.example.ui.components.Glass3DCard
import com.example.ui.components.Glass3DChip
import com.example.ui.components.GlassButtonStyle
import com.example.ui.components.TiltOffset
import com.example.ui.i18n.LocalSaylocStrings
import com.example.ui.theme.DetectedEmerald
import com.example.ui.theme.LocalSaylocPalette
import com.example.ui.theme.WarningAmber

@Composable
fun OnboardingScreen(
    health: PermissionHealthStatus,
    uiLanguage: UiLanguage,
    tilt: TiltOffset,
    onSelectLanguage: (UiLanguage) -> Unit,
    onRequestMic: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestDeviceAdmin: () -> Unit,
    onRequestBatteryExemption: () -> Unit,
    onTapFeedback: () -> Unit,
    onFinish: () -> Unit
) {
    val strings = LocalSaylocStrings.current
    val palette = LocalSaylocPalette.current
    // Steps: 0 Welcome + Honesty, 1 Mic, 2 Notifications, 3 Device Admin, 4 Battery
    var step by remember { mutableIntStateOf(0) }
    val totalSteps = 5

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar: step indicator + quick EN/HI toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(totalSteps) { index ->
                        val active = index == step
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (active) 28.dp else 10.dp)
                                .clip(CircleShape)
                                .background(
                                    if (active) {
                                        Brush.horizontalGradient(listOf(palette.accentStart, palette.rimGlow))
                                    } else {
                                        Brush.horizontalGradient(
                                            listOf(
                                                palette.textSecondary.copy(alpha = 0.25f),
                                                palette.textSecondary.copy(alpha = 0.25f)
                                            )
                                        )
                                    }
                                )
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Glass3DChip(
                        label = "EN",
                        selected = uiLanguage == UiLanguage.EN,
                        onClick = { onSelectLanguage(UiLanguage.EN) },
                        testTag = "onboard_lang_en"
                    )
                    Glass3DChip(
                        label = "हिंदी",
                        selected = uiLanguage == UiLanguage.HI,
                        onClick = { onSelectLanguage(UiLanguage.HI) },
                        testTag = "onboard_lang_hi"
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            AnimatedContent(
                targetState = step,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    (slideInHorizontally(tween(280)) { it / 3 } + scaleIn(tween(280), initialScale = 0.94f) + fadeIn(tween(240)))
                        .togetherWith(
                            slideOutHorizontally(tween(220)) { -it / 3 } + scaleOut(tween(220), targetScale = 0.94f) + fadeOut(tween(180))
                        )
                },
                label = "onboardingStep"
            ) { currentStep ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    when (currentStep) {
                        0 -> WelcomeStepContent(tilt = tilt)
                        1 -> PermissionStepCard(
                            icon = Icons.Rounded.Mic,
                            title = strings.onboardMicTitle,
                            body = strings.onboardMicBody,
                            isGranted = health.hasMicrophone,
                            tilt = tilt,
                            onGrant = onRequestMic,
                            onTapFeedback = onTapFeedback,
                            grantTestTag = "onboard_grant_mic"
                        )
                        2 -> PermissionStepCard(
                            icon = Icons.Rounded.NotificationsActive,
                            title = strings.onboardNotifTitle,
                            body = strings.onboardNotifBody,
                            isGranted = health.hasNotifications,
                            tilt = tilt,
                            onGrant = onRequestNotifications,
                            onTapFeedback = onTapFeedback,
                            grantTestTag = "onboard_grant_notif"
                        )
                        3 -> PermissionStepCard(
                            icon = Icons.Rounded.AdminPanelSettings,
                            title = strings.onboardAdminTitle,
                            body = strings.onboardAdminBody,
                            isGranted = health.isDeviceAdminActive,
                            tilt = tilt,
                            onGrant = onRequestDeviceAdmin,
                            onTapFeedback = onTapFeedback,
                            grantTestTag = "onboard_grant_admin"
                        )
                        4 -> PermissionStepCard(
                            icon = Icons.Rounded.BatterySaver,
                            title = strings.onboardBatteryTitle,
                            body = strings.onboardBatteryBody,
                            isGranted = health.isBatteryOptimizationIgnored,
                            tilt = tilt,
                            onGrant = onRequestBatteryExemption,
                            onTapFeedback = onTapFeedback,
                            grantTestTag = "onboard_grant_battery"
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Footer navigation buttons (Skip always available on permission steps)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (step > 0) {
                    Glass3DButton(
                        text = strings.skipBtn,
                        onClick = {
                            if (step < totalSteps - 1) step++ else onFinish()
                        },
                        style = GlassButtonStyle.SECONDARY,
                        onTapFeedback = onTapFeedback,
                        modifier = Modifier.weight(1f),
                        testTag = "onboard_skip_btn"
                    )
                }
                Glass3DButton(
                    text = if (step == totalSteps - 1) strings.finishSetupBtn else strings.nextBtn,
                    onClick = {
                        if (step < totalSteps - 1) step++ else onFinish()
                    },
                    style = GlassButtonStyle.PRIMARY,
                    onTapFeedback = onTapFeedback,
                    modifier = Modifier.weight(1.4f),
                    testTag = "onboard_next_btn"
                )
            }
        }
    }
}

@Composable
private fun WelcomeStepContent(tilt: TiltOffset) {
    val strings = LocalSaylocStrings.current
    val palette = LocalSaylocPalette.current

    Box(
        modifier = Modifier
            .size(132.dp)
            .clip(CircleShape)
            .border(
                2.dp,
                Brush.linearGradient(listOf(palette.accentStart, palette.rimGlow)),
                CircleShape
            )
    ) {
        Image(
            painter = painterResource(R.drawable.sayloc_logo_orb),
            contentDescription = "Sayloc Logo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }

    Spacer(Modifier.height(18.dp))

    Text(
        text = strings.onboardWelcomeTitle,
        style = MaterialTheme.typography.displaySmall,
        color = palette.textPrimary,
        textAlign = TextAlign.Center
    )
    Text(
        text = strings.tagline,
        style = MaterialTheme.typography.titleMedium,
        color = palette.rimGlow,
        textAlign = TextAlign.Center
    )

    Spacer(Modifier.height(16.dp))

    Glass3DCard(modifier = Modifier.fillMaxWidth(), tilt = tilt) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Shield,
                contentDescription = null,
                tint = DetectedEmerald,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = strings.privacyBadge,
                style = MaterialTheme.typography.titleSmall,
                color = DetectedEmerald
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = strings.onboardWelcomeBody,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary
        )
    }

    Spacer(Modifier.height(14.dp))

    // Honest security disclosure card about PIN/Pattern/Biometrics
    Glass3DCard(
        modifier = Modifier.fillMaxWidth(),
        tilt = tilt,
        accentBorder = true
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = WarningAmber,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = strings.onboardHonestyTitle,
                style = MaterialTheme.typography.titleSmall,
                color = WarningAmber
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = strings.onboardHonestyBody,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textPrimary
        )
    }
}

@Composable
private fun PermissionStepCard(
    icon: ImageVector,
    title: String,
    body: String,
    isGranted: Boolean,
    tilt: TiltOffset,
    onGrant: () -> Unit,
    onTapFeedback: () -> Unit,
    grantTestTag: String
) {
    val strings = LocalSaylocStrings.current
    val palette = LocalSaylocPalette.current

    Glass3DCard(
        modifier = Modifier.fillMaxWidth(),
        tilt = tilt,
        accentBorder = isGranted
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        if (isGranted) listOf(DetectedEmerald, Color(0xFF0284C7))
                        else listOf(palette.accentStart, palette.accentEnd)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Rounded.CheckCircle else icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
        }

        Spacer(Modifier.height(18.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = palette.textPrimary
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = palette.textSecondary
        )

        Spacer(Modifier.height(22.dp))

        if (isGranted) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(DetectedEmerald.copy(alpha = 0.16f))
                    .border(1.dp, DetectedEmerald.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = DetectedEmerald,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = strings.grantedBadge,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = DetectedEmerald
                )
            }
        } else {
            Glass3DButton(
                text = strings.grantBtn,
                icon = icon,
                onClick = onGrant,
                style = GlassButtonStyle.PRIMARY,
                onTapFeedback = onTapFeedback,
                testTag = grantTestTag
            )
        }
    }
}
