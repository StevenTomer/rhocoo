package org.rhocoo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.rhocoo.roku.RokuDevice
import org.rhocoo.roku.RokuKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteScreen(
    device: RokuDevice,
    notificationEnabled: Boolean,
    onNotificationEnabled: (Boolean) -> Unit,
    onChangeDevice: () -> Unit,
    onKey: (RokuKey) -> Unit,
    onType: (String) -> Unit,
    snackbar: @Composable () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val press: (RokuKey) -> Unit = {
        haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
        onKey(it)
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(device.name)
                        Text(device.model, style = MaterialTheme.typography.bodySmall)
                    }
                },
                actions = { TextButton(onClick = onChangeDevice) { Text("Change device") } },
            )
        },
        snackbarHost = snackbar,
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Remote in notification shade", Modifier.weight(1f))
                Switch(checked = notificationEnabled, onCheckedChange = onNotificationEnabled)
            }

            KeyRow(RokuKey.Power, RokuKey.Back, RokuKey.Home, onKey = press)
            DPad(press)
            KeyRow(RokuKey.Rev, RokuKey.Play, RokuKey.Fwd, RokuKey.Replay, onKey = press)
            KeyRow(RokuKey.Mute, RokuKey.VolumeDown, RokuKey.VolumeUp, onKey = press)
            TypeOnTv(onType)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun KeyRow(vararg keys: RokuKey, onKey: (RokuKey) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        keys.forEach { KeyButton(it, onKey) }
    }
}

@Composable
private fun KeyButton(key: RokuKey, onKey: (RokuKey) -> Unit, size: Dp = 60.dp) {
    FilledTonalIconButton(onClick = { onKey(key) }, modifier = Modifier.size(size)) {
        Icon(painterResource(key.icon), contentDescription = key.label, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun DPad(onKey: (RokuKey) -> Unit) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            KeyButton(RokuKey.Up, onKey, 68.dp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                KeyButton(RokuKey.Left, onKey, 68.dp)
                Box(Modifier.padding(8.dp)) {
                    FilledIconButton(onClick = { onKey(RokuKey.Select) }, modifier = Modifier.size(76.dp)) {
                        Text("OK", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
                KeyButton(RokuKey.Right, onKey, 68.dp)
            }
            KeyButton(RokuKey.Down, onKey, 68.dp)
        }
    }
}

@Composable
private fun TypeOnTv(onType: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val send = {
        onType(text)
        text = ""
    }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text("Type on TV (search boxes)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = { send() }),
        trailingIcon = { TextButton(onClick = send, enabled = text.isNotEmpty()) { Text("Send") } },
        modifier = Modifier.fillMaxWidth(),
    )
}
