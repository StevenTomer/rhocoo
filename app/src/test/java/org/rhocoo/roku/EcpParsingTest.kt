package org.rhocoo.roku

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EcpParsingTest {

    @Test fun hostFromSsdpResponse() {
        val response = "HTTP/1.1 200 OK\r\nCache-Control: max-age=3600\r\nST: roku:ecp\r\n" +
            "Location: http://192.168.1.134:8060/\r\nUSN: uuid:roku:ecp:P0A070000007\r\n\r\n"
        assertEquals("192.168.1.134:8060", EcpParsing.hostFromSsdp(response))
    }

    @Test fun hostFromSsdpWithoutLocation() {
        assertNull(EcpParsing.hostFromSsdp("HTTP/1.1 200 OK\r\nST: roku:ecp\r\n\r\n"))
    }

    @Test fun deviceInfoPrefersUserName() {
        val xml = """
            <device-info>
              <serial-number>X01234567</serial-number>
              <model-name>7000X</model-name>
              <friendly-model-name>TCL&amp;Roku TV</friendly-model-name>
              <friendly-device-name>65" TCL Roku TV</friendly-device-name>
              <user-device-name>Living Room</user-device-name>
            </device-info>
        """.trimIndent()
        val d = EcpParsing.parseDeviceInfo(xml, "10.0.0.5:8060")!!
        assertEquals(RokuDevice("X01234567", "Living Room", "TCL&Roku TV", "10.0.0.5:8060"), d)
    }

    @Test fun deviceInfoFallsBackToFriendlyName() {
        val xml = "<device-info><serial-number>S1</serial-number><model-name>3930X</model-name>" +
            "<friendly-device-name>Roku Express</friendly-device-name><user-device-name></user-device-name></device-info>"
        assertEquals("Roku Express", EcpParsing.parseDeviceInfo(xml, "h")!!.name)
    }

    @Test fun deviceInfoWithoutSerialIsRejected() {
        assertNull(EcpParsing.parseDeviceInfo("<device-info><model-name>x</model-name></device-info>", "h"))
    }
}
