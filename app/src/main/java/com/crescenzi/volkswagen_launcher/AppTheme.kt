package com.crescenzi.volkswagen_launcher

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = AppColor.Ink,
    onPrimary = AppColor.Background,
    background = AppColor.Background,
    onBackground = AppColor.Ink,
    surface = AppColor.Background,
    onSurface = AppColor.Ink,
    onSurfaceVariant = AppColor.InkMuted,
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, typography = AppType, content = content)
}
