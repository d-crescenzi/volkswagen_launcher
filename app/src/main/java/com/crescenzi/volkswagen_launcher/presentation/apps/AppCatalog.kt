package com.crescenzi.volkswagen_launcher.presentation.apps

import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.rounded.Shop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.ui.graphics.vector.ImageVector

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

fun Context.loadMappedApps(): List<LaunchApp> {
    val pm = packageManager
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
