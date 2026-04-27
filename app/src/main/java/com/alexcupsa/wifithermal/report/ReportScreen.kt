package com.alexcupsa.wifithermal.report

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alexcupsa.wifithermal.core.engine.audit.ReportRedactor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    onBack: () -> Unit,
    viewModel: ReportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports") },
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
            Text(
                text = "Generate audit reports",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Pick a redaction level for the recipient. Reports go to the app's exports folder; tap Share to send via any installed app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            RedactionPicker(state.redactionLevel, viewModel::setRedactionLevel)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Incident report", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "All incidents in DB + active anomalies + scope. CSV + PDF.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.exportIncidentReport() },
                        enabled = !state.exporting,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (state.exporting) CircularProgressIndicator(modifier = Modifier.height(20.dp))
                        else Text("Export incident report")
                    }
                    state.lastIncidentReport?.let { artifact ->
                        Spacer(Modifier.height(8.dp))
                        ArtifactRow(
                            label = "Last export",
                            sha = artifact.sha256,
                            csvPath = artifact.csvPath,
                            pdfPath = artifact.pdfPath,
                            onShareCsv = {
                                context.startActivity(viewModel.shareIntentFor(artifact.csvPath, "text/csv"))
                            },
                            onSharePdf = artifact.pdfPath?.let { p ->
                                { context.startActivity(viewModel.shareIntentFor(p, "application/pdf")) }
                            },
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Whitelist export", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "All authorized AP entries (BSSID, SSID, type, expected security, owner). CSV.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.exportWhitelist() },
                        enabled = !state.exporting,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (state.exporting) CircularProgressIndicator(modifier = Modifier.height(20.dp))
                        else Text("Export whitelist CSV")
                    }
                    state.lastWhitelistReport?.let { artifact ->
                        Spacer(Modifier.height(8.dp))
                        ArtifactRow(
                            label = "Last export",
                            sha = null,
                            csvPath = artifact.csvPath,
                            pdfPath = null,
                            onShareCsv = {
                                context.startActivity(viewModel.shareIntentFor(artifact.csvPath, "text/csv"))
                            },
                            onSharePdf = null,
                        )
                    }
                }
            }

            state.errorMessage?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun RedactionPicker(
    selected: ReportRedactor.Level,
    onSelect: (ReportRedactor.Level) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Redaction", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "NONE: full data (internal only). PARTIAL: BSSIDs masked except last 4 hex digits. FULL: pseudonyms + SSID hashed for external sharing.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ReportRedactor.Level.entries.forEach { level ->
                    FilterChip(
                        selected = selected == level,
                        onClick = { onSelect(level) },
                        label = { Text(level.name) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtifactRow(
    label: String,
    sha: String?,
    csvPath: String,
    pdfPath: String?,
    onShareCsv: () -> Unit,
    onSharePdf: (() -> Unit)?,
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(
            text = csvPath.substringAfterLast('/'),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
        )
        sha?.let {
            Text(
                text = "sha256: ${it.take(16)}…",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onShareCsv) { Text("Share CSV") }
            if (onSharePdf != null) {
                OutlinedButton(onClick = onSharePdf) { Text("Share PDF") }
            }
        }
    }
}

@Suppress("unused") // for FontWeight.Bold lint
private val keepImport = FontWeight.Bold
