package com.alexcupsa.wifithermal.core.engine.wifi

/**
 * OUI (Organizationally Unique Identifier) lookup table for WiFi AP vendors.
 *
 * Maps the first 3 octets of a BSSID (e.g. "AA:BB:CC") to the manufacturer
 * name. Contains the ~120 most common OUI prefixes seen in residential and
 * enterprise WiFi deployments, sourced from the IEEE MA-L registry.
 *
 * This is a static in-memory table -- no network or file I/O required.
 */
object OuiLookup {

    private val ouiTable: Map<String, String> = mapOf(
        // Cisco Systems
        "00:1A:2B" to "Cisco",
        "00:1B:0D" to "Cisco",
        "00:1C:0E" to "Cisco",
        "00:1E:BD" to "Cisco",
        "00:22:BD" to "Cisco",
        "00:25:45" to "Cisco",
        "00:26:0B" to "Cisco",
        "00:40:96" to "Cisco",
        "58:AC:78" to "Cisco",
        "68:86:A7" to "Cisco",
        "B0:AA:77" to "Cisco",

        // Cisco Meraki
        "00:18:0A" to "Meraki",
        "AC:17:C8" to "Meraki",
        "E8:ED:F3" to "Meraki",

        // TP-Link
        "14:CC:20" to "TP-Link",
        "30:B5:C2" to "TP-Link",
        "50:C7:BF" to "TP-Link",
        "54:C8:0F" to "TP-Link",
        "60:32:B1" to "TP-Link",
        "90:F6:52" to "TP-Link",
        "B0:4E:26" to "TP-Link",
        "C0:06:C3" to "TP-Link",
        "EC:08:6B" to "TP-Link",
        "98:DA:C4" to "TP-Link",

        // ASUS
        "04:D9:F5" to "ASUS",
        "10:C3:7B" to "ASUS",
        "1C:87:2C" to "ASUS",
        "2C:56:DC" to "ASUS",
        "38:D5:47" to "ASUS",
        "40:B0:76" to "ASUS",
        "60:45:CB" to "ASUS",
        "AC:9E:17" to "ASUS",
        "F4:6D:04" to "ASUS",

        // Netgear
        "00:14:6C" to "Netgear",
        "00:1E:2A" to "Netgear",
        "20:0C:C8" to "Netgear",
        "28:C6:8E" to "Netgear",
        "30:46:9A" to "Netgear",
        "4C:60:DE" to "Netgear",
        "84:1B:5E" to "Netgear",
        "A0:04:60" to "Netgear",
        "B0:7F:B9" to "Netgear",
        "C4:04:15" to "Netgear",
        "E4:F4:C6" to "Netgear",

        // Ubiquiti
        "04:18:D6" to "Ubiquiti",
        "18:E8:29" to "Ubiquiti",
        "24:5A:4C" to "Ubiquiti",
        "44:D9:E7" to "Ubiquiti",
        "68:72:51" to "Ubiquiti",
        "74:83:C2" to "Ubiquiti",
        "78:8A:20" to "Ubiquiti",
        "80:2A:A8" to "Ubiquiti",
        "B4:FB:E4" to "Ubiquiti",
        "DC:9F:DB" to "Ubiquiti",
        "F0:9F:C2" to "Ubiquiti",
        "FC:EC:DA" to "Ubiquiti",

        // Aruba / HPE
        "00:0B:86" to "Aruba",
        "00:1A:1E" to "Aruba",
        "00:24:6C" to "Aruba",
        "20:4C:03" to "Aruba",
        "24:DE:C6" to "Aruba",
        "40:E3:D6" to "Aruba",
        "6C:F3:7F" to "Aruba",
        "D8:C7:C8" to "Aruba",

        // Ruckus (CommScope)
        "00:25:C4" to "Ruckus",
        "24:79:2A" to "Ruckus",
        "58:B6:33" to "Ruckus",
        "74:91:1A" to "Ruckus",
        "AC:67:06" to "Ruckus",
        "C4:01:7C" to "Ruckus",
        "EC:58:EA" to "Ruckus",

        // Huawei
        "00:E0:FC" to "Huawei",
        "04:F9:38" to "Huawei",
        "20:A6:CD" to "Huawei",
        "48:46:FB" to "Huawei",
        "70:8A:09" to "Huawei",
        "88:28:B3" to "Huawei",
        "C8:D1:5E" to "Huawei",
        "CC:A2:23" to "Huawei",

        // D-Link
        "00:1B:11" to "D-Link",
        "00:22:B0" to "D-Link",
        "14:D6:4D" to "D-Link",
        "1C:7E:E5" to "D-Link",
        "28:10:7B" to "D-Link",
        "78:54:2E" to "D-Link",
        "B8:A3:86" to "D-Link",
        "C4:A8:1D" to "D-Link",

        // Linksys (Belkin)
        "00:04:5A" to "Linksys",
        "00:14:BF" to "Linksys",
        "20:AA:4B" to "Linksys",
        "58:6D:8F" to "Linksys",
        "C0:56:27" to "Linksys",

        // Apple
        "00:03:93" to "Apple",
        "00:1C:B3" to "Apple",
        "28:CF:E9" to "Apple",
        "3C:22:FB" to "Apple",
        "70:56:81" to "Apple",
        "A8:5C:2C" to "Apple",
        "AC:BC:32" to "Apple",
        "F0:D1:A9" to "Apple",

        // Samsung
        "00:07:AB" to "Samsung",
        "00:16:32" to "Samsung",
        "08:D4:2B" to "Samsung",
        "14:49:E0" to "Samsung",
        "30:07:4D" to "Samsung",
        "78:BD:BC" to "Samsung",
        "A0:82:1F" to "Samsung",
        "BC:72:B1" to "Samsung",

        // Intel
        "00:03:47" to "Intel",
        "00:13:CE" to "Intel",
        "3C:A9:F4" to "Intel",
        "68:05:CA" to "Intel",
        "8C:EC:4B" to "Intel",
        "A4:C4:94" to "Intel",

        // Qualcomm
        "00:03:7F" to "Qualcomm",
        "00:A0:C6" to "Qualcomm",
        "54:E4:3A" to "Qualcomm",
        "98:B8:E3" to "Qualcomm",

        // Broadcom
        "00:10:18" to "Broadcom",
        "00:90:4C" to "Broadcom",
        "28:80:88" to "Broadcom",
        "98:A4:04" to "Broadcom",

        // MediaTek
        "00:0C:E7" to "MediaTek",
        "00:0C:43" to "MediaTek",
        "18:0F:76" to "MediaTek",

        // Realtek
        "00:E0:4C" to "Realtek",
        "48:5D:60" to "Realtek",
        "52:54:00" to "Realtek",

        // MikroTik
        "00:0C:42" to "MikroTik",
        "18:FD:74" to "MikroTik",
        "48:8F:5A" to "MikroTik",
        "6C:3B:6B" to "MikroTik",
        "B8:69:F4" to "MikroTik",
        "CC:2D:E0" to "MikroTik",
        "E4:8D:8C" to "MikroTik",

        // Fortinet
        "00:09:0F" to "Fortinet",
        "08:5B:0E" to "Fortinet",
        "70:4C:A5" to "Fortinet",
        "90:6C:AC" to "Fortinet",

        // Sophos
        "00:1A:8C" to "Sophos",
        "B4:74:9F" to "Sophos",

        // Zyxel
        "00:13:49" to "Zyxel",
        "00:19:CB" to "Zyxel",
        "00:A0:C5" to "Zyxel",
        "40:4A:03" to "Zyxel",
        "BC:CF:4F" to "Zyxel",

        // AVM (FRITZ!Box)
        "24:65:11" to "AVM (FRITZ!Box)",
        "3C:A6:2F" to "AVM (FRITZ!Box)",
        "C8:0E:14" to "AVM (FRITZ!Box)",
        "E0:28:6D" to "AVM (FRITZ!Box)",

        // Google
        "00:1A:11" to "Google",
        "3C:5A:B4" to "Google",
        "54:60:09" to "Google",
        "94:EB:2C" to "Google",
        "A4:77:33" to "Google",
        "F4:F5:D8" to "Google",

        // Amazon (Eero, Ring)
        "40:B4:CD" to "Amazon",
        "44:65:0D" to "Amazon",
        "68:37:E9" to "Amazon",
        "74:C2:46" to "Amazon",
        "A0:02:DC" to "Amazon",
        "FC:65:DE" to "Amazon",

        // Xiaomi
        "00:9E:C8" to "Xiaomi",
        "28:6C:07" to "Xiaomi",
        "34:CE:00" to "Xiaomi",
        "58:44:98" to "Xiaomi",
        "64:09:80" to "Xiaomi",
        "7C:1D:D9" to "Xiaomi",
        "8C:DE:F9" to "Xiaomi",
        "B0:E2:35" to "Xiaomi",

        // EnGenius
        "00:02:6F" to "EnGenius",
        "88:DC:96" to "EnGenius",

        // Cambium Networks
        "58:C1:7A" to "Cambium",

        // Juniper / Mist
        "5C:5B:35" to "Juniper Mist",
        "CC:88:26" to "Juniper Mist",

        // Extreme Networks
        "00:04:96" to "Extreme",
        "B4:C7:99" to "Extreme",

        // Motorola
        "00:08:0E" to "Motorola",
        "00:0C:E5" to "Motorola",
    )

    /**
     * Look up the vendor name for a BSSID.
     *
     * @param bssid MAC address in any common format (colon, hyphen, or
     *              contiguous hex). Case-insensitive.
     * @return Vendor name, or `null` if the OUI is not in the table.
     */
    fun lookup(bssid: String): String? {
        val normalized = normalizeBssid(bssid)
        val prefix = normalized.take(8) // "AA:BB:CC"
        return ouiTable[prefix]
    }

    /**
     * Normalize a BSSID to upper-case colon-separated form.
     *
     * Handles inputs like "aa:bb:cc:dd:ee:ff", "AA-BB-CC-DD-EE-FF",
     * and "AABBCCDDEEFF".
     */
    private fun normalizeBssid(bssid: String): String {
        val hex = bssid.replace("[^0-9A-Fa-f]".toRegex(), "").uppercase()
        return hex.chunked(2).joinToString(":")
    }
}
