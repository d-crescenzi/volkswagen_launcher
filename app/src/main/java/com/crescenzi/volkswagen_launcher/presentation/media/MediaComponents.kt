package com.crescenzi.volkswagen_launcher.presentation.media

import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crescenzi.volkswagen_launcher.R
import com.crescenzi.volkswagen_launcher.theme.AppColor
import com.crescenzi.volkswagen_launcher.theme.AppDimens
import com.crescenzi.volkswagen_launcher.widgets.CircleButton
import kotlinx.coroutines.delay

@Composable
internal fun MediaControls(controller: MediaController, modifier: Modifier = Modifier) {
    var metadata by remember(controller) { mutableStateOf(controller.metadata) }
    var state by remember(controller) { mutableStateOf(controller.playbackState) }

    DisposableEffect(controller) {
        val cb = object : MediaController.Callback() {
            override fun onMetadataChanged(m: MediaMetadata?) { metadata = m }
            override fun onPlaybackStateChanged(s: PlaybackState?) { state = s }
        }
        controller.registerCallback(cb)
        onDispose { controller.unregisterCallback(cb) }
    }

    val title = metadata?.getText(MediaMetadata.METADATA_KEY_TITLE)?.toString() ?: ""
    val artist = metadata?.getText(MediaMetadata.METADATA_KEY_ARTIST)?.toString()
        ?: metadata?.getText(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)?.toString() ?: ""
    val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
    val isPlaying = state?.state == PlaybackState.STATE_PLAYING

    var position by remember(controller) { mutableLongStateOf(0L) }
    LaunchedEffect(state, metadata) {
        while (true) {
            val s = state
            position = when {
                s == null -> 0L
                s.state == PlaybackState.STATE_PLAYING -> {
                    val delta = (SystemClock.elapsedRealtime() - s.lastPositionUpdateTime) * s.playbackSpeed
                    (s.position + delta).toLong().coerceAtLeast(0L)
                }
                else -> s.position.coerceAtLeast(0L)
            }
            delay(500)
        }
    }

    val prevCd = stringResource(R.string.cd_prev)
    val nextCd = stringResource(R.string.cd_next)
    val pauseCd = stringResource(R.string.cd_pause)
    val playCd = stringResource(R.string.cd_play)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.widthIn(max = 560.dp).fillMaxWidth()
    ) {
        Text(
            text = title,
            color = AppColor.Ink,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = artist,
            color = AppColor.InkMuted,
            fontSize = 17.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(20.dp))

        val fraction = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = AppColor.Ink,
            trackColor = AppColor.Track,
            gapSize = 0.dp,
            drawStopIndicator = {}
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(fmt(position), color = AppColor.InkMuted, fontSize = 12.sp)
            Text(fmt(duration), color = AppColor.InkMuted, fontSize = 12.sp)
        }

        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            CircleButton(Icons.Rounded.SkipPrevious, prevCd, size = AppDimens.MediaButton, iconSize = AppDimens.MediaButtonIcon) {
                controller.transportControls.skipToPrevious()
            }
            Spacer(Modifier.width(28.dp))
            CircleButton(
                icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                desc = if (isPlaying) pauseCd else playCd,
                size = AppDimens.MediaPlayButton,
                iconSize = AppDimens.MediaPlayButtonIcon
            ) {
                if (isPlaying) controller.transportControls.pause() else controller.transportControls.play()
            }
            Spacer(Modifier.width(28.dp))
            CircleButton(Icons.Rounded.SkipNext, nextCd, size = AppDimens.MediaButton, iconSize = AppDimens.MediaButtonIcon) {
                controller.transportControls.skipToNext()
            }
        }
    }
}

private fun fmt(ms: Long): String {
    if (ms <= 0) return "0:00"
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
