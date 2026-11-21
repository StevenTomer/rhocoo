package org.rhocoo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.rhocoo.roku.RokuDevice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicePickerScreen(
    devices: List<RokuDevice>,
    scanning: Boolean,
    selectedSerial: String?,
    onScan: () -> Unit,
    onChoose: (RokuDevice) -> Unit,
    onManual: (String) -> Unit,
    onCancel: (() -> Unit)?,
    snackbar: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choose your Roku") },
                actions = { if (onCancel != null) TextButton(onClick = onCancel) { Text("Cancel") } },
            )
        },
        snackbarHost = snackbar,
    ) { padding ->
        // consumeWindowInsets + imePadding: lift the IP field above the keyboard without double-counting the nav bar.
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (scanning) "Searching your Wi-Fi…" else "${devices.size} found",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                if (scanning) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Button(onClick = onScan) { Text("Scan again") }
                }
            }
            HorizontalDivider()
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = 4.dp)) {
                items(devices, key = { it.serial }) { device ->
                    DeviceRow(device, device.serial == selectedSerial, onChoose)
                }
            }
            HorizontalDivider()
            ManualEntry(onManual)
        }
    }
}

/** One Roku in a picker list; shared by the full-screen picker and the notification's dialog. */
@Composable
internal fun DeviceRow(device: RokuDevice, selected: Boolean, onChoose: (RokuDevice) -> Unit) {
    ListItem(
        headlineContent = { Text(device.name) },
        supportingContent = { Text("${device.model} · ${device.host.substringBefore(':')}") },
        leadingContent = { RadioButton(selected = selected, onClick = { onChoose(device) }) },
        modifier = Modifier.clickable { onChoose(device) },
    )
}

@Composable
internal fun ManualEntry(onManual: (String) -> Unit) {
    var ip by rememberSaveable { mutableStateOf("") }
    Row(
        Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = ip,
            onValueChange = { ip = it },
            label = { Text("Or enter the TV's IP address") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { onManual(ip) }),
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(4.dp))
        Button(onClick = { onManual(ip) }, enabled = ip.isNotBlank()) { Text("Add") }
    }
}
