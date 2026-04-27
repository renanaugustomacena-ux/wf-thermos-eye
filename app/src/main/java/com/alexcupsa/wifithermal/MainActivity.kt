package com.alexcupsa.wifithermal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.alexcupsa.wifithermal.common.PermissionGate
import com.alexcupsa.wifithermal.core.ui.theme.WifiThermalTheme
import com.alexcupsa.wifithermal.navigation.MainNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WifiThermalTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PermissionGate {
                        MainNavHost()
                    }
                }
            }
        }
    }
}
