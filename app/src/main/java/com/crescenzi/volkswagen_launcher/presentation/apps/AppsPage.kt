package com.crescenzi.volkswagen_launcher.presentation.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.crescenzi.volkswagen_launcher.core.safeOpen
import com.crescenzi.volkswagen_launcher.presentation.islandShadow
import com.crescenzi.volkswagen_launcher.theme.AppColor
import com.crescenzi.volkswagen_launcher.theme.AppDimens

// Fixed app-card content width; the card clips to a peek column at p = 0 and to
// the full grid at p = 1.
private const val APPS_CONTENT = 748

@Composable
fun AppsCard(p: Float, apps: List<LaunchApp>, onExpand: () -> Unit, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val shape = RoundedCornerShape(AppDimens.CardRadius)
    val labelAlpha = ((p - 0.45f) / 0.45f).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .islandShadow(shape)
            .clip(shape)
            .background(AppColor.current.card)
    ) {
        // Tapping the peek expands the grid; disabled once expanded so cells take over.
        Box(
            Modifier
                .fillMaxSize()
                .clickable(
                    enabled = p < 0.5f,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onExpand() }
        )

        Column(
            modifier = Modifier
                .width(APPS_CONTENT.dp)
                .fillMaxHeight()
                .padding(horizontal = 6.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterVertically)
        ) {
            apps.chunked(6).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    row.forEach { app ->
                        AppCell(app, labelAlpha = labelAlpha) { ctx.safeOpen(app.launch) }
                    }
                }
            }
        }
    }
}
