package com.alexcupsa.wifithermal.core.data.termux

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed interface TermuxResult<out T> {
    data class Success<T>(val data: T) : TermuxResult<T>
    data class Error(val message: String) : TermuxResult<Nothing>
    data object NotInstalled : TermuxResult<Nothing>
    data object ToolNotFound : TermuxResult<Nothing>
}

data class TermuxTool(
    val name: String,
    val path: String,
    val installed: Boolean,
)

@Singleton
class TermuxBridge @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val TERMUX_PACKAGE = "com.termux"
        private const val TERMUX_PREFIX = "/data/data/com.termux/files/usr"
        private const val TERMUX_BIN = "$TERMUX_PREFIX/bin"
        private const val TERMUX_HOME = "/data/data/com.termux/files/home"

        val REQUIRED_TOOLS = listOf(
            "aircrack-ng",
            "airodump-ng",
            "aireplay-ng",
            "reaver",
            "bully",
            "hcxdumptool",
            "hcxpcapngtool",
            "hashcat",
            "wifite",
        )
    }

    fun isTermuxInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(TERMUX_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    suspend fun checkInstalledTools(): List<TermuxTool> = withContext(Dispatchers.IO) {
        if (!isTermuxInstalled()) return@withContext emptyList()

        REQUIRED_TOOLS.map { tool ->
            val path = "$TERMUX_BIN/$tool"
            val exists = File(path).exists()
            TermuxTool(
                name = tool,
                path = path,
                installed = exists,
            )
        }
    }

    suspend fun isToolInstalled(tool: String): Boolean = withContext(Dispatchers.IO) {
        if (!isTermuxInstalled()) return@withContext false
        File("$TERMUX_BIN/$tool").exists()
    }

    fun launchTermux() {
        val intent = context.packageManager.getLaunchIntentForPackage(TERMUX_PACKAGE)
        intent?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(it)
        }
    }

    fun runInTermux(command: String) {
        val intent = Intent().apply {
            setClassName(TERMUX_PACKAGE, "com.termux.app.RunCommandService")
            action = "com.termux.RUN_COMMAND"
            putExtra("com.termux.RUN_COMMAND_PATH", "$TERMUX_BIN/bash")
            putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-c", command))
            putExtra("com.termux.RUN_COMMAND_WORKDIR", TERMUX_HOME)
            putExtra("com.termux.RUN_COMMAND_BACKGROUND", false)
            putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startService(intent)
    }

    fun buildInstallCommand(): String {
        return """
            pkg update -y && \
            pkg install -y root-repo && \
            pkg install -y aircrack-ng reaver hashcat hcxtools
        """.trimIndent().replace("\n", " ")
    }

    fun getTermuxPrefix(): String = TERMUX_PREFIX
    fun getTermuxBin(): String = TERMUX_BIN
    fun getTermuxHome(): String = TERMUX_HOME
}
