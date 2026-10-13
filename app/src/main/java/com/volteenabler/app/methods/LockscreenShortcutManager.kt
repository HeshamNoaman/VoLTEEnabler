package com.volteenabler.app.methods

import android.content.Context
import com.volteenabler.app.shizuku.SettingsHelper

object LockscreenShortcutManager {
    private const val KEY_SHORTCUT = "lock_application_shortcut"
    private const val PREFS_NAME = "volte_prefs"
    private const val PREF_BACKUP_SHORTCUT = "backup_lock_shortcut"

    const val IMS_SETTINGS_ACTIVITY = "com.samsung.advp.imssettings/.MainActivity"
    const val DEFAULT_CAMERA = "com.sec.android.app.camera/com.sec.android.app.camera.Camera"
    const val DEFAULT_DIALER = "com.samsung.android.dialer/com.samsung.android.dialer.DialtactsActivity"

    data class ParsedShortcuts(
        val left: String,
        val right: String
    ) {
        fun toSettingsString(): String = "1;$left;1;$right;"

        companion object {
            fun parse(raw: String): ParsedShortcuts {
                if (raw.isBlank() || !raw.contains(";")) {
                    return ParsedShortcuts(DEFAULT_DIALER, DEFAULT_CAMERA)
                }
                val parts = raw.split(";")
                val left = parts.getOrNull(1)?.ifBlank { DEFAULT_DIALER } ?: DEFAULT_DIALER
                val right = parts.getOrNull(3)?.ifBlank { DEFAULT_CAMERA } ?: DEFAULT_CAMERA
                return ParsedShortcuts(left, right)
            }
        }
    }

    suspend fun getCurrentRaw(context: Context): String {
        val current = SettingsHelper.getSystem(KEY_SHORTCUT)
        return current.ifBlank {
            "1;$DEFAULT_DIALER;1;$DEFAULT_CAMERA;"
        }
    }

    suspend fun applyImsShortcut(context: Context): Boolean {
        val currentRaw = SettingsHelper.getSystem(KEY_SHORTCUT)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Save backup if not already saved
        if (currentRaw.isNotBlank() && !currentRaw.contains(IMS_SETTINGS_ACTIVITY)) {
            prefs.edit().putString(PREF_BACKUP_SHORTCUT, currentRaw).apply()
        }

        val parsed = ParsedShortcuts.parse(currentRaw)
        val updated = parsed.copy(right = IMS_SETTINGS_ACTIVITY)
        return SettingsHelper.putSystem(KEY_SHORTCUT, updated.toSettingsString())
    }

    suspend fun restoreCameraShortcut(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val backup = prefs.getString(PREF_BACKUP_SHORTCUT, null)

        val target = if (!backup.isNullOrBlank() && !backup.contains(IMS_SETTINGS_ACTIVITY)) {
            backup
        } else {
            val currentRaw = SettingsHelper.getSystem(KEY_SHORTCUT)
            val parsed = ParsedShortcuts.parse(currentRaw)
            parsed.copy(right = DEFAULT_CAMERA).toSettingsString()
        }

        val success = SettingsHelper.putSystem(KEY_SHORTCUT, target)
        if (success) {
            prefs.edit().remove(PREF_BACKUP_SHORTCUT).apply()
        }
        return success
    }
}
