package com.crescenzi.volkswagen_launcher.widgets

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import com.crescenzi.volkswagen_launcher.theme.AppColor
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private const val BADGE_SPIN_MS = 16_000
private const val BADGE_MORPH_STEP_MS = 2_400
private const val BADGE_MORPH_HOLD_FRACTION = 0.4f

// The five expressive shapes the badge rotates and morphs through.
private val BADGE_SHAPES
    get() = listOf(
        RoundedPolygon(numVertices = 4, rounding = CornerRounding(0.35f)),
        RoundedPolygon(numVertices = 5, rounding = CornerRounding(0.3f)),
        RoundedPolygon(numVertices = 6, rounding = CornerRounding(0.3f)),
        RoundedPolygon.star(numVerticesPerRadius = 4, innerRadius = 0.55f, rounding = CornerRounding(0.25f)),
        RoundedPolygon.star(numVerticesPerRadius = 6, innerRadius = 0.6f, rounding = CornerRounding(0.3f))
    )

/**
 * Slowly spinning blob that morphs through [BADGE_SHAPES] with an upright glyph on
 * top. The empty-state emblem, echoing the transport button's spin-and-morph.
 */
@Composable
fun MorphingBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 84.dp,
    iconSize: Dp = 40.dp,
    fill: Color = AppColor.current.primary,
    glyph: Color = AppColor.current.onPrimary,
) {
    val morphs = remember {
        val polygons = BADGE_SHAPES
        polygons.indices.map { Morph(polygons[it], polygons[(it + 1) % polygons.size]) }
    }
    val cycle = remember { Animatable(0f) }
    val spin = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch {
                while (true) {
                    spin.snapTo(spin.value % 360f)
                    spin.animateTo(spin.value + 360f, tween(BADGE_SPIN_MS, easing = LinearEasing))
                }
            }
            launch {
                while (true) {
                    cycle.snapTo(cycle.value % morphs.size)
                    cycle.animateTo(cycle.value + 1f, tween(BADGE_MORPH_STEP_MS, easing = LinearEasing))
                }
            }
        }
    }

    fun badgeShape(): Shape {
        val index = cycle.value.toInt() % morphs.size
        val step = (cycle.value - cycle.value.toInt() - BADGE_MORPH_HOLD_FRACTION) / (1f - BADGE_MORPH_HOLD_FRACTION)
        return MorphShape(morphs[index], FastOutSlowInEasing.transform(step.coerceIn(0f, 1f)))
    }

    Box(contentAlignment = Alignment.Center, modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    rotationZ = spin.value % 360f
                    shape = badgeShape()
                    clip = true
                }
                .drawBehind { drawRect(fill) }
        )
        Icon(icon, contentDescription = null, tint = glyph, modifier = Modifier.size(iconSize))
    }
}
