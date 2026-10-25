package com.crescenzi.volkswagen_launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.crescenzi.volkswagen_launcher.theme.AppColor
import kotlinx.coroutines.delay

@Composable
fun MediaPanel(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var hasAccess by remember { mutableStateOf(isNotifAccess(context)) }

    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) hasAccess = isNotifAccess(context)
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (!hasAccess) {
            Text(stringResource(R.string.media_no_access_title), color = AppColor.Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.media_no_access_hint), color = AppColor.InkMuted, fontSize = 14.sp)
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(AppColor.Ink)
                    .clickable { safeOpen(context, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                    .padding(horizontal = 28.dp, vertical = 12.dp)
            ) {
                Text(stringResource(R.string.media_enable), color = AppColor.Background, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            return@Column
        }

        val msm = remember { context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager }
        val comp = remember { ComponentName(context, MediaControlService::class.java) }
        var selected by remember { mutableStateOf<MediaController?>(null) }

        DisposableEffect(Unit) {
            fun pick(list: List<MediaController>) {
                val ok = selected?.let { c -> list.any { it.sessionToken == c.sessionToken } } ?: false
                if (!ok) selected = list.firstOrNull()
            }
            val listener = MediaSessionManager.OnActiveSessionsChangedListener { pick(it ?: emptyList()) }
            pick(try { msm.getActiveSessions(comp) } catch (e: SecurityException) { emptyList() })
            msm.addOnActiveSessionsChangedListener(listener, comp)
            onDispose { msm.removeOnActiveSessionsChangedListener(listener) }
        }

        val current = selected
        if (current == null) {
            Text(stringResource(R.string.media_idle_title), color = AppColor.Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.media_idle_hint), color = AppColor.InkMuted, fontSize = 14.sp)
        } else {
            MediaControls(current)
        }
    }
}

@Composable
private fun MediaControls(controller: MediaController) {
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
        modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth()
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
            CircleButton(Icons.Rounded.SkipPrevious, prevCd, size = 60.dp, iconSize = 34.dp) {
                controller.transportControls.skipToPrevious()
            }
            Spacer(Modifier.width(22.dp))
            CircleButton(
                icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                desc = if (isPlaying) pauseCd else playCd,
                size = 84.dp,
                iconSize = 46.dp
            ) {
                if (isPlaying) controller.transportControls.pause() else controller.transportControls.play()
            }
            Spacer(Modifier.width(22.dp))
            CircleButton(Icons.Rounded.SkipNext, nextCd, size = 60.dp, iconSize = 34.dp) {
                controller.transportControls.skipToNext()
            }
        }
    }
}

fun isNotifAccess(context: Context): Boolean {
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        ?: return false
    val me = ComponentName(context, MediaControlService::class.java)
    return flat.split(":").any { ComponentName.unflattenFromString(it) == me }
}

fun safeOpen(context: Context, intent: Intent) {
    try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.cannot_open), Toast.LENGTH_SHORT).show()
    }
}

private fun fmt(ms: Long): String {
    if (ms <= 0) return "0:00"
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
