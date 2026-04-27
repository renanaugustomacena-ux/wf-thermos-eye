package com.alexcupsa.wifithermal.security

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alexcupsa.wifithermal.core.model.SecurityAuditResult
import com.alexcupsa.wifithermal.core.model.SecurityFinding
import com.alexcupsa.wifithermal.core.model.SecurityScore
import com.alexcupsa.wifithermal.core.model.Severity
import com.alexcupsa.wifithermal.core.ui.theme.SignalExcellent
import com.alexcupsa.wifithermal.core.ui.theme.SignalFair
import com.alexcupsa.wifithermal.core.ui.theme.SignalGood
import com.alexcupsa.wifithermal.core.ui.theme.SignalUnusable
import com.alexcupsa.wifithermal.core.ui.theme.SignalWeak

@Composable
fun SecurityScreen(
    viewModel: SecurityAuditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val auditResult = state.audit

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Security Audit",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${state.results.size} access points audited",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (auditResult == null) {
            item {
                Spacer(Modifier.height(32.dp))
                Text(
                    text = "Start a scan to run security audit",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
            return@LazyColumn
        }

        item { ScoreCard(auditResult) }

        item { SecurityDistributionCard(auditResult) }

        if (auditResult.findings.isNotEmpty()) {
            item {
                Text(
                    text = "${auditResult.findings.size} findings",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        items(auditResult.findings) { finding ->
            FindingCard(finding)
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun ScoreCard(result: SecurityAuditResult) {
    val (scoreLabel, scoreColor) = when (result.overallScore) {
        SecurityScore.EXCELLENT -> "Excellent" to SignalExcellent
        SecurityScore.GOOD -> "Good" to SignalGood
        SecurityScore.FAIR -> "Fair" to SignalFair
        SecurityScore.POOR -> "Poor" to SignalWeak
        SecurityScore.CRITICAL -> "Critical" to SignalUnusable
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = scoreColor.copy(alpha = 0.15f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Shield,
                contentDescription = null,
                tint = scoreColor,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = "Security Score",
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = scoreLabel,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor,
                )
            }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                val criticalCount = result.findings.count { it.severity == Severity.CRITICAL }
                val highCount = result.findings.count { it.severity == Severity.HIGH }
                if (criticalCount > 0) {
                    Text(
                        text = "$criticalCount critical",
                        style = MaterialTheme.typography.labelSmall,
                        color = SignalUnusable,
                    )
                }
                if (highCount > 0) {
                    Text(
                        text = "$highCount high",
                        style = MaterialTheme.typography.labelSmall,
                        color = SignalWeak,
                    )
                }
            }
        }
    }
}

@Composable
private fun SecurityDistributionCard(result: SecurityAuditResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Encryption Distribution", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                DistItem("Open", result.openCount, SignalUnusable)
                DistItem("WEP", result.wepCount, SignalWeak)
                DistItem("WPA", result.wpaCount, SignalFair)
                DistItem("WPA2", result.wpa2Count, SignalGood)
                DistItem("WPA3", result.wpa3Count, SignalExcellent)
            }
        }
    }
}

@Composable
private fun DistItem(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleLarge,
            fontFamily = FontFamily.Monospace,
            color = if (count > 0) color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FindingCard(finding: SecurityFinding) {
    val severityColor = when (finding.severity) {
        Severity.CRITICAL -> SignalUnusable
        Severity.HIGH -> SignalWeak
        Severity.MEDIUM -> SignalFair
        Severity.LOW -> SignalGood
        Severity.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(severityColor),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = finding.severity.name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = severityColor,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = finding.title,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = finding.ssid.ifEmpty { finding.bssid },
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = finding.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = finding.recommendation,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f))
                    .padding(8.dp),
            )
        }
    }
}
