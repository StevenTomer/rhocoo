package org.rhocoo.notify

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.service.quicksettings.TileService
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.rhocoo.R
import org.rhocoo.roku.DeviceStore
import org.rhocoo.roku.RokuKey
import org.rhocoo.ui.DeviceDialogActivity
import org.rhocoo.ui.MainActivity

/** The mini remote that lives in the notification shade. */
object RemoteNotification {

    private const val OLD_CHANNEL_ID = "remote"
    private const val CHANNEL_ID = "remote_top"
    const val NOTIFICATION_ID = 1

    /** View ids in the notification layouts, and the key each one sends. */
    private val buttons = mapOf(
        R.id.key_volume_down to RokuKey.VolumeDown,
        R.id.key_volume_up to RokuKey.VolumeUp,
        R.id.key_left to RokuKey.Left,
        R.id.key_right to RokuKey.Right,
        R.id.key_up to RokuKey.Up,
        R.id.key_down to RokuKey.Down,
        R.id.key_select to RokuKey.Select,
        R.id.key_play to RokuKey.Play,
        R.id.key_back to RokuKey.Back,
        R.id.key_home to RokuKey.Home,
        R.id.key_rev to RokuKey.Rev,
        R.id.key_fwd to RokuKey.Fwd,
        R.id.key_replay to RokuKey.Replay,
        R.id.key_mute to RokuKey.Mute,
        R.id.key_power to RokuKey.Power,
    )

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Shows (or refreshes) the remote if it is enabled and a Roku is selected; otherwise removes it.
     *
     * To keep the remote at the top of the shade, it is held by [RemoteService], a foreground
     * service that does no work: Android puts colorized foreground-service notifications in its
     * own section above conversations and everyday notifications (the slot navigation apps use).
     * Android only allows starting that service at certain moments (app on screen, boot, a tap on
     * the notification...); when it refuses, the remote is posted as a plain notification instead.
     */
    fun sync(context: Context) {
        if (shouldShow(context)) {
            ensureChannel(context)
            val inForeground = try {
                ContextCompat.startForegroundService(context, Intent(context, RemoteService::class.java))
                true
            } catch (_: IllegalStateException) {
                false // ForegroundServiceStartNotAllowedException (Android 12+) or background start limits
            } catch (_: SecurityException) {
                false
            }
            if (!inForeground) {
                @Suppress("MissingPermission") // shouldShow() checked canPost()
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, build(context, colorized = false)!!)
            }
        } else {
            context.stopService(Intent(context, RemoteService::class.java))
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        }
        TileService.requestListeningState(context, ComponentName(context, RemoteTileService::class.java))
    }

    fun shouldShow(context: Context): Boolean {
        val store = DeviceStore.get(context)
        return store.notificationEnabled.value && store.selected.value != null && canPost(context)
    }

    /**
     * The remote notification, or null if no Roku is selected. [colorized] fills the card with the
     * brand purple (only honoured for foreground services) and switches the icons to white.
     */
    fun build(context: Context, colorized: Boolean): Notification? {
        val device = DeviceStore.get(context).selected.value ?: return null
        ensureChannel(context)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_rhocoo)
            .setColor(ContextCompat.getColor(context, R.color.brand_purple))
            .setColorized(colorized)
            .setSubText(device.name)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(remoteViews(context, R.layout.notif_remote_collapsed, colorized))
            .setCustomBigContentView(remoteViews(context, R.layout.notif_remote_expanded, colorized))
            .setContentIntent(openApp(context))
            .setDeleteIntent(RemoteKeyReceiver.dismissedIntent(context))
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun remoteViews(context: Context, layout: Int, colorized: Boolean) =
        RemoteViews(context.packageName, layout).apply {
            for ((id, key) in buttons) {
                setOnClickPendingIntent(id, RemoteKeyReceiver.keyIntent(context, key))
            }
            // Only the expanded layout has the gear.
            if (layout == R.layout.notif_remote_expanded) {
                setOnClickPendingIntent(R.id.key_settings, chooseDevice(context))
            }
            if (colorized) {
                // White on the purple card. (Views missing from this layout are skipped by RemoteViews.)
                for (id in buttons.keys + R.id.key_settings) {
                    if (id == R.id.key_select) setTextColor(id, Color.WHITE)
                    else setInt(id, "setColorFilter", Color.WHITE)
                }
            }
        }

    private fun openApp(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    /** Opens the floating "Remote controls" dialog over whatever app is in front. */
    private fun chooseDevice(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context, 1,
            Intent(context, DeviceDialogActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        // 0.1/0.2 used a default-importance channel; an app can't raise a channel's importance
        // later, so that one is replaced.
        manager.deleteNotificationChannel(OLD_CHANNEL_ID)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        // High importance ranks the remote above everyday notifications. It stays silent: no
        // sound, no vibration, and setSilent() keeps it from popping up over other apps.
        val channel = NotificationChannel(
            CHANNEL_ID, context.getString(R.string.channel_remote), NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_remote_desc)
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }
}
