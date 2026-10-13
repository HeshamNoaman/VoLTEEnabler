package com.volteenabler.app.methods

import com.volteenabler.app.shizuku.SettingsHelper
import com.volteenabler.app.shizuku.ShellExecutor
import com.volteenabler.app.shizuku.ShellResult

object SideKeyManager {
    private const val KEY_ENABLE = "function_key_config_doublepress"
    private const val KEY_TYPE = "function_key_config_doublepress_type"
    private const val KEY_VALUE = "function_key_config_doublepress_value"

    const val IMS_COMPONENT = "com.samsung.advp.imssettings/com.samsung.advp.imssettings.MainActivity"
    const val CAMERA_COMPONENT = "com.sec.android.app.camera/com.sec.android.app.camera.Camera"

    suspend fun applyImsDoublePress(): Boolean {
        val s1 = SettingsHelper.putGlobal(KEY_ENABLE, "1")
        val s2 = SettingsHelper.putGlobal(KEY_TYPE, "2")
        val s3 = SettingsHelper.putGlobal(KEY_VALUE, IMS_COMPONENT)
        return s1 && s2 && s3
    }

    suspend fun openSideKeySettings(): ShellResult {
        return ShellExecutor.exec("am start -a com.samsung.android.intent.action.SIDE_KEY_SETTINGS")
    }

    suspend fun triggerDoublePower(): ShellResult {
        return ShellExecutor.exec("input keyevent 26 26")
    }

    suspend fun restoreCameraDoublePress(): Boolean {
        val s1 = SettingsHelper.putGlobal(KEY_ENABLE, "1")
        val s2 = SettingsHelper.putGlobal(KEY_TYPE, "1")
        val s3 = SettingsHelper.putGlobal(KEY_VALUE, CAMERA_COMPONENT)
        return s1 && s2 && s3
    }
}
