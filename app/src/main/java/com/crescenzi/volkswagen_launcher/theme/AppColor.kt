package com.crescenzi.volkswagen_launcher.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// The only place hex colours live. Two palettes (light/night) feed the animated
// palette provided by AppTheme; consumers read the current one via AppColor.current.
@Immutable
data class AppPalette(
    val canvas: Color,      // page behind the cards
    val card: Color,        // "island" surfaces
    val cardCircle: Color,  // circles for buttons/icons inside cards
    val ink: Color,         // primary text, icons
    val inkMuted: Color,    // secondary text, times, section labels
    val track: Color,       // inactive track (progress, volume)
    val primary: Color,     // the single accent
    val onPrimary: Color,   // glyph over primary
)

val LightPalette = AppPalette(
    canvas = Color(0xFFECEDF0),
    card = Color(0xFFFFFFFF),
    cardCircle = Color(0xFFEEEFF1),
    ink = Color(0xFF121212),
    inkMuted = Color(0xFF6B7075),
    track = Color(0xFFD9DBDE),
    primary = Color(0xFF007AFF),
    onPrimary = Color(0xFFFFFFFF),
)

val NightPalette = AppPalette(
    canvas = Color(0xFF000000),
    card = Color(0xFF151619),
    cardCircle = Color(0xFF26272B),
    ink = Color(0xFFECEDEF),
    inkMuted = Color(0xFF8E949A),
    track = Color(0xFF36383C),
    primary = Color(0xFF0A84FF),
    onPrimary = Color(0xFFFFFFFF),
)

val LocalAppPalette = staticCompositionLocalOf { LightPalette }

// Accessor so call sites stay terse: AppColor.current.ink
object AppColor {
    val current: AppPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalAppPalette.current
}
