package com.crescenzi.volkswagen_launcher.widgets

import androidx.compose.animation.core.Animatable
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
import androidx.compose.ui.graphics.Color
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

private const val SPIN_MS = 12_000

// Playing shape: a rounded 8-point star, kept plump (high inner radius) so it fills the
// button like the paused circle. Paused: a plain circle. Both carry the same vertex count
// so the circle↔star morph matches features 1:1 and never folds.
private val PLAY_STAR get() = RoundedPolygon.star(
    numVerticesPerRadius = 8,
    radius = 1f,
    innerRadius = 0.8f,
    rounding = CornerRounding(0.5f),
    innerRounding = CornerRounding(0.5f)
)

/**
 * Central transport button. While [playing] it is an 8-point star filled with the accent
 * that spins steadily; paused, it relaxes into a plain circle. The play/pause glyph
 * crossfades on top.
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
    val morph = remember { Morph(RoundedPolygon.circle(numVertices = 16), PLAY_STAR) }

    // 1 = circle (paused), 0 = star (playing). While playing the star spins steadily.
    val circleness = remember { Animatable(if (playing) 0f else 1f) }
    val spin = remember { Animatable(0f) }
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
            }
        } else {
            circleness.animateTo(1f, spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow))
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
        // Drawn as a filled, anti-aliased path and rotated here, rather than clipping a
        // layer: a rotating outline clip is hard-edged and shimmers on the car screen.
        // Grey when idle, accent while playing — the colour read happens at draw time.
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    val progress = (1f - circleness.value).coerceIn(0f, 1f)
                    rotate(spin.value % 360f) {
                        drawPath(morph.toPath(progress, this.size), color = lerp(fillPlaying, fillPaused, circleness.value))
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
