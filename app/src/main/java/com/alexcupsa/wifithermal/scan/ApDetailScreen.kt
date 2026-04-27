package com.alexcupsa.wifithermal.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alexcupsa.wifithermal.core.engine.analysis.ThroughputEstimator
import com.alexcupsa.wifithermal.core.model.ProcessedScanResult
import com.alexcupsa.wifithermal.core.model.SignalQuality
import com.alexcupsa.wifithermal.core.ui.theme.SignalExcellent
import com.alexcupsa.wifithermal.core.ui.theme.SignalFair
import com.alexcupsa.wifithermal.core.ui.theme.SignalGood
import com.alexcupsa.wifithermal.core.ui.theme.SignalUnusable
import com.alexcupsa.wifithermal.core.ui.theme.SignalWeak
import com.alexcupsa.wifithermal.service.WifiScanService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApDetailScreen(
    bssid: String,
    onBack: () -> Unit,
) {
    val scanResults by WifiScanService.scanResults.collectAsState()
    val ap = remember(scanResults, bssid) {
        scanResults.firstOrNull { it.bssid == bssid }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ap?.ssid?.ifEmpty { "Hidden AP" } ?: "AP Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (ap == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("AP not found in scan results", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SignalCard(ap)
            IdentityCard(ap)
            RadioCard(ap)
            ThroughputCard(ap)
            SecurityCard(ap)
        }
    }
}

@Composable
private fun SignalCard(ap: ProcessedScanResult) {
    val color = signalColor(ap.signalQuality)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "${ap.rssi} dBm",
                    style = MaterialTheme.typography.displaySmall,
                    fontFamily = FontFamily.Monospace,
                    color = color,
                )
                Text(
                    text = "Smoothed: ${"%.1f".format(ap.smoothedRssi)} dBm",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = ap.signalQuality.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color,
                )
                Text(
                    text = "~${"%.1f".format(ap.estimatedDistance)} m",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                )
                if (ap.isConnected) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Wifi, null, tint = SignalExcellent, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Connected", style = MaterialTheme.typography.labelSmall, color = SignalExcellent)
                    }
                }
            }
        }
    }
}

@Composable
private fun IdentityCard(ap: ProcessedScanResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Identity", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            InfoRow("SSID", ap.ssid.ifEmpty { "<hidden>" })
            InfoRow("BSSID", ap.bssid)
            ap.vendor?.let { InfoRow("Vendor", it) }
            InfoRow("Standard", ap.standard.name.replace("_", " "))
        }
    }
}

@Composable
private fun RadioCard(ap: ProcessedScanResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Radio", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            InfoRow("Frequency", "${ap.frequency} MHz")
            InfoRow("Channel", "${ap.channel}")
            InfoRow("Channel Width", "${ap.channelWidth.name.replace("MHZ_", "")} MHz")
            InfoRow("Band", ap.band.name.replace("BAND_", "").replace("_", ".") + " GHz")
        }
    }
}

@Composable
private fun ThroughputCard(ap: ProcessedScanResult) {
    val estimate = remember(ap) {
        ThroughputEstimator.estimate(ap.smoothedRssi, ap.standard, ap.channelWidth)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Speed, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Throughput Estimate", style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(8.dp))
            InfoRow("Max Theoretical", "${"%.0f".format(estimate.maxTheoreticalMbps)} Mbps")
            InfoRow("Estimated Actual", "${"%.1f".format(estimate.estimatedActualMbps)} Mbps")
            InfoRow("Confidence", "${"%.0f".format(estimate.confidence * 100)}%")
        }
    }
}

@Composable
private fun SecurityCard(ap: ProcessedScanResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Security", style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(8.dp))
            InfoRow("Encryption", ap.security.name.replace("_", " "))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun signalColor(quality: SignalQuality): Color = when (quality) {
    SignalQuality.EXCELLENT -> SignalExcellent
    SignalQuality.GOOD -> SignalGood
    SignalQuality.FAIR -> SignalFair
    SignalQuality.WEAK -> SignalWeak
    SignalQuality.UNUSABLE -> SignalUnusable
}
