package com.crescenzi.volkswagen_launcher.presentation.media

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.crescenzi.volkswagen_launcher.R
import com.crescenzi.volkswagen_launcher.core.hasNotificationAccess
import com.crescenzi.volkswagen_launcher.core.safeOpen
import com.crescenzi.volkswagen_launcher.service.MediaControlService
import com.crescenzi.volkswagen_launcher.theme.AppColor

@Composable
fun MediaPanel(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var hasAccess by remember { mutableStateOf(ctx.hasNotificationAccess()) }

    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) hasAccess = ctx.hasNotificationAccess()
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
                    .clickable { ctx.safeOpen(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                    .padding(horizontal = 28.dp, vertical = 12.dp)
            ) {
                Text(stringResource(R.string.media_enable), color = AppColor.Background, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            return@Column
        }

        val msm = remember { ctx.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager }
        val comp = remember { ComponentName(ctx, MediaControlService::class.java) }
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
