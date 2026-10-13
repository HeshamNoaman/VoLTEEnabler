package com.volteenabler.app.methods

import com.volteenabler.app.shizuku.SettingsHelper
import com.volteenabler.app.shizuku.ShellExecutor

object AssistantManager {
    private const val KEY_ASSISTANT = "assistant"
    private const val KEY_CAMERA_GESTURE = "camera_double_tap_power_gesture_disabled"

    const val IMS_ASSISTANT = "com.samsung.advp.imssettings/com.samsung.advp.imssettings.MainActivity"
    const val GOOGLE_ASSISTANT = "com.google.android.googlequicksearchbox/com.google.android.voiceinteraction.GsaVoiceInteractionService"

    suspend fun applyImsAssistant(): Boolean {
        val s1 = SettingsHelper.putSecure(KEY_ASSISTANT, IMS_ASSISTANT)
        val s2 = SettingsHelper.putSecure(KEY_CAMERA_GESTURE, "1")
        return s1 && s2
    }

    suspend fun triggerHomeLongPress() {
        ShellExecutor.exec("input keyevent KEYCODE_HOME; sleep 0.2; input keyevent --longpress KEYCODE_HOME")
    }

    suspend fun restoreGoogleAssistant(): Boolean {
        val s1 = SettingsHelper.putSecure(KEY_ASSISTANT, GOOGLE_ASSISTANT)
        val s2 = SettingsHelper.putSecure(KEY_CAMERA_GESTURE, "0")
        return s1 && s2
    }
}
