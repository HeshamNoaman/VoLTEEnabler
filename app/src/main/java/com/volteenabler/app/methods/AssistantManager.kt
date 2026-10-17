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
        // KEYCODE_ASSIST (219) immediately triggers the configured assistant on Samsung One UI
        ShellExecutor.exec("input keyevent 219")
    }

    suspend fun restoreGoogleAssistant(): Boolean {
        val s1 = SettingsHelper.putSecure(KEY_ASSISTANT, GOOGLE_ASSISTANT)
        val s2 = SettingsHelper.putSecure(KEY_CAMERA_GESTURE, "0")
        return s1 && s2
    }

    /**
     * Opens Android's native Voice Input & Assistant settings screen.
     * Works without Shizuku via standard Android Intent.
     */
    fun openAssistantSettings(context: android.content.Context): Boolean {
        val intents = listOf(
            android.content.Intent(android.provider.Settings.ACTION_VOICE_INPUT_SETTINGS),
            android.content.Intent("android.settings.VOICE_INPUT_SETTINGS"),
            android.content.Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
            android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
        )
        for (intent in intents) {
            try {
                intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                return true
            } catch (_: Exception) {
                // Continue to next fallback
            }
        }
        return false
    }
}
