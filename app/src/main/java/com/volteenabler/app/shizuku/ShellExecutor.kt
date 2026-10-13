package com.volteenabler.app.shizuku

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

data class ShellResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String
) {
    val isSuccess: Boolean get() = exitCode == 0
}

object ShellExecutor {
    private const val TAG = "ShellExecutor"

    suspend fun exec(command: String): ShellResult = withContext(Dispatchers.IO) {
        val useShizuku = ShizukuManager.isAuthorized.value

        try {
            val process: Process = if (useShizuku) {
                try {
                    // Shizuku.newProcess is private static in rikka.shizuku.Shizuku, use getDeclaredMethod
                    val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                        "newProcess",
                        Array<String>::class.java,
                        Array<String>::class.java,
                        String::class.java
                    )
                    newProcessMethod.isAccessible = true
                    newProcessMethod.invoke(
                        null,
                        arrayOf("sh", "-c", command) as Any,
                        null,
                        null
                    ) as Process
                } catch (e: Exception) {
                    Log.e(TAG, "Shizuku newProcess reflection failed, falling back to Runtime.exec", e)
                    Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
                }
            } else {
                Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            }

            val stdoutBuilder = StringBuilder()
            val stderrBuilder = StringBuilder()

            val stdoutReader = BufferedReader(InputStreamReader(process.inputStream))
            val stderrReader = BufferedReader(InputStreamReader(process.errorStream))

            val stdoutThread = Thread {
                try {
                    var line: String?
                    while (stdoutReader.readLine().also { line = it } != null) {
                        stdoutBuilder.append(line).append("\n")
                    }
                } catch (_: Exception) {}
            }

            val stderrThread = Thread {
                try {
                    var line: String?
                    while (stderrReader.readLine().also { line = it } != null) {
                        stderrBuilder.append(line).append("\n")
                    }
                } catch (_: Exception) {}
            }

            stdoutThread.start()
            stderrThread.start()

            val exitCode = process.waitFor()
            stdoutThread.join(2000)
            stderrThread.join(2000)

            val out = stdoutBuilder.toString().trimEnd()
            val err = stderrBuilder.toString().trimEnd()
            Log.d(TAG, "exec '$command' -> exit=$exitCode, out='$out', err='$err'")

            ShellResult(
                exitCode = exitCode,
                stdout = out,
                stderr = err
            )
        } catch (e: Exception) {
            Log.e(TAG, "exec exception for '$command'", e)
            ShellResult(
                exitCode = -1,
                stdout = "",
                stderr = e.message ?: "Execution failed"
            )
        }
    }
}
