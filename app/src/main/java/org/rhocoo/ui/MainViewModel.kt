package org.rhocoo.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import org.rhocoo.R
import org.rhocoo.notify.RemoteNotification
import org.rhocoo.roku.DeviceStore
import org.rhocoo.roku.RokuClient
import org.rhocoo.roku.RokuController
import org.rhocoo.roku.RokuDevice
import org.rhocoo.roku.RokuDiscovery
import org.rhocoo.roku.RokuKey
import org.rhocoo.roku.SendFailure
import org.rhocoo.roku.EcpDisabledException

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val store = DeviceStore.get(app)
    val selected = store.selected
    val notificationEnabled = store.notificationEnabled

    /** True while the device picker is showing: on first run, or after "Change device". */
    var picking by mutableStateOf(store.selected.value == null)
        private set
    var scanning by mutableStateOf(false)
        private set
    var found by mutableStateOf<List<RokuDevice>>(emptyList())
        private set

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    fun startPicking() {
        picking = true
        scan()
    }

    fun cancelPicking() {
        if (store.selected.value != null) picking = false
    }

    fun scan() {
        if (scanning) return
        scanning = true
        viewModelScope.launch {
            val result = runCatching { RokuDiscovery.discover(getApplication()) }.getOrDefault(emptyList())
            // Keep manually added devices that discovery didn't see.
            found = (result + found).distinctBy { it.serial }.sortedBy { it.name.lowercase() }
            scanning = false
            if (found.isEmpty()) _messages.tryEmit("No Rokus found. Check Wi-Fi, or enter the TV's IP address.")
        }
    }

    fun addManual(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return
        val host = if (':' in trimmed) trimmed else "$trimmed:${RokuClient.PORT}"
        viewModelScope.launch {
            val device = try {
                RokuClient(host).deviceInfo()
            } catch (_: EcpDisabledException) {
                _messages.tryEmit(getApplication<Application>().getString(R.string.ecp_disabled_hint))
                return@launch
            } catch (_: Exception) {
                null
            }
            if (device == null) {
                _messages.tryEmit("No Roku answered at $host.")
            } else {
                found = (listOf(device) + found).distinctBy { it.serial }
                choose(device)
            }
        }
    }

    fun choose(device: RokuDevice) {
        store.select(device)
        picking = false
        RemoteNotification.sync(getApplication())
    }

    fun press(key: RokuKey) {
        viewModelScope.launch { report(RokuController.send(getApplication(), key)) }
    }

    fun type(text: String) {
        if (text.isEmpty()) return
        viewModelScope.launch { report(RokuController.type(getApplication(), text)) }
    }

    fun setNotificationEnabled(enabled: Boolean) {
        store.setNotificationEnabled(enabled)
        RemoteNotification.sync(getApplication())
    }

    private fun report(failure: SendFailure?) {
        val app = getApplication<Application>()
        val message = when (failure) {
            null -> return
            SendFailure.NoDevice -> app.getString(R.string.no_device)
            SendFailure.EcpDisabled -> app.getString(R.string.ecp_disabled_hint)
            SendFailure.Unreachable -> app.getString(R.string.unreachable, selected.value?.name ?: "the TV")
        }
        _messages.tryEmit(message)
    }
}
