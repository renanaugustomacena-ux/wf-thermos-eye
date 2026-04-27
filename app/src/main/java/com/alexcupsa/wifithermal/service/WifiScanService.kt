package com.alexcupsa.wifithermal.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.IBinder
import com.alexcupsa.wifithermal.core.data.adapter.WifiScanAdapter
import com.alexcupsa.wifithermal.core.data.repository.WifiScanStateRepository
import com.alexcupsa.wifithermal.core.model.ScanStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@AndroidEntryPoint
class WifiScanService : Service() {

    @Inject lateinit var scanAdapter: WifiScanAdapter
    @Inject lateinit var scanState: WifiScanStateRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var wifiManager: WifiManager
    private lateinit var connectivityManager: ConnectivityManager
    private var scanJob: Job? = null

    private val timestampsMutex = Mutex()
    private val scanTimestamps = ArrayDeque<Long>(THROTTLE_MAX_SCANS)

    @Volatile private var connectedBssid: String? = null

    private val scanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
            if (success || scanState.scanResults.value.isEmpty()) {
                processScanResults()
            }
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            val wifiInfo = capabilities.transportInfo as? WifiInfo ?: return
            val bssid = wifiInfo.bssid?.takeIf { it != REDACTED_BSSID }
            connectedBssid = bssid
        }

        override fun onLost(network: Network) {
            connectedBssid = null
        }
    }

    override fun onCreate() {
        super.onCreate()
        wifiManager = getSystemService(WIFI_SERVICE) as WifiManager
        connectivityManager = getSystemService(ConnectivityManager::class.java)
        ScanNotificationManager.createChannel(this)
        registerReceiver(
            scanReceiver,
            IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION),
            RECEIVER_NOT_EXPORTED,
        )
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Android 14+ requires startForeground() within 5 seconds of every
        // onStartCommand, including null-intent restarts. Promote first, then
        // route on the action.
        promoteToForeground()

        when (intent?.action) {
            ACTION_START_SCANNING -> startScanning()
            ACTION_STOP_SCANNING -> stopScanning()
            ACTION_SINGLE_SCAN -> performSingleScan()
            else -> {
                // Null intent (process restart with no pending command) or unknown
                // action — we already called startForeground, now stop cleanly.
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf(startId)
            }
        }
        // We do not want Android to re-deliver this command; consumers re-issue
        // explicit start intents on user action.
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scanJob?.cancel()
        scope.cancel()
        scanState.updateStatus(ScanStatus.IDLE)
        runCatching { unregisterReceiver(scanReceiver) }
        runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
        super.onDestroy()
    }

    private fun promoteToForeground() {
        startForeground(
            ScanNotificationManager.NOTIFICATION_ID,
            ScanNotificationManager.buildNotification(this, scanning = scanJob?.isActive == true),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
        )
    }

    private fun startScanning() {
        scanState.updateStatus(ScanStatus.SCANNING)

        scanJob?.cancel()
        scanJob = scope.launch {
            while (true) {
                if (canScan()) {
                    triggerScan()
                    delay(SCAN_INTERVAL_MS)
                } else {
                    scanState.updateStatus(ScanStatus.THROTTLED)
                    val waitMs = timeUntilNextScan()
                    delay(waitMs)
                    scanState.updateStatus(ScanStatus.SCANNING)
                }
            }
        }
    }

    private fun stopScanning() {
        scanJob?.cancel()
        scanJob = null
        scanState.updateStatus(ScanStatus.IDLE)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun performSingleScan() {
        scanState.updateStatus(ScanStatus.SCANNING)

        scope.launch {
            val started = if (canScan()) {
                triggerScan()
                delay(SCAN_RESULT_WAIT_MS)
                true
            } else {
                scanState.updateStatus(ScanStatus.THROTTLED)
                false
            }
            if (started) {
                processScanResults()
                scanState.updateStatus(ScanStatus.COMPLETE)
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun triggerScan() {
        recordScanTimestamp()
        val started = wifiManager.startScan()
        if (!started) {
            // OS-level throttle bucket overrun (independent of our local window).
            scanState.updateStatus(ScanStatus.THROTTLED)
        }
    }

    private fun processScanResults() {
        try {
            val raw = wifiManager.scanResults ?: return
            val processed = scanAdapter.process(raw, connectedBssid)
                .sortedByDescending { it.smoothedRssi }
            scanState.updateResults(processed)
            updateNotification(processed.size)
        } catch (_: SecurityException) {
            // Location permission revoked while service was running.
            scanState.updateStatus(ScanStatus.IDLE)
        }
    }

    private fun updateNotification(apCount: Int) {
        val notification = ScanNotificationManager.buildNotification(
            this,
            apCount = apCount,
            scanning = scanState.scanStatus.value == ScanStatus.SCANNING,
        )
        val nm = getSystemService(android.app.NotificationManager::class.java)
        nm.notify(ScanNotificationManager.NOTIFICATION_ID, notification)
    }

    // HC-1: Android throttles to 4 scans per 2 minutes for foreground apps.
    // We track our own timestamps to stay under the limit proactively. Mutex
    // because the deque is mutated from coroutines on Dispatchers.Default.
    private suspend fun canScan(): Boolean = timestampsMutex.withLock {
        val now = System.currentTimeMillis()
        pruneOldTimestamps(now)
        scanTimestamps.size < THROTTLE_MAX_SCANS
    }

    private suspend fun timeUntilNextScan(): Long = timestampsMutex.withLock {
        if (scanTimestamps.isEmpty()) return@withLock 0L
        val oldest = scanTimestamps.first()
        val available = oldest + THROTTLE_WINDOW_MS - System.currentTimeMillis()
        available.coerceAtLeast(1000L)
    }

    private suspend fun recordScanTimestamp() = timestampsMutex.withLock {
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

        // Sentinel value Android returns instead of a real BSSID when the caller
        // does not hold the perms required to read it.
        private const val REDACTED_BSSID = "02:00:00:00:00:00"

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
