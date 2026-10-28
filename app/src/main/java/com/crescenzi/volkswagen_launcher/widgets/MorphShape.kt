package com.crescenzi.volkswagen_launcher.widgets

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Cubic
import androidx.graphics.shapes.Morph

// Builds a closed Compose Path from a run of cubics. Polygons are authored around
// the origin with radius 1, so [fitTo] maps the [-1,1] box onto [0,size].
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

// One frame of [morph] at [progress] (0 = start polygon, 1 = end polygon).
class MorphShape(private val morph: Morph, private val progress: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = cubicsToPath { emit -> morph.forEachCubic(progress) { emit(it) } }.fitTo(size)
        return Outline.Generic(path)
    }
}
