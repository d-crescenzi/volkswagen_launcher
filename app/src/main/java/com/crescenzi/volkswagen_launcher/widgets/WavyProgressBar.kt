package com.crescenzi.volkswagen_launcher.widgets

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.crescenzi.volkswagen_launcher.theme.AppColor
import kotlin.math.PI
import kotlin.math.sin

private const val TWO_PI = (2.0 * PI).toFloat()

// Expressive-style progress: the played part is a sine wave that scrolls while
// [playing], the rest a flat track. Keeps the launcher palette.
@Composable
fun WavyProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    playing: Boolean = true,
    color: Color = AppColor.current.primary,
    trackColor: Color = AppColor.current.track,
    amplitude: Dp = 3.dp,
    wavelength: Dp = 18.dp,
    stroke: Dp = 4.dp
) {
    val phase = if (playing) {
        val transition = rememberInfiniteTransition(label = "wavy")
        val animated by transition.animateFloat(
            initialValue = 0f,
            targetValue = TWO_PI,
            animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
            label = "wavyPhase"
        )
        animated
    } else {
        0f
    }

    Canvas(modifier) {
        val w = size.width
        val cy = size.height / 2f
        val sw = stroke.toPx()
        val amp = amplitude.toPx()
        val wl = wavelength.toPx().coerceAtLeast(1f)
        val headX = w * progress.coerceIn(0f, 1f)
        val gap = sw * 1.6f

        if (headX > 0.5f) {
            val path = Path()
            path.moveTo(0f, cy + amp * sin(phase))
            var x = 0f
            while (x <= headX) {
                path.lineTo(x, cy + amp * sin(TWO_PI * x / wl + phase))
                x += 2f
            }
            drawPath(path, color = color, style = Stroke(width = sw, cap = StrokeCap.Round))
        }

        val trackStart = (headX + gap).coerceAtMost(w)
        if (trackStart < w) {
            drawLine(trackColor, Offset(trackStart, cy), Offset(w, cy), strokeWidth = sw, cap = StrokeCap.Round)
        }
    }
}
