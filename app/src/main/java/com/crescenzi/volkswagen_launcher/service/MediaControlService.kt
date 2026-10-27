package com.crescenzi.volkswagen_launcher.service

import android.service.notification.NotificationListenerService

/**
 * Enables reading and controlling the system's active MediaSessions
 * (MediaSessionManager.getActiveSessions requires an enabled notification
 * listener). It only needs to exist and be granted notification access.
 */
class MediaControlService : NotificationListenerService()
