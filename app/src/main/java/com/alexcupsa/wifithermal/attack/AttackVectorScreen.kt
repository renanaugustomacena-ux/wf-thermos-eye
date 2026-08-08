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
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alexcupsa.wifithermal.core.engine.router.BruteForceResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttackVectorScreen(
    onBack: () -> Unit,
    onNavigateToWps: () -> Unit = {},
    viewModel: AttackVectorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attack Vectors") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
            RootStatusCard(state, viewModel::checkRoot)
            SavedPasswordsCard(state, viewModel::extractSavedPasswords)
            VendorKeygenCard(state, viewModel::updateKeygenTarget, viewModel::runKeygen)
            RouterBruteForceCard(state, viewModel::detectRouter, viewModel::startBruteForce)
            WpsMonitorCard(onNavigateToWps)
        }
    }
}

@Composable
private fun WpsMonitorCard(onNavigate: () -> Unit) {
    Card(
        onClick = onNavigate,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                )
                Spacer(Modifier.padding(horizontal = 8.dp))
                Text(
                    text = "WPS & Monitor Mode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Pixie Dust attack, WPS PIN brute force, PMKID and handshake capture. Requires Termux with aircrack-ng/reaver installed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RootStatusCard(state: AttackVectorUiState, onCheckRoot: () -> Unit) {
    val isRooted = state.isRooted
    val containerColor = when (isRooted) {
        true -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        false -> MaterialTheme.colorScheme.errorContainer
        null -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = when (isRooted) {
                        true -> MaterialTheme.colorScheme.primary
                        false -> MaterialTheme.colorScheme.error
                        null -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Spacer(Modifier.padding(horizontal = 8.dp))
                Column {
                    Text(
                        text = when (isRooted) {
                            true -> "Root Access Available"
                            false -> "Root Access NOT Available"
                            null -> "Root Status Unknown"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = when (isRooted) {
                            true -> "All attack vectors enabled"
                            false -> "Only keygen attacks available"
                            null -> "Tap to check root status"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onCheckRoot,
                enabled = !state.rootCheckInProgress,
            ) {
                if (state.rootCheckInProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        strokeWidth = 2.dp,
                    )
                }
                Text("Check Root")
            }
        }
    }
}

@Composable
private fun SavedPasswordsCard(state: AttackVectorUiState, onExtract: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null)
                Spacer(Modifier.padding(horizontal = 8.dp))
                Text(
                    text = "Saved WiFi Passwords",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Extract saved passwords from /data/misc/wifi/ (requires root)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onExtract,
                enabled = state.isRooted == true && !state.savedPasswordsLoading,
            ) {
                if (state.savedPasswordsLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        strokeWidth = 2.dp,
                    )
                }
                Text("Extract Passwords")
            }

            state.savedPasswordsError?.let { error ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (state.savedPasswords.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Found ${state.savedPasswords.size} networks:",
                    style = MaterialTheme.typography.labelMedium,
                )
                state.savedPasswords.forEach { cred ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = cred.ssid,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = cred.keyMgmt,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                            cred.password?.let { pwd ->
                                Text(
                                    text = pwd,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Text(
                                text = "Source: ${cred.source}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VendorKeygenCard(
    state: AttackVectorUiState,
    onUpdate: (String, String) -> Unit,
    onGenerate: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Key, contentDescription = null)
                Spacer(Modifier.padding(horizontal = 8.dp))
                Text(
                    text = "Vendor Keygen",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Generate default passwords for Italian ISPs (Fastweb, TIM, Vodafone, WindTre)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = state.keygenTargetSsid,
                onValueChange = { onUpdate(it, state.keygenTargetBssid) },
                label = { Text("SSID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.keygenTargetBssid,
                onValueChange = { onUpdate(state.keygenTargetSsid, it) },
                label = { Text("BSSID (MAC)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("AA:BB:CC:DD:EE:FF") },
            )
            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onGenerate,
                enabled = state.keygenTargetBssid.length >= 17,
            ) {
                Text("Generate Candidates")
            }

            state.keygenResult?.let { result ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Vendor: ${result.vendor}",
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = "Algorithm: ${result.algorithm}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Candidates (${result.candidates.size}):",
                    style = MaterialTheme.typography.labelMedium,
                )
                result.candidates.forEach { candidate ->
                    Text(
                        text = "  $candidate",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun RouterBruteForceCard(
    state: AttackVectorUiState,
    onDetect: () -> Unit,
    onBruteForce: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Router, contentDescription = null)
                Spacer(Modifier.padding(horizontal = 8.dp))
                Text(
                    text = "Router Admin Brute Force",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Detect router gateway and test common admin credentials",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onDetect,
                    enabled = !state.routerDetecting,
                ) {
                    if (state.routerDetecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 8.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                    Text("Detect Router")
                }
                Button(
                    onClick = onBruteForce,
                    enabled = state.routerInfo != null && !state.bruteForceRunning,
                ) {
                    Text("Brute Force")
                }
            }

            state.routerInfo?.let { router ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Gateway: ${router.gatewayIp}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    text = buildString {
                        append("Ports: ")
                        router.httpPort?.let { append("HTTP:$it ") }
                        router.httpsPort?.let { append("HTTPS:$it ") }
                        router.sshPort?.let { append("SSH:$it ") }
                        router.telnetPort?.let { append("Telnet:$it") }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
            }

            state.bruteForceProgress?.let { progress ->
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress.attemptsMade.toFloat() / progress.totalAttempts },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "${progress.attemptsMade}/${progress.totalAttempts} - ${progress.status}",
                    style = MaterialTheme.typography.labelSmall,
                )
            }

            state.bruteForceResult?.let { result ->
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (result is BruteForceResult.Success) {
                            Icons.Default.Check
                        } else {
                            Icons.Default.Close
                        },
                        contentDescription = null,
                        tint = if (result is BruteForceResult.Success) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    when (result) {
                        is BruteForceResult.Success -> {
                            Column {
                                Text(
                                    text = "SUCCESS",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = "${result.username}:${result.password}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "Method: ${result.method}",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                        is BruteForceResult.Exhausted -> {
                            Text(
                                text = "All credentials exhausted",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        is BruteForceResult.RateLimited -> {
                            Text(
                                text = "Rate limited by router",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        is BruteForceResult.Error -> {
                            Text(
                                text = "Error: ${result.message}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}
