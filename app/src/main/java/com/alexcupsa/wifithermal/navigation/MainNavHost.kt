package com.alexcupsa.wifithermal.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.alexcupsa.wifithermal.analysis.ChannelScreen
import com.alexcupsa.wifithermal.audit.AlertsScreen
import com.alexcupsa.wifithermal.audit.AuditDashboardScreen
import com.alexcupsa.wifithermal.audit.IncidentLogScreen
import com.alexcupsa.wifithermal.audit.TriangulationScreen
import com.alexcupsa.wifithermal.authorization.AuthorizationScreen
import com.alexcupsa.wifithermal.report.ReportScreen
import com.alexcupsa.wifithermal.scan.ApDetailScreen
import com.alexcupsa.wifithermal.scan.ScanScreen
import com.alexcupsa.wifithermal.security.SecurityScreen
import com.alexcupsa.wifithermal.settings.SettingsScreen
import com.alexcupsa.wifithermal.whitelist.WhitelistEditScreen
import com.alexcupsa.wifithermal.whitelist.WhitelistScreen

private enum class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    DASHBOARD("dashboard", "Audit", Icons.Default.Dashboard),
    ALERTS("alerts", "Alerts", Icons.Default.Warning),
    INCIDENTS("incidents", "Log", Icons.AutoMirrored.Filled.List),
    WHITELIST("whitelist", "Whitelist", Icons.Default.Shield),
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
                    AuditDashboardScreen(
                        onNavigateToAuthorization = { navController.navigate("authorization") },
                        onNavigateToAlerts = { navController.navigate("alerts") },
                        onNavigateToWhitelist = { navController.navigate("whitelist") },
                        onNavigateToReports = { navController.navigate("reports") },
                    )
                }
                composable("alerts") {
                    AlertsScreen(
                        onNavigateToAp = { bssid ->
                            navController.navigate("ap/${android.net.Uri.encode(bssid)}")
                        },
                        onNavigateToTriangulate = { bssid ->
                            navController.navigate("triangulate/${android.net.Uri.encode(bssid)}")
                        },
                    )
                }
                composable("incidents") {
                    IncidentLogScreen()
                }
                composable(
                    "triangulate/{bssid}",
                    arguments = listOf(navArgument("bssid") { type = NavType.StringType }),
                ) { entry ->
                    val bssid = entry.arguments?.getString("bssid") ?: ""
                    TriangulationScreen(bssid = bssid, onBack = { navController.popBackStack() })
                }
                composable("whitelist") {
                    WhitelistScreen(
                        onNavigateToEdit = { bssid ->
                            val arg = bssid?.let { android.net.Uri.encode(it) } ?: "new"
                            navController.navigate("whitelist/edit/$arg")
                        },
                    )
                }
                composable(
                    "whitelist/edit/{bssid}",
                    arguments = listOf(navArgument("bssid") { type = NavType.StringType }),
                ) { entry ->
                    val raw = entry.arguments?.getString("bssid")
                    val bssidArg = if (raw == "new" || raw.isNullOrEmpty()) null else raw
                    WhitelistEditScreen(
                        bssidArg = bssidArg,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable("authorization") {
                    AuthorizationScreen(onBack = { navController.popBackStack() })
                }
                composable("reports") {
                    ReportScreen(onBack = { navController.popBackStack() })
                }
                composable("scan") {
                    ScanScreen(
                        onApClick = { bssid ->
                            navController.navigate("ap/${android.net.Uri.encode(bssid)}")
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
                ) { entry ->
                    val bssid = entry.arguments?.getString("bssid") ?: ""
                    ApDetailScreen(bssid = bssid, onBack = { navController.popBackStack() })
                }
                composable("settings") {
                    SettingsScreen()
                }
            }
        }
    }
}
