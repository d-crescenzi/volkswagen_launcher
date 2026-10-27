package com.crescenzi.volkswagen_launcher.core

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import com.crescenzi.volkswagen_launcher.R
import com.crescenzi.volkswagen_launcher.service.MediaControlService

fun Context.hasNotificationAccess(): Boolean {
    val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        ?: return false
    val me = ComponentName(this, MediaControlService::class.java)
    return flat.split(":").any { ComponentName.unflattenFromString(it) == me }
}

fun Context.safeOpen(intent: Intent) {
    try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        Toast.makeText(this, getString(R.string.cannot_open), Toast.LENGTH_SHORT).show()
    }
}
