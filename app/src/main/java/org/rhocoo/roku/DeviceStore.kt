package org.rhocoo.roku

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The chosen Roku and app settings. The selection persists until the user picks another device;
 * only [updateHost] changes it behind the scenes (same serial, new IP after a DHCP change).
 */
class DeviceStore private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("rhocoo", Context.MODE_PRIVATE)

    private val _selected = MutableStateFlow(read())
    val selected: StateFlow<RokuDevice?> = _selected.asStateFlow()

    private val _notificationEnabled = MutableStateFlow(prefs.getBoolean(KEY_NOTIF, true))
    val notificationEnabled: StateFlow<Boolean> = _notificationEnabled.asStateFlow()

    /** Every Roku seen so far (found by a scan or entered by IP), so pickers can list them even if a scan misses one. */
    val known: List<RokuDevice>
        get() = prefs.getStringSet(KEY_KNOWN, emptySet()).orEmpty()
            .mapNotNull(::decode).sortedBy { it.name.lowercase() }

    fun remember(devices: Collection<RokuDevice>) {
        if (devices.isEmpty()) return
        val merged = (devices + known).distinctBy { it.serial }
        prefs.edit { putStringSet(KEY_KNOWN, merged.map(::encode).toSet()) }
    }

    fun select(device: RokuDevice) {
        remember(listOf(device))
        prefs.edit {
            putString(KEY_SERIAL, device.serial)
            putString(KEY_NAME, device.name)
            putString(KEY_MODEL, device.model)
            putString(KEY_HOST, device.host)
        }
        _selected.value = device
    }

    fun updateHost(serial: String, host: String) {
        val current = _selected.value ?: return
        if (current.serial != serial || current.host == host) return
        prefs.edit { putString(KEY_HOST, host) }
        _selected.value = current.copy(host = host)
    }

    fun setNotificationEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_NOTIF, enabled) }
        _notificationEnabled.value = enabled
    }

    private fun read(): RokuDevice? {
        val serial = prefs.getString(KEY_SERIAL, null) ?: return null
        val host = prefs.getString(KEY_HOST, null) ?: return null
        return RokuDevice(
            serial = serial,
            name = prefs.getString(KEY_NAME, null) ?: "Roku",
            model = prefs.getString(KEY_MODEL, null) ?: "",
            host = host,
        )
    }

    private fun encode(d: RokuDevice) = listOf(d.serial, d.name, d.model, d.host).joinToString(SEP)

    private fun decode(s: String): RokuDevice? {
        val p = s.split(SEP)
        return if (p.size == 4) RokuDevice(p[0], p[1], p[2], p[3]) else null
    }

    companion object {
        private const val SEP = "\u001F" // unit separator: can't appear in a TV name
        private const val KEY_KNOWN = "known_devices"
        private const val KEY_SERIAL = "serial"
        private const val KEY_NAME = "name"
        private const val KEY_MODEL = "model"
        private const val KEY_HOST = "host"
        private const val KEY_NOTIF = "notification_enabled"

        @Volatile private var instance: DeviceStore? = null

        fun get(context: Context): DeviceStore =
            instance ?: synchronized(this) {
                instance ?: DeviceStore(context).also { instance = it }
            }
    }
}
