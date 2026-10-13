package com.volteenabler.app.data

import com.volteenabler.app.shizuku.ShellExecutor

data class DeviceInfo(
    val model: String = "Loading...",
    val brand: String = "",
    val manufacturer: String = "",
    val androidVersion: String = "",
    val oneUiVersion: String = "",
    val csc: String = "",
    val simState: String = ""
) {
    val displayOneUi: String
        get() = when {
            oneUiVersion.isBlank() -> "N/A"
            oneUiVersion.length >= 5 -> {
                val major = oneUiVersion.substring(0, 1)
                val minor = oneUiVersion.substring(1, 2)
                "One UI $major.$minor ($oneUiVersion)"
            }
            else -> "One UI $oneUiVersion"
        }

    companion object {
        suspend fun load(): DeviceInfo {
            val model = ShellExecutor.exec("getprop ro.product.model").stdout.ifBlank { "Unknown" }
            val brand = ShellExecutor.exec("getprop ro.product.brand").stdout
            val manufacturer = ShellExecutor.exec("getprop ro.product.manufacturer").stdout
            val androidVer = ShellExecutor.exec("getprop ro.build.version.release").stdout
            val oneUiVer = ShellExecutor.exec("getprop ro.build.version.oneui").stdout
            val csc = ShellExecutor.exec("getprop ro.csc.sales_code").stdout
            val sim = ShellExecutor.exec("getprop gsm.sim.state").stdout

            return DeviceInfo(
                model = model,
                brand = brand,
                manufacturer = manufacturer,
                androidVersion = androidVer,
                oneUiVersion = oneUiVer,
                csc = csc,
                simState = sim
            )
        }
    }
}
