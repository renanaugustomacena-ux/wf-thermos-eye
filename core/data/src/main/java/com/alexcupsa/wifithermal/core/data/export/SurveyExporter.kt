package com.alexcupsa.wifithermal.core.data.export

import com.alexcupsa.wifithermal.core.data.repository.MeasurementRepository
import com.alexcupsa.wifithermal.core.data.repository.SurveyRepository
import com.alexcupsa.wifithermal.core.model.ApMeasurement
import com.alexcupsa.wifithermal.core.model.MeasurementPoint
import com.alexcupsa.wifithermal.core.model.Survey
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SurveyExporter @Inject constructor(
    private val surveyRepository: SurveyRepository,
    private val measurementRepository: MeasurementRepository,
) {
    suspend fun exportCsv(surveyId: Long): String {
        val survey = surveyRepository.getSurveyById(surveyId) ?: return ""
        val measurements = measurementRepository.getMeasurements(surveyId).first()

        val sb = StringBuilder()
        sb.appendLine("# WiFi Thermal Scanner - Survey Export")
        sb.appendLine("# Survey: ${survey.name}")
        sb.appendLine("# Created: ${survey.createdAt}")
        sb.appendLine("# Status: ${survey.status}")
        sb.appendLine("# Points: ${measurements.size}")
        sb.appendLine()
        sb.appendLine("point_id,x,y,floor,confidence,timestamp,bssid,ssid,rssi,smoothed_rssi,frequency,channel,channel_width,band,security,standard")

        for (point in measurements) {
            for (ap in point.apMeasurements) {
                sb.appendLine(csvRow(point, ap))
            }
        }
        return sb.toString()
    }

    suspend fun exportJson(surveyId: Long): String {
        val survey = surveyRepository.getSurveyById(surveyId) ?: return "{}"
        val measurements = measurementRepository.getMeasurements(surveyId).first()

        val sb = StringBuilder()
        sb.appendLine("{")
        sb.appendLine("  \"survey\": {")
        sb.appendLine("    \"id\": ${survey.id},")
        sb.appendLine("    \"name\": ${jsonString(survey.name)},")
        sb.appendLine("    \"status\": ${jsonString(survey.status.name)},")
        sb.appendLine("    \"createdAt\": ${survey.createdAt},")
        sb.appendLine("    \"totalPoints\": ${survey.totalPoints}")
        sb.appendLine("  },")
        sb.appendLine("  \"measurements\": [")

        measurements.forEachIndexed { i, point ->
            sb.appendLine("    {")
            sb.appendLine("      \"id\": ${point.id},")
            sb.appendLine("      \"x\": ${point.position.x},")
            sb.appendLine("      \"y\": ${point.position.y},")
            sb.appendLine("      \"floor\": ${point.position.floor},")
            sb.appendLine("      \"timestamp\": ${point.timestamp},")
            sb.appendLine("      \"aps\": [")

            point.apMeasurements.forEachIndexed { j, ap ->
                sb.append("        {")
                sb.append("\"bssid\":${jsonString(ap.bssid)},")
                sb.append("\"ssid\":${jsonString(ap.ssid)},")
                sb.append("\"rssi\":${ap.rssi},")
                sb.append("\"smoothedRssi\":${ap.smoothedRssi},")
                sb.append("\"frequency\":${ap.frequency},")
                sb.append("\"channel\":${ap.channel},")
                sb.append("\"channelWidth\":${jsonString(ap.channelWidth.name)},")
                sb.append("\"band\":${jsonString(ap.band.name)},")
                sb.append("\"security\":${jsonString(ap.security.name)},")
                sb.append("\"standard\":${jsonString(ap.standard.name)}")
                sb.append("}")
                if (j < point.apMeasurements.size - 1) sb.appendLine(",") else sb.appendLine()
            }

            sb.appendLine("      ]")
            sb.append("    }")
            if (i < measurements.size - 1) sb.appendLine(",") else sb.appendLine()
        }

        sb.appendLine("  ]")
        sb.appendLine("}")
        return sb.toString()
    }

    private fun csvRow(point: MeasurementPoint, ap: ApMeasurement): String =
        "${point.id},${point.position.x},${point.position.y},${point.position.floor}," +
            "${point.position.confidence},${point.timestamp}," +
            "${csvEscape(ap.bssid)},${csvEscape(ap.ssid)},${ap.rssi},${ap.smoothedRssi}," +
            "${ap.frequency},${ap.channel},${ap.channelWidth.name},${ap.band.name}," +
            "${ap.security.name},${ap.standard.name}"

    private fun csvEscape(value: String): String =
        if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }

    private fun jsonString(value: String): String {
        val escaped = value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return "\"$escaped\""
    }
}
