package com.alexcupsa.wifithermal.audit

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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alexcupsa.wifithermal.core.model.audit.AlertSeverity
import com.alexcupsa.wifithermal.core.model.audit.RogueAlert

@Composable
fun AlertsScreen(
    onNavigateToAp: (String) -> Unit,
    onNavigateToTriangulate: (String) -> Unit,
    viewModel: AlertsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Alerts", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = state.severityFilter == null,
                onClick = { viewModel.setFilter(null) },
                label = { Text("All") },
            )
            AlertSeverity.entries.forEach { sev ->
                FilterChip(
                    selected = state.severityFilter == sev,
                    onClick = { viewModel.setFilter(sev) },
                    label = { Text(sev.name) },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        if (state.alerts.isEmpty()) {
            Text(
                text = "No alerts in current scope.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.alerts, key = { it.bssid + "|" + it::class.simpleName }) { alert ->
                AlertCard(
                    alert = alert,
                    onAuthorize = { viewModel.authorize(alert) },
                    onInspect = { onNavigateToAp(alert.bssid) },
                    onLocate = { onNavigateToTriangulate(alert.bssid) },
                )
            }
        }
    }
}

@Composable
private fun AlertCard(
    alert: RogueAlert,
    onAuthorize: () -> Unit,
    onInspect: () -> Unit,
    onLocate: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = alert.title(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                AssistChip(
                    onClick = {},
                    label = { Text(alert.severity.name) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = severityContainer(alert.severity),
                    ),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = alert.subtitle(),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onInspect) { Text("Inspect") }
                OutlinedButton(onClick = onLocate) { Text("Locate") }
                TextButton(onClick = onAuthorize) { Text("Authorize") }
            }
        }
    }
}

private fun RogueAlert.title(): String = when (this) {
    is RogueAlert.UnknownAccessPoint -> "Unknown AP: ${ssid.ifEmpty { "<hidden>" }}"
    is RogueAlert.EvilTwin -> "EVIL TWIN of '$impersonatedSsid'"
}

private fun RogueAlert.subtitle(): String = when (this) {
    is RogueAlert.UnknownAccessPoint ->
        "$bssid | $rssi dBm | ${security.name}" + (vendor?.let { " | $it" }.orEmpty())
    is RogueAlert.EvilTwin ->
        "Clone $bssid vs authorized $authorizedBssid | $rssi dBm" + (vendor?.let { " | $it" }.orEmpty())
}

@Composable
private fun severityContainer(severity: AlertSeverity) = when (severity) {
    AlertSeverity.CRITICAL -> MaterialTheme.colorScheme.errorContainer
    AlertSeverity.HIGH -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
    AlertSeverity.MEDIUM -> MaterialTheme.colorScheme.tertiaryContainer
    AlertSeverity.LOW -> MaterialTheme.colorScheme.secondaryContainer
    AlertSeverity.INFO -> MaterialTheme.colorScheme.primaryContainer
}
