package org.rhocoo.roku

/** Pure parsing helpers for ECP/SSDP payloads (kept free of Android APIs so they unit-test on the JVM). */
internal object EcpParsing {

    /** Extracts "host:port" from an SSDP response's LOCATION header, e.g. `http://192.168.1.5:8060/`. */
    fun hostFromSsdp(response: String): String? {
        val location = response.lineSequence()
            .firstOrNull { it.startsWith("LOCATION:", ignoreCase = true) }
            ?.substringAfter(':')?.trim() ?: return null
        return Regex("""^https?://([^/]+)""", RegexOption.IGNORE_CASE)
            .find(location)?.groupValues?.get(1)
    }

    /** Builds a [RokuDevice] from `/query/device-info` XML. Returns null if there is no serial number. */
    fun parseDeviceInfo(xml: String, host: String): RokuDevice? {
        val serial = tag(xml, "serial-number") ?: return null
        val model = tag(xml, "friendly-model-name") ?: tag(xml, "model-name") ?: "Roku"
        val name = tag(xml, "user-device-name")
            ?: tag(xml, "friendly-device-name")
            ?: tag(xml, "default-device-name")
            ?: model
        return RokuDevice(serial = serial, name = name, model = model, host = host)
    }

    private fun tag(xml: String, name: String): String? =
        Regex("<$name>(.*?)</$name>", RegexOption.DOT_MATCHES_ALL)
            .find(xml)?.groupValues?.get(1)?.trim()?.let(::unescape)?.takeIf { it.isNotEmpty() }

    private fun unescape(s: String) = s
        .replace("&lt;", "<").replace("&gt;", ">")
        .replace("&quot;", "\"").replace("&apos;", "'").replace("&amp;", "&")
}
