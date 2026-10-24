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
fun BeamComposeTheme(themeRevision: Int = 0, content: @Composable () -> Unit) {
    val inheritedDensity = LocalDensity.current
    val textScaleAlreadyApplied = LocalBeamTextScaleApplied.current
    val layoutFontScale = if (textScaleAlreadyApplied) LocalBeamLayoutFontScale.current else inheritedDensity.fontScale
    val textScale = Prefs.getPrefs()?.let { Prefs.getUiTextScale() } ?: 1.0f
    val appDensity = remember(inheritedDensity, textScale, textScaleAlreadyApplied) {
        if (textScaleAlreadyApplied) inheritedDensity
        else Density(inheritedDensity.density, inheritedDensity.fontScale * textScale)
    }
    val background = Color(ThemesRepo.getColor(android.R.attr.windowBackground))
    val foreground = Color(ThemesRepo.getColor(android.R.attr.textColorPrimary))
    val secondary = Color(ThemesRepo.getColor(android.R.attr.textColorSecondary))
    val accent = Color(ThemesRepo.getColor(android.R.attr.colorAccent))
    val onAccent = if (ColorUtils.calculateLuminance(accent.toArgbCompat()) > 0.179) Color.Black else Color.White
    val dark = ColorUtils.calculateLuminance(background.toArgbCompat()) < 0.5
    val colors = if (dark) {
        darkColorScheme(
            primary = accent,
            onPrimary = onAccent,
            background = background,
            onBackground = foreground,
            surface = background,
            onSurface = foreground,
            onSurfaceVariant = secondary,
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
