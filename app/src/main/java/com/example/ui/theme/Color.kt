package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.data.AccentTheme

// Signature Sayloc Liquid-Glass Navy Palette
val DeepNavy = Color(0xFF070522)
val DeepNavyElevated = Color(0xFF100C36)
val DeepNavySurface = Color(0xFF151142)
val LightFrostBg = Color(0xFFEEF1FF)
val LightFrostSurface = Color(0xFFF7F9FF)

// Semantic Neon States
val ListeningCyan = Color(0xFF38BDF8)
val DetectedEmerald = Color(0xFF34D399)
val LockCrimson = Color(0xFFF43F5E)
val WarningAmber = Color(0xFFFBBF24)

data class SaylocGlassPalette(
    val isDark: Boolean,
    val bgTop: Color,
    val bgBottom: Color,
    val orbAmbientPrimary: Color,
    val orbAmbientSecondary: Color,
    val accentStart: Color,
    val accentEnd: Color,
    val rimGlow: Color,
    val cardTop: Color,
    val cardBottom: Color,
    val cardBorderTop: Color,
    val cardBorderBottom: Color,
    val cardShadow: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val pillBg: Color,
    val dangerStart: Color,
    val dangerEnd: Color
)

fun paletteFor(accent: AccentTheme, isDark: Boolean): SaylocGlassPalette {
    val (start, end, rim) = when (accent) {
        AccentTheme.ELECTRIC_VIOLET -> Triple(
            Color(0xFFA855F7),
            Color(0xFF2563EB),
            Color(0xFF60A5FA)
        )
        AccentTheme.CYAN_PULSE -> Triple(
            Color(0xFF06B6D4),
            Color(0xFF2563EB),
            Color(0xFF67E8F9)
        )
        AccentTheme.NEON_MAGENTA -> Triple(
            Color(0xFFEC4899),
            Color(0xFF7C3AED),
            Color(0xFFF472B6)
        )
        AccentTheme.AURORA_INDIGO -> Triple(
            Color(0xFF6366F1),
            Color(0xFF14B8A6),
            Color(0xFF5EEAD4)
        )
    }

    return if (isDark) {
        SaylocGlassPalette(
            isDark = true,
            bgTop = Color(0xFF0C0830),
            bgBottom = DeepNavy,
            orbAmbientPrimary = start.copy(alpha = 0.42f),
            orbAmbientSecondary = end.copy(alpha = 0.45f),
            accentStart = start,
            accentEnd = end,
            rimGlow = rim,
            cardTop = Color(0xFF231D5B).copy(alpha = 0.72f),
            cardBottom = Color(0xFF100C34).copy(alpha = 0.86f),
            cardBorderTop = Color.White.copy(alpha = 0.34f),
            cardBorderBottom = rim.copy(alpha = 0.28f),
            cardShadow = Color(0xFF020110).copy(alpha = 0.85f),
            textPrimary = Color(0xFFF8FAFC),
            textSecondary = Color(0xFFB4B9E0),
            pillBg = Color(0xFF14103B).copy(alpha = 0.90f),
            dangerStart = Color(0xFFF43F5E),
            dangerEnd = Color(0xFF9333EA)
        )
    } else {
        SaylocGlassPalette(
            isDark = false,
            bgTop = Color(0xFFF3F5FF),
            bgBottom = Color(0xFFE1E7FF),
            orbAmbientPrimary = start.copy(alpha = 0.24f),
            orbAmbientSecondary = end.copy(alpha = 0.22f),
            accentStart = start,
            accentEnd = end,
            rimGlow = end,
            cardTop = Color.White.copy(alpha = 0.86f),
            cardBottom = Color(0xFFE8EDFF).copy(alpha = 0.90f),
            cardBorderTop = Color.White.copy(alpha = 0.96f),
            cardBorderBottom = start.copy(alpha = 0.26f),
            cardShadow = Color(0xFF1E1B4B).copy(alpha = 0.18f),
            textPrimary = Color(0xFF0E0B2B),
            textSecondary = Color(0xFF474C78),
            pillBg = Color.White.copy(alpha = 0.92f),
            dangerStart = Color(0xFFE11D48),
            dangerEnd = Color(0xFF7C3AED)
        )
    }
}
