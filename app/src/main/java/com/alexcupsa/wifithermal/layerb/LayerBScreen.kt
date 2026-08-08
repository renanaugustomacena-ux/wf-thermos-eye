package com.alexcupsa.wifithermal.layerb

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alexcupsa.wifithermal.core.data.ble.BleSnifferClient
import com.alexcupsa.wifithermal.core.model.audit.CapturedHandshake
import com.alexcupsa.wifithermal.core.model.audit.CrackStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayerBScreen(
    onBack: () -> Unit,
    onNavigateToAuthorization: () -> Unit,
    viewModel: LayerBViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val blePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        if (results.values.all { it }) viewModel.startSniffer()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Layer B — Capture") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ScopeStatusCard(state, onNavigateToAuthorization)

            BackendConfigCard(state, viewModel::setBackendUrl, viewModel::setAuthToken)

            BleControlCard(
                state = state,
                onStart = {
                    if (viewModel.isPermissionGranted()) {
                        viewModel.startSniffer()
                    } else {
                        blePermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_CONNECT,
                            ),
                        )
                    }
                },
                onStop = viewModel::stopSniffer,
            )

            if (state.captures.isEmpty()) {
                Text(
                    text = "No captures yet. Connect the ESP32 sniffer companion (firmware in /companion/README.md) and walk it within range of an authorized AP.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = "Captures (${state.captures.size})",
                    style = MaterialTheme.typography.titleMedium,
                )
                LazyColumn(
                    modifier = Modifier.height(((state.captures.size).coerceAtMost(8) * 120).dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.captures, key = { it.id }) { capture ->
                        CaptureRow(
                            capture = capture,
                            onSubmit = { viewModel.submitForCracking(capture) },
                            onPoll = { viewModel.pollCrackingResult(capture) },
                            onDelete = { viewModel.deleteCapture(capture) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScopeStatusCard(state: LayerBUiState, onNavigateToAuthorization: () -> Unit) {
    val container = if (state.offensiveAuthorized) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    Card(
        onClick = onNavigateToAuthorization,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (state.offensiveAuthorized) Icons.Default.Bluetooth else Icons.Default.LockOpen,
                    contentDescription = null,
                    tint = if (state.offensiveAuthorized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.padding(horizontal = 4.dp))
                Text(
                    text = if (state.offensiveAuthorized)
                        "Offensive scope authorized (${state.authorizedBssids.size} BSSID)"
                    else "OFFENSIVE SCOPE EMPTY — Layer B inert",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(4.dp))
            if (state.offensiveAuthorized) {
                state.authorizedBssids.forEach { mac ->
                    Text(
                        text = "  $mac",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            } else {
                Text(
                    text = "Add BSSIDs to AuthorizationScope.offensiveScope.authorizedBssids before any capture can be persisted. Tap to open Authorization screen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BackendConfigCard(
    state: LayerBUiState,
    onUrl: (String) -> Unit,
    onToken: (String) -> Unit,
) {
    var url by remember(state.backendUrl) { mutableStateOf(state.backendUrl.orEmpty()) }
    var token by remember { mutableStateOf("") }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Cracking backend", style = MaterialTheme.typography.titleSmall)
            Text(
                text = "Tailscale-routed HTTPS endpoint (https://server.tailnet.ts.net) running hashcat -m 22000.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Backend URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text("Auth token (Bearer)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onUrl(url) }) { Text("Save URL") }
                OutlinedButton(
                    onClick = { onToken(token) },
                    enabled = token.isNotBlank(),
                ) { Text("Save token") }
            }
        }
    }
}

@Composable
private fun BleControlCard(state: LayerBUiState, onStart: () -> Unit, onStop: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("ESP32 sniffer companion", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "BLE state: ${state.bleState.name}" +
                    (state.connectedDevice?.let { "  |  $it" }.orEmpty()),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                text = "Frames received this session: ${state.frameCount}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onStart, enabled = state.offensiveAuthorized) {
                    Text("Connect / Scan")
                }
                OutlinedButton(onClick = onStop) { Text("Stop") }
            }
            if (!state.offensiveAuthorized) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Sniffer connect disabled while offensive scope is empty.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun CaptureRow(
    capture: CapturedHandshake,
    onSubmit: () -> Unit,
    onPoll: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = capture.bssid,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = capture.status.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = colorFor(capture.status),
                )
            }
            Text(
                text = "${capture.kind.name}  |  ${capture.ssid.ifEmpty { "<hidden>" }}  |  ${capture.payloadHex.length / 2}B",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            capture.crackResult?.let {
                Text(
                    text = "Result: $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (capture.status == CrackStatus.PENDING) {
                    OutlinedButton(onClick = onSubmit) { Text("Submit") }
                }
                if (capture.status == CrackStatus.SUBMITTED) {
                    OutlinedButton(onClick = onPoll) { Text("Poll") }
                }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}

@Composable
private fun colorFor(status: CrackStatus) = when (status) {
    CrackStatus.CRACKED -> MaterialTheme.colorScheme.primary
    CrackStatus.EXHAUSTED, CrackStatus.FAILED, CrackStatus.REJECTED_OUT_OF_SCOPE -> MaterialTheme.colorScheme.error
    CrackStatus.SUBMITTED -> MaterialTheme.colorScheme.tertiary
    CrackStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
}
