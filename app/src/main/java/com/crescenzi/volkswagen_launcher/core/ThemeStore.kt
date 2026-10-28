package com.crescenzi.volkswagen_launcher.core

import android.content.Context

// Persists the user's light/night choice. Absent = follow the system setting.
object ThemeStore {
    private const val PREFS = "theme"
    private const val KEY_DARK = "dark"

    fun loadDark(context: Context, fallback: Boolean): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return if (prefs.contains(KEY_DARK)) prefs.getBoolean(KEY_DARK, fallback) else fallback
    }

    fun saveDark(context: Context, dark: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DARK, dark)
            .apply()
    }
}
