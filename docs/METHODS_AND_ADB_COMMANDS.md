# Comprehensive Guide: VoLTE Activation Methods & ADB Commands Reference
## دليل شامل لطرق تفعيل ميزة VoLTE وأوامر ADB البرمجية المصاحبة

This technical reference document provides an in-depth explanation of all VoLTE (Voice over LTE) activation methods implemented in the **VoLTE Enabler** app, detailing the underlying Android OS mechanics, bypass tricks, raw ADB shell commands, and the corresponding Kotlin codebase implementations.

---

## 📑 Table of Contents
1. [The VoLTE Problem on Samsung One UI & Android 13/14](#1-the-volte-problem-on-samsung-one-ui--android-1314)
2. [Method 1: Lockscreen Shortcut (Recommended Bypass)](#2-method-1-lockscreen-shortcut-recommended-bypass)
3. [Method 2: Side Key / Power Button Double-Press](#3-method-2-side-key--power-button-double-press)
4. [Method 3: Digital Assistant / Long-Press Home Remap](#4-method-3-digital-assistant--long-press-home-remap)
5. [Method 4: Direct Launch & Samsung Band Selection](#5-method-4-direct-launch--samsung-band-selection)
6. [Method 5: 1-Click Samsung IMS Package Repair & Enable](#6-method-5-1-click-samsung-ims-package-repair--enable)
7. [Network Tools, Secret Menus & APN Profiles](#7-network-tools-secret-menus--apn-profiles)
8. [Shizuku Integration Architecture (UID 2000)](#8-shizuku-integration-architecture-uid-2000)

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

## 3. Method 2: Side Key / Power Button Double-Press

### Why It Works
Samsung One UI includes built-in settings allowing users to customize the behavior of double-pressing the physical Power / Side key.
By configuring the double-press value to point to `com.samsung.advp.imssettings`, double-pressing the power button triggers the system framework key handler to launch IMS Settings.

### Raw ADB Commands

#### 1. Set double-press type to custom app (Type 1):
```bash
settings put system function_key_config_doublepress_type 1
```

#### 2. Assign IMS Settings as the double-press target:
```bash
settings put system function_key_config_doublepress_value 'com.samsung.advp.imssettings/com.samsung.advp.imssettings.MainActivity'
```

#### 3. Open Side Key Settings (Optional verification UI):
```bash
am start -a android.intent.action.MAIN -n com.android.settings/.Settings\$SideKeySettingsActivity
```

#### 4. Restore Default Camera Double-Press (Type 0):
```bash
settings put system function_key_config_doublepress_type 0
settings put system function_key_config_doublepress_value 'com.sec.android.app.camera/com.sec.android.app.camera.Camera'
```

### Associated Code in App
- **File**: [`SideKeyManager.kt`](file:///d:/github_project/enable-volte-app/VoLTEEnabler/app/src/main/java/com/volteenabler/app/methods/SideKeyManager.kt)
  - `applyImsDoublePress()`: Sets type `1` and value `com.samsung.advp.imssettings/...`.
  - `restoreCameraDoublePress()`: Restores type `0` and camera component.
  - `openSideKeySettings()`: Opens Samsung's Side Key settings page directly.

---

## 4. Method 3: Digital Assistant / Long-Press Home Remap

### Why It Works
Android stores the component assigned to handle the assistant interaction (triggered by long-pressing Home or swiping from the bottom corner) in `Settings.Secure.assistant`. 
Setting this to IMS Settings allows triggering the menu using the assistant shortcut or sending the system assist keyevent (`KEYCODE_ASSIST` = 219).

### Raw ADB Commands

#### 1. Read current assistant:
```bash
settings get secure assistant
```

#### 2. Set IMS Settings as Assistant:
```bash
settings put secure assistant 'com.samsung.advp.imssettings/com.samsung.advp.imssettings.MainActivity'
```

#### 3. Trigger Assistant Programmatically:
```bash
input keyevent 219
```

#### 4. Restore Default Google Assistant:
```bash
settings put secure assistant 'com.google.android.googlequicksearchbox/com.google.android.apps.gsa.staticplugins.opa.OpaAssistInteractionService'
```

### Associated Code in App
- **File**: [`AssistantManager.kt`](file:///d:/github_project/enable-volte-app/VoLTEEnabler/app/src/main/java/com/volteenabler/app/methods/AssistantManager.kt)
  - `applyImsAssistant()`: Backs up current assistant and applies IMS settings.
  - `triggerHomeLongPress()`: Executes `input keyevent 219`.
  - `restoreGoogleAssistant()`: Restores Google Assistant.

---

## 5. Method 4: Direct Launch & Samsung Band Selection

### Why It Works
- On **Android 10 through Android 12** (and some pre-One UI 5 ROMs), `com.samsung.advp.imssettings/.MainActivity` was exported or permitted direct invocation by shell users.
- Samsung Telephony UI also exposes a hidden band selection activity (`com.samsung.android.app.telephonyui/.hiddennetworksetting.MainActivity`) that allows users to lock specific LTE/5G bands (Band 3, 20, 28, 41, etc.).

### Raw ADB Commands

#### 1. Direct Launch IMS Settings:
```bash
am start -n com.samsung.advp.imssettings/.MainActivity
```

#### 2. Launch Samsung Hidden Network Band Selection:
```bash
am start -n com.samsung.android.app.telephonyui/.hiddennetworksetting.MainActivity
```

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
am start -a android.settings.APN_SETTINGS
```

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

### 5. Hardware Reboot Commands
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
