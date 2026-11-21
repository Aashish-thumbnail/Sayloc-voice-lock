package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalReduceEffects
import com.example.ui.theme.LocalSaylocPalette

/**
 * Full-bleed atmospheric deep navy / frost liquid-glass background canvas.
 */
@Composable
fun SaylocBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalSaylocPalette.current
    val reduceEffects = LocalReduceEffects.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(palette.bgTop, palette.bgBottom)
                )
            )
            .drawBehind {
                if (!reduceEffects) {
                    // Ambient top-left violet aurora
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(palette.orbAmbientPrimary, Color.Transparent),
                            center = Offset(size.width * 0.12f, size.height * 0.14f),
                            radius = size.width * 0.78f
                        ),
                        radius = size.width * 0.78f,
                        center = Offset(size.width * 0.12f, size.height * 0.14f)
                    )
                    // Ambient bottom-right electric blue glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(palette.orbAmbientSecondary, Color.Transparent),
                            center = Offset(size.width * 0.90f, size.height * 0.72f),
                            radius = size.width * 0.85f
                        ),
                        radius = size.width * 0.85f,
                        center = Offset(size.width * 0.90f, size.height * 0.72f)
                    )
                }
            },
        content = content
    )
}

/**
 * Sculpted 2.5D Liquid-Glass Card with layered shadow, specular top rim, inner glow,
 * and optional accelerometer parallax tilt.
 */
@Composable
fun Glass3DCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    tilt: TiltOffset = TiltOffset(),
    accentBorder: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalSaylocPalette.current
    val reduceEffects = LocalReduceEffects.current
    val density = LocalDensity.current.density
    val shape = RoundedCornerShape(cornerRadius)

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.975f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "cardScale"
    )
    val elevationDp by animateDpAsState(
        targetValue = if (pressed && onClick != null) 3.dp else if (reduceEffects) 4.dp else 14.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "cardElevation"
    )

    val borderBrush = if (accentBorder) {
        Brush.linearGradient(
            colors = listOf(
                palette.accentStart.copy(alpha = 0.85f),
                palette.rimGlow.copy(alpha = 0.85f),
                palette.accentEnd.copy(alpha = 0.75f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                palette.cardBorderTop,
                palette.cardBorderBottom
            )
        )
    }

    val cardModifier = modifier
        .graphicsLayer {
            scaleX = pressScale
            scaleY = pressScale
            if (!reduceEffects) {
                rotationX = tilt.pitchDeg * 0.45f
                rotationY = tilt.rollDeg * 0.45f
                cameraDistance = 14f * density
            }
        }
        .shadow(
            elevation = elevationDp,
            shape = shape,
            ambientColor = palette.cardShadow,
            spotColor = if (accentBorder) palette.accentStart.copy(alpha = 0.55f) else palette.cardShadow
        )
        .clip(shape)
        .background(
            Brush.verticalGradient(
                colors = listOf(palette.cardTop, palette.cardBottom)
            )
        )
        .drawBehind {
            if (!reduceEffects) {
                // Soft inner top-edge glass reflection
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (palette.isDark) 0.11f else 0.45f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = size.height * 0.42f
                    ),
                    cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
                )
            }
        }
        .border(width = 1.25.dp, brush = borderBrush, shape = shape)
        .let { base ->
            if (onClick != null) {
                base.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
            } else {
                base
            }
        }

    Column(
        modifier = cardModifier.padding(contentPadding),
        content = content
    )
}

enum class GlassButtonStyle {
    PRIMARY,
    SECONDARY,
    DANGER,
    SUCCESS
}

/**
 * Tactile 3D Liquid-Glass Button with sculpted bottom depth lip, luminous rim highlight,
 * spring sink-in press animation, and haptic response.
 */
@Composable
fun Glass3DButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: GlassButtonStyle = GlassButtonStyle.PRIMARY,
    enabled: Boolean = true,
    onTapFeedback: (() -> Unit)? = null,
    testTag: String? = null
) {
    val palette = LocalSaylocPalette.current
    val reduceEffects = LocalReduceEffects.current
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.955f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "btnScale"
    )
    val sinkY by animateDpAsState(
        targetValue = if (pressed && enabled) 3.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "btnSink"
    )
    val bottomLipHeight by animateDpAsState(
        targetValue = if (pressed && enabled) 1.dp else 4.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "btnLip"
    )

    val (gradStart, gradEnd, lipColor, textColor) = when (style) {
        GlassButtonStyle.PRIMARY -> Quadruple(
            palette.accentStart,
            palette.accentEnd,
            Color(0xFF1A1468),
            Color.White
        )
        GlassButtonStyle.DANGER -> Quadruple(
            palette.dangerStart,
            palette.dangerEnd,
            Color(0xFF590D2E),
            Color.White
        )
        GlassButtonStyle.SUCCESS -> Quadruple(
            Color(0xFF10B981),
            Color(0xFF0284C7),
            Color(0xFF064E3B),
            Color.White
        )
        GlassButtonStyle.SECONDARY -> Quadruple(
            palette.cardTop,
            palette.cardBottom,
            if (palette.isDark) Color(0xFF08051E) else Color(0xFFB4BFE8),
            palette.textPrimary
        )
    }

    val shape: Shape = RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 52.dp)
            .let { if (testTag != null) it.testTag(testTag) else it }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.45f
            }
    ) {
        // 3D Base depth lip
        if (!reduceEffects) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(y = bottomLipHeight)
                    .shadow(
                        elevation = if (pressed) 2.dp else 10.dp,
                        shape = shape,
                        ambientColor = palette.cardShadow,
                        spotColor = gradStart.copy(alpha = 0.5f)
                    )
                    .clip(shape)
                    .background(lipColor)
            )
        }

        // Main glossy face
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
                .offset(y = sinkY)
                .clip(shape)
                .background(
                    Brush.linearGradient(colors = listOf(gradStart, gradEnd))
                )
                .drawBehind {
                    // Top specular liquid-glass sheen
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (style == GlassButtonStyle.SECONDARY) 0.22f else 0.32f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = size.height * 0.52f
                        ),
                        cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx())
                    )
                }
                .border(
                    width = 1.25.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.55f),
                            palette.rimGlow.copy(alpha = 0.35f)
                        )
                    ),
                    shape = shape
                )
                .clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onTapFeedback?.invoke()
                        onClick()
                    }
                )
                .padding(horizontal = 18.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Compact 3D Glass Chip for filters, presets, language toggles, and routine steps.
 */
@Composable
fun Glass3DChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    testTag: String? = null
) {
    val palette = LocalSaylocPalette.current
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(14.dp)

    val borderBrush = if (selected) {
        Brush.linearGradient(listOf(palette.accentStart, palette.rimGlow))
    } else {
        Brush.verticalGradient(listOf(palette.cardBorderTop, palette.cardBorderBottom))
    }

    val bgBrush = if (selected) {
        Brush.linearGradient(
            listOf(
                palette.accentStart.copy(alpha = 0.92f),
                palette.accentEnd.copy(alpha = 0.92f)
            )
        )
    } else {
        Brush.verticalGradient(listOf(palette.cardTop, palette.cardBottom))
    }

    Row(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .let { if (testTag != null) it.testTag(testTag) else it }
            .shadow(
                elevation = if (selected) 8.dp else 3.dp,
                shape = shape,
                spotColor = if (selected) palette.accentStart else palette.cardShadow
            )
            .clip(shape)
            .background(bgBrush)
            .border(1.dp, borderBrush, shape)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.White else palette.rimGlow,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.White else palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Illuminated 3D Glass Toggle Switch with glowing spherical thumb.
 */
@Composable
fun Glass3DToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String? = null
) {
    val palette = LocalSaylocPalette.current
    val haptic = LocalHapticFeedback.current
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 24.dp else 3.dp,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMedium),
        label = "toggleThumb"
    )
    val trackColor by animateColorAsState(
        targetValue = if (checked) palette.accentStart else palette.cardBottom,
        label = "toggleTrack"
    )

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .let { if (testTag != null) it.testTag(testTag) else it }
            .clickable(enabled = enabled) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCheckedChange(!checked)
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(54.dp)
                .height(30.dp)
                .clip(CircleShape)
                .background(
                    if (checked) {
                        Brush.horizontalGradient(listOf(palette.accentStart, palette.accentEnd))
                    } else {
                        Brush.verticalGradient(listOf(trackColor, palette.cardTop))
                    }
                )
                .border(
                    width = 1.dp,
                    color = if (checked) Color.White.copy(alpha = 0.55f) else palette.cardBorderTop,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            // Specular 3D spherical thumb
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(24.dp)
                    .shadow(6.dp, CircleShape, spotColor = palette.rimGlow)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.White, Color(0xFFDDE6FF)),
                            center = Offset(8f, 8f)
                        )
                    )
                    .border(0.75.dp, Color.White, CircleShape)
            )
        }
    }
}

/**
 * Recessed 3D Glass Slider with glowing gradient track and specular thumb.
 */
@Composable
fun Glass3DSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    testTag: String? = null
) {
    val palette = LocalSaylocPalette.current
    var widthPx by remember { mutableFloatStateOf(1f) }
    val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start))
        .coerceIn(0f, 1f)

    fun updateFromX(x: Float) {
        val ratio = (x / widthPx.coerceAtLeast(1f)).coerceIn(0f, 1f)
        val mapped = valueRange.start + ratio * (valueRange.endInclusive - valueRange.start)
        onValueChange(mapped)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .let { if (testTag != null) it.testTag(testTag) else it }
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(valueRange) {
                detectTapGestures { offset -> updateFromX(offset.x) }
            }
            .pointerInput(valueRange) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    updateFromX(change.position.x)
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
        ) {
            val trackHeight = 10.dp.toPx()
            val centerY = size.height / 2f
            val corner = CornerRadius(trackHeight / 2f, trackHeight / 2f)
            val thumbRadius = 12.dp.toPx()
            val usableWidth = (size.width - thumbRadius * 2f).coerceAtLeast(1f)
            val thumbX = thumbRadius + usableWidth * fraction

            // Recessed groove
            drawRoundRect(
                color = if (palette.isDark) Color(0xFF0A0724) else Color(0xFFCBD5F5),
                topLeft = Offset(thumbRadius, centerY - trackHeight / 2f),
                size = Size(usableWidth, trackHeight),
                cornerRadius = corner
            )

            // Luminous active bar
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(palette.accentStart, palette.accentEnd, palette.rimGlow)
                ),
                topLeft = Offset(thumbRadius, centerY - trackHeight / 2f),
                size = Size((thumbX - thumbRadius).coerceAtLeast(trackHeight), trackHeight),
                cornerRadius = corner
            )

            // Outer thumb glow
            drawCircle(
                color = palette.rimGlow.copy(alpha = 0.35f),
                radius = thumbRadius + 5.dp.toPx(),
                center = Offset(thumbX, centerY)
            )
            // Specular 3D sphere thumb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color(0xFFDBEAFE), palette.accentEnd),
                    center = Offset(thumbX - 4.dp.toPx(), centerY - 4.dp.toPx()),
                    radius = thumbRadius * 1.4f
                ),
                radius = thumbRadius,
                center = Offset(thumbX, centerY)
            )
        }
    }
}

data class PillNavItem(
    val id: String,
    val label: String,
    val icon: ImageVector
)

/**
 * Floating Pill-Shaped 3D Glass Bottom Navigation with spring-sliding glowing selector
 * and full WindowInsets.navigationBars protection.
 */
@Composable
fun FloatingPillNav(
    items: List<PillNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalSaylocPalette.current
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(32.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .shadow(
                    elevation = 18.dp,
                    shape = shape,
                    ambientColor = palette.cardShadow,
                    spotColor = palette.accentStart.copy(alpha = 0.45f)
                )
                .clip(shape)
                .background(palette.pillBg)
                .border(
                    width = 1.25.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.42f),
                            palette.rimGlow.copy(alpha = 0.35f),
                            palette.accentStart.copy(alpha = 0.35f)
                        )
                    ),
                    shape = shape
                )
                .padding(6.dp)
        ) {
            val itemWidth = maxWidth / items.size.coerceAtLeast(1)
            val indicatorOffset by animateDpAsState(
                targetValue = itemWidth * selectedIndex,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "pillIndicator"
            )

            // Sliding 3D selector pill
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(itemWidth)
                    .fillMaxHeight()
                    .shadow(8.dp, RoundedCornerShape(26.dp), spotColor = palette.accentStart)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(palette.accentStart, palette.accentEnd)
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(26.dp)
                    )
            )

            Row(modifier = Modifier.fillMaxSize()) {
                items.forEachIndexed { idx, item ->
                    val isSelected = idx == selectedIndex
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag("nav_${item.id}")
                            .clip(RoundedCornerShape(26.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (!isSelected) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelect(idx)
                                }
                            },
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (isSelected) Color.White else palette.textSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) Color.White else palette.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
