package com.volteenabler.app.methods

import com.volteenabler.app.data.ImsPackageInfo
import com.volteenabler.app.shizuku.ShellExecutor

object ImsRepairManager {

    suspend fun scanPackages(context: android.content.Context? = null): List<ImsPackageInfo> {
        val list = ImsPackageInfo.getRequiredPackages()

        // 1. If Shizuku is authorized, use shell commands for complete accuracy (including debloated user 0 packages)
        if (com.volteenabler.app.shizuku.ShizukuManager.isAuthorized.value) {
            try {
                val allPackages = ShellExecutor.exec("pm list packages -u").stdout
                val disabledPackages = ShellExecutor.exec("pm list packages -d").stdout

                if (allPackages.isNotBlank()) {
                    return list.map { pkg ->
                        val isInstalled = allPackages.contains("package:${pkg.packageName}")
                        val isDisabled = disabledPackages.contains("package:${pkg.packageName}")
                        pkg.copy(
                            isInstalled = isInstalled,
                            isEnabled = isInstalled && !isDisabled
                        )
                    }
                }
            } catch (_: Exception) {}
        }

        // 2. Native Android PackageManager fallback (Works 100% without Shizuku)
        if (context != null) {
            val pm = context.packageManager
            return list.map { pkg ->
                try {
                    val appInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        pm.getApplicationInfo(pkg.packageName, android.content.pm.PackageManager.ApplicationInfoFlags.of(0))
                    } else {
                        pm.getApplicationInfo(pkg.packageName, 0)
                    }
                    pkg.copy(
                        isInstalled = true,
                        isEnabled = appInfo.enabled
                    )
                } catch (_: Exception) {
                    pkg.copy(isInstalled = false, isEnabled = false)
                }
            }
        }

        return list
    }

    suspend fun repairPackage(packageName: String): Boolean {
        // Reinstall if uninstalled for user 0
        ShellExecutor.exec("cmd package install-existing $packageName")
        // Enable package
        val enableResult = ShellExecutor.exec("pm enable $packageName")
        return enableResult.isSuccess
    }

    suspend fun repairAll(context: android.content.Context? = null): List<ImsPackageInfo> {
        val packages = ImsPackageInfo.getRequiredPackages()
        for (pkg in packages) {
            repairPackage(pkg.packageName)
        }
        return scanPackages(context)
    }
}
