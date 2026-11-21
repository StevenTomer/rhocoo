package org.rhocoo.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.rhocoo.notify.RemoteNotification

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val permissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        RemoteNotification.sync(this)
        if (viewModel.picking) viewModel.scan()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val needed = buildList {
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
            if (Build.VERSION.SDK_INT >= 37) add(Manifest.permission.ACCESS_LOCAL_NETWORK)
        }
        if (savedInstanceState == null) {
            if (needed.isNotEmpty()) permissions.launch(needed.toTypedArray())
            else if (viewModel.picking) viewModel.scan()
        }

        setContent {
            RhoCooTheme {
                val snackbar = remember { SnackbarHostState() }
                LaunchedEffect(Unit) {
                    viewModel.messages.collect { snackbar.showSnackbar(it) }
                }
                val selected by viewModel.selected.collectAsStateWithLifecycle()
                val notifOn by viewModel.notificationEnabled.collectAsStateWithLifecycle()
                val device = selected
                if (viewModel.picking || device == null) {
                    DevicePickerScreen(
                        devices = viewModel.found,
                        scanning = viewModel.scanning,
                        selectedSerial = device?.serial,
                        onScan = viewModel::scan,
                        onChoose = viewModel::choose,
                        onManual = viewModel::addManual,
                        onCancel = if (device != null) viewModel::cancelPicking else null,
                        snackbar = { SnackbarHost(snackbar) },
                    )
                } else {
                    RemoteScreen(
                        device = device,
                        notificationEnabled = notifOn,
                        onNotificationEnabled = viewModel::setNotificationEnabled,
                        onChangeDevice = viewModel::startPicking,
                        onKey = viewModel::press,
                        onType = viewModel::type,
                        snackbar = { SnackbarHost(snackbar) },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        RemoteNotification.sync(this)
    }
}
