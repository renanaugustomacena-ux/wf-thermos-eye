package com.alexcupsa.wifithermal.report

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alexcupsa.wifithermal.core.data.export.IncidentReportExporter
import com.alexcupsa.wifithermal.core.data.export.WhitelistExporter
import com.alexcupsa.wifithermal.core.data.repository.AuditPipeline
import com.alexcupsa.wifithermal.core.data.repository.AuthorizationManifestRepository
import com.alexcupsa.wifithermal.core.data.repository.IncidentRepository
import com.alexcupsa.wifithermal.core.data.repository.WhitelistRepository
import com.alexcupsa.wifithermal.core.engine.audit.ReportRedactor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ReportUiState(
    val redactionLevel: ReportRedactor.Level = ReportRedactor.Level.NONE,
    val exporting: Boolean = false,
    val lastIncidentReport: ReportArtifact? = null,
    val lastWhitelistReport: ReportArtifact? = null,
    val errorMessage: String? = null,
)

data class ReportArtifact(
    val csvPath: String,
    val pdfPath: String?,
    val sha256: String?,
    val timestampMs: Long,
)

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val application: Application,
    private val incidentRepo: IncidentRepository,
    private val whitelistRepo: WhitelistRepository,
    private val scopeRepo: AuthorizationManifestRepository,
    private val pipeline: AuditPipeline,
    private val incidentExporter: IncidentReportExporter,
    private val whitelistExporter: WhitelistExporter,
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(ReportUiState())
    val state: StateFlow<ReportUiState> = _state.asStateFlow()

    fun setRedactionLevel(level: ReportRedactor.Level) {
        _state.value = _state.value.copy(redactionLevel = level)
    }

    fun exportIncidentReport() {
        if (_state.value.exporting) return
        _state.value = _state.value.copy(exporting = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val incidents = incidentRepo.recent(limit = 1000).first()
                val anomalies = pipeline.anomalies.value
                val scope = scopeRepo.scope.value
                val output = incidentExporter.export(
                    IncidentReportExporter.Inputs(
                        incidents = incidents,
                        anomalies = anomalies,
                        scope = scope,
                        redactionLevel = _state.value.redactionLevel,
                    ),
                )
                _state.value = _state.value.copy(
                    exporting = false,
                    lastIncidentReport = ReportArtifact(
                        csvPath = output.csvFile.absolutePath,
                        pdfPath = output.pdfFile.absolutePath,
                        sha256 = output.sha256,
                        timestampMs = System.currentTimeMillis(),
                    ),
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    exporting = false,
                    errorMessage = "Export failed: ${e.message}",
                )
            }
        }
    }

    fun exportWhitelist() {
        if (_state.value.exporting) return
        _state.value = _state.value.copy(exporting = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val entries = whitelistRepo.observeAll().first()
                val file = whitelistExporter.export(entries, _state.value.redactionLevel)
                _state.value = _state.value.copy(
                    exporting = false,
                    lastWhitelistReport = ReportArtifact(
                        csvPath = file.absolutePath,
                        pdfPath = null,
                        sha256 = null,
                        timestampMs = System.currentTimeMillis(),
                    ),
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    exporting = false,
                    errorMessage = "Export failed: ${e.message}",
                )
            }
        }
    }

    fun shareIntentFor(path: String, mimeType: String): Intent {
        val file = File(path)
        val uri: Uri = FileProvider.getUriForFile(
            application,
            "${application.packageName}.fileprovider",
            file,
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
