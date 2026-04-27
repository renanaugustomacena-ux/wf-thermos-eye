package com.alexcupsa.wifithermal.audit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexcupsa.wifithermal.core.data.repository.AuditPipeline
import com.alexcupsa.wifithermal.core.data.repository.IncidentRepository
import com.alexcupsa.wifithermal.core.model.audit.AnomalyFlag
import com.alexcupsa.wifithermal.core.model.audit.IncidentEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class IncidentLogUiState(
    val incidents: List<IncidentEvent> = emptyList(),
    val anomalies: List<AnomalyFlag> = emptyList(),
)

@HiltViewModel
class IncidentLogViewModel @Inject constructor(
    incidentRepo: IncidentRepository,
    pipeline: AuditPipeline,
) : ViewModel() {

    val state: StateFlow<IncidentLogUiState> = combine(
        incidentRepo.recent(limit = 200),
        pipeline.anomalies,
    ) { incidents, anomalies ->
        IncidentLogUiState(incidents = incidents, anomalies = anomalies)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), IncidentLogUiState())
}
