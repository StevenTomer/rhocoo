package org.rhocoo.notify

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import org.rhocoo.roku.DeviceStore

/** Quick Settings tile that shows or hides the notification remote. */
class RemoteTileService : TileService() {

    override fun onStartListening() = refresh()

    override fun onClick() {
        val store = DeviceStore.get(this)
        store.setNotificationEnabled(!store.notificationEnabled.value)
        RemoteNotification.sync(this)
        refresh()
    }

    private fun refresh() {
        val tile = qsTile ?: return
        val store = DeviceStore.get(this)
        val device = store.selected.value
        tile.state = when {
            device == null || !RemoteNotification.canPost(this) -> Tile.STATE_UNAVAILABLE
            store.notificationEnabled.value -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        if (android.os.Build.VERSION.SDK_INT >= 29) tile.subtitle = device?.name
        tile.updateTile()
    }
}
