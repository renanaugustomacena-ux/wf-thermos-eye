package com.alexcupsa.wifithermal.authorization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexcupsa.wifithermal.core.data.repository.AuthorizationManifestRepository
import com.alexcupsa.wifithermal.core.model.audit.AuthorizationScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AuthorizationUiState(
    val scope: AuthorizationScope? = null,
    val expired: Boolean = false,
)

@HiltViewModel
class AuthorizationViewModel @Inject constructor(
    private val repo: AuthorizationManifestRepository,
) : ViewModel() {

    val state: StateFlow<AuthorizationUiState> = repo.scope
        .map { scope ->
            AuthorizationUiState(
                scope = scope,
                expired = scope?.isExpired() ?: false,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AuthorizationUiState())

    fun save(
        organizationName: String,
        authorizedBy: String,
        ssidPatterns: List<String>,
        bssidPrefixes: List<String>,
        expiresAt: Long?,
        notes: String,
    ) {
        val scope = AuthorizationScope(
            organizationName = organizationName.trim(),
            authorizedBy = authorizedBy.trim(),
            authorizedAt = System.currentTimeMillis(),
            expiresAt = expiresAt,
            ssidPatterns = ssidPatterns.map { it.trim() }.filter { it.isNotEmpty() },
            bssidPrefixes = bssidPrefixes.map { it.trim().uppercase() }.filter { it.isNotEmpty() },
            notes = notes.trim(),
        )
        repo.save(scope)
    }

    fun clear() {
        repo.clear()
    }
}
