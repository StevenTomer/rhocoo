package org.rhocoo.notify

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.rhocoo.R
import org.rhocoo.roku.DeviceStore
import org.rhocoo.roku.RokuController
import org.rhocoo.roku.RokuKey
import org.rhocoo.roku.SendFailure

/**
 * Handles taps on the notification buttons. It sends the key straight to the TV without
 * opening any screen, so the app in front stays in front.
 */
class RemoteKeyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        when (intent.action) {
            ACTION_KEY -> {
                val key = intent.getStringExtra(EXTRA_KEY)
                    ?.let { name -> RokuKey.entries.firstOrNull { it.name == name } } ?: return
                val pending = goAsync()
                RokuController.scope.launch {
                    try {
                        RokuController.send(app, key)?.let { showFailure(app, it) }
                    } finally {
                        pending.finish()
                    }
                }
            }
            // Swiping the remote away (allowed for ongoing notifications since Android 14) turns it
            // off; the Quick Settings tile or the app brings it back.
            ACTION_DISMISSED -> {
                DeviceStore.get(app).setNotificationEnabled(false)
                RemoteNotification.sync(app)
            }
        }
    }

    private suspend fun showFailure(context: Context, failure: SendFailure) {
        val message = when (failure) {
            SendFailure.NoDevice -> context.getString(R.string.no_device)
            SendFailure.EcpDisabled -> context.getString(R.string.ecp_disabled_hint)
            SendFailure.Unreachable -> context.getString(
                R.string.unreachable, DeviceStore.get(context).selected.value?.name ?: "the TV",
            )
        }
        withContext(Dispatchers.Main) { Toast.makeText(context, message, Toast.LENGTH_LONG).show() }
    }

    companion object {
        private const val ACTION_KEY = "org.rhocoo.action.KEY"
        private const val ACTION_DISMISSED = "org.rhocoo.action.DISMISSED"
        private const val EXTRA_KEY = "key"

        fun keyIntent(context: Context, key: RokuKey): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                key.ordinal + 1, // a distinct request code per key keeps the PendingIntents separate
                Intent(context, RemoteKeyReceiver::class.java)
                    .setAction(ACTION_KEY)
                    .putExtra(EXTRA_KEY, key.name),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        fun dismissedIntent(context: Context): PendingIntent =
            PendingIntent.getBroadcast(
                context, 0,
                Intent(context, RemoteKeyReceiver::class.java).setAction(ACTION_DISMISSED),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
    }
}
