package com.crescenzi.volkswagen_launcher.presentation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.crescenzi.volkswagen_launcher.theme.LocalIsDark

// Soft shadow that lifts the "island" cards off the canvas in light mode; in night
// the cards separate by luminance, so the shadow fades to nothing.
private val ShadowColor = Color(0x30121212)

@Composable
fun Modifier.islandShadow(shape: Shape): Modifier {
    val dark = LocalIsDark.current
    val elevation by animateDpAsState(if (dark) 0.dp else 14.dp, label = "islandShadow")
    return this.shadow(elevation, shape, clip = false, ambientColor = ShadowColor, spotColor = ShadowColor)
}
