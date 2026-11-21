package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.data.AccentTheme
import com.example.data.ThemeMode

val LocalSaylocPalette = staticCompositionLocalOf {
    paletteFor(AccentTheme.ELECTRIC_VIOLET, isDark = true)
}

val LocalReduceEffects = staticCompositionLocalOf { false }

@Composable
fun SaylocTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    accentTheme: AccentTheme = AccentTheme.ELECTRIC_VIOLET,
    reduceEffects: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.AUTO -> isSystemInDarkTheme()
    }
    val glassPalette = paletteFor(accentTheme, isDark)

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = glassPalette.accentStart,
            onPrimary = Color.White,
            secondary = glassPalette.accentEnd,
            onSecondary = Color.White,
            tertiary = glassPalette.rimGlow,
            background = DeepNavy,
            onBackground = glassPalette.textPrimary,
            surface = DeepNavyElevated,
            onSurface = glassPalette.textPrimary,
            surfaceVariant = DeepNavySurface,
            onSurfaceVariant = glassPalette.textSecondary,
            error = LockCrimson,
            onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = glassPalette.accentStart,
            onPrimary = Color.White,
            secondary = glassPalette.accentEnd,
            onSecondary = Color.White,
            tertiary = glassPalette.rimGlow,
            background = LightFrostBg,
            onBackground = glassPalette.textPrimary,
            surface = LightFrostSurface,
            onSurface = glassPalette.textPrimary,
            surfaceVariant = Color(0xFFE2E7FF),
            onSurfaceVariant = glassPalette.textSecondary,
            error = LockCrimson,
            onError = Color.White
        )
    }

    CompositionLocalProvider(
        LocalSaylocPalette provides glassPalette,
        LocalReduceEffects provides reduceEffects
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
