package com.alexcupsa.wifithermal.core.common.root

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

sealed interface RootResult<out T> {
    data class Success<T>(val data: T) : RootResult<T>
    data class Error(val code: Int, val message: String) : RootResult<Nothing>
    data object NoRoot : RootResult<Nothing>
    data object Timeout : RootResult<Nothing>
}

@Singleton
class RootShell @Inject constructor() {

    private var rootAvailable: Boolean? = null

    suspend fun isRooted(): Boolean = withContext(Dispatchers.IO) {
        rootAvailable?.let { return@withContext it }
        val result = exec("id")
        val available = result is RootResult.Success && result.data.contains("uid=0")
        rootAvailable = available
        available
    }

    suspend fun exec(command: String, timeoutMs: Long = 30_000): RootResult<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val process = Runtime.getRuntime().exec("su")
                val stdin = DataOutputStream(process.outputStream)
                val stdout = BufferedReader(InputStreamReader(process.inputStream))
                val stderr = BufferedReader(InputStreamReader(process.errorStream))

                stdin.writeBytes("$command\n")
                stdin.writeBytes("exit\n")
                stdin.flush()
                stdin.close()

                val completed = process.waitFor(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
                if (!completed) {
                    process.destroyForcibly()
                    return@withContext RootResult.Timeout
                }

                val output = stdout.readText()
                val errorOutput = stderr.readText()
                val exitCode = process.exitValue()

                stdout.close()
                stderr.close()

                if (exitCode == 0) {
                    RootResult.Success(output.trim())
                } else {
                    RootResult.Error(exitCode, errorOutput.ifBlank { output }.trim())
                }
            }.getOrElse { e ->
                val msg = e.message.orEmpty()
                if (msg.contains("Permission denied") || msg.contains("not found")) {
                    RootResult.NoRoot
                } else {
                    RootResult.Error(-1, msg)
                }
            }
        }

    suspend fun execLines(command: String, timeoutMs: Long = 30_000): RootResult<List<String>> {
        return when (val result = exec(command, timeoutMs)) {
            is RootResult.Success -> RootResult.Success(
                result.data.lines().filter { it.isNotBlank() }
            )
            is RootResult.Error -> result
            RootResult.NoRoot -> RootResult.NoRoot
            RootResult.Timeout -> RootResult.Timeout
        }
    }

    suspend fun readFile(path: String): RootResult<String> = exec("cat '$path'")

    suspend fun fileExists(path: String): Boolean {
        val result = exec("test -f '$path' && echo yes || echo no")
        return result is RootResult.Success && result.data.trim() == "yes"
    }

    suspend fun listDir(path: String): RootResult<List<String>> = execLines("ls -1 '$path'")
}
