package com.alexcupsa.wifithermal.core.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Latest fix from the device's fused location provider, kept in-memory.
 * The scan service writes here; the audit pipeline reads from here when
 * correlating Wi-Fi sightings to physical position for triangulation.
 *
 * Null until the first fix arrives. Out-of-app readers should fall back
 * gracefully if location is not yet available.
 */
@Singleton
class LocationStateRepository @Inject constructor() {

    private val _location = MutableStateFlow<DeviceLocation?>(null)
    val location: StateFlow<DeviceLocation?> = _location.asStateFlow()

    fun update(loc: DeviceLocation) {
        _location.value = loc
    }

    fun clear() {
        _location.value = null
    }
}

data class DeviceLocation(
    val lat: Double,
    val lon: Double,
    val accuracyM: Float,
    val timestamp: Long,
)
