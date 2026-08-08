package com.alexcupsa.wifithermal.core.data.export

import android.content.Context
import com.alexcupsa.wifithermal.core.engine.audit.ReportRedactor
import com.alexcupsa.wifithermal.core.model.audit.AuthorizedAccessPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CSV-only export of the operator whitelist. Useful for handover, audit
 * trail, or backup before changes. PDF version not provided — the whitelist
 * is a static config artefact, not a finding.
 */
@Singleton
class WhitelistExporter @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun export(
        entries: List<AuthorizedAccessPoint>,
        redactionLevel: ReportRedactor.Level = ReportRedactor.Level.NONE,
        generatedAt: Long = System.currentTimeMillis(),
    ): File {
        val dir = File(context.filesDir, "exports").apply { mkdirs() }
        val ts = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date(generatedAt))
        val file = File(dir, "wf-audit-whitelist-$ts.csv")
        file.writeText(buildCsv(entries, redactionLevel))
        return file
    }

    internal fun buildCsv(
        entries: List<AuthorizedAccessPoint>,
        redactionLevel: ReportRedactor.Level,
    ): String {
        val sb = StringBuilder()
        sb.appendLine("bssid,ssid,device_type,expected_security,location,owner,notes,authorized_at_iso")
        entries.sortedBy { it.ssid }.forEachIndexed { idx, entry ->
            val mb = ReportRedactor.maskBssid(entry.bssid, redactionLevel, idx)
            val ms = ReportRedactor.maskSsid(entry.ssid, redactionLevel)
            sb.append(csvEscape(mb))
            sb.append(",")
            sb.append(csvEscape(ms))
            sb.append(",")
            sb.append(entry.deviceType.name)
            sb.append(",")
            sb.append(entry.expectedSecurity.name)
            sb.append(",")
            sb.append(csvEscape(entry.location.orEmpty()))
            sb.append(",")
            sb.append(csvEscape(entry.owner.orEmpty()))
            sb.append(",")
            sb.append(csvEscape(entry.notes.orEmpty()))
            sb.append(",")
            sb.append(formatIso(entry.authorizedAt))
            sb.appendLine()
        }
        return sb.toString()
    }

    private fun csvEscape(value: String): String =
        if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }

    private fun formatIso(epochMs: Long): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.format(Date(epochMs))
}
