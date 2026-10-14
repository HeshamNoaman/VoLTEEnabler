## VoLTE Enabler v1.0.0 - Initial Production Release

A standalone native Android application that connects directly to **[Shizuku](https://shizuku.rikka.app/)** to enable VoLTE (Voice over LTE) on Samsung Galaxy and other Android devices **without requiring a PC, ADB cables, or root access**.

---

### ✨ Features & Capabilities

- 🔓 **Rootless Samsung One UI Bypass (Android 13 & 14)**:
  - **Method 1 (Lockscreen Shortcut)**: Temporarily remap bottom-right lockscreen shortcut to launch unexported `com.samsung.advp.imssettings/.MainActivity` with system privileges. Includes 1-tap restore.
  - **Method 2 (Side Key Double-Press)**: Rebinds the Samsung power button double-press action to open IMS Settings directly.
  - **Method 3 (Digital Assistant Remap)**: Rebinds long-press Home/Assistant action to trigger IMS Settings.
  - **Method 4 (Direct Activity Launch)**: For Android 10–12 devices where activity is exported, plus 1-tap Samsung Hidden Band Selection.
  - **Method 5 (1-Click IMS Package Repair)**: Automatically detects, enables, and reinstalls disabled/hidden Samsung IMS packages (`com.sec.ims`, `com.sec.imsservice`, `com.samsung.advp.imssettings`, etc.).

- 🛠️ **Network Utilities & Secret Menus**:
  - Direct launcher for hidden **RadioInfo (`*#*#4636#*#*`)** to toggle VoLTE Provisioned, Wi-Fi Calling, and Preferred Network Type.
  - One-tap access to APN settings and Mobile Data Quick Settings tile restore.
  - MediaTek EngineerMode, MTK Band Selection, and LG Network shortcuts.
  - Power Reboot menu (Normal, Recovery, Download / Odin, Bootloader).

- 📋 **Carrier APN Presets**:
  - One-tap copy profiles for Yemen Mobile 4G Data, VoLTE, XCAP, and generic Global VoLTE APNs.

- 💻 **Interactive Privileged ADB Terminal**:
  - Run arbitrary ADB shell commands directly on your phone with live output and diagnostic presets.

- 🌍 **Bilingual Support**:
  - Full support for English and Arabic with complete Right-to-Left (RTL) layout.

---

### 📥 Which APK Should I Download?

- **`app-release.apk`** *(Recommended)*: Signed production release APK with optimized package size (~10 MB).
- **`app-debug.apk`**: Debug build for development and detailed log inspection.

---

### 📋 Prerequisites

1. Android 10.0 to Android 14+ (Samsung Galaxy One UI 2.0 to 6.1+ recommended).
2. [Shizuku](https://github.com/RikkaApps/Shizuku/releases) installed and running (via Wireless Debugging or PC ADB).
3. Grant **VoLTE Enabler** permission in the Shizuku manager when prompted.
