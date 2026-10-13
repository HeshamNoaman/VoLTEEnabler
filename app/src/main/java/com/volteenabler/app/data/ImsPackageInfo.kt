package com.volteenabler.app.data

data class ImsPackageInfo(
    val packageName: String,
    val title: String,
    val description: String,
    var isInstalled: Boolean = false,
    var isEnabled: Boolean = false
) {
    companion object {
        fun getRequiredPackages(): List<ImsPackageInfo> = listOf(
            ImsPackageInfo(
                packageName = "com.samsung.advp.imssettings",
                title = "Samsung IMS Settings",
                description = "Core GUI configuration app for GCF Mode & VoLTE switches"
            ),
            ImsPackageInfo(
                packageName = "com.sec.imsservice",
                title = "Samsung IMS Service",
                description = "Background daemon running the SIP/IMS stack and call signaling"
            ),
            ImsPackageInfo(
                packageName = "com.sec.ims",
                title = "Samsung IMS Framework",
                description = "Framework provider for IP Multimedia Subsystem features"
            ),
            ImsPackageInfo(
                packageName = "com.sec.imslogger",
                title = "Samsung IMS Logger",
                description = "Diagnostic and logging module for IMS connection monitoring"
            ),
            ImsPackageInfo(
                packageName = "com.sec.epdg",
                title = "Samsung ePDG Service",
                description = "Enhanced Packet Data Gateway for Wi-Fi Calling & handover"
            )
        )
    }
}
