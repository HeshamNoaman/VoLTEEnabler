package com.volteenabler.app.methods

import com.volteenabler.app.data.ImsPackageInfo
import com.volteenabler.app.shizuku.ShellExecutor

object ImsRepairManager {

    suspend fun scanPackages(): List<ImsPackageInfo> {
        val allPackages = ShellExecutor.exec("pm list packages -u").stdout
        val disabledPackages = ShellExecutor.exec("pm list packages -d").stdout

        val list = ImsPackageInfo.getRequiredPackages()
        return list.map { pkg ->
            val isInstalled = allPackages.contains("package:${pkg.packageName}")
            val isDisabled = disabledPackages.contains("package:${pkg.packageName}")
            pkg.copy(
                isInstalled = isInstalled,
                isEnabled = isInstalled && !isDisabled
            )
        }
    }

    suspend fun repairPackage(packageName: String): Boolean {
        // Reinstall if uninstalled for user 0
        ShellExecutor.exec("cmd package install-existing $packageName")
        // Enable package
        val enableResult = ShellExecutor.exec("pm enable $packageName")
        return enableResult.isSuccess
    }

    suspend fun repairAll(): List<ImsPackageInfo> {
        val packages = ImsPackageInfo.getRequiredPackages()
        for (pkg in packages) {
            repairPackage(pkg.packageName)
        }
        return scanPackages()
    }
}
