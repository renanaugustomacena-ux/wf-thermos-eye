package com.alexcupsa.wifithermal.core.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

fun Long.toFormattedDate(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.toFormattedDuration(): String {
    val seconds = this / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    return when {
        hours > 0 -> "%d:%02d:%02d".format(hours, minutes % 60, seconds % 60)
        else -> "%d:%02d".format(minutes, seconds % 60)
    }
}

fun Double.formatRssi(): String = "${roundToInt()} dBm"

fun Double.formatDistance(): String = when {
    this < 1.0 -> "%.1f m".format(this)
    this < 10.0 -> "%.1f m".format(this)
    else -> "%.0f m".format(this)
}

fun Double.formatPercent(): String = "%.0f%%".format(this * 100)
