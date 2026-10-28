package com.crescenzi.volkswagen_launcher

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.crescenzi.volkswagen_launcher.core.ThemeStore
import com.crescenzi.volkswagen_launcher.presentation.LauncherScreen
import com.crescenzi.volkswagen_launcher.theme.AppColor
import com.crescenzi.volkswagen_launcher.theme.AppTheme
import com.crescenzi.volkswagen_launcher.theme.LightPalette
import com.crescenzi.volkswagen_launcher.theme.NightPalette

class MainActivity : ComponentActivity() {
    // Ignore the system font-size setting; the launcher owns its text sizing.
    override fun attachBaseContext(newBase: Context) {
        val config = Configuration(newBase.resources.configuration)
        config.fontScale = 1f
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideStatusBar()

        val systemDark = resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val initialDark = ThemeStore.loadDark(this, fallback = systemDark)

        setContent {
            var dark by remember { mutableStateOf(initialDark) }

            LaunchedEffect(dark) {
                window.navigationBarColor =
                    (if (dark) NightPalette else LightPalette).canvas.toArgb()
                WindowCompat.getInsetsController(window, window.decorView)
                    .isAppearanceLightNavigationBars = !dark
            }

            AppTheme(dark = dark) {
                Surface(modifier = Modifier.fillMaxSize(), color = AppColor.current.canvas) {
                    LauncherScreen(
                        dark = dark,
                        onToggleTheme = {
                            dark = !dark
                            ThemeStore.saveDark(this@MainActivity, dark)
                        }
                    )
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
