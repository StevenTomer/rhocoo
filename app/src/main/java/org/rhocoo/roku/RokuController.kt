package org.rhocoo.roku

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import java.io.IOException

/** Why a command didn't reach the TV. */
enum class SendFailure { NoDevice, EcpDisabled, Unreachable }

/**
 * Sends commands to the selected Roku. Commands run one at a time, in tap order, so rapid
 * taps (e.g. volume) arrive in sequence. If the saved IP stops answering, it rediscovers the
 * same TV by serial number and retries once.
 */
object RokuController {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val serial = Dispatchers.IO.limitedParallelism(1)

    /** Process-wide scope for work started from broadcast receivers and tiles. */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Returns null on success. */
    suspend fun send(context: Context, key: RokuKey): SendFailure? =
        run(context) { it.keypress(key) }

    suspend fun type(context: Context, text: String): SendFailure? =
        run(context) { client -> text.forEach { client.literal(it) } }

    private suspend fun run(context: Context, block: suspend (RokuClient) -> Unit): SendFailure? =
        withContext(serial) {
            val store = DeviceStore.get(context)
            val device = store.selected.value ?: return@withContext SendFailure.NoDevice
            try {
                block(RokuClient(device.host))
                null
            } catch (_: EcpDisabledException) {
                SendFailure.EcpDisabled
            } catch (_: IOException) {
                // Maybe the TV got a new IP address: look for the same serial number.
                val moved = runCatching { RokuDiscovery.discover(context, timeoutMs = 2000) }
                    .getOrDefault(emptyList())
                    .firstOrNull { it.serial == device.serial && it.host != device.host }
                    ?: return@withContext SendFailure.Unreachable
                store.updateHost(moved.serial, moved.host)
                try {
                    block(RokuClient(moved.host))
                    null
                } catch (_: EcpDisabledException) {
                    SendFailure.EcpDisabled
                } catch (_: IOException) {
                    SendFailure.Unreachable
                }
            }
        }
}
