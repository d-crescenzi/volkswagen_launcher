package com.crescenzi.volkswagen_launcher.presentation.volume

import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeMute
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.crescenzi.volkswagen_launcher.R
import com.crescenzi.volkswagen_launcher.theme.AppColor
import kotlin.math.roundToInt

/**
 * Vertical, Material 3 Expressive-styled media volume slider. Drives the real
 * STREAM_MUSIC volume and reflects external changes (hardware keys, system UI).
 * The visible track is slim but the touch area spans the full [TOUCH_WIDTH]
 * column, so tapping or dragging anywhere down the strip adjusts the volume.
 */
@Composable
fun VolumeSlider(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val audio = remember { ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVol = remember { audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }

    var vol by remember { mutableIntStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC)) }

    // Reflect volume changes made outside the app (hardware keys, other apps).
    DisposableEffect(Unit) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                vol = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
            }
        }
        ctx.contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, observer)
        onDispose { ctx.contentResolver.unregisterContentObserver(observer) }
    }

    // Live drag fraction so the handle tracks the finger even between volume steps.
    var dragFraction by remember { mutableFloatStateOf(vol.toFloat() / maxVol) }
    var dragging by remember { mutableStateOf(false) }
    val targetFraction = if (dragging) dragFraction else vol.toFloat() / maxVol
    val fraction by animateFloatAsState(
        targetValue = targetFraction.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 520f),
        label = "volumeFraction"
    )

    fun applyFromY(y: Float, heightPx: Float) {
        val f = (1f - y / heightPx).coerceIn(0f, 1f)
        dragFraction = f
        val newVol = (f * maxVol).roundToInt().coerceIn(0, maxVol)
        if (newVol != vol) {
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
            vol = newVol
        }
    }

    val label = stringResource(R.string.cd_volume)

    Column(
        modifier = modifier.width(TOUCH_WIDTH.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(
            modifier = Modifier
                .width(TOUCH_WIDTH.dp)
                .fillMaxHeight()
                .pointerInput(maxVol) {
                    detectTapGestures { offset -> applyFromY(offset.y, size.height.toFloat()) }
                }
                .pointerInput(maxVol) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            dragging = true
                            applyFromY(offset.y, size.height.toFloat())
                        },
                        onDragEnd = { dragging = false },
                        onDragCancel = { dragging = false }
                    ) { change, _ ->
                        applyFromY(change.position.y, size.height.toFloat())
                    }
                }
        ) {
            val trackW = 42.dp.toPx()
            val thumbW = 54.dp.toPx()
            val thumbH = 16.dp.toPx()
            val gap = 9.dp.toPx()
            val cx = size.width / 2f
            val radius = CornerRadius(trackW / 2f, trackW / 2f)

            val thumbHalf = thumbH / 2f
            val cy = (size.height - fraction * size.height).coerceIn(thumbHalf, size.height - thumbHalf)

            // Inactive track (above the thumb).
            val inactiveBottom = (cy - thumbHalf - gap).coerceAtLeast(0f)
            if (inactiveBottom > 0f) {
                drawRoundRect(
                    color = AppColor.Track,
                    topLeft = Offset(cx - trackW / 2f, 0f),
                    size = Size(trackW, inactiveBottom),
                    cornerRadius = radius
                )
            }

            // Active track (below the thumb, down to the bottom).
            val activeTop = (cy + thumbHalf + gap).coerceAtMost(size.height)
            if (activeTop < size.height) {
                drawRoundRect(
                    color = AppColor.Primary,
                    topLeft = Offset(cx - trackW / 2f, activeTop),
                    size = Size(trackW, size.height - activeTop),
                    cornerRadius = radius
                )
            }

            // Expressive handle.
            drawRoundRect(
                color = AppColor.Primary,
                topLeft = Offset(cx - thumbW / 2f, cy - thumbHalf),
                size = Size(thumbW, thumbH),
                cornerRadius = CornerRadius(thumbHalf, thumbHalf)
            )
        }

        Spacer(Modifier.height(14.dp))
        val icon = when {
            vol == 0 -> Icons.Rounded.VolumeMute
            fraction < 0.5f -> Icons.Rounded.VolumeDown
            else -> Icons.Rounded.VolumeUp
        }
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = AppColor.Ink,
            modifier = Modifier.size(32.dp)
        )
    }
}

private const val TOUCH_WIDTH = 96
