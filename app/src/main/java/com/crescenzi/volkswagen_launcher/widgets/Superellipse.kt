package com.crescenzi.volkswagen_launcher.widgets

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sign

private const val STEPS = 96

// Superellipse (Lamé curve): exponent 2 is a circle, 4 is an iOS-style squircle,
// higher reads boxier. Animating the exponent morphs smoothly between them.
class SuperellipseShape(private val exponent: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val a = size.width / 2f
        val b = size.height / 2f
        val k = 2f / exponent
        val path = Path()
        for (i in 0..STEPS) {
            val t = i.toFloat() / STEPS * 2f * PI.toFloat()
            val c = cos(t)
            val s = sin(t)
            val x = a + a * sign(c) * c.absoluteValue.pow(k)
            val y = b + b * sign(s) * s.absoluteValue.pow(k)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}
