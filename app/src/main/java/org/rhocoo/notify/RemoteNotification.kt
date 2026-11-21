package org.rhocoo.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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

    private const val CHANNEL_ID = "remote"
    private const val NOTIFICATION_ID = 1

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

    /** Posts (or refreshes) the remote if it is enabled and a Roku is selected; otherwise removes it. */
    fun sync(context: Context) {
        val store = DeviceStore.get(context)
        val device = store.selected.value
        if (!store.notificationEnabled.value || device == null || !canPost(context)) {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        } else {
            ensureChannel(context)
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_rhocoo)
                .setColor(ContextCompat.getColor(context, R.color.brand_purple))
                .setSubText(device.name)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(remoteViews(context, R.layout.notif_remote_collapsed))
                .setCustomBigContentView(remoteViews(context, R.layout.notif_remote_expanded))
                .setContentIntent(openApp(context))
                .setDeleteIntent(RemoteKeyReceiver.dismissedIntent(context))
                .setOngoing(true)
                .setSilent(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            @Suppress("MissingPermission") // checked by canPost()
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
        TileService.requestListeningState(context, ComponentName(context, RemoteTileService::class.java))
    }

    private fun remoteViews(context: Context, layout: Int) =
        RemoteViews(context.packageName, layout).apply {
            for ((id, key) in buttons) {
                setOnClickPendingIntent(id, RemoteKeyReceiver.keyIntent(context, key))
            }
            // Only the expanded layout has the gear.
            if (layout == R.layout.notif_remote_expanded) {
                setOnClickPendingIntent(R.id.key_settings, chooseDevice(context))
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
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        // Default importance keeps the remote in the main section of the shade, not under
        // "Silent"; sound and vibration are off so it never makes noise.
        val channel = NotificationChannel(
            CHANNEL_ID, context.getString(R.string.channel_remote), NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.channel_remote_desc)
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }
}
