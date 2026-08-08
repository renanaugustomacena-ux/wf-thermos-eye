package com.alexcupsa.wifithermal.attack

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WpsAttackScreen(
    onBack: () -> Unit,
    viewModel: WpsAttackViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.statusMessage, state.errorMessage) {
        state.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        state.errorMessage?.let {
            snackbarHostState.showSnackbar("Error: $it")
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WPS & Monitor Mode") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.checkEnvironment() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            EnvironmentCard(state, viewModel::openTermux, viewModel::installTools)
            InterfaceCard(state, viewModel::selectInterface, viewModel::enableMonitorMode, viewModel::disableMonitorMode)
            TargetCard(state, viewModel::updateTarget)
            WpsAttackCard(state, viewModel::startPixieDustAttack, viewModel::startBruteForceAttack, viewModel::cancelAttack)
            CaptureCard(state, viewModel::startPmkidCapture, viewModel::startHandshakeCapture, viewModel::cancelAttack)
            ResultCard(state)
        }
    }
}

@Composable
private fun EnvironmentCard(
    state: WpsAttackUiState,
    onOpenTermux: () -> Unit,
    onInstallTools: () -> Unit,
) {
    val allToolsInstalled = state.termuxTools.isNotEmpty() && state.termuxTools.all { it.installed }
    val containerColor = when {
        state.isRooted == true && state.termuxInstalled && allToolsInstalled ->
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        state.isRooted == true && state.termuxInstalled ->
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.errorContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Environment",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (state.isRooted == true) Icons.Default.Check else Icons.Default.Close,
                    null,
                    tint = if (state.isRooted == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
                Text("  Root: ${if (state.isRooted == true) "Available" else "Not available"}")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (state.termuxInstalled) Icons.Default.Check else Icons.Default.Close,
                    null,
                    tint = if (state.termuxInstalled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
                Text("  Termux: ${if (state.termuxInstalled) "Installed" else "Not installed"}")
            }

            if (state.termuxTools.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Tools:", style = MaterialTheme.typography.labelMedium)
                state.termuxTools.forEach { tool ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (tool.installed) Icons.Default.Check else Icons.Default.Close,
                            null,
                            tint = if (tool.installed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 16.dp),
                        )
                        Text(
                            "  ${tool.name}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpenTermux, enabled = state.termuxInstalled) {
                    Text("Open Termux")
                }
                OutlinedButton(onClick = onInstallTools, enabled = state.termuxInstalled) {
                    Text("Install Tools")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InterfaceCard(
    state: WpsAttackUiState,
    onSelect: (String) -> Unit,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Wifi, null)
                Spacer(Modifier.padding(horizontal = 8.dp))
                Text(
                    text = "WiFi Interface",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.height(8.dp))

            if (state.wifiInterfaces.isEmpty()) {
                Text(
                    text = "No WiFi interfaces found. Root access required.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            } else {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    OutlinedTextField(
                        value = state.selectedInterface ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Interface") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        state.wifiInterfaces.forEach { iface ->
                            DropdownMenuItem(
                                text = { Text("${iface.name} (${iface.mode})") },
                                onClick = {
                                    onSelect(iface.name)
                                    expanded = false
                                },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (state.monitorModeEnabled) {
                        "Monitor: ${state.monitorInterface}"
                    } else {
                        "Monitor mode: OFF"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )

                if (state.monitorModeEnabled) {
                    OutlinedButton(onClick = onDisable) {
                        Text("Disable")
                    }
                } else {
                    Button(
                        onClick = onEnable,
                        enabled = state.selectedInterface != null && state.isRooted == true,
                    ) {
                        Text("Enable Monitor")
                    }
                }
            }
        }
    }
}

@Composable
private fun TargetCard(
    state: WpsAttackUiState,
    onUpdate: (String, String, Int) -> Unit,
) {
    var ssid by remember(state.targetSsid) { mutableStateOf(state.targetSsid) }
    var bssid by remember(state.targetBssid) { mutableStateOf(state.targetBssid) }
    var channel by remember(state.targetChannel) { mutableStateOf(state.targetChannel.toString()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Target AP",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = ssid,
                onValueChange = { ssid = it },
                label = { Text("SSID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = bssid,
                onValueChange = { bssid = it },
                label = { Text("BSSID") },
                singleLine = true,
                placeholder = { Text("AA:BB:CC:DD:EE:FF") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = channel,
                onValueChange = { channel = it },
                label = { Text("Channel") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    onUpdate(ssid, bssid, channel.toIntOrNull() ?: 1)
                },
            ) {
                Text("Set Target")
            }
        }
    }
}

@Composable
private fun WpsAttackCard(
    state: WpsAttackUiState,
    onPixieDust: () -> Unit,
    onBruteForce: () -> Unit,
    onCancel: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, null)
                Spacer(Modifier.padding(horizontal = 8.dp))
                Text(
                    text = "WPS PIN Attack",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.height(8.dp))

            Text(
                text = "Requires monitor mode and reaver/bully in Termux",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))

            if (state.attackRunning) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        strokeWidth = 2.dp,
                    )
                    Text(
                        text = state.attackProgress,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onCancel) {
                    Text("Cancel")
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onPixieDust,
                        enabled = state.monitorModeEnabled && state.targetBssid.isNotBlank(),
                    ) {
                        Text("Pixie Dust")
                    }
                    OutlinedButton(
                        onClick = onBruteForce,
                        enabled = state.monitorModeEnabled && state.targetBssid.isNotBlank(),
                    ) {
                        Text("Brute Force")
                    }
                }
            }
        }
    }
}

@Composable
private fun CaptureCard(
    state: WpsAttackUiState,
    onPmkid: () -> Unit,
    onHandshake: () -> Unit,
    onCancel: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Handshake/PMKID Capture",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))

            Text(
                text = "Capture WPA handshake or PMKID for offline cracking",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))

            if (state.captureRunning) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        strokeWidth = 2.dp,
                    )
                    Text(
                        text = state.captureProgress,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onCancel) {
                    Text("Cancel")
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onPmkid,
                        enabled = state.monitorModeEnabled,
                    ) {
                        Text("PMKID")
                    }
                    OutlinedButton(
                        onClick = onHandshake,
                        enabled = state.monitorModeEnabled && state.targetBssid.isNotBlank(),
                    ) {
                        Text("Handshake")
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultCard(state: WpsAttackUiState) {
    val result = state.attackResult ?: state.capturedPassword?.let {
        WpsAttackResult.Success("", it)
    } ?: return

    val (containerColor, icon, title) = when (result) {
        is WpsAttackResult.Success -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            Icons.Default.Check,
            "SUCCESS",
        )
        is WpsAttackResult.Locked -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            Icons.Default.Lock,
            "AP LOCKED",
        )
        WpsAttackResult.RateLimited -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            Icons.Default.Lock,
            "RATE LIMITED",
        )
        is WpsAttackResult.Failed -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            Icons.Default.Close,
            "FAILED",
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null)
                Spacer(Modifier.padding(horizontal = 8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(8.dp))

            when (result) {
                is WpsAttackResult.Success -> {
                    if (result.pin.isNotBlank()) {
                        Text(
                            text = "PIN: ${result.pin}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                    Text(
                        text = "Password: ${result.password}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                is WpsAttackResult.Locked -> {
                    Text(
                        text = "AP locked for ${result.seconds} seconds",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                WpsAttackResult.RateLimited -> {
                    Text(
                        text = "AP is rate limiting WPS requests",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                is WpsAttackResult.Failed -> {
                    Text(
                        text = result.reason,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
