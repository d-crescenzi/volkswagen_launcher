package com.crescenzi.volkswagen_launcher.widgets

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.rotate
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
import kotlin.math.roundToInt

private const val SPIN_MS = 12_000
private const val MORPH_STEP_MS = 2_600
private const val MORPH_HOLD_FRACTION = 0.4f

// Lobe counts the star cycles through. Big jumps (not +2 steps) so the change reads on the
// low-res car screen; a per-shape circle of the matching vertex count keeps the paused
// relax clean.
private val PLAY_POINTS = listOf(5, 11, 6, 12, 7, 10)

// Each playing shape is a full star (high inner radius, so it stays as big as the paused
// circle) with as many lobes as [points].
private fun playStar(points: Int) = RoundedPolygon.star(
    numVerticesPerRadius = points,
    radius = 1f,
    innerRadius = 0.82f,
    rounding = CornerRounding(0.4f),
    innerRounding = CornerRounding(0.4f)
)

/**
 * Central transport button. While [playing] it spins and morphs through a set of 8-point
 * stars filled with the accent; paused, it relaxes into a plain circle. The play/pause
 * glyph crossfades on top. The shape is drawn as a filled, anti-aliased path and rotated
 * in the draw scope, never clipped — a rotating outline clip is hard-edged and glitches.
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
    val (cycleMorphs, circleMorphs) = remember {
        val polygons = PLAY_POINTS.map { playStar(it) }
        polygons.indices.map { Morph(polygons[it], polygons[(it + 1) % polygons.size]) } to
            PLAY_POINTS.mapIndexed { i, n -> Morph(RoundedPolygon.circle(numVertices = 2 * n), polygons[i]) }
    }

    // 1 = circle (paused), 0 = star (playing). While playing the star spins and cycles.
    val circleness = remember { Animatable(if (playing) 0f else 1f) }
    val spin = remember { Animatable(0f) }
    val cycle = remember { Animatable(0f) }
    LaunchedEffect(playing) {
        if (playing) {
            coroutineScope {
                launch {
                    circleness.animateTo(0f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow))
                }
                launch {
                    while (true) {
                        spin.snapTo(spin.value % 360f)
                        spin.animateTo(spin.value + 360f, tween(SPIN_MS, easing = LinearEasing))
                    }
                }
                launch {
                    while (true) {
                        cycle.snapTo(cycle.value % cycleMorphs.size)
                        cycle.animateTo(cycle.value + 1f, tween(MORPH_STEP_MS, easing = LinearEasing))
                    }
                }
            }
        } else {
            circleness.animateTo(1f, spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow))
        }
    }

    // Current frame: circle↔current star while relaxing, else the breathing cycle.
    fun transportPath(canvas: Size): Path {
        val toCircle = circleness.value
        return if (toCircle > 0f) {
            circleMorphs[cycle.value.roundToInt() % circleMorphs.size].toPath((1f - toCircle).coerceIn(0f, 1f), canvas)
        } else {
            val index = cycle.value.toInt() % cycleMorphs.size
            val step = (cycle.value - cycle.value.toInt() - MORPH_HOLD_FRACTION) / (1f - MORPH_HOLD_FRACTION)
            cycleMorphs[index].toPath(FastOutSlowInEasing.transform(step.coerceIn(0f, 1f)), canvas)
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
            // Stable circle clip for the touch area; the shape sits inside it.
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
        // Grey when idle, accent while playing — the colour read happens at draw time.
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    rotate(spin.value % 360f) {
                        drawPath(transportPath(this.size), color = lerp(fillPlaying, fillPaused, circleness.value))
                    }
                }
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
