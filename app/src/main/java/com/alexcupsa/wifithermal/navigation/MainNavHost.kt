package com.alexcupsa.wifithermal.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.alexcupsa.wifithermal.core.model.ScanStatus
import com.alexcupsa.wifithermal.core.model.SignalQuality
import com.alexcupsa.wifithermal.core.ui.theme.SignalExcellent
import com.alexcupsa.wifithermal.core.ui.theme.SignalFair
import com.alexcupsa.wifithermal.core.ui.theme.SignalGood
import com.alexcupsa.wifithermal.core.ui.theme.SignalUnusable
import com.alexcupsa.wifithermal.core.ui.theme.SignalWeak
import com.alexcupsa.wifithermal.dashboard.DashboardUiState
import com.alexcupsa.wifithermal.dashboard.DashboardViewModel
import com.alexcupsa.wifithermal.analysis.ChannelScreen
import com.alexcupsa.wifithermal.heatmap.HeatmapScreen
import com.alexcupsa.wifithermal.scan.ApDetailScreen
import com.alexcupsa.wifithermal.scan.ScanScreen
import com.alexcupsa.wifithermal.security.SecurityScreen
import com.alexcupsa.wifithermal.settings.SettingsScreen
import com.alexcupsa.wifithermal.survey.ActiveSurveyScreen
import com.alexcupsa.wifithermal.survey.ExportScreen
import com.alexcupsa.wifithermal.survey.SurveyListScreen

private enum class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    DASHBOARD("dashboard", "Dashboard", Icons.Default.Dashboard),
    SCAN("scan", "Scan", Icons.Default.Wifi),
    SURVEYS("surveys", "Surveys", Icons.Default.Map),
    SETTINGS("settings", "Settings", Icons.Default.Settings),
}

@Composable
fun MainNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomNavRoutes = BottomNavItem.entries.map { it.route }.toSet()
    val showBottomBar = currentDestination?.route in bottomNavRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    BottomNavItem.entries.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            NavHost(navController = navController, startDestination = "dashboard") {
                composable("dashboard") {
                    DashboardScreen(
                        onNavigateToScan = { navController.navigate("scan") },
                        onNavigateToSurveys = { navController.navigate("surveys") },
                        onNavigateToChannels = { navController.navigate("channels") },
                        onNavigateToSecurity = { navController.navigate("security") },
                    )
                }
                composable("scan") {
                    ScanScreen(
                        onApClick = { bssid ->
                            navController.navigate("ap/$bssid")
                        },
                    )
                }
                composable("channels") {
                    ChannelScreen()
                }
                composable("security") {
                    SecurityScreen()
                }
                composable(
                    "ap/{bssid}",
                    arguments = listOf(navArgument("bssid") { type = NavType.StringType }),
                ) { backStackEntry ->
                    val bssid = backStackEntry.arguments?.getString("bssid") ?: ""
                    ApDetailScreen(bssid = bssid, onBack = { navController.popBackStack() })
                }
                composable(
                    "heatmap/{surveyId}",
                    arguments = listOf(navArgument("surveyId") { type = NavType.LongType }),
                ) {
                    HeatmapScreen(onBack = { navController.popBackStack() })
                }
                composable("surveys") {
                    SurveyListScreen(
                        onSurveyClick = { surveyId ->
                            navController.navigate("survey/$surveyId")
                        },
                    )
                }
                composable("settings") {
                    SettingsScreen()
                }
                composable(
                    "survey/{surveyId}",
                    arguments = listOf(navArgument("surveyId") { type = NavType.LongType }),
                ) {
                    ActiveSurveyScreen(
                        onComplete = { navController.popBackStack() },
                    )
                }
                composable(
                    "export/{surveyId}",
                    arguments = listOf(navArgument("surveyId") { type = NavType.LongType }),
                ) {
                    ExportScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}

@Composable
private fun DashboardScreen(
    onNavigateToScan: () -> Unit,
    onNavigateToSurveys: () -> Unit,
    onNavigateToChannels: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "WiFi Thermal Scanner",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(24.dp))

        ConnectedNetworkCard(uiState)

        Spacer(modifier = Modifier.height(12.dp))

        ApOverviewCard(uiState, onNavigateToScan)

        Spacer(modifier = Modifier.height(12.dp))

        SignalDistributionCard(uiState)

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            QuickActionCard(
                title = "Channels",
                subtitle = "Analysis",
                onClick = onNavigateToChannels,
                modifier = Modifier.weight(1f),
            )
            QuickActionCard(
                title = "Security",
                subtitle = "Audit",
                onClick = onNavigateToSecurity,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        SurveysCard(uiState, onNavigateToSurveys)

        Spacer(modifier = Modifier.weight(1f))

        TextButton(
            onClick = { viewModel.triggerScan() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = if (uiState.scanStatus == ScanStatus.SCANNING) "Scanning..." else "Quick Scan",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun ConnectedNetworkCard(state: DashboardUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (state.connectedAp != null) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Connected Network", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            if (state.connectedAp != null) {
                val ap = state.connectedAp
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Wifi, null, tint = SignalExcellent, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.size(8.dp))
                    Column {
                        Text(
                            text = ap.ssid.ifEmpty { "<hidden>" },
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "${ap.rssi} dBm | Ch ${ap.channel} | ${ap.security.name}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                Text("Not connected", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ApOverviewCard(state: DashboardUiState, onTap: () -> Unit) {
    Card(
        onClick = onTap,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Visible Access Points", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatItem("Total", "${state.totalAps}")
                StatItem("2.4 GHz", "${state.aps24}")
                StatItem("5 GHz", "${state.aps5}")
            }
            if (state.scanStatus == ScanStatus.SCANNING) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun SignalDistributionCard(state: DashboardUiState) {
    if (state.signalDistribution.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Signal Quality Distribution", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                val qualityColors = mapOf(
                    SignalQuality.EXCELLENT to SignalExcellent,
                    SignalQuality.GOOD to SignalGood,
                    SignalQuality.FAIR to SignalFair,
                    SignalQuality.WEAK to SignalWeak,
                    SignalQuality.UNUSABLE to SignalUnusable,
                )
                for ((quality, color) in qualityColors) {
                    val count = state.signalDistribution[quality] ?: 0
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$count",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace,
                            color = color,
                        )
                        Text(
                            text = quality.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SurveysCard(state: DashboardUiState, onTap: () -> Unit) {
    Card(
        onClick = onTap,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Surveys", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${state.surveyCount} surveys recorded",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
