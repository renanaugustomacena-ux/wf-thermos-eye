package com.alexcupsa.wifithermal.core.data.mapper

import com.alexcupsa.wifithermal.core.database.entity.ApMeasurementEntity
import com.alexcupsa.wifithermal.core.database.entity.MeasurementPointEntity
import com.alexcupsa.wifithermal.core.database.relation.MeasurementWithAps
import com.alexcupsa.wifithermal.core.model.ApMeasurement
import com.alexcupsa.wifithermal.core.model.ChannelWidth
import com.alexcupsa.wifithermal.core.model.MeasurementPoint
import com.alexcupsa.wifithermal.core.model.Position
import com.alexcupsa.wifithermal.core.model.SecurityType
import com.alexcupsa.wifithermal.core.model.WifiBand
import com.alexcupsa.wifithermal.core.model.WifiStandard

fun MeasurementWithAps.toDomain(): MeasurementPoint = MeasurementPoint(
    id = point.id,
    position = Position(
        x = point.x,
        y = point.y,
        floor = point.floor,
        confidence = point.positionConfidence,
    ),
    timestamp = point.timestamp,
    apMeasurements = apMeasurements.map { it.toDomain() },
)

fun ApMeasurementEntity.toDomain(): ApMeasurement = ApMeasurement(
    bssid = bssid,
    ssid = ssid,
    rssi = rssi,
    smoothedRssi = smoothedRssi,
    frequency = frequency,
    channel = channel,
    channelWidth = try { ChannelWidth.valueOf(channelWidth) } catch (_: Exception) { ChannelWidth.MHZ_20 },
    band = try { WifiBand.valueOf(band) } catch (_: Exception) { WifiBand.BAND_2_4_GHZ },
    security = try { SecurityType.valueOf(security) } catch (_: Exception) { SecurityType.UNKNOWN },
    standard = try { WifiStandard.valueOf(standard) } catch (_: Exception) { WifiStandard.LEGACY },
)

fun MeasurementPoint.toEntity(surveyId: Long): MeasurementPointEntity = MeasurementPointEntity(
    id = id,
    surveyId = surveyId,
    x = position.x,
    y = position.y,
    floor = position.floor,
    positionConfidence = position.confidence,
    timestamp = timestamp,
)

fun ApMeasurement.toEntity(measurementPointId: Long): ApMeasurementEntity = ApMeasurementEntity(
    measurementPointId = measurementPointId,
    bssid = bssid,
    ssid = ssid,
    rssi = rssi,
    smoothedRssi = smoothedRssi,
    frequency = frequency,
    channel = channel,
    channelWidth = channelWidth.name,
    band = band.name,
    security = security.name,
    standard = standard.name,
    capabilities = "",
)
