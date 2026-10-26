package com.crescenzi.volkswagen_launcher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.crescenzi.volkswagen_launcher.theme.AppColor
import com.crescenzi.volkswagen_launcher.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideStatusBar()
        setContent {
            AppTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = AppColor.Background) {
                    Root()
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideStatusBar()
    }

    // Hide only the top status bar; keep the bottom navigation bar.
    private fun hideStatusBar() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val c = WindowCompat.getInsetsController(window, window.decorView)
        c.hide(WindowInsetsCompat.Type.statusBars())
        c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}

@Composable
private fun Root() {
    val context = LocalContext.current
    val apps = remember { loadMappedApps(context) }

    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    BackHandler(enabled = true) {
        if (pagerState.currentPage != 0) scope.launch { pagerState.animateScrollToPage(0) }
    }

    Column(
        modifier = Modifier
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
                    else -> AppsGrid(apps)
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

@Composable
private fun TopBar() {
    val context = LocalContext.current
    val romeTz = remember { TimeZone.getTimeZone("Europe/Rome") }
    val hourFmt = remember { SimpleDateFormat("H", Locale.ITALIAN).apply { timeZone = romeTz } }

    var offset by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withContext(Dispatchers.IO) { fetchNetworkTimeOffset() }?.let { offset = it }
            delay(30 * 60 * 1000L)
        }
    }
    var tick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { tick = System.currentTimeMillis(); delay(60_000L) } }

    val hour = hourFmt.format(Date(tick + offset)).toIntOrNull() ?: 12
    val greetingWord = if (hour < 12) stringResource(R.string.greeting_morning)
    else stringResource(R.string.greeting_evening)
    val greeting = stringResource(R.string.greeting, greetingWord, stringResource(R.string.user_name))

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = greeting,
            color = AppColor.Ink,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        CircleButton(Icons.Rounded.Settings, stringResource(R.string.cd_settings)) {
            safeOpen(context, Intent(Settings.ACTION_SETTINGS))
        }
    }
}

@Composable
private fun AppsGrid(apps: List<LaunchApp>) {
    val context = LocalContext.current
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 104.dp),
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = false,
        contentPadding = PaddingValues(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        items(apps, key = { it.pkg }) { app ->
            AppCell(app) { safeOpen(context, app.launch) }
        }
    }
}

@Composable
private fun AppCell(app: LaunchApp, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(66.dp).clip(CircleShape).background(AppColor.Circle),
            contentAlignment = Alignment.Center
        ) {
            Icon(app.icon, contentDescription = app.label, tint = AppColor.Ink, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = app.label,
            color = AppColor.Ink,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PageIndicator(pagerState: PagerState, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pagerState.pageCount) { iteration ->
            val isCurrent by remember { derivedStateOf { pagerState.currentPage == iteration } }
            val animatedColor by animateColorAsState(
                if (isCurrent) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                label = "indicatorColor"
            )
            Box(
                modifier = Modifier
                    .padding(3.dp)
                    .animateContentSize()
                    .height(12.dp)
                    .width(if (isCurrent) 36.dp else 12.dp)
                    .drawBehind {
                        drawRoundRect(color = animatedColor, cornerRadius = CornerRadius(16.dp.toPx()))
                    }
            )
        }
    }
}

private val EDGE_FADE = 36.dp

data class LaunchApp(
    val label: String,
    val pkg: String,
    val icon: ImageVector,
    val launch: Intent
)

private data class AppDef(val pkg: String, val icon: ImageVector)

private val APP_DEFS = listOf(
    AppDef("com.autochips.bluetooth", Icons.Rounded.Bluetooth),
    AppDef("com.xygala.canbus", Icons.Rounded.Tune),
    AppDef("com.android.chrome", Icons.Rounded.Public),
    AppDef("com.autochips.android.backcar", Icons.Rounded.Videocam),
    AppDef("com.pve.download", Icons.Rounded.Download),
    AppDef("com.mediatek.filemanager", Icons.Rounded.Folder),
    AppDef("com.android.gallery3d", Icons.Rounded.PhotoLibrary),
    AppDef("com.xyauto.Settings", Icons.Rounded.DirectionsCar),
    AppDef("com.google.android.apps.maps", Icons.Rounded.Map),
    AppDef("com.acloud.stub.localmusic", Icons.Rounded.LibraryMusic),
    AppDef("com.android.vending", Icons.Rounded.Shop),
    AppDef("com.txznet.smartadapter", Icons.Rounded.RecordVoiceOver),
    AppDef("com.acloud.stub.localradio", Icons.Rounded.Radio),
    AppDef("com.android.soundrecorder", Icons.Rounded.Mic),
    AppDef("com.xy.tda7729", Icons.Rounded.Equalizer),
    AppDef("com.pve.theme", Icons.Rounded.Palette),
    AppDef("com.acloud.stub.video", Icons.Rounded.Movie),
    AppDef("com.google.android.youtube", Icons.Rounded.PlayCircle),
)

private fun loadMappedApps(context: Context): List<LaunchApp> {
    val pm = context.packageManager
    return APP_DEFS.mapNotNull { def ->
        val launch = pm.getLaunchIntentForPackage(def.pkg) ?: return@mapNotNull null
        val label = try {
            pm.getApplicationLabel(pm.getApplicationInfo(def.pkg, 0)).toString()
        } catch (e: Exception) {
            def.pkg
        }
        LaunchApp(label, def.pkg, def.icon, launch)
    }
}

private fun fetchNetworkTimeOffset(): Long? {
    return try {
        val conn = (URL("https://www.google.com/generate_204").openConnection() as HttpURLConnection).apply {
            requestMethod = "HEAD"; connectTimeout = 4000; readTimeout = 4000; useCaches = false
        }
        conn.connect()
        val dateHeader = conn.getHeaderField("Date")
        conn.disconnect()
        if (dateHeader.isNullOrEmpty()) return null
        val sdf = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US)
        val serverMs = sdf.parse(dateHeader)?.time ?: return null
        serverMs - System.currentTimeMillis()
    } catch (e: Exception) {
        null
    }
}
