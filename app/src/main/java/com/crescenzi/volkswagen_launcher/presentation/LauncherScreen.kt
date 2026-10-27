package com.crescenzi.volkswagen_launcher.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.crescenzi.volkswagen_launcher.presentation.apps.AppsGrid
import com.crescenzi.volkswagen_launcher.presentation.apps.loadMappedApps
import com.crescenzi.volkswagen_launcher.presentation.media.MediaPanel
import com.crescenzi.volkswagen_launcher.presentation.volume.VolumeSlider
import com.crescenzi.volkswagen_launcher.theme.AppColor
import kotlinx.coroutines.launch

@Composable
fun LauncherScreen(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val apps = remember { ctx.loadMappedApps() }

    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    BackHandler(enabled = true) {
        if (pagerState.currentPage != 0) scope.launch { pagerState.animateScrollToPage(0) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColor.Background)
            .navigationBarsPadding()
            .padding(start = 30.dp, end = 30.dp, top = 20.dp)
    ) {
        TopBar()

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                when (page) {
                    0 -> Box(Modifier.fillMaxSize()) {
                        MediaPanel(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.Center)
                        )
                        VolumeSlider(
                            modifier = Modifier
                                .fillMaxHeight(0.78f)
                                .align(Alignment.CenterStart)
                        )
                    }
                    else -> AppsGrid(apps = apps)
                }
            }

            // Soft lateral fades so horizontally scrolling content melts into the
            // edges instead of showing a hard cut.
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(EDGE_FADE)
                    .background(
                        Brush.horizontalGradient(listOf(AppColor.Background, Color.Transparent))
                    )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(EDGE_FADE)
                    .background(
                        Brush.horizontalGradient(listOf(Color.Transparent, AppColor.Background))
                    )
            )
        }

        PageIndicator(
            pagerState = pagerState,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 6.dp, bottom = 22.dp)
        )
    }
}

private val EDGE_FADE = 36.dp
