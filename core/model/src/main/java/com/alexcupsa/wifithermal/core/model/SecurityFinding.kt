package com.alexcupsa.wifithermal.core.model

data class SecurityFinding(
    val severity: Severity,
    val bssid: String,
    val ssid: String,
    val title: String,
    val description: String,
    val recommendation: String,
)

data class SecurityAuditResult(
    val findings: List<SecurityFinding>,
    val totalAps: Int,
    val openCount: Int,
    val wepCount: Int,
    val wpaCount: Int,
    val wpa2Count: Int,
    val wpa3Count: Int,
    val overallScore: SecurityScore,
)
