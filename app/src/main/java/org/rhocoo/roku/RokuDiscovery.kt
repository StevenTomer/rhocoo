package org.rhocoo.roku

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

/** Finds Rokus on the Wi-Fi network with an SSDP M-SEARCH for `roku:ecp`. */
object RokuDiscovery {

    private const val SSDP_ADDR = "239.255.255.250"
    private const val SSDP_PORT = 1900
    private val M_SEARCH = (
        "M-SEARCH * HTTP/1.1\r\n" +
            "HOST: $SSDP_ADDR:$SSDP_PORT\r\n" +
            "MAN: \"ssdp:discover\"\r\n" +
            "ST: roku:ecp\r\n" +
            "MX: 2\r\n\r\n"
        ).toByteArray()

    /** Returns every Roku that answered within [timeoutMs], with names from `/query/device-info`. */
    suspend fun discover(context: Context, timeoutMs: Int = 3000): List<RokuDevice> {
        val hosts = findHosts(context, timeoutMs)
        return coroutineScope {
            hosts.map { host ->
                async { runCatching { RokuClient(host).deviceInfo() }.getOrNull() }
            }.awaitAll()
        }.filterNotNull().distinctBy { it.serial }.sortedBy { it.name.lowercase() }
    }

    private suspend fun findHosts(context: Context, timeoutMs: Int): Set<String> =
        withContext(Dispatchers.IO) {
            val wifi = context.applicationContext.getSystemService(WifiManager::class.java)
            val lock = wifi.createMulticastLock("rhocoo-ssdp").apply { setReferenceCounted(false) }
            val hosts = linkedSetOf<String>()
            lock.acquire()
            try {
                DatagramSocket().use { socket ->
                    val group = InetAddress.getByName(SSDP_ADDR)
                    // UDP is lossy; send the search a few times.
                    repeat(3) { socket.send(DatagramPacket(M_SEARCH, M_SEARCH.size, group, SSDP_PORT)) }
                    val deadline = System.currentTimeMillis() + timeoutMs
                    val buf = ByteArray(2048)
                    while (true) {
                        val left = deadline - System.currentTimeMillis()
                        if (left <= 0) break
                        socket.soTimeout = left.toInt()
                        val packet = DatagramPacket(buf, buf.size)
                        try {
                            socket.receive(packet)
                        } catch (_: SocketTimeoutException) {
                            break
                        }
                        EcpParsing.hostFromSsdp(String(packet.data, 0, packet.length))?.let(hosts::add)
                    }
                }
            } finally {
                lock.release()
            }
            hosts
        }
}
