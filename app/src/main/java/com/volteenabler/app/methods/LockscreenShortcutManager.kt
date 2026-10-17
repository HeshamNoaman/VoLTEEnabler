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

    data class FormattedShortcuts(
        val leftLabel: String,
        val leftComponent: String,
        val rightLabel: String,
        val rightComponent: String,
        val isRightIms: Boolean
    )

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

    fun getFormattedShortcuts(context: Context, raw: String): FormattedShortcuts {
        val parsed = ParsedShortcuts.parse(raw)
        val leftLabel = resolveLabel(context, parsed.left)
        val rightLabel = resolveLabel(context, parsed.right)
        val isRightIms = parsed.right.contains("imssettings", ignoreCase = true)
        return FormattedShortcuts(
            leftLabel = leftLabel,
            leftComponent = parsed.left,
            rightLabel = rightLabel,
            rightComponent = parsed.right,
            isRightIms = isRightIms
        )
    }

    private fun resolveLabel(context: Context, componentStr: String): String {
        if (componentStr.contains("imssettings", ignoreCase = true)) {
            return context.getString(com.volteenabler.app.R.string.shortcut_ims)
        }
        if (componentStr.contains("camera", ignoreCase = true)) {
            return context.getString(com.volteenabler.app.R.string.shortcut_camera)
        }
        if (componentStr.contains("dialer", ignoreCase = true) || componentStr.contains("phone", ignoreCase = true)) {
            return context.getString(com.volteenabler.app.R.string.shortcut_phone)
        }
        val cn = android.content.ComponentName.unflattenFromString(componentStr)
        if (cn != null) {
            try {
                val ai = context.packageManager.getActivityInfo(cn, 0)
                val label = ai.loadLabel(context.packageManager).toString()
                if (label.isNotBlank()) return label
            } catch (_: Exception) {
                try {
                    val appInfo = context.packageManager.getApplicationInfo(cn.packageName, 0)
                    val label = appInfo.loadLabel(context.packageManager).toString()
                    if (label.isNotBlank()) return label
                } catch (_: Exception) {
                    return cn.shortClassName.removePrefix(".")
                }
            }
        }
        return componentStr
    }

    fun canWriteSettings(context: Context): Boolean {
        return android.provider.Settings.System.canWrite(context)
    }

    fun requestWriteSettingsPermission(context: Context) {
        val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            data = android.net.Uri.parse("package:${context.packageName}")
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    suspend fun getCurrentRaw(context: Context): String {
        val nativeVal = try {
            android.provider.Settings.System.getString(context.contentResolver, KEY_SHORTCUT)
        } catch (_: Exception) { null }

        if (!nativeVal.isNullOrBlank()) {
            return nativeVal
        }

        val current = SettingsHelper.getSystem(KEY_SHORTCUT)
        return current.ifBlank {
            "1;$DEFAULT_DIALER;1;$DEFAULT_CAMERA;"
        }
    }

    suspend fun applyImsShortcut(context: Context): Boolean {
        val currentRaw = getCurrentRaw(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Save backup if not already saved
        if (currentRaw.isNotBlank() && !currentRaw.contains(IMS_SETTINGS_ACTIVITY)) {
            prefs.edit().putString(PREF_BACKUP_SHORTCUT, currentRaw).apply()
        }

        val parsed = ParsedShortcuts.parse(currentRaw)
        val updated = parsed.copy(right = IMS_SETTINGS_ACTIVITY)
        val target = updated.toSettingsString()

        // 1. Try native write if app has WRITE_SETTINGS permission
        if (canWriteSettings(context)) {
            try {
                val ok = android.provider.Settings.System.putString(
                    context.contentResolver,
                    KEY_SHORTCUT,
                    target
                )
                if (ok) return true
            } catch (e: Exception) {
                android.util.Log.w("LockscreenShortcut", "Native putString failed: ${e.message}")
            }
        }

        // 2. Fall back to Shizuku Shell
        return SettingsHelper.putSystem(KEY_SHORTCUT, target)
    }

    suspend fun restoreCameraShortcut(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val backup = prefs.getString(PREF_BACKUP_SHORTCUT, null)

        val target = if (!backup.isNullOrBlank() && !backup.contains(IMS_SETTINGS_ACTIVITY)) {
            backup
        } else {
            val currentRaw = getCurrentRaw(context)
            val parsed = ParsedShortcuts.parse(currentRaw)
            parsed.copy(right = DEFAULT_CAMERA).toSettingsString()
        }

        // 1. Try native write if permitted
        if (canWriteSettings(context)) {
            try {
                val ok = android.provider.Settings.System.putString(
                    context.contentResolver,
                    KEY_SHORTCUT,
                    target
                )
                if (ok) {
                    prefs.edit().remove(PREF_BACKUP_SHORTCUT).apply()
                    return true
                }
            } catch (_: Exception) {}
        }

        // 2. Fall back to Shizuku Shell
        val success = SettingsHelper.putSystem(KEY_SHORTCUT, target)
        if (success) {
            prefs.edit().remove(PREF_BACKUP_SHORTCUT).apply()
        }
        return success
    }
}
