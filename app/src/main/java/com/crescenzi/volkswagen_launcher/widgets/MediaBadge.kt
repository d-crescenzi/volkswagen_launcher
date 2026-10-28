package com.crescenzi.volkswagen_launcher.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import com.crescenzi.volkswagen_launcher.theme.AppColor

// Rounded 8-point star, matching the transport button's playing shape.
private val BADGE_SHAPE get() = RoundedPolygon.star(
    numVerticesPerRadius = 8,
    innerRadius = 0.8f,
    rounding = CornerRounding(0.5f),
    innerRounding = CornerRounding(0.5f)
)

/** Static star emblem with an upright glyph on top. The media empty-state badge. */
@Composable
fun MediaBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 84.dp,
    iconSize: Dp = 40.dp,
    fill: Color = AppColor.current.primary,
    glyph: Color = AppColor.current.onPrimary,
) {
    val polygon = remember { BADGE_SHAPE }
    Box(contentAlignment = Alignment.Center, modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind { drawPath(polygon.toPath(this.size), color = fill) }
        )
        Icon(icon, contentDescription = null, tint = glyph, modifier = Modifier.size(iconSize))
    }
}
