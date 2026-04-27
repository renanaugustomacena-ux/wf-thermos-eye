package com.alexcupsa.wifithermal.common

import android.Manifest

// minSdk = 34 (Android 14). NEARBY_WIFI_DEVICES (API 33+) and POST_NOTIFICATIONS
// (API 33+) are always present, so no SDK_INT branch is needed.
val SCAN_PERMISSIONS: List<String> = listOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.NEARBY_WIFI_DEVICES,
    Manifest.permission.POST_NOTIFICATIONS,
)
