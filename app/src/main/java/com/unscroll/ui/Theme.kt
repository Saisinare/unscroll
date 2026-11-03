package com.unscroll.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class Palette(
    val bg: Color,
    val card: Color,
    val cardAlt: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    /** High-contrast surface for primary buttons and hero cards. */
    val inverse: Color,
    val onInverse: Color,
    val nav: Color,
    val navIcon: Color,
    val good: Color = Color(0xFF3CCB6E),
    val warn: Color = Color(0xFFF4B740),
    val bad: Color = Color(0xFFF0645A),
    val mint: Color,
    val lilac: Color,
    val peach: Color,
    val sky: Color,
)

private val LightPalette = Palette(
    bg = Color(0xFFEFEFEC), card = Color(0xFFFFFFFF), cardAlt = Color(0xFFF6F6F3),
    ink = Color(0xFF111111), muted = Color(0xFF8C8C87), line = Color(0xFFE3E3DF),
    inverse = Color(0xFF141414), onInverse = Color(0xFFFFFFFF),
    nav = Color(0xFF141414), navIcon = Color(0xFFB9B9B5),
    mint = Color(0xFFE1F4E6), lilac = Color(0xFFEAE8FB), peach = Color(0xFFFBE9DE), sky = Color(0xFFE2EDF9),
)

private val DarkPalette = Palette(
    bg = Color(0xFF0C0C0D), card = Color(0xFF19191B), cardAlt = Color(0xFF232326),
    ink = Color(0xFFF3F3F0), muted = Color(0xFF8E8E92), line = Color(0xFF2C2C30),
    inverse = Color(0xFFF3F3F0), onInverse = Color(0xFF111111),
    nav = Color(0xFF232326), navIcon = Color(0xFF8E8E92),
    mint = Color(0xFF1A2B20), lilac = Color(0xFF23223A), peach = Color(0xFF2F241C), sky = Color(0xFF1B2533),
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

val palette: Palette
    @Composable @ReadOnlyComposable get() = LocalPalette.current

private val UnscrollType = Typography().let { t ->
    t.copy(
        displayLarge = t.displayLarge.copy(fontSize = 60.sp, lineHeight = 64.sp, fontWeight = FontWeight.Medium, letterSpacing = (-2.5).sp),
        displaySmall = t.displaySmall.copy(fontSize = 42.sp, lineHeight = 46.sp, fontWeight = FontWeight.Medium, letterSpacing = (-1.5).sp),
        headlineLarge = t.headlineLarge.copy(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1).sp),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = t.titleSmall.copy(letterSpacing = 0.sp),
        bodyLarge = t.bodyLarge.copy(letterSpacing = (-0.1).sp),
        bodyMedium = t.bodyMedium.copy(letterSpacing = (-0.05).sp),
        bodySmall = t.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.sp),
        labelLarge = t.labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
        labelMedium = t.labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
    )
}

@Composable
fun UnscrollTheme(content: @Composable () -> Unit) {
    val p = if (isSystemInDarkTheme()) DarkPalette else LightPalette
    val base = if (p == DarkPalette) darkColorScheme() else lightColorScheme()
    val scheme = base.copy(
        primary = p.ink, onPrimary = p.onInverse, background = p.bg, onBackground = p.ink,
        surface = p.card, onSurface = p.ink, surfaceVariant = p.cardAlt, onSurfaceVariant = p.muted,
        surfaceContainerLow = p.card, surfaceContainer = p.card, surfaceContainerHigh = p.card,
        outline = p.line, outlineVariant = p.line,
    )
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme, typography = UnscrollType, content = content)
    }
}
