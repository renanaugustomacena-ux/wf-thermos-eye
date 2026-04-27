package com.alexcupsa.wifithermal.whitelist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alexcupsa.wifithermal.core.model.SecurityType
import com.alexcupsa.wifithermal.core.model.audit.DeviceType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhitelistEditScreen(
    bssidArg: String?,
    onBack: () -> Unit,
    viewModel: WhitelistViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val existing = remember(state.entries, bssidArg) {
        bssidArg?.let { argBssid -> state.entries.firstOrNull { it.bssid.equals(argBssid, ignoreCase = true) } }
    }

    var bssid by remember(existing) { mutableStateOf(existing?.bssid ?: "") }
    var ssid by remember(existing) { mutableStateOf(existing?.ssid ?: "") }
    var location by remember(existing) { mutableStateOf(existing?.location ?: "") }
    var owner by remember(existing) { mutableStateOf(existing?.owner ?: "") }
    var notes by remember(existing) { mutableStateOf(existing?.notes ?: "") }
    var deviceType by remember(existing) { mutableStateOf(existing?.deviceType ?: DeviceType.ENTERPRISE_AP) }
    var expectedSecurity by remember(existing) {
        mutableStateOf(existing?.expectedSecurity ?: SecurityType.WPA2_PSK)
    }

    // Once we have the existing entry loaded, refresh fields if user navigated
    // back to a record that loads asynchronously after first composition.
    LaunchedEffect(existing?.bssid) {
        if (existing != null) {
            bssid = existing.bssid
            ssid = existing.ssid
            location = existing.location.orEmpty()
            owner = existing.owner.orEmpty()
            notes = existing.notes.orEmpty()
            deviceType = existing.deviceType
            expectedSecurity = existing.expectedSecurity
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "New entry" else "Edit entry") },
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
            OutlinedTextField(
                value = bssid,
                onValueChange = { bssid = it.uppercase() },
                label = { Text("BSSID (MAC)") },
                singleLine = true,
                enabled = existing == null, // PK; do not allow rename
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = ssid,
                onValueChange = { ssid = it },
                label = { Text("SSID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            EnumDropdown(
                label = "Device type",
                options = DeviceType.entries,
                selected = deviceType,
                onSelect = { deviceType = it },
                render = { it.name },
            )
            EnumDropdown(
                label = "Expected security",
                options = SecurityType.entries,
                selected = expectedSecurity,
                onSelect = { expectedSecurity = it },
                render = { it.name },
            )
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = owner,
                onValueChange = { owner = it },
                label = { Text("Owner / department") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    viewModel.upsert(
                        bssid = bssid.trim(),
                        ssid = ssid.trim(),
                        location = location,
                        owner = owner,
                        deviceType = deviceType,
                        expectedSecurity = expectedSecurity,
                        notes = notes,
                    )
                    onBack()
                },
                enabled = bssid.isNotBlank() && ssid.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (existing == null) "Authorize" else "Save")
            }

            if (existing == null) {
                Text(
                    text = "BSSID is the primary key. SSID alone is unsafe to whitelist (anyone can name a hotspot to match).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> EnumDropdown(
    label: String,
    options: Iterable<T>,
    selected: T,
    onSelect: (T) -> Unit,
    render: (T) -> String,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = render(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(render(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
