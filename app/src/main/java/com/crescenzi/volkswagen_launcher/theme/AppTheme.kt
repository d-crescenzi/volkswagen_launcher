package com.crescenzi.volkswagen_launcher.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Theme colours crossfade when the light/night switch is flipped.
private const val THEME_FADE_MS = 400

val LocalIsDark = staticCompositionLocalOf { false }

@Composable
fun AppTheme(dark: Boolean = false, content: @Composable () -> Unit) {
    val target = if (dark) NightPalette else LightPalette
    val spec = tween<Color>(THEME_FADE_MS)

    val canvas by animateColorAsState(target.canvas, spec, label = "canvas")
    val card by animateColorAsState(target.card, spec, label = "card")
    val cardCircle by animateColorAsState(target.cardCircle, spec, label = "cardCircle")
    val ink by animateColorAsState(target.ink, spec, label = "ink")
    val inkMuted by animateColorAsState(target.inkMuted, spec, label = "inkMuted")
    val track by animateColorAsState(target.track, spec, label = "track")
    val primary by animateColorAsState(target.primary, spec, label = "primary")
    val onPrimary by animateColorAsState(target.onPrimary, spec, label = "onPrimary")

    val palette = AppPalette(canvas, card, cardCircle, ink, inkMuted, track, primary, onPrimary)

    val colorScheme = if (dark) {
        darkColorScheme(
            primary = target.primary,
            onPrimary = target.onPrimary,
            background = target.canvas,
            onBackground = target.ink,
            surface = target.card,
            onSurface = target.ink,
            onSurfaceVariant = target.inkMuted,
        )
    } else {
        lightColorScheme(
            primary = target.primary,
            onPrimary = target.onPrimary,
            background = target.canvas,
            onBackground = target.ink,
            surface = target.card,
            onSurface = target.ink,
            onSurfaceVariant = target.inkMuted,
        )
    }

    CompositionLocalProvider(LocalAppPalette provides palette, LocalIsDark provides dark) {
        MaterialTheme(colorScheme = colorScheme, typography = AppType, content = content)
    }
}
