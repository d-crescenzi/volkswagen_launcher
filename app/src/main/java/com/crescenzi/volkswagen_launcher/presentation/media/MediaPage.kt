package com.crescenzi.volkswagen_launcher.presentation.media

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.crescenzi.volkswagen_launcher.R
import com.crescenzi.volkswagen_launcher.core.hasNotificationAccess
import com.crescenzi.volkswagen_launcher.core.isBluetoothOn
import com.crescenzi.volkswagen_launcher.core.safeOpen
import com.crescenzi.volkswagen_launcher.service.MediaControlService
import com.crescenzi.volkswagen_launcher.theme.AppColor
import com.crescenzi.volkswagen_launcher.theme.AppDimens
import com.crescenzi.volkswagen_launcher.presentation.islandShadow
import com.crescenzi.volkswagen_launcher.presentation.volume.VolumeSlider

// Copy + icon + action for a non-playing media state.
internal class MediaStatus(
    val icon: ImageVector,
    val titleRes: Int,
    val hintRes: Int,
    val shortRes: Int,
    val ctaRes: Int?,
    val ctaShortRes: Int?,
    val ctaIcon: ImageVector?,
    val action: () -> Unit,
)

private sealed interface MediaState {
    data class Playing(val controller: MediaController) : MediaState
    data class Empty(val status: MediaStatus) : MediaState
}

// Fixed media-card content width; shrinks under the card clip as the carousel moves.
private const val MEDIA_CONTENT = 884
private const val VOLUME_COLUMN = 96
private const val MINI_WIDTH = 232

private fun clamp(x: Float) = x.coerceIn(0f, 1f)

@Composable
fun MediaCard(p: Float, onCollapse: () -> Unit, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current

    // Notification access can change while we are backgrounded in Settings.
    var hasAccess by remember { mutableStateOf(ctx.hasNotificationAccess()) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) hasAccess = ctx.hasNotificationAccess()
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    var btOn by remember { mutableStateOf(ctx.isBluetoothOn()) }
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) { btOn = ctx.isBluetoothOn() }
        }
        ctx.registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        onDispose { ctx.unregisterReceiver(receiver) }
    }

    val msm = remember { ctx.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager }
    val comp = remember { ComponentName(ctx, MediaControlService::class.java) }
    var selected by remember { mutableStateOf<MediaController?>(null) }
    DisposableEffect(hasAccess) {
        if (!hasAccess) {
            selected = null
            return@DisposableEffect onDispose { }
        }
        fun pick(list: List<MediaController>) {
            val ok = selected?.let { c -> list.any { it.sessionToken == c.sessionToken } } ?: false
            if (!ok) selected = list.firstOrNull()
        }
        val listener = MediaSessionManager.OnActiveSessionsChangedListener { pick(it ?: emptyList()) }
        pick(try { msm.getActiveSessions(comp) } catch (e: SecurityException) { emptyList() })
        msm.addOnActiveSessionsChangedListener(listener, comp)
        onDispose { msm.removeOnActiveSessionsChangedListener(listener) }
    }

    val noAccess = remember { statusNoAccess(ctx) }
    val btOff = remember { statusBtOff(ctx) }
    val idle = remember { statusIdle() }
    val controller = selected

    // Priority: no access → bluetooth off → playing → idle.
    val state: MediaState = when {
        !hasAccess -> MediaState.Empty(noAccess)
        !btOn -> MediaState.Empty(btOff)
        controller != null -> MediaState.Playing(controller)
        else -> MediaState.Empty(idle)
    }

    val oMedia = clamp(1f - 1.8f * p)        // full player + volume fade out
    val oMini = clamp((p - 0.5f) / 0.4f)     // mini player fades in
    val fullActive = p < 0.5f
    val miniActive = p > 0.5f

    Box(
        modifier = modifier
            .islandShadow(RoundedCornerShape(AppDimens.CardRadius))
            .clip(RoundedCornerShape(AppDimens.CardRadius))
            .background(AppColor.current.card)
    ) {
        Box(Modifier.width(MEDIA_CONTENT.dp).fillMaxHeight()) {
            // Full player / empty state.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 120.dp, end = 48.dp)
                    .alpha(oMedia),
                contentAlignment = Alignment.Center
            ) {
                when (state) {
                    is MediaState.Playing -> FullPlayer(
                        controller = state.controller,
                        active = fullActive
                    )
                    is MediaState.Empty -> EmptyFull(state.status, fullActive)
                }
            }

            // Mini player / compact empty state (visible once collapsed).
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .width(MINI_WIDTH.dp)
                    .fillMaxHeight()
                    .alpha(oMini)
            ) {
                // Tapping the mini card away from a control brings media back to front.
                Box(
                    Modifier
                        .fillMaxSize()
                        .clickable(
                            enabled = miniActive,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onCollapse() }
                )
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    when (state) {
                        is MediaState.Playing -> MiniPlayer(state.controller, miniActive)
                        is MediaState.Empty -> EmptyMini(state.status, miniActive)
                    }
                }
            }

            // Volume column (left edge). Kept on top so it stays usable whatever the
            // media state; pointer input is released once the card is collapsing.
            VolumeSlider(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .width(VOLUME_COLUMN.dp)
                    .fillMaxHeight()
                    .alpha(oMedia),
                interactive = fullActive
            )
        }
    }
}

private fun statusNoAccess(ctx: Context) = MediaStatus(
    icon = Icons.Rounded.NotificationsOff,
    titleRes = R.string.media_no_access_title,
    hintRes = R.string.media_no_access_hint,
    shortRes = R.string.media_no_access_short,
    ctaRes = R.string.media_enable,
    ctaShortRes = R.string.media_enable,
    ctaIcon = Icons.Rounded.Key,
    action = { ctx.safeOpen(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
)

private fun statusBtOff(ctx: Context) = MediaStatus(
    icon = Icons.Rounded.BluetoothDisabled,
    titleRes = R.string.media_bt_off_title,
    hintRes = R.string.media_bt_off_hint,
    shortRes = R.string.media_bt_off_short,
    ctaRes = R.string.media_bt_off_cta,
    ctaShortRes = R.string.media_enable,
    ctaIcon = Icons.Rounded.Bluetooth,
    action = { ctx.safeOpen(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) },
)

private fun statusIdle() = MediaStatus(
    icon = Icons.Rounded.MusicNote,
    titleRes = R.string.media_idle_title,
    hintRes = R.string.media_idle_hint,
    shortRes = R.string.media_idle_short,
    ctaRes = null,
    ctaShortRes = null,
    ctaIcon = null,
    action = {},
)
