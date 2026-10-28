package com.crescenzi.volkswagen_launcher.presentation

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crescenzi.volkswagen_launcher.R
import com.crescenzi.volkswagen_launcher.core.networkTimeOffset
import com.crescenzi.volkswagen_launcher.core.safeOpen
import com.crescenzi.volkswagen_launcher.theme.AppColor
import com.crescenzi.volkswagen_launcher.theme.AppDimens
import com.crescenzi.volkswagen_launcher.widgets.CircleButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
internal fun TopBar(dark: Boolean, onToggleTheme: () -> Unit, modifier: Modifier = Modifier) {
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

    val shadow = if (dark) 0.dp else 8.dp

    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = greeting,
            color = AppColor.current.ink,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CircleButton(
                icon = if (dark) Icons.Rounded.DarkMode else Icons.Outlined.DarkMode,
                desc = stringResource(R.string.cd_theme),
                size = AppDimens.TopButton,
                iconSize = AppDimens.TopButtonIcon,
                background = AppColor.current.card,
                shadow = shadow,
                onClick = onToggleTheme
            )
            CircleButton(
                icon = Icons.Rounded.Settings,
                desc = stringResource(R.string.cd_settings),
                size = AppDimens.TopButton,
                iconSize = AppDimens.TopButtonIcon,
                background = AppColor.current.card,
                shadow = shadow
            ) {
                ctx.safeOpen(Intent(Settings.ACTION_SETTINGS))
            }
        }
    }
}
