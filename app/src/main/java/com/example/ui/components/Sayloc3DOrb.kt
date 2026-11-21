package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ListeningState
import com.example.ui.theme.DetectedEmerald
import com.example.ui.theme.ListeningCyan
import com.example.ui.theme.LocalReduceEffects
import com.example.ui.theme.LocalSaylocPalette
import com.example.ui.theme.LockCrimson
import com.example.ui.theme.WarningAmber

/**
 * Interactive 3D Liquid-Glass Centerpiece Orb showcasing the custom Sayloc orb logo,
 * breathing scale, audio-reactive pulsing radar rings, emerald detection shockwave ripple,
 * specular glass crescent highlight, and accelerometer 2.5D parallax tilt.
 */
@Composable
fun Sayloc3DOrb(
    listeningState: ListeningState,
    rmsLevel: Float,
    lastDetectionTimestamp: Long,
    tilt: TiltOffset,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 228.dp
) {
    val palette = LocalSaylocPalette.current
    val reduceEffects = LocalReduceEffects.current
    val density = LocalDensity.current.density
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "orbLoops")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.975f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathScale"
    )
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulsePhase"
    )

    // Expanding ripple animation triggered each time a voice command matches
    val rippleProgress = remember { Animatable(1f) }
    LaunchedEffect(lastDetectionTimestamp) {
        if (lastDetectionTimestamp > 0L) {
            rippleProgress.snapTo(0f)
            rippleProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
            )
        }
    }

    val rmsBoost by animateFloatAsState(
        targetValue = if (listeningState == ListeningState.LISTENING) rmsLevel * 0.08f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "rmsBoost"
    )
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "orbPress"
    )

    val stateGlowColor = when (listeningState) {
        ListeningState.LISTENING -> ListeningCyan
        ListeningState.DETECTED -> DetectedEmerald
        ListeningState.PAUSED_BATTERY -> WarningAmber
        ListeningState.ERROR -> LockCrimson
        ListeningState.IDLE -> palette.accentStart
    }

    val effectiveScale = if (reduceEffects) pressScale else (breathScale + rmsBoost) * pressScale
    val outerContainerSize = size + 64.dp

    Box(
        modifier = modifier
            .size(outerContainerSize),
        contentAlignment = Alignment.Center
    ) {
        // Pulsing radar rings + ambient glow + detection shockwave ripple
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val orbRadius = (size.toPx() / 2f)

            // Ambient radial halo
            if (!reduceEffects) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            stateGlowColor.copy(alpha = 0.42f),
                            palette.accentEnd.copy(alpha = 0.16f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = orbRadius * 1.48f
                    ),
                    radius = orbRadius * 1.48f,
                    center = center
                )
            }

            // Active listening pulsing concentric rings
            if (listeningState == ListeningState.LISTENING && !reduceEffects) {
                for (ring in 0..2) {
                    val p = (pulsePhase + ring * 0.33f) % 1f
                    val radius = orbRadius * (0.98f + p * (0.42f + rmsLevel * 0.14f))
                    val alpha = (1f - p) * (0.58f + rmsLevel * 0.30f)
                    drawCircle(
                        color = stateGlowColor.copy(alpha = alpha.coerceIn(0f, 0.85f)),
                        radius = radius,
                        center = center,
                        style = Stroke(width = (2.5f - p * 1.5f).dp.toPx())
                    )
                }
            }

            // Command detected shockwave ripple
            if (rippleProgress.value < 1f) {
                val p = rippleProgress.value
                drawCircle(
                    color = DetectedEmerald.copy(alpha = (1f - p) * 0.9f),
                    radius = orbRadius * (0.95f + p * 0.55f),
                    center = center,
                    style = Stroke(width = (5f * (1f - p) + 1f).dp.toPx())
                )
            }
        }

        // Interactive glossy 3D orb sphere with attached Sayloc logo
        Box(
            modifier = Modifier
                .size(size)
                .testTag("home_3d_orb")
                .graphicsLayer {
                    scaleX = effectiveScale
                    scaleY = effectiveScale
                    if (!reduceEffects) {
                        rotationX = tilt.pitchDeg * 1.25f
                        rotationY = tilt.rollDeg * 1.25f
                        cameraDistance = 12f * density
                    }
                }
                .shadow(
                    elevation = if (reduceEffects) 8.dp else 26.dp,
                    shape = CircleShape,
                    ambientColor = stateGlowColor,
                    spotColor = stateGlowColor
                )
                .clip(CircleShape)
                .border(
                    width = 2.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.85f),
                            stateGlowColor,
                            palette.accentEnd,
                            Color.White.copy(alpha = 0.35f)
                        )
                    ),
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            // User's custom Sayloc glass orb artwork cropped seamlessly into the 3D sphere
            Image(
                painter = painterResource(id = R.drawable.sayloc_logo_orb),
                contentDescription = "Sayloc Voice Orb",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Zoom slightly so the inner glowing orb fills the sphere edge-to-edge
                        scaleX = 1.32f
                        scaleY = 1.32f
                    }
            )

            // Specular liquid-glass dome sheen & rim highlight
            if (!reduceEffects) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.34f),
                                Color.White.copy(alpha = 0.06f),
                                Color.Transparent
                            ),
                            center = Offset(this.size.width * 0.30f, this.size.height * 0.24f),
                            radius = this.size.width * 0.48f
                        )
                    )
                    // Subtle state tint around inner rim
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                stateGlowColor.copy(alpha = 0.36f)
                            ),
                            center = Offset(this.size.width / 2f, this.size.height / 2f),
                            radius = this.size.width * 0.52f
                        )
                    )
                }
            }
        }
    }
}
