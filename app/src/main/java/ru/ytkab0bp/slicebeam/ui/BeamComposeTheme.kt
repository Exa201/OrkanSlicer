package ru.ytkab0bp.slicebeam.ui

import android.graphics.Color as AndroidColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.graphics.ColorUtils
import ru.ytkab0bp.slicebeam.theme.ThemesRepo
import ru.ytkab0bp.slicebeam.utils.Prefs

/** Maps the app's selected light/dark palette into the shared Compose Material theme. */
val LocalBeamThemeRevision = staticCompositionLocalOf { 0 }
private val LocalBeamTextScaleApplied = staticCompositionLocalOf { false }
/** System accessibility font scale for geometry decisions, excluding the app's text-only zoom. */
val LocalBeamLayoutFontScale = staticCompositionLocalOf { 1f }

@Composable
fun BeamComposeTheme(themeRevision: Int = 0, accentOverride: Color? = null, content: @Composable () -> Unit) {
    val inheritedDensity = LocalDensity.current
    val textScaleAlreadyApplied = LocalBeamTextScaleApplied.current
    val layoutFontScale = if (textScaleAlreadyApplied) LocalBeamLayoutFontScale.current else inheritedDensity.fontScale
    val textScale = Prefs.getPrefs()?.let { Prefs.getUiTextScale() } ?: 1.0f
    val appDensity = remember(inheritedDensity, textScale, textScaleAlreadyApplied) {
        if (textScaleAlreadyApplied) inheritedDensity
        else Density(inheritedDensity.density, inheritedDensity.fontScale * textScale)
    }
    val requestedBackground = Color(ThemesRepo.getColor(android.R.attr.windowBackground))
    val dark = ColorUtils.calculateLuminance(requestedBackground.toArgbCompat()) < 0.5
    val background = if (dark) ColorFlow.background else requestedBackground
    val foreground = if (dark) ColorFlow.textPrimary else Color(ThemesRepo.getColor(android.R.attr.textColorPrimary))
    val secondary = if (dark) ColorFlow.textSecondary else Color(ThemesRepo.getColor(android.R.attr.textColorSecondary))
    val accent = accentOverride ?: if (dark) ColorFlow.model else Color(ThemesRepo.getColor(android.R.attr.colorAccent))
    val onAccent = if (ColorUtils.calculateLuminance(accent.toArgbCompat()) > 0.179) Color.Black else Color.White
    val colors = if (dark) {
        darkColorScheme(
            primary = accent,
            onPrimary = onAccent,
            background = background,
            onBackground = foreground,
            surface = ColorFlow.surface,
            surfaceDim = ColorFlow.background,
            surfaceBright = ColorFlow.elevated,
            surfaceContainerLowest = ColorFlow.background,
            surfaceContainerLow = ColorFlow.surface,
            surfaceContainer = ColorFlow.surface,
            surfaceContainerHigh = ColorFlow.elevated,
            surfaceContainerHighest = ColorFlow.elevated,
            onSurface = foreground,
            onSurfaceVariant = secondary,
            surfaceVariant = ColorFlow.elevated,
            outline = ColorFlow.outline,
            error = ColorFlow.error,
            onError = ColorFlow.background,
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = onAccent,
            background = background,
            onBackground = foreground,
            surface = background,
            onSurface = foreground,
            onSurfaceVariant = secondary,
        )
    }
    CompositionLocalProvider(
        LocalDensity provides appDensity,
        LocalBeamLayoutFontScale provides layoutFontScale,
        LocalBeamThemeRevision provides themeRevision,
        LocalBeamTextScaleApplied provides true,
    ) {
        MaterialTheme(colorScheme = colors, content = content)
    }
}

private fun Color.toArgbCompat(): Int =
    AndroidColor.argb((alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())

/** Keeps the user's accent hue while ensuring accent labels remain readable on the app surface. */
private fun ensureAccentContrast(accent: Int, background: Int): Int {
    val opaqueAccent = ColorUtils.setAlphaComponent(accent, 0xFF)
    if (ColorUtils.calculateContrast(opaqueAccent, background) >= MIN_ACCENT_TEXT_CONTRAST) return opaqueAccent

    val target = if (ColorUtils.calculateContrast(AndroidColor.BLACK, background) >=
        ColorUtils.calculateContrast(AndroidColor.WHITE, background)
    ) AndroidColor.BLACK else AndroidColor.WHITE

    var lower = 0f
    var upper = 1f
    repeat(12) {
        val amount = (lower + upper) / 2f
        val candidate = ColorUtils.blendARGB(opaqueAccent, target, amount)
        if (ColorUtils.calculateContrast(candidate, background) >= MIN_ACCENT_TEXT_CONTRAST) upper = amount
        else lower = amount
    }
    return ColorUtils.blendARGB(opaqueAccent, target, upper)
}

private const val MIN_ACCENT_TEXT_CONTRAST = 4.5
