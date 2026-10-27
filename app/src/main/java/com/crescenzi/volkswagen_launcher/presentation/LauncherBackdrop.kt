package com.crescenzi.volkswagen_launcher.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import com.crescenzi.volkswagen_launcher.theme.AppColor
import com.crescenzi.volkswagen_launcher.widgets.PolygonShape
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

private const val BACKDROP_ALPHA = 0.06f

private const val BACKDROP_COLUMNS = 4
private const val BACKDROP_ROWS = 2

private val BACKDROP_MIN_SIZE = 56.dp
private val BACKDROP_MAX_SIZE = 130.dp

private val MIN_DRIFT = 6.dp
private val MAX_DRIFT = 18.dp

private const val MIN_SPIN = -4f
private const val MAX_SPIN = 4f

private const val MIN_LIFE = 7f
private const val MAX_LIFE = 13f
private const val FADE_IN = 0.1f
private const val FADE_OUT = 0.3f

private const val BIRTH_SPREAD = 2.4f
private const val BIRTH_JITTER = 0.5f

private const val FRAME_CAP_SECONDS = 1f / 30f

private const val FULL_TURN = 6.2831855f

private val BACKDROP_SHAPES
    get() = listOf(
        RoundedPolygon(numVertices = 4, rounding = CornerRounding(0.35f)),
        RoundedPolygon(numVertices = 5, rounding = CornerRounding(0.3f)),
        RoundedPolygon(numVertices = 6, rounding = CornerRounding(0.3f)),
        RoundedPolygon.star(numVerticesPerRadius = 4, innerRadius = 0.55f, rounding = CornerRounding(0.25f)),
        RoundedPolygon.star(numVerticesPerRadius = 6, innerRadius = 0.6f, rounding = CornerRounding(0.3f))
    )

private fun range(from: Float, to: Float) = from + Random.nextFloat() * (to - from)

private class KeepOut(val center: Offset, val radius: Float)

private class World {
    var bounds = Size.Zero
    var keepOut = KeepOut(Offset.Zero, 0f)
    var seeded = false

    val ready: Boolean get() = bounds.width > 0f && bounds.height > 0f
}

private class Mote(private val shape: Shape, val size: Float, private val cell: Int) {
    var x = 0f
        private set
    var y = 0f
        private set
    var angle = 0f
        private set
    var alpha = 0f
        private set

    private var vx = 0f
    private var vy = 0f
    private var spin = 0f
    private var age = 0f
    private var life = 1f

    private var outline: Outline? = null

    fun outline(density: Density, direction: LayoutDirection): Outline =
        outline ?: shape.createOutline(Size(size, size), direction, density).also { outline = it }

    private fun stayInCell(bounds: Size) {
        val cellWidth = bounds.width / BACKDROP_COLUMNS
        val cellHeight = bounds.height / BACKDROP_ROWS
        val left = cellWidth * (cell % BACKDROP_COLUMNS)
        val top = cellHeight * (cell / BACKDROP_COLUMNS)
        val right = (left + cellWidth - size).coerceAtLeast(left)
        val bottom = (top + cellHeight - size).coerceAtLeast(top)

        if (x < left) {
            x = left
            vx = -vx
        } else if (x > right) {
            x = right
            vx = -vx
        }

        if (y < top) {
            y = top
            vy = -vy
        } else if (y > bottom) {
            y = bottom
            vy = -vy
        }
    }

    private fun clearKeepOut(keepOut: KeepOut, bounce: Boolean) {
        val reach = keepOut.radius + size / 2f
        var dx = x + size / 2f - keepOut.center.x
        var dy = y + size / 2f - keepOut.center.y
        var distance = hypot(dx, dy)

        if (distance >= reach) return

        if (distance < 1f) {
            val heading = range(0f, FULL_TURN)
            dx = cos(heading)
            dy = sin(heading)
            distance = 1f
        }

        val nx = dx / distance
        val ny = dy / distance
        x = keepOut.center.x + nx * reach - size / 2f
        y = keepOut.center.y + ny * reach - size / 2f

        if (!bounce) return
        val into = vx * nx + vy * ny
        if (into < 0f) {
            vx -= 2f * into * nx
            vy -= 2f * into * ny
        }
    }

    fun spawn(bounds: Size, keepOut: KeepOut, waiting: Boolean) {
        val cellWidth = bounds.width / BACKDROP_COLUMNS
        val cellHeight = bounds.height / BACKDROP_ROWS

        x = cellWidth * (cell % BACKDROP_COLUMNS) + range(0f, (cellWidth - size).coerceAtLeast(0f))
        y = cellHeight * (cell / BACKDROP_COLUMNS) + range(0f, (cellHeight - size).coerceAtLeast(0f))

        val heading = range(0f, FULL_TURN)
        val speed = range(MIN_DRIFT.value, MAX_DRIFT.value)
        vx = cos(heading) * speed
        vy = sin(heading) * speed

        angle = range(0f, 360f)
        spin = range(MIN_SPIN, MAX_SPIN)
        life = range(MIN_LIFE, MAX_LIFE)
        age = if (waiting) {
            -(cell.toFloat() / (BACKDROP_COLUMNS * BACKDROP_ROWS)) * BIRTH_SPREAD - range(0f, BIRTH_JITTER)
        } else {
            0f
        }

        clearKeepOut(keepOut, bounce = false)
    }

    fun step(dt: Float, bounds: Size, keepOut: KeepOut, scale: Float) {
        age += dt

        if (age < 0f) {
            alpha = 0f
            return
        }

        if (age >= life) {
            spawn(bounds, keepOut, waiting = false)
            return
        }

        x += vx * scale * dt
        y += vy * scale * dt
        angle += (spin + Math.toDegrees((vx * scale / (size / 2f)).toDouble()).toFloat()) * dt
        stayInCell(bounds)
        clearKeepOut(keepOut, bounce = true)

        val progress = age / life
        alpha = when {
            progress < FADE_IN -> progress / FADE_IN
            progress > 1f - FADE_OUT -> (1f - progress) / FADE_OUT
            else -> 1f
        }
    }
}

// Drifting, slowly spinning shapes behind the launcher, kept clear of a circle
// at the centre so the media controls stay legible. A quiet Ink watermark.
@Composable
fun LauncherBackdrop(modifier: Modifier = Modifier, keepOutRadius: Dp = 260.dp) {
    val density = LocalDensity.current

    var bounds by remember { mutableStateOf(Size.Zero) }
    var frame by remember { mutableLongStateOf(0L) }

    val keepOutPx = with(density) { keepOutRadius.toPx() }

    val world = remember { World() }
    world.bounds = bounds
    world.keepOut = KeepOut(Offset(bounds.width / 2f, bounds.height / 2f), keepOutPx)

    val shapes = BACKDROP_SHAPES.map { PolygonShape(it) }
    val motes = remember(density) {
        List(BACKDROP_COLUMNS * BACKDROP_ROWS) {
            val size = with(density) { range(BACKDROP_MIN_SIZE.value, BACKDROP_MAX_SIZE.value).dp.toPx() }
            Mote(shapes[it % shapes.size], size, cell = it)
        }
    }

    LaunchedEffect(motes) {
        val scale = density.density
        var previous = 0L
        while (true) {
            withFrameNanos { now ->
                if (!world.ready) return@withFrameNanos

                if (!world.seeded) {
                    world.seeded = true
                    motes.forEach { it.spawn(world.bounds, world.keepOut, waiting = true) }
                }

                val dt = if (previous == 0L) 0f else ((now - previous) / 1_000_000_000f).coerceAtMost(FRAME_CAP_SECONDS)
                previous = now
                motes.forEach { it.step(dt, world.bounds, world.keepOut, scale) }
                frame = now
            }
        }
    }

    Canvas(modifier = modifier.onSizeChanged { bounds = Size(it.width.toFloat(), it.height.toFloat()) }) {
        frame

        motes.forEach { mote ->
            withTransform({
                translate(mote.x, mote.y)
                rotate(mote.angle, pivot = Offset(mote.size / 2f, mote.size / 2f))
            }) {
                drawOutline(mote.outline(this, layoutDirection), color = AppColor.Ink, alpha = mote.alpha * BACKDROP_ALPHA)
            }
        }
    }
}
