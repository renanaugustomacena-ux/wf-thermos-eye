package com.alexcupsa.wifithermal.whitelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexcupsa.wifithermal.core.data.repository.WhitelistRepository
import com.alexcupsa.wifithermal.core.model.SecurityType
import com.alexcupsa.wifithermal.core.model.audit.AuthorizedAccessPoint
import com.alexcupsa.wifithermal.core.model.audit.DeviceType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WhitelistUiState(
    val entries: List<AuthorizedAccessPoint> = emptyList(),
)

@HiltViewModel
class WhitelistViewModel @Inject constructor(
    private val repo: WhitelistRepository,
) : ViewModel() {

    val state: StateFlow<WhitelistUiState> = repo.observeAll()
        .map { WhitelistUiState(entries = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WhitelistUiState())

    fun upsert(
        bssid: String,
        ssid: String,
        location: String?,
        owner: String?,
        deviceType: DeviceType,
        expectedSecurity: SecurityType,
        notes: String?,
    ) {
        val ap = AuthorizedAccessPoint(
            bssid = bssid.uppercase(),
            ssid = ssid,
            location = location?.takeIf { it.isNotBlank() },
            owner = owner?.takeIf { it.isNotBlank() },
            deviceType = deviceType,
            expectedSecurity = expectedSecurity,
            notes = notes?.takeIf { it.isNotBlank() },
            authorizedAt = System.currentTimeMillis(),
        )
        viewModelScope.launch { repo.upsert(ap) }
    }

    fun remove(bssid: String) {
        viewModelScope.launch { repo.remove(bssid) }
    }
}
