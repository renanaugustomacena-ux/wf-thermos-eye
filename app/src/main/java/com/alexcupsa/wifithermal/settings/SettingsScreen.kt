package com.alexcupsa.wifithermal.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.alexcupsa.wifithermal.core.model.ColorScheme

object SettingsKeys {
    const val PREFS_NAME = "wifi_thermal_settings"
    const val PATH_LOSS_EXPONENT = "path_loss_exponent"
    const val IDW_POWER = "idw_power"
    const val SMOOTHING_RADIUS = "smoothing_radius"
    const val COLOR_SCHEME = "color_scheme"
    const val USER_HEIGHT_CM = "user_height_cm"
    const val SCAN_INTERVAL_SEC = "scan_interval_sec"
}

fun Context.appPrefs(): SharedPreferences =
    getSharedPreferences(SettingsKeys.PREFS_NAME, Context.MODE_PRIVATE)

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { context.appPrefs() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        SettingsSection("Signal Processing") {
            SliderSetting(
                label = "Path Loss Exponent",
                value = prefs.getFloat(SettingsKeys.PATH_LOSS_EXPONENT, 3.0f),
                range = 1.5f..6.0f,
                format = "%.1f",
                onValueChange = { prefs.edit().putFloat(SettingsKeys.PATH_LOSS_EXPONENT, it).apply() },
            )
        }

        SettingsSection("Heatmap") {
            SliderSetting(
                label = "IDW Power",
                value = prefs.getFloat(SettingsKeys.IDW_POWER, 2.0f),
                range = 1.0f..5.0f,
                format = "%.1f",
                onValueChange = { prefs.edit().putFloat(SettingsKeys.IDW_POWER, it).apply() },
            )
            SliderSetting(
                label = "Smoothing Radius",
                value = prefs.getFloat(SettingsKeys.SMOOTHING_RADIUS, 5.0f),
                range = 0f..15f,
                format = "%.0f",
                onValueChange = { prefs.edit().putFloat(SettingsKeys.SMOOTHING_RADIUS, it).apply() },
            )
            ColorSchemeSetting(
                current = prefs.getString(SettingsKeys.COLOR_SCHEME, "THERMAL") ?: "THERMAL",
                onSelect = { prefs.edit().putString(SettingsKeys.COLOR_SCHEME, it).apply() },
            )
        }

        SettingsSection("Position Tracking") {
            SliderSetting(
                label = "Your Height (cm)",
                value = prefs.getFloat(SettingsKeys.USER_HEIGHT_CM, 175f),
                range = 140f..210f,
                format = "%.0f",
                onValueChange = { prefs.edit().putFloat(SettingsKeys.USER_HEIGHT_CM, it).apply() },
            )
        }

        SettingsSection("Scanning") {
            SliderSetting(
                label = "Scan Interval (sec)",
                value = prefs.getFloat(SettingsKeys.SCAN_INTERVAL_SEC, 32f),
                range = 15f..120f,
                format = "%.0f",
                onValueChange = { prefs.edit().putFloat(SettingsKeys.SCAN_INTERVAL_SEC, it).apply() },
            )
        }

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("About", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "WiFi Thermal Scanner v1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Motorola G35 | Android 14 | API 34",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SliderSetting(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    format: String,
    onValueChange: (Float) -> Unit,
) {
    var current by remember { mutableFloatStateOf(value) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = format.format(current),
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = current,
            onValueChange = { current = it },
            onValueChangeFinished = { onValueChange(current) },
            valueRange = range,
        )
    }
}

@Composable
private fun ColorSchemeSetting(current: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Color Scheme", style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = { expanded = true }) {
            Text(current, fontFamily = FontFamily.Monospace)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ColorScheme.entries.forEach { scheme ->
                DropdownMenuItem(
                    text = { Text(scheme.name) },
                    onClick = {
                        onSelect(scheme.name)
                        expanded = false
                    },
                )
            }
        }
    }
}
