package com.alexcupsa.wifithermal.survey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexcupsa.wifithermal.core.data.repository.SurveyRepository
import com.alexcupsa.wifithermal.core.model.Survey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SurveyListUiState(
    val surveys: List<Survey> = emptyList(),
    val showCreateDialog: Boolean = false,
    val showDeleteConfirm: Long? = null,
)

@HiltViewModel
class SurveyListViewModel @Inject constructor(
    private val surveyRepository: SurveyRepository,
) : ViewModel() {

    private val _showCreateDialog = MutableStateFlow(false)
    private val _showDeleteConfirm = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<SurveyListUiState> = combine(
        surveyRepository.getAllSurveys(),
        _showCreateDialog,
        _showDeleteConfirm,
    ) { surveys, showCreate, deleteId ->
        SurveyListUiState(
            surveys = surveys,
            showCreateDialog = showCreate,
            showDeleteConfirm = deleteId,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SurveyListUiState())

    fun showCreateDialog() {
        _showCreateDialog.value = true
    }

    fun dismissCreateDialog() {
        _showCreateDialog.value = false
    }

    fun createSurvey(name: String, description: String?) {
        viewModelScope.launch {
            surveyRepository.createSurvey(name, description, null)
            _showCreateDialog.value = false
        }
    }

    fun requestDelete(surveyId: Long) {
        _showDeleteConfirm.value = surveyId
    }

    fun dismissDeleteConfirm() {
        _showDeleteConfirm.value = null
    }

    fun confirmDelete() {
        val id = _showDeleteConfirm.value ?: return
        viewModelScope.launch {
            surveyRepository.deleteSurvey(id)
            _showDeleteConfirm.value = null
        }
    }

    fun completeSurvey(surveyId: Long) {
        viewModelScope.launch {
            surveyRepository.completeSurvey(surveyId)
        }
    }
}
