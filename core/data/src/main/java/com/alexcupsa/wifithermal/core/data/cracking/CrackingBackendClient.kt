package com.alexcupsa.wifithermal.core.data.cracking

import android.content.Context
import android.content.SharedPreferences
import com.alexcupsa.wifithermal.core.data.repository.AuthorizationManifestRepository
import com.alexcupsa.wifithermal.core.data.repository.CapturedHandshakeRepository
import com.alexcupsa.wifithermal.core.engine.audit.OffensiveScopeGuard
import com.alexcupsa.wifithermal.core.model.audit.CapturedHandshake
import com.alexcupsa.wifithermal.core.model.audit.CrackStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Submits captured handshakes to a Hashcat-backed server reachable over
 * the operator's Tailscale mesh. The backend URL is operator-configured;
 * the client never falls back to public-internet hosts and never uses
 * cleartext.
 *
 * Two scope checks before transmission:
 *   1. The capture row's BSSID is re-evaluated against
 *      [OffensiveScopeGuard]. If the offensive scope was tightened after
 *      capture (e.g. the operator removed a MAC from authorizedBssids),
 *      submission is blocked here.
 *   2. The backend URL must be HTTPS (no http://). Without TLS, mutual
 *      auth is impossible and submitting raw handshake bytes over a LAN
 *      is a needless leak.
 *
 * Tailscale gives identity-pinned routing; the backend can additionally
 * authenticate on the Tailscale identity header (X-Tailscale-User).
 */
@Singleton
class CrackingBackendClient @Inject constructor(
    @ApplicationContext context: Context,
    private val captureRepo: CapturedHandshakeRepository,
    private val scopeRepo: AuthorizationManifestRepository,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("crack_backend", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun backendUrl(): String? = prefs.getString(KEY_BACKEND_URL, null)?.takeIf { it.isNotBlank() }

    fun setBackendUrl(url: String) {
        prefs.edit().putString(KEY_BACKEND_URL, url.trim()).apply()
    }

    fun authToken(): String? = prefs.getString(KEY_AUTH_TOKEN, null)?.takeIf { it.isNotBlank() }

    fun setAuthToken(token: String) {
        prefs.edit().putString(KEY_AUTH_TOKEN, token.trim()).apply()
    }

    sealed interface SubmitOutcome {
        data class Submitted(val backendJobId: String) : SubmitOutcome
        data class Rejected(val reason: String) : SubmitOutcome
        data class Failed(val message: String) : SubmitOutcome
    }

    suspend fun submit(capture: CapturedHandshake): SubmitOutcome {
        val backendUrl = backendUrl()
            ?: return SubmitOutcome.Failed("Backend URL not configured.")
        if (!backendUrl.startsWith("https://")) {
            return SubmitOutcome.Rejected("Backend URL must use https:// over Tailscale.")
        }

        val decision = OffensiveScopeGuard.evaluate(scopeRepo.scope.value, capture.bssid)
        if (decision !is OffensiveScopeGuard.Decision.Allowed) {
            captureRepo.updateOutcome(capture.id, CrackStatus.REJECTED_OUT_OF_SCOPE)
            return SubmitOutcome.Rejected("OffensiveScopeGuard refused: ${(decision as OffensiveScopeGuard.Decision.Refused).reason.name}")
        }

        return try {
            val request = SubmitRequest(
                bssid = capture.bssid,
                ssid = capture.ssid,
                kind = capture.kind.name,
                payloadHex = capture.payloadHex,
            )
            val (status, body) = doPost("$backendUrl/jobs", json.encodeToString(SubmitRequest.serializer(), request))
            if (status in 200..299) {
                val resp = json.decodeFromString(SubmitResponse.serializer(), body)
                captureRepo.updateOutcome(capture.id, CrackStatus.SUBMITTED, backendJobId = resp.jobId)
                SubmitOutcome.Submitted(resp.jobId)
            } else {
                captureRepo.updateOutcome(capture.id, CrackStatus.FAILED)
                SubmitOutcome.Failed("HTTP $status: $body")
            }
        } catch (e: Exception) {
            captureRepo.updateOutcome(capture.id, CrackStatus.FAILED)
            SubmitOutcome.Failed(e.message ?: e::class.simpleName.orEmpty())
        }
    }

    suspend fun pollJob(capture: CapturedHandshake): SubmitOutcome {
        val backendUrl = backendUrl()
            ?: return SubmitOutcome.Failed("Backend URL not configured.")
        val jobId = capture.backendJobId
            ?: return SubmitOutcome.Failed("Capture has no backend job id.")
        return try {
            val (status, body) = doGet("$backendUrl/jobs/$jobId")
            if (status in 200..299) {
                val resp = json.decodeFromString(JobStatusResponse.serializer(), body)
                val newStatus = when (resp.status) {
                    "cracked" -> CrackStatus.CRACKED
                    "exhausted" -> CrackStatus.EXHAUSTED
                    "running", "queued" -> CrackStatus.SUBMITTED
                    else -> CrackStatus.FAILED
                }
                captureRepo.updateOutcome(capture.id, newStatus, jobId, resp.password)
                SubmitOutcome.Submitted(jobId)
            } else {
                SubmitOutcome.Failed("HTTP $status: $body")
            }
        } catch (e: Exception) {
            SubmitOutcome.Failed(e.message ?: e::class.simpleName.orEmpty())
        }
    }

    private fun doPost(urlString: String, body: String): Pair<Int, String> {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 30_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            authToken()?.let { setRequestProperty("Authorization", "Bearer $it") }
        }
        conn.outputStream.use { it.write(body.toByteArray()) }
        val status = conn.responseCode
        val text = (if (status in 200..299) conn.inputStream else conn.errorStream)
            ?.bufferedReader()?.use { it.readText() }.orEmpty()
        conn.disconnect()
        return status to text
    }

    private fun doGet(urlString: String): Pair<Int, String> {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 30_000
            authToken()?.let { setRequestProperty("Authorization", "Bearer $it") }
        }
        val status = conn.responseCode
        val text = (if (status in 200..299) conn.inputStream else conn.errorStream)
            ?.bufferedReader()?.use { it.readText() }.orEmpty()
        conn.disconnect()
        return status to text
    }

    @Serializable
    private data class SubmitRequest(
        val bssid: String,
        val ssid: String,
        val kind: String,
        val payloadHex: String,
    )

    @Serializable
    private data class SubmitResponse(val jobId: String)

    @Serializable
    private data class JobStatusResponse(
        val status: String,        // queued | running | cracked | exhausted | failed
        val password: String? = null,
    )

    companion object {
        private const val KEY_BACKEND_URL = "backend_url"
        private const val KEY_AUTH_TOKEN = "auth_token"
    }
}
