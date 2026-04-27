package com.alexcupsa.wifithermal.common

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.DisposableEffect

@Composable
fun PermissionGate(
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity

    fun checkAllGranted(): Boolean = SCAN_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    var allGranted by remember { mutableStateOf(checkAllGranted()) }
    var permanentlyDenied by remember { mutableStateOf(false) }

    // Re-check on resume so returning from Settings flips the gate without
    // requiring an extra tap.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                allGranted = checkAllGranted()
                if (allGranted) permanentlyDenied = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (allGranted) {
        content()
        return
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        allGranted = results.values.all { it }
        if (!allGranted && activity != null) {
            // Android returns false for any denied permission. If
            // shouldShowRequestPermissionRationale also returns false for that
            // permission, the user has chosen "Don't ask again" — the system
            // dialog will never appear again from launch(). Send them to
            // Settings instead.
            permanentlyDenied = results.entries.any { (perm, granted) ->
                !granted && !ActivityCompat.shouldShowRequestPermissionRationale(activity, perm)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.LocationOn,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Permissions Required",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (permanentlyDenied) {
                "Permissions were denied. Open Settings to enable Location and " +
                    "Nearby devices for WiFi Thermal Scanner."
            } else {
                "WiFi Thermal Scanner needs location permission to scan for WiFi networks. " +
                    "Android requires location access for WiFi scanning because WiFi signals " +
                    "can be used to determine your location."
            },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Your location data stays on this device and is never transmitted anywhere.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = {
                if (permanentlyDenied) {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        },
                    )
                } else {
                    launcher.launch(SCAN_PERMISSIONS.toTypedArray())
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (permanentlyDenied) "Open Settings" else "Grant Permissions")
        }
    }
}
