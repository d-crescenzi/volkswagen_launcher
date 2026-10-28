package com.crescenzi.volkswagen_launcher.widgets

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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

// Playing shape: a single rounded 6-point star. Paused: a plain circle. Both carry the
// same vertex count so the circle↔star morph matches features 1:1 and never folds.
private val PLAY_STAR get() = RoundedPolygon.star(
    numVerticesPerRadius = 6,
    radius = 1f,
    innerRadius = 0.65f,
    rounding = CornerRounding(0.4f),
    innerRounding = CornerRounding(0.4f)
)

/**
 * Central transport button. While [playing] it is a 6-point star filled with the accent;
 * paused, it relaxes into a plain circle. The play/pause glyph crossfades on top.
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
    val morph = remember { Morph(RoundedPolygon.circle(numVertices = 12), PLAY_STAR) }

    // 1 = circle (paused), 0 = star (playing).
    val circleness = remember { Animatable(if (playing) 0f else 1f) }
    LaunchedEffect(playing) {
        circleness.animateTo(
            targetValue = if (playing) 0f else 1f,
            animationSpec = spring(
                dampingRatio = if (playing) Spring.DampingRatioLowBouncy else Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }

    fun transportShape(): Shape = MorphShape(morph, (1f - circleness.value).coerceIn(0f, 1f))

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
                .graphicsLayer {
                    shape = transportShape()
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
