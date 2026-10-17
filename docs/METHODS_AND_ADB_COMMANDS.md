# Comprehensive Guide: VoLTE Activation Methods & ADB Commands Reference

**English** | [العربية](METHODS_AND_ADB_COMMANDS_AR.md)

This technical reference document provides an in-depth explanation of all VoLTE (Voice over LTE) activation methods implemented in the **VoLTE Enabler** app, detailing the underlying Android OS mechanics, bypass tricks, raw ADB shell commands, and the corresponding Kotlin codebase implementations.

---

## 📑 Table of Contents
1. [The VoLTE Problem on Samsung One UI & Android 13/14](#1-the-volte-problem-on-samsung-one-ui--android-1314)
2. [Method 1: Lockscreen Shortcut (Recommended Bypass - No Shizuku Required)](#2-method-1-lockscreen-shortcut-recommended-bypass)
3. [Method 2: Side Key / Power Button Double-Press (Legacy Android 10-12 Only)](#3-method-2-side-key--power-button-double-press-android-1012-only)
4. [Method 3: Digital Assistant / Long-Press Home Remap (Works 100% via Shizuku)](#4-method-3-digital-assistant--long-press-home-remap-works-100-via-shizuku)
5. [Method 4: 1-Click Samsung IMS Package Repair & Enable](#5-method-4-1-click-samsung-ims-package-repair--enable)
6. [Network Tools, Secret Menus & APN Profiles](#6-network-tools-secret-menus--apn-profiles)
7. [Shizuku Integration Architecture (UID 2000)](#7-shizuku-integration-architecture-uid-2000)

---

## 1. The VoLTE Problem on Samsung One UI & Android 13/14

### Background
Voice over LTE (VoLTE) allows voice calls to be transmitted over high-speed 4G LTE/5G data networks instead of dropping down to legacy 2G/3G networks (CSFB). On Samsung devices, VoLTE depends on:
1. **Samsung IMS Stack**: System packages (`com.sec.ims`, `com.sec.imsservice`, `com.samsung.advp.imssettings`).
2. **Carrier Profile (CSC)**: Firmware-configured carrier policies. If a user inserts a SIM card from an unsupported or non-whitelisted carrier (e.g., Yemen Mobile or smaller MVNOs), Samsung disables VoLTE by default.
3. **GCF MODE (Global Certification Forum)**: An internal test flag in Samsung IMS Settings that forces the modem to ignore carrier whitelists and enable VoLTE on any compliant LTE network.

### The Security Barrier
On Samsung One UI 5 and 6 (Android 13 & 14), Samsung set `android:exported="false"` on the IMS configuration activity:
- **Target Component**: `com.samsung.advp.imssettings/.MainActivity`
- **Result**: Trying to launch it directly from an app or via ADB `am start` triggers:
  ```text
  java.lang.SecurityException: Permission Denial: starting Intent { cmp=com.samsung.advp.imssettings/.MainActivity } not exported from uid 10146
  ```

---

## 2. Method 1: Lockscreen Shortcut (Recommended Bypass)

### Why It Works (The SystemUI Bypass)
Samsung's lockscreen allows users to configure two app shortcuts on the bottom-left and bottom-right corners (by default, Phone and Camera). 
- When the user swipes a lockscreen shortcut, **Samsung's `SystemUI` process (`com.android.systemui`, running as UID 1000/system)** launches the target activity.
- Because `SystemUI` holds internal platform privileges, it can start unexported activities that third-party apps and normal ADB shells cannot directly launch.
- By injecting `com.samsung.advp.imssettings/.MainActivity` into the lockscreen shortcut setting, the user can swipe the icon on the lockscreen to bypass the security block.

### Raw ADB Commands

#### 1. Check current shortcut configuration:
```bash
settings get system lock_application_shortcut
```
*Sample output:*
```text
1;com.samsung.android.dialer/com.samsung.android.dialer.DialtactsActivity;1;com.sec.android.app.camera/com.sec.android.app.camera.Camera;
```
*Format Explanation:*
`1;<Left_App_Component>;1;<Right_App_Component>;`
- First `1`: Left shortcut enabled
- Second `1`: Right shortcut enabled

#### 2. Apply IMS Settings to Right Shortcut:
```bash
settings put system lock_application_shortcut '1;com.samsung.android.dialer/com.samsung.android.dialer.DialtactsActivity;1;com.samsung.advp.imssettings/.MainActivity;'
```

#### 3. Restore Default Camera Shortcut:
```bash
settings put system lock_application_shortcut '1;com.samsung.android.dialer/com.samsung.android.dialer.DialtactsActivity;1;com.sec.android.app.camera/com.sec.android.app.camera.Camera;'
```

### Associated Code in App
- **File**: [`LockscreenShortcutManager.kt`](file:///d:/github_project/enable-volte-app/VoLTEEnabler/app/src/main/java/com/volteenabler/app/methods/LockscreenShortcutManager.kt)
  - `getCurrentRaw(context)`: Reads `lock_application_shortcut` via `SettingsHelper.getSystem`.
  - `applyImsShortcut(context)`: Saves the user's existing shortcut to `SharedPreferences`, parses the semi-colon delimited string, updates the right slot with `com.samsung.advp.imssettings/.MainActivity`, and writes via `SettingsHelper.putSystem`.
  - `restoreCameraShortcut(context)`: Restores from saved backup, or defaults to Samsung Camera.
- **UI Screen**: [`VolteMethodsScreen.kt`](file:///d:/github_project/enable-volte-app/VoLTEEnabler/app/src/main/java/com/volteenabler/app/ui/screens/VolteMethodsScreen.kt)
- **Dialog Guidance**: [`InstructionDialog.kt`](file:///d:/github_project/enable-volte-app/VoLTEEnabler/app/src/main/java/com/volteenabler/app/ui/components/InstructionDialog.kt)

### User Operational Steps
1. Tap **"Set IMS to Lockscreen"** in the app.
2. Press the **Power** button once to lock the phone screen.
3. Wake the screen. The bottom-right lock icon is now the IMS Settings icon.
4. Swipe the IMS icon and unlock with your PIN / Pattern / Fingerprint.
5. In Samsung IMS Settings, check the box for **"GCF MODE"**.
6. In the confirmation dialog, tap **"OK"**. VoLTE registers immediately.
7. Return to the app and tap **"Restore Camera Shortcut"**.

---

## 3. Method 2: Side Key / Power Button Double-Press (Android 10–12 Only)

### Status & Why It Is Restricted on One UI 5 & 6 (Android 13/14)
On **Android 10 through 12 (One UI 2–4)**, configuring `function_key_config_doublepress_value` successfully remapped the physical Side Key double-press to launch IMS Settings.

However, reverse engineering of One UI 6.1 binaries (`SettingsProvider.apk` and `services.jar`) proved that Samsung implemented a two-tier block on modern firmwares:
1. **Database Blacklist (`SettingsProvider.apk -> AdbCommandUtils`)**:
   Samsung added `ADB_CHANGE_NOT_ALLOWED_GLOBAL_SETTINGS` which explicitly includes `function_key_config_doublepress_value`. When an ADB shell (`uid=2000`) or root (`uid=0`) command attempts to write to this key, `AdbCommandUtils.isAdbChangeAllowed` rejects it with:
   ```text
   Invalid command; no put performed
   ```
2. **Launcher Category Enforcement (`services.jar -> KeyCustomizationInfoManager`)**:
   During key dispatch, `KeyCustomizationInfoManager.getSideKeyDoubleInfoFromGlobalSetting()` filters candidate applications strictly for `android.intent.category.LAUNCHER`. Because `com.samsung.advp.imssettings/.MainActivity` is a hidden system activity with only `category.DEFAULT` (no launcher icon), it is discarded. Furthermore, it does not appear in Samsung's native Side Key app picker.

> [!NOTE]
> For the complete binary disassembly and Dalvik bytecode proof, see [`decompiled_apks/samsung_sidekey_analysis/README.md`](../../decompiled_apks/samsung_sidekey_analysis/README.md).

### Legacy ADB Commands (Android 10–12):
```bash
# 1. Disable AOSP camera gesture override:
adb shell settings put secure camera_double_tap_power_gesture_disabled 1

# 2. Set IMS Settings as double-press target:
adb shell settings put global function_key_config_doublepress_value 'com.samsung.advp.imssettings/com.samsung.advp.imssettings.MainActivity'

# 3. Enable double-press (Type 2 = Open custom app):
adb shell "settings put global function_key_config_doublepress 1; settings put global function_key_config_doublepress_type 2"

# 4. Trigger double-press:
adb shell input keyevent 26 26

# 5. Restore Camera:
adb shell "settings put secure camera_double_tap_power_gesture_disabled 0; settings put global function_key_config_doublepress_type 0; settings put global function_key_config_doublepress_value 'com.sec.android.app.camera/com.sec.android.app.camera.Camera'"
```

---

## 4. Method 3: Digital Assistant / Long-Press Home Remap (Works 100% via Shizuku)

### Why It Works
Android stores the default digital assistant in `Settings.Secure.assistant`. When configured to `com.samsung.advp.imssettings/.MainActivity`, Android's `AssistUtils` and `VoiceInteractionManagerService` (running as system `UID 1000`) launch the assistant activity directly with system privileges.

On Samsung One UI 6, triggering `KEYCODE_ASSIST` (219) or holding the Home button on the navigation bar instantly launches IMS Settings!

### Raw ADB / Shizuku Commands:

#### 1. Assign IMS Settings as Assistant:
```bash
adb shell settings put secure assistant 'com.samsung.advp.imssettings/com.samsung.advp.imssettings.MainActivity'
```

#### 2. Trigger Assist Keyevent (Launches IMS Settings Instantly):
```bash
adb shell input keyevent 219
```
*(Key code 219 corresponds to `KEYCODE_ASSIST`)*

#### 3. Restore Default Google Assistant:
```bash
adb shell settings put secure assistant 'com.google.android.googlequicksearchbox/com.google.android.apps.gsa.staticplugins.opa.omnihost.OpaRootHostActivity'
```

### Associated Code in App
- **File**: [`AssistantManager.kt`](file:///d:/github_project/enable-volte-app/VoLTEEnabler/app/src/main/java/com/volteenabler/app/methods/AssistantManager.kt)
  - `applyImsAssistant()`: Backs up current assistant and applies IMS Settings via `Settings.Secure.putString`.
  - `triggerHomeLongPress()`: Executes `input keyevent 219` (`KEYCODE_ASSIST`) for instant launch.
  - `restoreGoogleAssistant()`: Restores Google Assistant component.

---

## 5. Method 4: 1-Click Samsung IMS Package Repair & Enable

### Why It Works
Often on carrier-branded or debloated devices, carrier bloat removers or carrier switching tools disable Samsung's core IMS services (`com.sec.ims`, `com.sec.imsservice`, etc.). When disabled, VoLTE cannot register even if GCF MODE is turned on.

Using Shizuku shell (`pm enable` and `cmd package install-existing`), VoLTE Enabler re-enables and reinstalls missing packages on-device without root.

### Raw ADB Commands:

#### 1. Reinstall Uninstalled Carrier IMS Packages:
```bash
cmd package install-existing com.sec.ims
cmd package install-existing com.sec.imsservice
cmd package install-existing com.samsung.advp.imssettings
cmd package install-existing com.sec.imslogger
cmd package install-existing com.sec.epdg
```

#### 2. Enable Disabled Packages:
```bash
pm enable com.sec.ims
pm enable com.sec.imsservice
pm enable com.samsung.advp.imssettings
pm enable com.sec.imslogger
pm enable com.sec.epdg
```

### Associated Code in App
- **File**: [`ImsRepairManager.kt`](file:///d:/github_project/enable-volte-app/VoLTEEnabler/app/src/main/java/com/volteenabler/app/methods/ImsRepairManager.kt)
- **Data Model**: [`ImsPackageInfo.kt`](file:///d:/github_project/enable-volte-app/VoLTEEnabler/app/src/main/java/com/volteenabler/app/data/ImsPackageInfo.kt)

### Associated Code in App
- **File**: [`VolteMethodsScreen.kt`](file:///d:/github_project/enable-volte-app/VoLTEEnabler/app/src/main/java/com/volteenabler/app/ui/screens/VolteMethodsScreen.kt) lines 193–223.

---

## 6. Method 5: 1-Click Samsung IMS Package Repair & Enable

### Why It Works
Certain carrier ROMs or debloater scripts aggressively disable or uninstall IMS packages (`pm uninstall -k --user 0`). Without these packages running, VoLTE cannot establish SIP registration.
This method scans for package presence, checks enabled status, and uses Android Package Manager commands via Shizuku to re-enable and reinstall them.

### Target Core Packages
| Package Name | Description |
|:---|:---|
| `com.samsung.advp.imssettings` | Samsung IMS Configuration User Interface |
| `com.sec.imsservice` | Core Samsung IMS Daemon Service |
| `com.sec.ims` | Samsung IMS Framework Provider |
| `com.sec.imslogger` | IMS Diagnostic & Registration Logger |
| `com.sec.epdg` | ePDG Wi-Fi Calling (VoWiFi) Handover Service |

### Raw ADB Commands

#### 1. Scan / Check Package Status:
```bash
pm list packages -u | grep com.samsung.advp.imssettings
pm list packages -d | grep com.sec.imsservice
```

#### 2. Re-enable Packages:
```bash
pm enable com.sec.ims
pm enable com.sec.imsservice
pm enable com.samsung.advp.imssettings
pm enable com.sec.imslogger
pm enable com.sec.epdg
```

#### 3. Reinstall if Uninstalled for User 0:
```bash
cmd package install-existing com.sec.ims
cmd package install-existing com.sec.imsservice
cmd package install-existing com.samsung.advp.imssettings
cmd package install-existing com.sec.imslogger
cmd package install-existing com.sec.epdg
```

### Associated Code in App
- **File**: [`ImsRepairManager.kt`](file:///d:/github_project/enable-volte-app/VoLTEEnabler/app/src/main/java/com/volteenabler/app/methods/ImsRepairManager.kt)
  - `scanPackages()`: Loops through required packages and checks `pm list packages`.
  - `repairAll()`: Runs `pm enable` and `cmd package install-existing` on all target packages in parallel.
- **Model**: [`ImsPackageInfo.kt`](file:///d:/github_project/enable-volte-app/VoLTEEnabler/app/src/main/java/com/volteenabler/app/data/ImsPackageInfo.kt)

---

## 7. Network Tools, Secret Menus & APN Profiles

### 1. RadioInfo (Testing Secret Code `*#*#4636#*#*`)
Opens the Android telephony engineering screen to toggle *VoLTE Provisioned*, *Wi-Fi Calling Provisioned*, and view real-time LTE signal metrics (RSRP, RSRQ, SINR).
```bash
am start -n com.android.settings/.RadioInfo
# Fallback for some Samsung devices:
am start -n com.android.phone/.settings.RadioInfo
```

### 2. Access Point Names (APN) Shortcut
Directly launches the system APN configuration screen:
```bash
# Standard Android 10–12 launch:
am start -a android.settings.APN_SETTINGS

# Critical Android 14 / One UI 6 Fix:
# On Android 14, Samsung ApnSettings closes immediately with "Due to SIM absent" if sub_id is omitted.
# It must be launched with the active SIM's subscription ID:
am start -a android.settings.APN_SETTINGS --ei sub_id <ACTIVE_SUB_ID>
```
*In Kotlin code ([`NetworkToolsScreen.kt`](file:///d:/github_project/enable-volte-app/VoLTEEnabler/app/src/main/java/com/volteenabler/app/ui/screens/NetworkToolsScreen.kt)), the app queries `SubscriptionManager.getDefaultDataSubscriptionId()` and attaches both `sub_id` and `android.provider.extra.SUB_ID` with `FLAG_ACTIVITY_NEW_TASK`.*

### 3. Mobile Data Quick Settings Tile Restorer
Some carrier CSC profiles hide the Mobile Data toggle from the Quick Settings notification shade. This command appends `MobileData` to `sysui_qs_tiles`:
```bash
# Read existing tiles:
settings get secure sysui_qs_tiles

# Append MobileData and Hotspot:
settings put secure sysui_qs_tiles '<existing_tiles>,MobileData,Hotspot'
```

### 4. VoLTE APN Profiles (Yemen Mobile & Global)
To achieve VoLTE calling, the device must connect to the carrier's dedicated IMS PDN (Packet Data Network).

#### Yemen Mobile 4G Data APN:
- **Name**: `ymdata`
- **APN**: `ymdata`
- **Username**: `ymdata`
- **Password**: `ymdata`
- **APN Type**: `default,mms,supl`

#### Yemen Mobile VoLTE (IMS) APN:
- **Name**: `ims`
- **APN**: `ims`
- **Username**: `ims`
- **Password**: `ims`
- **APN Type**: `ims,default`

#### Yemen Mobile VoLTE (XCAP) APN (Supplementary Services):
- **Name**: `xcap`
- **APN**: `xcap`
- **Username**: `xcap`
- **Password**: `xcap`
- **APN Type**: `xcap`

### 5. Keep Screen On While Plugged In (Useful during Setup)
```bash
# Keep screen on while charging:
adb shell settings put global stay_on_while_plugged_in 1

# Disable / restore normal sleep timeout:
adb shell settings put global stay_on_while_plugged_in 0
```

### 6. Device Diagnostics & Troubleshooting Commands
From internal engineering and deployment workflows:

#### Query Device IMEI via Telephony Service:
```bash
adb shell "service call iphonesubinfo 1 s16 com.android.shell | cut -c 52-66 | tr -d '.[:space:]'"
```

#### Check Current Screen Power State:
```bash
adb shell "dumpsys display | grep mScreenState"
```

#### Inspect Foreground App, Active Window, & Focus:
```bash
adb shell "dumpsys window | grep -E 'mFocusedApp|mCurrentFocus|top'"
```

#### List All System Packages (including uninstalled/disabled for user 0):
```bash
adb shell pm list packages -u -f
```

### 7. Hardware Reboot Commands
```bash
reboot               # Normal Reboot
reboot recovery      # Recovery Mode
reboot download      # Samsung Odin / Download Mode
reboot bootloader    # Fastboot / Bootloader
```

---

## 8. Shizuku Integration Architecture (UID 2000)

### Why Shizuku?
Normally, modifying `Settings.System` and `Settings.Secure` or executing `pm enable` requires root (`su`) or an active USB debugging cable connected to a computer (`adb shell`).
**Shizuku** acts as an on-device daemon running under Android's ADB shell identity (`UID 2000`). It provides an IPC Binder service that client apps can communicate with to execute privileged commands without root.

### The Reflection Mechanism (`getDeclaredMethod`)
In the Shizuku API library (`dev.rikka.shizuku:api`), `Shizuku.newProcess` is declared as `private static` in `rikka.shizuku.Shizuku`:
```java
// Inside rikka.shizuku.Shizuku:
private static ShizukuRemoteProcess newProcess(String[] cmd, String[] env, String dir) {
    IRemoteProcess remoteProcess = requireService().newProcess(cmd, env, dir);
    return new ShizukuRemoteProcess(remoteProcess);
}
```

To invoke this safely from Kotlin without compilation visibility errors, our app uses Java reflection with `getDeclaredMethod`:

```kotlin
// Inside ShellExecutor.kt:
val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
    "newProcess",
    Array<String>::class.java,
    Array<String>::class.java,
    String::class.java
)
newProcessMethod.isAccessible = true

val process = newProcessMethod.invoke(
    null,
    arrayOf("sh", "-c", command) as Any,
    null,
    null
) as Process
```

### Execution Pipeline
```
[Compose UI Action]
        │
        ▼
[Manager Class (e.g. LockscreenShortcutManager)]
        │
        ▼
[SettingsHelper (settings put system/secure/global)]
        │
        ▼
[ShellExecutor.exec(command)]
        │
        ├── Shizuku Authorized? ───► [Shizuku Binder IPC (UID 2000)] ──► [Android Shell Daemon]
        │                                                                       │
        └── Fallback (Local)    ───► [Runtime.getRuntime().exec()]               ▼
                                                                        [Settings Provider Updated]
```

This ensures the user can complete the entire VoLTE enablement and modem bypass on their phone in seconds without touching a PC.
