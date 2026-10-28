package com.crescenzi.volkswagen_launcher.widgets

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.star
import com.crescenzi.volkswagen_launcher.theme.AppColor
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.roundToInt

private const val SPIN_MS = 12_000
private const val MORPH_STEP_MS = 2_000
private const val MORPH_HOLD_FRACTION = 0.35f
private const val SETTLE_MS = 450
private const val COAST_DEGREES = 40f

// Rounded "cookie" with [points] lobes, authored around the origin with radius 1.
private fun cookie(points: Int) = RoundedPolygon.star(
    numVerticesPerRadius = points,
    radius = 1f,
    innerRadius = 0.8f,
    rounding = CornerRounding(0.5f),
    innerRounding = CornerRounding(0.5f)
)

private val PLAY_SHAPES get() = listOf(cookie(9), cookie(12), cookie(8), cookie(6), cookie(10), cookie(7))

/**
 * Central transport button. While [playing] it spins and morphs through a set of
 * shapes; paused, it relaxes into a plain circle. The play/pause glyph crossfades
 * on top and stays upright. Keeps the launcher palette (Circle fill, Ink glyph).
 */
@Composable
fun MorphingPlayButton(
    playing: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 104.dp,
    iconSize: Dp = 58.dp,
    fillPlaying: Color = AppColor.current.primary,
    fillPaused: Color = AppColor.current.cardCircle,
    glyphPlaying: Color = AppColor.current.onPrimary,
    glyphPaused: Color = AppColor.current.ink,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val (cookieMorphs, circleMorphs) = remember {
        val polygons = PLAY_SHAPES
        val circle = RoundedPolygon.circle(numVertices = 12)
        polygons.indices.map { Morph(polygons[it], polygons[(it + 1) % polygons.size]) } to
            polygons.map { Morph(circle, it) }
    }

    // Underneath the wind-up/down: spin and morph cycle run while playing; on pause the
    // cycle settles to a whole shape and the spin coasts to a stop while it relaxes to a circle.
    val cycle = remember { Animatable(0f) }
    val spin = remember { Animatable(0f) }
    val circleness = remember { Animatable(if (playing) 0f else 1f) }

    LaunchedEffect(playing) {
        if (playing) {
            coroutineScope {
                launch {
                    circleness.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
                }
                launch {
                    while (true) {
                        spin.snapTo(spin.value % 360f)
                        spin.animateTo(spin.value + 360f, tween(SPIN_MS, easing = LinearEasing))
                    }
                }
                launch {
                    while (true) {
                        cycle.snapTo(cycle.value % cookieMorphs.size)
                        cycle.animateTo(cycle.value + 1f, tween(MORPH_STEP_MS, easing = LinearEasing))
                    }
                }
            }
        } else {
            coroutineScope {
                launch { cycle.animateTo(ceil(cycle.value), tween(SETTLE_MS, easing = LinearOutSlowInEasing)) }
                launch { spin.animateTo(spin.value + COAST_DEGREES, tween(SETTLE_MS, easing = LinearOutSlowInEasing)) }
                launch { circleness.animateTo(1f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)) }
            }
        }
    }

    fun badgeShape(): Shape {
        val toCircle = circleness.value
        return if (toCircle > 0f) {
            MorphShape(circleMorphs[cycle.value.roundToInt() % circleMorphs.size], (1f - toCircle).coerceIn(0f, 1f))
        } else {
            val index = cycle.value.toInt() % cookieMorphs.size
            val step = (cycle.value - cycle.value.toInt() - MORPH_HOLD_FRACTION) / (1f - MORPH_HOLD_FRACTION)
            MorphShape(cookieMorphs[index], FastOutSlowInEasing.transform(step.coerceIn(0f, 1f)))
        }
    }

    val pressed = remember { mutableStateOf(false) }
    val scale = animateFloatAsState(
        targetValue = if (pressed.value) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "playBounce"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            // Stable circle clip for the touch area; the morphing shape spins inside it.
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        awaitFirstDown(requireUnconsumed = false)
                        pressed.value = true
                        waitForUpOrCancellation()
                        pressed.value = false
                    }
                }
            }
    ) {
        // Only the fill spins; the glyphs are siblings on top, so they stay upright.
        // Grey when idle, Apple blue while playing — the colour read happens at draw time.
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    rotationZ = spin.value % 360f
                    shape = badgeShape()
                    clip = true
                }
                .drawBehind { drawRect(lerp(fillPlaying, fillPaused, circleness.value)) }
        )

        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = glyphPaused,
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer {
                    alpha = circleness.value
                    val s = 0.7f + 0.3f * circleness.value
                    scaleX = s
                    scaleY = s
                }
        )
        Icon(
            imageVector = Icons.Rounded.Pause,
            contentDescription = null,
            tint = glyphPlaying,
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer {
                    alpha = 1f - circleness.value
                    val s = 0.7f + 0.3f * (1f - circleness.value)
                    scaleX = s
                    scaleY = s
                }
        )
    }
}
