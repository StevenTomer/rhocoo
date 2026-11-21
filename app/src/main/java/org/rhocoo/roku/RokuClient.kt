package org.rhocoo.roku

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** The TV answered but refused control: "Control by mobile apps" is turned off on the Roku. */
class EcpDisabledException : IOException("Roku refused control (Control by mobile apps is disabled)")

/** Talks ECP (plain HTTP on port 8060) to one Roku at [host] ("ip:port"). */
class RokuClient(private val host: String) {

    suspend fun keypress(key: RokuKey) = post("/keypress/${key.ecp}")

    /** Types one character into the focused text field on the TV. */
    suspend fun literal(c: Char) = post("/keypress/Lit_" + URLEncoder.encode(c.toString(), "UTF-8"))

    suspend fun deviceInfo(): RokuDevice? =
        EcpParsing.parseDeviceInfo(get("/query/device-info"), host)

    private suspend fun post(path: String) = withContext(Dispatchers.IO) {
        request(path, "POST")
        Unit
    }

    private suspend fun get(path: String): String = withContext(Dispatchers.IO) {
        request(path, "GET")
    }

    private fun request(path: String, method: String): String {
        val conn = URL("http://$host$path").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = CONNECT_TIMEOUT_MS
            conn.readTimeout = READ_TIMEOUT_MS
            conn.useCaches = false
            if (method == "POST") {
                conn.doOutput = true
                conn.setFixedLengthStreamingMode(0)
                conn.outputStream.close()
            }
            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_FORBIDDEN) throw EcpDisabledException()
            if (code !in 200..299) throw IOException("HTTP $code from $host$path")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        const val PORT = 8060
        private const val CONNECT_TIMEOUT_MS = 1500
        private const val READ_TIMEOUT_MS = 2500
    }
}
