package com.volteenabler.app.shizuku

object SettingsHelper {

    suspend fun getSystem(key: String): String {
        val result = ShellExecutor.exec("settings get system $key")
        val out = result.stdout.trim()
        return if (out == "null") "" else out
    }

    suspend fun putSystem(key: String, value: String): Boolean {
        // Safe escaping
        val escaped = value.replace("'", "'\\''")
        val result = ShellExecutor.exec("settings put system $key '$escaped'")
        return result.isSuccess
    }

    suspend fun getSecure(key: String): String {
        val result = ShellExecutor.exec("settings get secure $key")
        val out = result.stdout.trim()
        return if (out == "null") "" else out
    }

    suspend fun putSecure(key: String, value: String): Boolean {
        val escaped = value.replace("'", "'\\''")
        val result = ShellExecutor.exec("settings put secure $key '$escaped'")
        return result.isSuccess
    }

    suspend fun getGlobal(key: String): String {
        val result = ShellExecutor.exec("settings get global $key")
        val out = result.stdout.trim()
        return if (out == "null") "" else out
    }

    suspend fun putGlobal(key: String, value: String): Boolean {
        val escaped = value.replace("'", "'\\''")
        val result = ShellExecutor.exec("settings put global $key '$escaped'")
        return result.isSuccess
    }
}
