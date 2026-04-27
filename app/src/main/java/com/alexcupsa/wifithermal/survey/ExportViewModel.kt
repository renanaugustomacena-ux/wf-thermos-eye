package com.alexcupsa.wifithermal.survey

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.alexcupsa.wifithermal.core.data.export.SurveyExporter
import com.alexcupsa.wifithermal.core.data.repository.SurveyRepository
import com.alexcupsa.wifithermal.core.model.Survey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class ExportUiState(
    val survey: Survey? = null,
    val exporting: Boolean = false,
    val exportedFile: String? = null,
)

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val application: Application,
    savedStateHandle: SavedStateHandle,
    private val surveyRepository: SurveyRepository,
    private val exporter: SurveyExporter,
) : AndroidViewModel(application) {

    private val surveyId: Long = savedStateHandle.get<Long>("surveyId") ?: 0L

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(survey = surveyRepository.getSurveyById(surveyId))
        }
    }

    fun exportCsv() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(exporting = true)
            val csv = withContext(Dispatchers.IO) { exporter.exportCsv(surveyId) }
            val file = writeExportFile("survey_${surveyId}.csv", csv)
            shareFile(file, "text/csv")
            _uiState.value = _uiState.value.copy(exporting = false, exportedFile = file.name)
        }
    }

    fun exportJson() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(exporting = true)
            val json = withContext(Dispatchers.IO) { exporter.exportJson(surveyId) }
            val file = writeExportFile("survey_${surveyId}.json", json)
            shareFile(file, "application/json")
            _uiState.value = _uiState.value.copy(exporting = false, exportedFile = file.name)
        }
    }

    private suspend fun writeExportFile(name: String, content: String): File {
        return withContext(Dispatchers.IO) {
            val dir = File(application.cacheDir, "exports")
            dir.mkdirs()
            val file = File(dir, name)
            file.writeText(content)
            file
        }
    }

    private fun shareFile(file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(
            application,
            "${application.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        application.startActivity(Intent.createChooser(intent, "Export Survey").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
