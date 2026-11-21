package org.rhocoo.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.rhocoo.roku.RokuDevice

/**
 * A small floating dialog, opened from the notification's gear button, for switching which
 * Roku the remote controls. It is translucent and runs in its own task, so the app the user was
 * in stays visible behind it and is back in front as soon as the dialog closes.
 */
class DeviceDialogActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.scan()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // let the dim scrim run under the status and navigation bars
        if (savedInstanceState == null) {
            if (Build.VERSION.SDK_INT >= 37 &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_LOCAL_NETWORK) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                permission.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
            } else {
                viewModel.scan()
            }
        }
        setContent {
            RhoCooTheme {
                val selected by viewModel.selected.collectAsStateWithLifecycle()
                var message by remember { mutableStateOf<String?>(null) }
                LaunchedEffect(Unit) { viewModel.messages.collect { message = it } }
                DeviceDialog(
                    devices = (listOfNotNull(selected) + viewModel.found).distinctBy { it.serial },
                    selectedSerial = selected?.serial,
                    scanning = viewModel.scanning,
                    message = message,
                    onScan = { message = null; viewModel.scan() },
                    onChoose = viewModel::choose,
                    onManual = { message = null; viewModel.addManual(it) },
                    onDismiss = ::finish,
                )
                // Close as soon as a TV is chosen, from the list or by IP address.
                LaunchedEffect(Unit) { viewModel.chosen.collect { finish() } }
            }
        }
    }
}

@Composable
private fun DeviceDialog(
    devices: List<RokuDevice>,
    selectedSerial: String?,
    scanning: Boolean,
    message: String?,
    onScan: () -> Unit,
    onChoose: (RokuDevice) -> Unit,
    onManual: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    // Scrim: tapping outside the card closes the dialog.
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f))
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
            .imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
            modifier = Modifier.padding(24.dp).widthIn(max = 480.dp).fillMaxWidth()
                // Swallow taps on the card so they don't reach the scrim.
                .clickable(remember { MutableInteractionSource() }, indication = null) {},
        ) {
            Column(Modifier.padding(top = 24.dp, bottom = 8.dp)) {
                Text(
                    "Remote controls",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                Row(
                    Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        message ?: if (scanning) "Searching your Wi-Fi…" else "Choose the TV",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    if (scanning) {
                        CircularProgressIndicator(Modifier.padding(12.dp).size(20.dp), strokeWidth = 2.dp)
                    } else {
                        TextButton(onClick = onScan) { Text("Scan again") }
                    }
                }
                HorizontalDivider(Modifier.padding(top = 8.dp))
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(devices, key = { it.serial }) { device ->
                        DeviceRow(device, device.serial == selectedSerial, onChoose)
                    }
                }
                HorizontalDivider()
                ManualEntry(onManual)
                Row(Modifier.fillMaxWidth().padding(end = 12.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
            }
        }
    }
}
