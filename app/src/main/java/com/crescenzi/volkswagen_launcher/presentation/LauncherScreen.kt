package com.crescenzi.volkswagen_launcher.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.crescenzi.volkswagen_launcher.presentation.apps.AppsCard
import com.crescenzi.volkswagen_launcher.presentation.apps.loadMappedApps
import com.crescenzi.volkswagen_launcher.presentation.media.MediaCard
import com.crescenzi.volkswagen_launcher.theme.AppColor
import com.crescenzi.volkswagen_launcher.theme.AppDimens

// Media-card width at p = 0 (full) and p = 1 (mini); App card takes the rest.
private const val MEDIA_FULL = 884f
private const val MEDIA_MINI = 232f

@Composable
fun LauncherScreen(dark: Boolean, onToggleTheme: () -> Unit, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val apps = remember { ctx.loadMappedApps() }
    val carousel = rememberCarouselState()

    BackHandler(enabled = carousel.page != 0) { carousel.goTo(0) }

    val p = carousel.p

    Box(modifier = modifier.fillMaxSize().background(AppColor.current.canvas)) {
        Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
            TopBar(
                dark = dark,
                onToggleTheme = onToggleTheme,
                modifier = Modifier.padding(start = 28.dp, end = 16.dp, top = 16.dp).height(AppDimens.TopBar)
            )
            Spacer(Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                val wMedia = MEDIA_FULL - (MEDIA_FULL - MEDIA_MINI) * p
                // m3 carousel
                Row(
                    modifier = Modifier.fillMaxSize().carouselDrag(carousel),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MediaCard(
                        p = p,
                        onCollapse = { carousel.goTo(0) },
                        modifier = Modifier.width(wMedia.dp).fillMaxHeight()
                    )
                    AppsCard(
                        p = p,
                        apps = apps,
                        onExpand = { carousel.goTo(1) },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }
    }
}
