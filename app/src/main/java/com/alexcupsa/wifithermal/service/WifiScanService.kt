package com.alexcupsa.wifithermal.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.IBinder
import com.alexcupsa.wifithermal.core.data.adapter.WifiScanAdapter
import com.alexcupsa.wifithermal.core.model.ProcessedScanResult
import com.alexcupsa.wifithermal.core.model.ScanStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WifiScanService : Service() {

    @Inject lateinit var scanAdapter: WifiScanAdapter

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var wifiManager: WifiManager
    private var scanJob: Job? = null

    private val scanTimestamps = ArrayDeque<Long>(THROTTLE_MAX_SCANS)

    private val scanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
            if (success || _scanResults.value.isEmpty()) {
                processScanResults()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        wifiManager = getSystemService(WIFI_SERVICE) as WifiManager
        ScanNotificationManager.createChannel(this)
        registerReceiver(
            scanReceiver,
            IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION),
            RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SCANNING -> startScanning()
            ACTION_STOP_SCANNING -> stopScanning()
            ACTION_SINGLE_SCAN -> performSingleScan()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scanJob?.cancel()
        scope.cancel()
        try { unregisterReceiver(scanReceiver) } catch (_: Exception) {}
        super.onDestroy()
    }

    private fun startScanning() {
        startForeground(
            ScanNotificationManager.NOTIFICATION_ID,
            ScanNotificationManager.buildNotification(this, scanning = true),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
        )
        _scanStatus.value = ScanStatus.SCANNING

        scanJob?.cancel()
        scanJob = scope.launch {
            while (true) {
                if (canScan()) {
                    triggerScan()
                    delay(SCAN_INTERVAL_MS)
                } else {
                    _scanStatus.value = ScanStatus.THROTTLED
                    val waitMs = timeUntilNextScan()
                    delay(waitMs)
                    _scanStatus.value = ScanStatus.SCANNING
                }
            }
        }
    }

    private fun stopScanning() {
        scanJob?.cancel()
        scanJob = null
        _scanStatus.value = ScanStatus.IDLE
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun performSingleScan() {
        startForeground(
            ScanNotificationManager.NOTIFICATION_ID,
            ScanNotificationManager.buildNotification(this, scanning = true),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
        )
        _scanStatus.value = ScanStatus.SCANNING

        scope.launch {
            if (canScan()) {
                triggerScan()
                delay(SCAN_RESULT_WAIT_MS)
            }
            processScanResults()
            _scanStatus.value = ScanStatus.COMPLETE
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    @Suppress("DEPRECATION")
    private fun triggerScan() {
        recordScanTimestamp()
        wifiManager.startScan()
    }

    @Suppress("DEPRECATION")
    private fun processScanResults() {
        try {
            val raw = wifiManager.scanResults ?: return
            val connInfo = wifiManager.connectionInfo
            val connBssid = connInfo?.bssid?.takeIf { it != "02:00:00:00:00:00" }
            val processed = scanAdapter.process(raw, connBssid)
                .sortedByDescending { it.smoothedRssi }
            _scanResults.value = processed
            updateNotification(processed.size)
        } catch (_: SecurityException) {
            // Location permission not granted
        }
    }

    private fun updateNotification(apCount: Int) {
        val notification = ScanNotificationManager.buildNotification(
            this,
            apCount = apCount,
            scanning = _scanStatus.value == ScanStatus.SCANNING,
        )
        val nm = getSystemService(android.app.NotificationManager::class.java)
        nm.notify(ScanNotificationManager.NOTIFICATION_ID, notification)
    }

    // HC-1: Android throttles to 4 scans per 2 minutes for foreground apps.
    // We track our own timestamps to stay under the limit proactively.
    private fun canScan(): Boolean {
        val now = System.currentTimeMillis()
        pruneOldTimestamps(now)
        return scanTimestamps.size < THROTTLE_MAX_SCANS
    }

    private fun timeUntilNextScan(): Long {
        if (scanTimestamps.isEmpty()) return 0
        val oldest = scanTimestamps.first()
        val available = oldest + THROTTLE_WINDOW_MS - System.currentTimeMillis()
        return available.coerceAtLeast(1000)
    }

    private fun recordScanTimestamp() {
        scanTimestamps.addLast(System.currentTimeMillis())
    }

    private fun pruneOldTimestamps(now: Long) {
        while (scanTimestamps.isNotEmpty() && now - scanTimestamps.first() > THROTTLE_WINDOW_MS) {
            scanTimestamps.removeFirst()
        }
    }

    companion object {
        const val ACTION_START_SCANNING = "com.alexcupsa.wifithermal.START_SCANNING"
        const val ACTION_STOP_SCANNING = "com.alexcupsa.wifithermal.STOP_SCANNING"
        const val ACTION_SINGLE_SCAN = "com.alexcupsa.wifithermal.SINGLE_SCAN"

        private const val THROTTLE_MAX_SCANS = 4
        private const val THROTTLE_WINDOW_MS = 120_000L
        private const val SCAN_INTERVAL_MS = 32_000L
        private const val SCAN_RESULT_WAIT_MS = 3_000L

        private val _scanResults = MutableStateFlow<List<ProcessedScanResult>>(emptyList())
        val scanResults: StateFlow<List<ProcessedScanResult>> = _scanResults.asStateFlow()

        private val _scanStatus = MutableStateFlow(ScanStatus.IDLE)
        val scanStatus: StateFlow<ScanStatus> = _scanStatus.asStateFlow()

        fun startIntent(context: Context): Intent =
            Intent(context, WifiScanService::class.java).apply {
                action = ACTION_START_SCANNING
            }

        fun stopIntent(context: Context): Intent =
            Intent(context, WifiScanService::class.java).apply {
                action = ACTION_STOP_SCANNING
            }

        fun singleScanIntent(context: Context): Intent =
            Intent(context, WifiScanService::class.java).apply {
                action = ACTION_SINGLE_SCAN
            }
    }
}
