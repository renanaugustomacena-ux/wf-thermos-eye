package com.alexcupsa.wifithermal.authorization

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorizationScreen(
    onBack: () -> Unit,
    viewModel: AuthorizationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var orgName by remember { mutableStateOf("") }
    var authorizedBy by remember { mutableStateOf("") }
    var ssidPatterns by remember { mutableStateOf("") }
    var bssidPrefixes by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var expiresAtIso by remember { mutableStateOf("") }

    LaunchedEffect(state.scope) {
        val scope = state.scope ?: return@LaunchedEffect
        orgName = scope.organizationName
        authorizedBy = scope.authorizedBy
        ssidPatterns = scope.ssidPatterns.joinToString(", ")
        bssidPrefixes = scope.bssidPrefixes.joinToString(", ")
        notes = scope.notes
        expiresAtIso = scope.expiresAt?.let {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it))
        }.orEmpty()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Authorization") },
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
            ScopeStatusCard(state)

            Text(
                text = "Authorization manifest",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Defines what the audit pipeline is allowed to inspect. Out-of-scope BSSIDs are silently dropped before reaching detectors, repos, or logs.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = orgName,
                onValueChange = { orgName = it },
                label = { Text("Organization name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = authorizedBy,
                onValueChange = { authorizedBy = it },
                label = { Text("Authorized by (your name + role)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = ssidPatterns,
                onValueChange = { ssidPatterns = it },
                label = { Text("SSID patterns (comma separated, '*' suffix wildcard)") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = bssidPrefixes,
                onValueChange = { bssidPrefixes = it },
                label = { Text("BSSID OUI prefixes (e.g. AA:BB:CC, comma separated)") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = expiresAtIso,
                onValueChange = { expiresAtIso = it },
                label = { Text("Expires at YYYY-MM-DD (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    viewModel.save(
                        organizationName = orgName,
                        authorizedBy = authorizedBy,
                        ssidPatterns = ssidPatterns.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                        bssidPrefixes = bssidPrefixes.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                        expiresAt = parseDate(expiresAtIso),
                        notes = notes,
                    )
                    onBack()
                },
                enabled = orgName.isNotBlank() && authorizedBy.isNotBlank() &&
                    (ssidPatterns.isNotBlank() || bssidPrefixes.isNotBlank()),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save")
            }

            OutlinedButton(
                onClick = { viewModel.clear() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Clear scope")
            }
        }
    }
}

@Composable
private fun ScopeStatusCard(state: AuthorizationUiState) {
    val scope = state.scope
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                scope == null -> MaterialTheme.colorScheme.errorContainer
                state.expired -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            },
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val headline = when {
                scope == null -> "No scope loaded — audit pipeline disabled"
                state.expired -> "Scope expired"
                else -> "Active scope: ${scope.organizationName}"
            }
            Text(headline, style = MaterialTheme.typography.titleSmall)
            if (scope != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Authorized by ${scope.authorizedBy}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Since ${formatDate(scope.authorizedAt)}" +
                        (scope.expiresAt?.let { " — expires ${formatDate(it)}" }.orEmpty()),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun parseDate(iso: String): Long? {
    val trimmed = iso.trim()
    if (trimmed.isEmpty()) return null
    return runCatching {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(trimmed)?.time
    }.getOrNull()
}

private fun formatDate(epochMs: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(epochMs))
