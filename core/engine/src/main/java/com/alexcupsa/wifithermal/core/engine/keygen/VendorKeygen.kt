package com.alexcupsa.wifithermal.core.engine.keygen

import java.security.MessageDigest

data class KeygenResult(
    val vendor: String,
    val ssid: String,
    val bssid: String,
    val candidates: List<String>,
    val algorithm: String,
)

object VendorKeygen {

    private val OUI_VENDOR = mapOf(
        "00:03:6F" to "Fastweb",
        "00:08:27" to "Fastweb",
        "00:0B:6B" to "Fastweb",
        "00:13:C8" to "Fastweb",
        "00:17:C2" to "Fastweb",
        "00:19:3E" to "Fastweb",
        "00:1A:2B" to "Fastweb",
        "00:1C:A2" to "Fastweb",
        "00:1D:8B" to "Fastweb",
        "00:1E:40" to "Fastweb",
        "00:1F:3C" to "Fastweb",
        "00:22:33" to "Fastweb",
        "00:23:8E" to "Fastweb",
        "00:24:D4" to "Fastweb",
        "00:26:F3" to "Fastweb",
        "00:A0:C5" to "Fastweb",
        "38:22:9D" to "TIM",
        "40:4A:03" to "TIM",
        "5C:D9:98" to "TIM",
        "64:7C:34" to "TIM",
        "74:DA:DA" to "TIM",
        "78:81:02" to "TIM",
        "84:16:F9" to "TIM",
        "88:71:B1" to "TIM",
        "90:84:0D" to "TIM",
        "98:F5:37" to "TIM",
        "A8:5A:F3" to "TIM",
        "B0:BE:76" to "TIM",
        "B4:A5:EF" to "TIM",
        "C8:3A:35" to "TIM",
        "D8:32:14" to "TIM",
        "E4:C1:46" to "TIM",
        "00:22:3F" to "Vodafone",
        "00:24:89" to "Vodafone",
        "00:26:44" to "Vodafone",
        "1C:1B:0D" to "Vodafone",
        "30:D3:2D" to "Vodafone",
        "50:67:F0" to "Vodafone",
        "74:31:70" to "Vodafone",
        "88:03:55" to "Vodafone",
        "AC:3A:7A" to "Vodafone",
        "E0:19:1D" to "Vodafone",
        "E8:40:40" to "Vodafone",
        "EC:1A:59" to "Vodafone",
        "E0:B9:E5" to "WindTre",
        "14:CC:20" to "WindTre",
        "24:F5:A2" to "WindTre",
        "28:28:5D" to "WindTre",
        "3C:DF:BD" to "WindTre",
        "48:F8:B3" to "WindTre",
        "50:FF:20" to "WindTre",
        "58:D5:6E" to "WindTre",
        "60:14:B3" to "WindTre",
        "64:6E:69" to "WindTre",
        "68:FF:7B" to "WindTre",
        "74:3A:EF" to "WindTre",
        "7C:03:9C" to "WindTre",
    )

    fun detectVendor(bssid: String): String? {
        val oui = bssid.uppercase().take(8)
        return OUI_VENDOR[oui]
    }

    fun generate(ssid: String, bssid: String): KeygenResult? {
        val vendor = detectVendor(bssid) ?: return null
        val normalizedBssid = bssid.uppercase().replace(":", "").replace("-", "")

        return when (vendor) {
            "Fastweb" -> generateFastweb(ssid, normalizedBssid)
            "TIM" -> generateTIM(ssid, normalizedBssid)
            "Vodafone" -> generateVodafone(ssid, normalizedBssid)
            "WindTre" -> generateWindTre(ssid, normalizedBssid)
            else -> null
        }
    }

    private fun generateFastweb(ssid: String, bssid: String): KeygenResult {
        val candidates = mutableListOf<String>()

        val hash = sha256("$bssid:FastwebWPA")
        candidates.add(hash.take(10).uppercase())

        val md5Hash = md5(bssid)
        candidates.add(md5Hash.take(10).uppercase())

        for (offset in -2..2) {
            val modifiedBssid = incrementBssid(bssid, offset)
            val modHash = sha256("$modifiedBssid:FastwebWPA")
            candidates.add(modHash.take(10).uppercase())
        }

        val serial = extractSerial(ssid)
        if (serial != null) {
            val serialHash = sha256("$serial$bssid")
            candidates.add(serialHash.take(10).uppercase())
        }

        return KeygenResult(
            vendor = "Fastweb",
            ssid = ssid,
            bssid = bssid,
            candidates = candidates.distinct(),
            algorithm = "SHA256(BSSID:FastwebWPA)",
        )
    }

    private fun generateTIM(ssid: String, bssid: String): KeygenResult {
        val candidates = mutableListOf<String>()

        val base = md5("$bssid:TIM_DEFAULT")
        candidates.add(base.take(8).uppercase())

        val numberMatch = Regex("""-(\d+)$""").find(ssid)
        if (numberMatch != null) {
            val serialPart = numberMatch.groupValues[1]
            val combined = sha256("$serialPart$bssid")
            candidates.add(combined.take(8).uppercase())
        }

        for (offset in -2..2) {
            val modifiedBssid = incrementBssid(bssid, offset)
            val hash = sha256("$modifiedBssid:TIMHUB")
            candidates.add(hash.take(8).uppercase())
        }

        val staticSuffix = bssid.takeLast(6)
        candidates.add(staticSuffix.uppercase())

        return KeygenResult(
            vendor = "TIM",
            ssid = ssid,
            bssid = bssid,
            candidates = candidates.distinct(),
            algorithm = "MD5/SHA256(BSSID:TIM_DEFAULT)",
        )
    }

    private fun generateVodafone(ssid: String, bssid: String): KeygenResult {
        val candidates = mutableListOf<String>()

        val serial = ssid.replace("Vodafone-", "").replace("vodafone-", "")
        val combined = sha256("$serial$bssid")
        candidates.add(combined.take(10).uppercase())

        for (offset in -3..3) {
            val modifiedBssid = incrementBssid(bssid, offset)
            val hash = sha256("$modifiedBssid:VodafoneIT")
            candidates.add(hash.take(10).uppercase())
        }

        val reversed = bssid.reversed()
        val revHash = md5(reversed)
        candidates.add(revHash.take(10).uppercase())

        return KeygenResult(
            vendor = "Vodafone",
            ssid = ssid,
            bssid = bssid,
            candidates = candidates.distinct(),
            algorithm = "SHA256(serial+BSSID)",
        )
    }

    private fun generateWindTre(ssid: String, bssid: String): KeygenResult {
        val candidates = mutableListOf<String>()

        val base = sha256("$bssid:INFOSTRADA")
        candidates.add(base.take(10).uppercase())

        val suffix = ssid.replace("WINDTRE-", "").replace("Wind3-", "")
        val withSuffix = sha256("$suffix$bssid")
        candidates.add(withSuffix.take(10).uppercase())

        for (offset in -2..2) {
            val modifiedBssid = incrementBssid(bssid, offset)
            val hash = md5("$modifiedBssid:TRE")
            candidates.add(hash.take(10).uppercase())
        }

        return KeygenResult(
            vendor = "WindTre",
            ssid = ssid,
            bssid = bssid,
            candidates = candidates.distinct(),
            algorithm = "SHA256(BSSID:INFOSTRADA)",
        )
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun md5(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun incrementBssid(bssid: String, offset: Int): String {
        val value = bssid.toLongOrNull(16) ?: return bssid
        val modified = (value + offset) and 0xFFFFFFFFFFFFL
        return "%012X".format(modified)
    }

    private fun extractSerial(ssid: String): String? {
        val patterns = listOf(
            Regex("""-(\d{5,})$"""),
            Regex("""_(\d{5,})$"""),
            Regex("""(\d{6,})"""),
        )
        for (pattern in patterns) {
            val match = pattern.find(ssid)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        return null
    }
}
