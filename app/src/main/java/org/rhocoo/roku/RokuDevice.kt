package org.rhocoo.roku

/** A Roku on the local network. [serial] is the stable identity; [host] may change with DHCP. */
data class RokuDevice(
    val serial: String,
    val name: String,
    val model: String,
    val host: String,
)
