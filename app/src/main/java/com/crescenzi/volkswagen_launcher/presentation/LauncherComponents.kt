package com.crescenzi.volkswagen_launcher.presentation

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crescenzi.volkswagen_launcher.R
import com.crescenzi.volkswagen_launcher.core.networkTimeOffset
import com.crescenzi.volkswagen_launcher.core.safeOpen
import com.crescenzi.volkswagen_launcher.theme.AppColor
import com.crescenzi.volkswagen_launcher.widgets.CircleButton
import androidx.compose.material3.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
internal fun TopBar(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val romeTz = remember { TimeZone.getTimeZone("Europe/Rome") }
    val hourFmt = remember { SimpleDateFormat("H", Locale.ITALIAN).apply { timeZone = romeTz } }

    var offset by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withContext(Dispatchers.IO) { networkTimeOffset() }?.let { offset = it }
            delay(30 * 60 * 1000L)
        }
    }
    var tick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { tick = System.currentTimeMillis(); delay(60_000L) } }

    val hour = hourFmt.format(Date(tick + offset)).toIntOrNull() ?: 12
    val greetingWord = if (hour < 12) stringResource(R.string.greeting_morning)
    else stringResource(R.string.greeting_evening)
    val greeting = stringResource(R.string.greeting, greetingWord, stringResource(R.string.user_name))

    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = greeting,
            color = AppColor.Ink,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        CircleButton(Icons.Rounded.Settings, stringResource(R.string.cd_settings), squircleOnPress = true) {
            ctx.safeOpen(Intent(Settings.ACTION_SETTINGS))
        }
    }
}

@Composable
internal fun PageIndicator(pagerState: PagerState, modifier: Modifier = Modifier) {
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
