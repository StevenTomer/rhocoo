package org.rhocoo.notify

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.ServiceCompat

/**
 * Holds the remote notification as a foreground service so Android keeps it at the top of the
 * notification shade. It does no work and uses no battery: it only owns the notification.
 */
class RemoteService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = if (RemoteNotification.shouldShow(this)) {
            RemoteNotification.build(this, colorized = true)
        } else {
            null
        }
        if (notification == null) {
            // startForegroundService() requires startForeground() even when stopping right away.
            RemoteNotification.build(this, colorized = false)?.let { startAsForeground(it) }
            stopSelf()
            return START_NOT_STICKY
        }
        startAsForeground(notification)
        return START_STICKY
    }

    private fun startAsForeground(notification: android.app.Notification) {
        ServiceCompat.startForeground(
            this, RemoteNotification.NOTIFICATION_ID, notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
