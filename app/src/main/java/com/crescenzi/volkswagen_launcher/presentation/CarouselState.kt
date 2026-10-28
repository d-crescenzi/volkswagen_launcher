package com.crescenzi.volkswagen_launcher.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// Width animation of the carousel: M3 emphasized easing over 500ms.
private const val SNAP_MS = 500
private val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)

// A full drag across the carousel runs roughly this far; release snaps past 25%.
private val DRAG_SPAN = 760.dp

// p = 0 → media in front, p = 1 → apps in front. While a drag is in flight the live
// finger position wins; otherwise the snap animation drives the value.
class CarouselState(private val scope: CoroutineScope) {
    private val anim = Animatable(0f)
    private val snapSpec = tween<Float>(SNAP_MS, easing = Emphasized)

    var page by mutableIntStateOf(0)
        private set

    private var dragging by mutableStateOf(false)
    private var dragP by mutableFloatStateOf(0f)
    private var startPage = 0

    val p: Float get() = if (dragging) dragP else anim.value

    fun goTo(target: Int) {
        page = target
        scope.launch {
            dragging = false
            anim.animateTo(target.toFloat(), snapSpec)
        }
    }

    internal fun onDragStarted() {
        startPage = page
        dragP = anim.value
        dragging = true
        scope.launch { anim.stop() }
    }

    internal fun onDrag(fraction: Float) {
        dragP = (dragP - fraction).coerceIn(0f, 1f)
    }

    internal fun onDragStopped() {
        val cur = dragP
        val target = if (startPage == 0) (if (cur > 0.25f) 1 else 0) else (if (cur < 0.75f) 0 else 1)
        page = target
        scope.launch {
            anim.snapTo(cur)
            dragging = false
            anim.animateTo(target.toFloat(), snapSpec)
        }
    }
}

@Composable
fun rememberCarouselState(): CarouselState {
    val scope = rememberCoroutineScope()
    return remember { CarouselState(scope) }
}

// Horizontal drag that follows the finger 1:1 and snaps on release. Taps fall
// through to the cards' own click handlers; vertical drags reach the volume slider.
@Composable
fun Modifier.carouselDrag(state: CarouselState): Modifier {
    val spanPx = with(LocalDensity.current) { DRAG_SPAN.toPx() }
    val dragState = rememberDraggableState { delta -> state.onDrag(delta / spanPx) }
    return this.draggable(
        state = dragState,
        orientation = Orientation.Horizontal,
        onDragStarted = { state.onDragStarted() },
        onDragStopped = { state.onDragStopped() }
    )
}
