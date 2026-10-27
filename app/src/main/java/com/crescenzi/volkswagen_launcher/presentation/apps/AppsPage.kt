package com.crescenzi.volkswagen_launcher.presentation.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.crescenzi.volkswagen_launcher.core.safeOpen
import com.crescenzi.volkswagen_launcher.theme.AppDimens

@Composable
fun AppsGrid(apps: List<LaunchApp>, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = AppDimens.AppGridMinCell),
        modifier = modifier.fillMaxSize(),
        userScrollEnabled = false,
        contentPadding = PaddingValues(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        items(apps, key = { it.pkg }) { app ->
            AppCell(app) { ctx.safeOpen(app.launch) }
        }
    }
}
