package com.alexcupsa.wifithermal.audit

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alexcupsa.wifithermal.core.model.ScanStatus
import com.alexcupsa.wifithermal.core.model.audit.AlertSeverity
import com.alexcupsa.wifithermal.core.model.audit.RogueAlert
import com.alexcupsa.wifithermal.core.ui.theme.SignalExcellent
import com.alexcupsa.wifithermal.core.ui.theme.SignalFair
import com.alexcupsa.wifithermal.core.ui.theme.SignalGood
import com.alexcupsa.wifithermal.core.ui.theme.SignalUnusable
import com.alexcupsa.wifithermal.core.ui.theme.SignalWeak
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AuditDashboardScreen(
    onNavigateToAuthorization: () -> Unit,
    onNavigateToAlerts: () -> Unit,
    onNavigateToWhitelist: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToChannels: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToLayerB: () -> Unit,
    onNavigateToAttack: () -> Unit = {},
    viewModel: AuditDashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            text = "Wireless Audit",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Internal authorization auditor",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(20.dp))

        ScopeBanner(state, onNavigateToAuthorization)

        Spacer(Modifier.height(12.dp))

        if (state.scanStatus == ScanStatus.SCANNING) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
        }

        SeverityRow(state.alertsBySeverity, onNavigateToAlerts)

        Spacer(Modifier.height(12.dp))

        ApCountsCard(state, onNavigateToWhitelist)

        Spacer(Modifier.height(12.dp))

        if (state.criticalAlerts.isNotEmpty()) {
            CriticalAlertsCard(state.criticalAlerts, onNavigateToAlerts)
            Spacer(Modifier.height(12.dp))
        }

        if (state.recentIncidents.isNotEmpty()) {
            RecentIncidentsCard(state.recentIncidents)
            Spacer(Modifier.height(12.dp))
        }

        InsightsRow(onNavigateToChannels, onNavigateToSecurity)
        Spacer(Modifier.height(12.dp))

        ReportsLink(onNavigateToReports)
        Spacer(Modifier.height(12.dp))

        LayerBLink(onNavigateToLayerB)
        Spacer(Modifier.height(12.dp))

        AttackVectorLink(onNavigateToAttack)
    }
}

@Composable
private fun LayerBLink(onTap: () -> Unit) {
    Card(
        onClick = onTap,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Layer B — Capture",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "ESP32 sniffer companion + handshake/PMKID capture + cracking pipeline. Hard-gated by OffensiveScopeGuard — inert until exact authorized BSSIDs are added in Authorization.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AttackVectorLink(onTap: () -> Unit) {
    Card(
        onClick = onTap,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Attack Vectors",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Root-based password extraction, vendor keygen, router admin brute force. Works standalone on rooted Android without ESP32 companion.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InsightsRow(onChannels: () -> Unit, onSecurity: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            onClick = onChannels,
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Channels", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Congestion + recommended channels for in-scope APs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Card(
            onClick = onSecurity,
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Security", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Crypto posture audit for in-scope APs (WPA2/WPA3, WPS exposure).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ReportsLink(onTap: () -> Unit) {
    Card(
        onClick = onTap,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Reports", style = MaterialTheme.typography.titleSmall)
            Text(
                text = "Export incident timeline + whitelist as CSV / PDF, with redaction levels for IT or HR sharing.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ScopeBanner(state: DashboardUiState, onTap: () -> Unit) {
    val scope = state.scope
    val (containerColor, headline) = when {
        scope == null -> MaterialTheme.colorScheme.errorContainer to "No authorization loaded"
        state.scopeExpired -> MaterialTheme.colorScheme.errorContainer to "Authorization EXPIRED"
        else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) to scope.organizationName
    }
    Card(
        onClick = onTap,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (scope == null || state.scopeExpired) Icons.Default.LockOpen else Icons.Default.Shield,
                    contentDescription = null,
                    tint = if (scope == null || state.scopeExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.size(8.dp))
                Text(headline, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (scope != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Authorized by: ${scope.authorizedBy}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Since ${formatDate(scope.authorizedAt)}" +
                        (scope.expiresAt?.let { " — expires ${formatDate(it)}" }.orEmpty()),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Scope: ${scope.ssidPatterns.size} SSID patterns, ${scope.bssidPrefixes.size} BSSID prefixes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Tap to load or create one. Without an authorization scope the audit pipeline is disabled.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SeverityRow(
    bySeverity: Map<AlertSeverity, Int>,
    onTap: () -> Unit,
) {
    Card(
        onClick = onTap,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Live alerts", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                AlertSeverity.entries.forEach { sev ->
                    SeverityPill(sev, bySeverity[sev] ?: 0)
                }
            }
        }
    }
}

@Composable
private fun SeverityPill(severity: AlertSeverity, count: Int) {
    val color = severity.color()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = if (count > 0) 0.85f else 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "$count",
                color = if (count > 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = severity.label(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ApCountsCard(state: DashboardUiState, onNavigateToWhitelist: () -> Unit) {
    Card(
        onClick = onNavigateToWhitelist,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Inventory", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Stat("Observed", state.totalApsObserved)
                Stat("In scope", state.inScopeCount)
                Stat("Whitelisted", state.authorizedCount)
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$value",
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CriticalAlertsCard(alerts: List<RogueAlert>, onTap: () -> Unit) {
    Card(
        onClick = onTap,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.size(8.dp))
                Text(
                    text = "Critical alerts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(8.dp))
            alerts.take(3).forEach { alert ->
                AlertSummaryRow(alert)
                Spacer(Modifier.height(4.dp))
            }
            if (alerts.size > 3) {
                Text(
                    text = "+ ${alerts.size - 3} more",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AlertSummaryRow(alert: RogueAlert) {
    val (title, subtitle) = when (alert) {
        is RogueAlert.UnknownAccessPoint ->
            "Unknown AP: ${alert.ssid.ifEmpty { "<hidden>" }}" to "${alert.bssid} | ${alert.rssi} dBm | ${alert.security.name}"
        is RogueAlert.EvilTwin ->
            "EVIL TWIN of '${alert.impersonatedSsid}'" to "Clone BSSID: ${alert.bssid}"
    }
    Column {
        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RecentIncidentsCard(incidents: List<com.alexcupsa.wifithermal.core.model.audit.IncidentEvent>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Recent incidents", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            incidents.take(5).forEach { ev ->
                Text(
                    text = "${formatTime(ev.timestamp)} | ${ev.kind.name} | ${ev.bssid}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    text = ev.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

private fun AlertSeverity.color(): Color = when (this) {
    AlertSeverity.CRITICAL -> SignalUnusable
    AlertSeverity.HIGH -> SignalWeak
    AlertSeverity.MEDIUM -> SignalFair
    AlertSeverity.LOW -> SignalGood
    AlertSeverity.INFO -> SignalExcellent
}

private fun AlertSeverity.label(): String = name.lowercase().replaceFirstChar { it.uppercase() }

private fun formatDate(epochMs: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(epochMs))

private fun formatTime(epochMs: Long): String =
    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(epochMs))
