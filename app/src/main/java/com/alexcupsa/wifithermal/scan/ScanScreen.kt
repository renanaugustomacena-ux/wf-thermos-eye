package com.alexcupsa.wifithermal.scan

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NetworkWifi
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alexcupsa.wifithermal.core.model.ProcessedScanResult
import com.alexcupsa.wifithermal.core.model.ScanStatus
import com.alexcupsa.wifithermal.core.model.SecurityType
import com.alexcupsa.wifithermal.core.model.SignalQuality
import com.alexcupsa.wifithermal.core.ui.theme.SignalExcellent
import com.alexcupsa.wifithermal.core.ui.theme.SignalFair
import com.alexcupsa.wifithermal.core.ui.theme.SignalGood
import com.alexcupsa.wifithermal.core.ui.theme.SignalUnusable
import com.alexcupsa.wifithermal.core.ui.theme.SignalWeak

@Composable
fun ScanScreen(
    onApClick: (String) -> Unit = {},
    viewModel: ScanViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            ScanFab(
                status = state.scanStatus,
                onStart = viewModel::startScanning,
                onStop = viewModel::stopScanning,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            ScanHeader(state)
            SortAndFilterBar(
                sortMode = state.sortMode,
                filterBand = state.filterBand,
                onSortChange = viewModel::setSortMode,
                onFilterChange = viewModel::setFilterBand,
            )
            if (state.scanStatus == ScanStatus.SCANNING) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (state.results.isEmpty()) {
                EmptyState(state.scanStatus)
            } else {
                ApList(results = state.results, onApClick = onApClick)
            }
        }
    }
}

@Composable
private fun ScanHeader(state: ScanUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "Live Scan",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "${state.results.size} access points",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        StatusBadge(state.scanStatus)
    }
}

@Composable
private fun StatusBadge(status: ScanStatus) {
    val (text, color) = when (status) {
        ScanStatus.IDLE -> "Idle" to MaterialTheme.colorScheme.onSurfaceVariant
        ScanStatus.SCANNING -> "Scanning" to SignalGood
        ScanStatus.THROTTLED -> "Throttled" to SignalFair
        ScanStatus.COMPLETE -> "Complete" to MaterialTheme.colorScheme.primary
    }
    val bgColor by animateColorAsState(color.copy(alpha = 0.15f), label = "statusBg")

    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

@Composable
private fun SortAndFilterBar(
    sortMode: SortMode,
    filterBand: String?,
    onSortChange: (SortMode) -> Unit,
    onFilterChange: (String?) -> Unit,
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            TextButton(onClick = { sortMenuExpanded = true }) {
                Text("Sort: ${sortMode.name}", style = MaterialTheme.typography.labelSmall)
            }
            DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                SortMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(mode.name) },
                        onClick = {
                            onSortChange(mode)
                            sortMenuExpanded = false
                        },
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        FilterChip(
            selected = filterBand == null,
            onClick = { onFilterChange(null) },
            label = { Text("All") },
        )
        FilterChip(
            selected = filterBand == "BAND_2_4_GHZ",
            onClick = { onFilterChange(if (filterBand == "BAND_2_4_GHZ") null else "BAND_2_4_GHZ") },
            label = { Text("2.4G") },
        )
        FilterChip(
            selected = filterBand == "BAND_5_GHZ",
            onClick = { onFilterChange(if (filterBand == "BAND_5_GHZ") null else "BAND_5_GHZ") },
            label = { Text("5G") },
        )
    }
}

@Composable
private fun ApList(results: List<ProcessedScanResult>, onApClick: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(results, key = { it.bssid }) { ap ->
            ApCard(ap, onClick = { onApClick(ap.bssid) })
        }
    }
}

@Composable
private fun ApCard(ap: ProcessedScanResult, onClick: () -> Unit = {}) {
    val signalColor = signalColor(ap.signalQuality)

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (ap.isConnected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SignalStrengthBar(ap.smoothedRssi, signalColor)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = ap.ssid.ifEmpty { "<hidden>" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (ap.isConnected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (ap.isConnected) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Wifi,
                            contentDescription = "Connected",
                            tint = SignalExcellent,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = ap.bssid,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ap.vendor?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ChipLabel("Ch ${ap.channel}", MaterialTheme.colorScheme.primary)
                    ChipLabel(ap.band.name.replace("BAND_", "").replace("_", "."), MaterialTheme.colorScheme.secondary)
                    SecurityChip(ap.security)
                    ChipLabel("${ap.channelWidth.name.replace("MHZ_", "")}MHz", MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${ap.rssi}",
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FontFamily.Monospace,
                    color = signalColor,
                )
                Text(
                    text = "dBm",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "~${"%.1f".format(ap.estimatedDistance)}m",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SignalStrengthBar(rssi: Double, color: Color) {
    val fraction = ((rssi + 100) / 60.0).coerceIn(0.0, 1.0).toFloat()
    Box(
        modifier = Modifier
            .width(6.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(color.copy(alpha = 0.2f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((40 * fraction).dp)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(3.dp))
                .background(color),
        )
    }
}

@Composable
private fun SecurityChip(security: SecurityType) {
    val (label, icon, color) = when (security) {
        SecurityType.OPEN -> Triple("Open", Icons.Default.LockOpen, SignalUnusable)
        SecurityType.WEP -> Triple("WEP", Icons.Default.Warning, SignalWeak)
        SecurityType.WPA_PSK, SecurityType.WPA_EAP -> Triple("WPA", Icons.Default.Lock, SignalFair)
        SecurityType.WPA2_PSK, SecurityType.WPA2_EAP -> Triple("WPA2", Icons.Default.Lock, SignalGood)
        SecurityType.WPA3_SAE -> Triple("WPA3", Icons.Default.Lock, SignalExcellent)
        SecurityType.UNKNOWN -> Triple("?", Icons.Default.Lock, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(10.dp))
        Spacer(Modifier.width(2.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
private fun ChipLabel(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
    )
}

@Composable
private fun ScanFab(
    status: ScanStatus,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    val isScanning = status == ScanStatus.SCANNING || status == ScanStatus.THROTTLED
    FloatingActionButton(
        onClick = { if (isScanning) onStop() else onStart() },
        containerColor = if (isScanning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
    ) {
        Icon(
            if (isScanning) Icons.Default.Stop else Icons.Default.PlayArrow,
            contentDescription = if (isScanning) "Stop" else "Scan",
        )
    }
}

@Composable
private fun EmptyState(status: ScanStatus) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.NetworkWifi,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (status == ScanStatus.IDLE) "Tap the play button to start scanning"
            else "Scanning for access points...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
