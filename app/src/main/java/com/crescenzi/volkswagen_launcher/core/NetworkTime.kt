package com.crescenzi.volkswagen_launcher.core

import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

// Offset (ms) to add to System.currentTimeMillis() to match network time, null if unavailable.
fun networkTimeOffset(): Long? {
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
