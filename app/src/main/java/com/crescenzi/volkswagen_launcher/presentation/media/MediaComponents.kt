package com.crescenzi.volkswagen_launcher.presentation.media

import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.SystemClock
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crescenzi.volkswagen_launcher.R
import com.crescenzi.volkswagen_launcher.theme.AppColor
import com.crescenzi.volkswagen_launcher.theme.AppDimens
import com.crescenzi.volkswagen_launcher.widgets.CircleButton
import com.crescenzi.volkswagen_launcher.widgets.MediaBadge
import com.crescenzi.volkswagen_launcher.widgets.MorphingPlayButton
import com.crescenzi.volkswagen_launcher.widgets.WavyProgressBar
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay

data class Playback(
    val title: String,
    val artist: String,
    val duration: Long,
    val playing: Boolean,
    val position: Long,
)

@Composable
internal fun rememberPlayback(controller: MediaController): Playback {
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
    val playing = state?.state == PlaybackState.STATE_PLAYING

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

    return Playback(title, artist, duration, playing, position)
}

// Full player: source, title, artist, wavy progress, times, transport.
@Composable
internal fun FullPlayer(
    controller: MediaController,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val p = rememberPlayback(controller)
    val prevCd = stringResource(R.string.cd_prev)
    val nextCd = stringResource(R.string.cd_next)

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(p.title, color = AppColor.current.ink, fontSize = 34.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        Spacer(Modifier.height(2.dp))
        Text(p.artist, color = AppColor.current.inkMuted, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)

        Spacer(Modifier.height(24.dp))
        val fraction = if (p.duration > 0) (p.position.toFloat() / p.duration).coerceIn(0f, 1f) else 0f
        WavyProgressBar(progress = fraction, playing = p.playing, modifier = Modifier.fillMaxWidth().height(14.dp))
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TimeText(p.position)
            TimeText(p.duration)
        }

        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            CircleButton(Icons.Rounded.SkipPrevious, prevCd, size = AppDimens.MediaButton, iconSize = AppDimens.MediaButtonIcon, enabled = active) {
                controller.transportControls.skipToPrevious()
            }
            MorphingPlayButton(playing = p.playing, size = AppDimens.MediaPlayButton, iconSize = AppDimens.MediaPlayButtonIcon, enabled = active) {
                togglePlay(controller, p.playing)
            }
            CircleButton(Icons.Rounded.SkipNext, nextCd, size = AppDimens.MediaButton, iconSize = AppDimens.MediaButtonIcon, enabled = active) {
                controller.transportControls.skipToNext()
            }
        }
    }
}

// Mini player: title (up to 2 lines), artist, wavy, play, prev/next row.
@Composable
internal fun MiniPlayer(
    controller: MediaController,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val p = rememberPlayback(controller)
    val prevCd = stringResource(R.string.cd_prev)
    val nextCd = stringResource(R.string.cd_next)

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(p.title, color = AppColor.current.ink, fontSize = 21.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        Spacer(Modifier.height(2.dp))
        Text(p.artist, color = AppColor.current.inkMuted, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)

        Spacer(Modifier.height(16.dp))
        val fraction = if (p.duration > 0) (p.position.toFloat() / p.duration).coerceIn(0f, 1f) else 0f
        WavyProgressBar(progress = fraction, playing = p.playing, amplitude = 2.5.dp, wavelength = 14.dp, stroke = 4.dp, modifier = Modifier.fillMaxWidth().height(12.dp))

        Spacer(Modifier.height(22.dp))
        MorphingPlayButton(playing = p.playing, size = AppDimens.MediaMiniPlayButton, iconSize = AppDimens.MediaMiniPlayButtonIcon, enabled = active) {
            togglePlay(controller, p.playing)
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CircleButton(Icons.Rounded.SkipPrevious, prevCd, size = AppDimens.MediaButton, iconSize = AppDimens.MediaMiniButtonIcon, enabled = active) {
                controller.transportControls.skipToPrevious()
            }
            CircleButton(Icons.Rounded.SkipNext, nextCd, size = AppDimens.MediaButton, iconSize = AppDimens.MediaMiniButtonIcon, enabled = active) {
                controller.transportControls.skipToNext()
            }
        }
    }
}

@Composable
private fun TimeText(ms: Long) {
    Text(
        fmt(ms),
        color = AppColor.current.inkMuted,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        style = TextStyle(fontFeatureSettings = "tnum")
    )
}

// Ink CTA used in empty states; morphs to a softer squircle while held.
@Composable
internal fun CtaButton(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    mini: Boolean = false,
    active: Boolean = true,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val radius by animateFloatAsState(
        targetValue = if (pressed) 16f else 28f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "ctaRadius"
    )
    Row(
        modifier = modifier
            .height(AppDimens.CtaHeight)
            .clip(RoundedCornerShape(radius.dp))
            .background(AppColor.current.ink)
            .clickable(interactionSource = interaction, indication = ripple(), enabled = active) { onClick() }
            .padding(start = if (mini) 18.dp else 26.dp, end = if (mini) 24.dp else 32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (mini) 8.dp else 10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = AppColor.current.card, modifier = Modifier.size(if (mini) 22.dp else 24.dp))
        Text(label, color = AppColor.current.card, fontSize = if (mini) 16.sp else 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

// Full empty state: big circle icon, title, hint, optional CTA.
@Composable
internal fun EmptyFull(status: MediaStatus, active: Boolean, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        MediaBadge(status.icon, size = AppDimens.MediaEmptyCircle, iconSize = AppDimens.MediaEmptyIcon)
        Spacer(Modifier.height(20.dp))
        Text(stringResource(status.titleRes), color = AppColor.current.ink, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(status.hintRes), color = AppColor.current.inkMuted, fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 440.dp))
        status.ctaRes?.let { cta ->
            Spacer(Modifier.height(22.dp))
            CtaButton(stringResource(cta), status.ctaIcon!!, active = active) { status.action() }
        }
    }
}

// Mini empty state: compact circle icon, short title, optional short CTA.
@Composable
internal fun EmptyMini(status: MediaStatus, active: Boolean, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        MediaBadge(status.icon, size = AppDimens.MediaMiniEmptyCircle, iconSize = AppDimens.MediaMiniEmptyIcon)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(status.shortRes), color = AppColor.current.ink, fontSize = 19.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        status.ctaShortRes?.let { cta ->
            Spacer(Modifier.height(18.dp))
            CtaButton(stringResource(cta), status.ctaIcon!!, mini = true, active = active) { status.action() }
        }
    }
}

private fun togglePlay(controller: MediaController, playing: Boolean) {
    if (playing) controller.transportControls.pause() else controller.transportControls.play()
}

private fun fmt(ms: Long): String {
    if (ms <= 0) return "0:00"
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
