package com.volteenabler.app.data

data class ApnProfile(
    val title: String,
    val description: String,
    val name: String,
    val apn: String,
    val username: String = "",
    val password: String = "",
    val apnType: String = ""
) {
    companion object {
        fun getDefaultProfiles(): List<ApnProfile> = listOf(
            ApnProfile(
                title = "Yemen Mobile 4G Data",
                description = "Primary high-speed internet APN",
                name = "ymdata",
                apn = "ymdata",
                username = "ymdata",
                password = "ymdata",
                apnType = "default,mms,supl"
            ),
            ApnProfile(
                title = "Yemen Mobile VoLTE (IMS)",
                description = "Required for VoLTE HD calling & registration",
                name = "ims",
                apn = "ims",
                username = "ims",
                password = "ims",
                apnType = "ims,default"
            ),
            ApnProfile(
                title = "Yemen Mobile VoLTE (XCAP)",
                description = "Required for supplementary services (call forwarding, call waiting)",
                name = "xcap",
                apn = "xcap",
                username = "xcap",
                password = "xcap",
                apnType = "xcap"
            ),
            ApnProfile(
                title = "Generic VoLTE IMS",
                description = "Standard APN used by many global carriers for IMS/VoLTE",
                name = "IMS",
                apn = "ims",
                username = "",
                password = "",
                apnType = "ims"
            )
        )
    }
}
