package com.crescenzi.volkswagen_launcher.widgets

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.graphics.shapes.Cubic
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon

// Builds a closed Compose Path from a run of cubics. Polygons are authored around the
// origin with radius 1, so [fitTo] maps the [-1,1] box onto [0,size].
private inline fun cubicsToPath(build: ((Cubic) -> Unit) -> Unit): Path {
    val path = Path()
    var first = true
    build { c ->
        if (first) {
            path.moveTo(c.anchor0X, c.anchor0Y)
            first = false
        }
        path.cubicTo(c.control0X, c.control0Y, c.control1X, c.control1Y, c.anchor1X, c.anchor1Y)
    }
    path.close()
    return path
}

private fun Path.fitTo(size: Size): Path {
    val m = Matrix()
    m.scale(size.width / 2f, size.height / 2f)
    m.translate(1f, 1f)
    transform(m)
    return this
}

// Filled path for one frame of [this] morph at [progress] (0 = start, 1 = end), fitted to
// [size] and centred on it. Meant to be drawn with drawPath (anti-aliased) rather than
// used as a layer clip, which is hard-edged and glitches when rotated.
fun Morph.toPath(progress: Float, size: Size): Path =
    cubicsToPath { emit -> forEachCubic(progress) { emit(it) } }.fitTo(size)

// Filled path for a single static [this] polygon, fitted to [size].
fun RoundedPolygon.toPath(size: Size): Path =
    cubicsToPath { emit -> cubics.forEach { emit(it) } }.fitTo(size)
