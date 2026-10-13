package com.volteenabler.app.methods

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.volteenabler.app.shizuku.SettingsHelper
import com.volteenabler.app.shizuku.ShellExecutor
import com.volteenabler.app.shizuku.ShellResult

object SideKeyManager {
    private const val TAG = "SideKeyManager"

    // Samsung Function Key settings keys
    private const val KEY_ENABLE = "function_key_config_doublepress"
    private const val KEY_TYPE = "function_key_config_doublepress_type"
    private const val KEY_VALUE = "function_key_config_doublepress_value"

    // AOSP / Samsung Camera Quick Launch gesture override
    // When 0, AOSP camera gesture intercepts double-tap power before Samsung function key!
    private const val KEY_CAMERA_DOUBLE_TAP_DISABLED = "camera_double_tap_power_gesture_disabled"

    const val IMS_COMPONENT = "com.samsung.advp.imssettings/com.samsung.advp.imssettings.MainActivity"
    const val CAMERA_COMPONENT = "com.sec.android.app.camera/com.sec.android.app.camera.Camera"

    /**
     * Applies IMS Settings as the double-press power target.
     * Sets:
     * - secure camera_double_tap_power_gesture_disabled = 1 (prevents camera gesture hijack)
     * - global & system function_key_config_doublepress = 1
     * - global & system function_key_config_doublepress_type = 2 (custom app)
     * - global & system function_key_config_doublepress_value = IMS_COMPONENT
     * - Also updates Samsung SemWindowManager KeyCustomizationInfo directly via reflection for One UI 5/6.
     */
    suspend fun applyImsDoublePress(): Boolean {
        // 1. Disable the AOSP camera gesture so it doesn't intercept double-press
        val s0 = SettingsHelper.putSecure(KEY_CAMERA_DOUBLE_TAP_DISABLED, "1")

        // 2. Set global Samsung function key configuration
        val s1 = SettingsHelper.putGlobal(KEY_ENABLE, "1")
        val s2 = SettingsHelper.putGlobal(KEY_TYPE, "2")
        val s3 = SettingsHelper.putGlobal(KEY_VALUE, IMS_COMPONENT)

        // 3. Mirror into system table for One UI framework compatibility
        SettingsHelper.putSystem(KEY_TYPE, "2")
        SettingsHelper.putSystem(KEY_VALUE, IMS_COMPONENT)

        // 4. Try updating Samsung SemWindowManager in-memory cache directly
        tryRegisterSemKeyCustomization(IMS_COMPONENT)

        return s0 && s1 && s2 && s3
    }

    /**
     * Simulates double-pressing the power button (KEYCODE_POWER = 26) twice.
     * adb shell input keyevent 26 26
     */
    suspend fun triggerDoublePower(): ShellResult {
        return ShellExecutor.exec("input keyevent 26 26")
    }

    /**
     * Opens Samsung's native Side Key settings screen.
     */
    suspend fun openSideKeySettings(): ShellResult {
        return ShellExecutor.exec("am start -a com.samsung.android.intent.action.SIDE_KEY_SETTINGS")
    }

    /**
     * Restores default camera behavior for double-press power.
     */
    suspend fun restoreCameraDoublePress(): Boolean {
        // 1. Re-enable camera gesture in secure table
        val s0 = SettingsHelper.putSecure(KEY_CAMERA_DOUBLE_TAP_DISABLED, "0")

        // 2. Reset global table to camera (Type 0 = Quick launch camera)
        val s1 = SettingsHelper.putGlobal(KEY_ENABLE, "1")
        val s2 = SettingsHelper.putGlobal(KEY_TYPE, "0")
        val s3 = SettingsHelper.putGlobal(KEY_VALUE, CAMERA_COMPONENT)

        // 3. Mirror into system table
        SettingsHelper.putSystem(KEY_TYPE, "0")
        SettingsHelper.putSystem(KEY_VALUE, CAMERA_COMPONENT)

        // 4. Remove Samsung SemWindowManager custom key mapping
        tryRemoveSemKeyCustomization()

        return s0 && s1 && s2 && s3
    }

    /**
     * Directly interacts with Samsung's hidden SemWindowManager to register KeyCustomizationInfo.
     * On Android 14 / One UI 6, PhoneWindowManagerExt uses an in-memory KeyCustomizationManager.
     */
    private fun tryRegisterSemKeyCustomization(componentName: String): Boolean {
        return try {
            val semWmClass = Class.forName("com.samsung.android.view.SemWindowManager")
            val getInstanceMethod = semWmClass.getMethod("getInstance")
            val semWm = getInstanceMethod.invoke(null) ?: return false

            val builderClass = Class.forName("com.samsung.android.view.SemWindowManager\$KeyCustomizationInfo\$Builder")
            val targetIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = ComponentName.unflattenFromString(componentName)
                flags = 0x10200000 // FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            }

            // Builder(int press, int id, int keyCode, Intent intent, String ownerPackage)
            // press: 2 (KEY_PRESS_DOUBLE), id: 1104, keyCode: 26, intent, owner: "com.android.settings"
            val constructor = builderClass.getConstructor(
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Intent::class.java,
                String::class.java
            )
            val builder = constructor.newInstance(2, 1104, 26, targetIntent, "com.android.settings")
            val buildMethod = builderClass.getMethod("build")
            val keyInfo = buildMethod.invoke(builder)

            val infoClass = Class.forName("com.samsung.android.view.SemWindowManager\$KeyCustomizationInfo")
            val putMethod = semWmClass.getMethod("putKeyCustomizationInfo", infoClass)
            putMethod.invoke(semWm, keyInfo)
            Log.d(TAG, "Successfully registered SemWindowManager KeyCustomizationInfo for $componentName")
            true
        } catch (t: Throwable) {
            Log.w(TAG, "SemWindowManager reflection registration skipped/failed: ${t.message}")
            false
        }
    }

    private fun tryRemoveSemKeyCustomization(): Boolean {
        return try {
            val semWmClass = Class.forName("com.samsung.android.view.SemWindowManager")
            val getInstanceMethod = semWmClass.getMethod("getInstance")
            val semWm = getInstanceMethod.invoke(null) ?: return false

            // removeKeyCustomizationInfo(String ownerPackage, int press, int keyCode)
            val removeMethod = semWmClass.getMethod(
                "removeKeyCustomizationInfo",
                String::class.java,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType
            )
            removeMethod.invoke(semWm, "com.android.settings", 2, 26)
            Log.d(TAG, "Successfully removed SemWindowManager KeyCustomizationInfo")
            true
        } catch (t: Throwable) {
            Log.w(TAG, "SemWindowManager reflection removal skipped/failed: ${t.message}")
            false
        }
    }
}
