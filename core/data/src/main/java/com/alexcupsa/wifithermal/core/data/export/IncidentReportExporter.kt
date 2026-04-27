package com.alexcupsa.wifithermal.core.data.export

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.alexcupsa.wifithermal.core.engine.audit.ReportRedactor
import com.alexcupsa.wifithermal.core.model.audit.AnomalyFlag
import com.alexcupsa.wifithermal.core.model.audit.AuthorizationScope
import com.alexcupsa.wifithermal.core.model.audit.IncidentEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds shareable reports of audit findings.
 *
 * Two formats: CSV (analyst tooling) and PDF (human read / HR tickets /
 * legal evidence). Both go through the same [ReportRedactor] level so
 * the operator picks once per export.
 *
 * Output goes to `filesDir/exports/` so [androidx.core.content.FileProvider]
 * can serve it via the existing `${applicationId}.fileprovider` authority.
 *
 * No PII handling beyond the redactor: the operator is responsible for
 * deciding what "needs redaction" given who they're sending the report to.
 */
@Singleton
class IncidentReportExporter @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    data class Inputs(
        val incidents: List<IncidentEvent>,
        val anomalies: List<AnomalyFlag>,
        val scope: AuthorizationScope?,
        val redactionLevel: ReportRedactor.Level = ReportRedactor.Level.NONE,
    )

    data class Output(val csvFile: File, val pdfFile: File, val sha256: String)

    fun export(inputs: Inputs, generatedAt: Long = System.currentTimeMillis()): Output {
        val dir = File(context.filesDir, "exports").apply { mkdirs() }
        val ts = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date(generatedAt))

        val csv = File(dir, "wf-audit-incidents-$ts.csv")
        csv.writeText(buildCsv(inputs))

        val pdf = File(dir, "wf-audit-incidents-$ts.pdf")
        writePdf(pdf, inputs, generatedAt)

        val combined = csv.readText().toByteArray() + pdf.readBytes()
        val sha = MessageDigest.getInstance("SHA-256").digest(combined)
            .joinToString("") { "%02x".format(it) }

        return Output(csvFile = csv, pdfFile = pdf, sha256 = sha)
    }

    internal fun buildCsv(inputs: Inputs): String {
        val sb = StringBuilder()
        sb.appendLine("timestamp_iso,kind,bssid,ssid,severity,summary")
        // Stable pseudonym index per bssid for FULL-redacted exports.
        val pseudonyms = inputs.incidents
            .map { it.bssid }
            .distinct()
            .withIndex()
            .associate { (idx, bssid) -> bssid to idx }

        for (event in inputs.incidents.sortedBy { it.timestamp }) {
            val pseudoIdx = pseudonyms[event.bssid] ?: 0
            val maskedBssid = ReportRedactor.maskBssid(event.bssid, inputs.redactionLevel, pseudoIdx)
            val maskedSsid = ReportRedactor.maskSsid(event.ssid.orEmpty(), inputs.redactionLevel)
            sb.append(formatIso(event.timestamp))
            sb.append(",")
            sb.append(event.kind.name)
            sb.append(",")
            sb.append(csvEscape(maskedBssid))
            sb.append(",")
            sb.append(csvEscape(maskedSsid))
            sb.append(",")
            sb.append(event.severity.name)
            sb.append(",")
            sb.append(csvEscape(event.summary))
            sb.appendLine()
        }
        return sb.toString()
    }

    private fun writePdf(file: File, inputs: Inputs, generatedAt: Long) {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 portrait
        var page = doc.startPage(pageInfo)
        var canvas = page.canvas
        val title = Paint().apply { textSize = 18f; isFakeBoldText = true }
        val h2 = Paint().apply { textSize = 14f; isFakeBoldText = true }
        val body = Paint().apply { textSize = 10f }
        val mono = Paint().apply { textSize = 9f; typeface = android.graphics.Typeface.MONOSPACE }

        val left = 40f
        var y = 60f

        canvas.drawText("Wireless Audit Report", left, y, title); y += 28f
        canvas.drawText("Generated ${formatIso(generatedAt)}", left, y, body); y += 14f
        canvas.drawText("Redaction: ${inputs.redactionLevel.name}", left, y, body); y += 24f

        canvas.drawText("Scope", left, y, h2); y += 18f
        if (inputs.scope == null) {
            canvas.drawText("No active authorization scope.", left, y, body); y += 16f
        } else {
            val s = inputs.scope
            canvas.drawText("Org: ${s.organizationName}", left, y, body); y += 14f
            canvas.drawText("Authorized by: ${s.authorizedBy}", left, y, body); y += 14f
            canvas.drawText("Since: ${formatIso(s.authorizedAt)}", left, y, body); y += 14f
            s.expiresAt?.let {
                canvas.drawText("Expires: ${formatIso(it)}", left, y, body); y += 14f
            }
            y += 6f
        }

        canvas.drawText("Summary", left, y, h2); y += 18f
        val total = inputs.incidents.size
        val bySev = inputs.incidents.groupingBy { it.severity.name }.eachCount()
        canvas.drawText("Total incidents: $total", left, y, body); y += 14f
        bySev.forEach { (sev, n) ->
            canvas.drawText("  $sev: $n", left, y, body); y += 14f
        }
        y += 6f

        canvas.drawText("Anomalies", left, y, h2); y += 18f
        if (inputs.anomalies.isEmpty()) {
            canvas.drawText("None at this time.", left, y, body); y += 14f
        } else {
            inputs.anomalies.forEach { flag ->
                val pseudoIdx = inputs.incidents.map { it.bssid }.distinct().indexOf(flag.bssid).coerceAtLeast(0)
                val masked = ReportRedactor.maskBssid(flag.bssid, inputs.redactionLevel, pseudoIdx)
                canvas.drawText("[${flag.kind.name}] $masked", left, y, mono); y += 13f
                canvas.drawText("  ${flag.description}", left, y, body); y += 14f
            }
        }
        y += 6f

        canvas.drawText("Timeline", left, y, h2); y += 18f
        val pseudonyms = inputs.incidents.map { it.bssid }.distinct().withIndex()
            .associate { (idx, bssid) -> bssid to idx }
        for (event in inputs.incidents.sortedByDescending { it.timestamp }) {
            if (y > pageInfo.pageHeight - 60f) {
                doc.finishPage(page)
                page = doc.startPage(pageInfo)
                canvas = page.canvas
                y = 60f
            }
            val pseudoIdx = pseudonyms[event.bssid] ?: 0
            val mb = ReportRedactor.maskBssid(event.bssid, inputs.redactionLevel, pseudoIdx)
            val ms = ReportRedactor.maskSsid(event.ssid.orEmpty(), inputs.redactionLevel)
            canvas.drawText("${formatIso(event.timestamp)}  ${event.severity.name}  $mb  $ms", left, y, mono); y += 12f
            canvas.drawText("  ${event.summary}", left, y, body); y += 14f
        }

        doc.finishPage(page)
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
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
