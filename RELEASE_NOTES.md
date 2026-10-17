## VoLTE Enabler v1.1.0 - Maintenance & One UI 6 Update

A standalone native Android application that enables VoLTE (Voice over LTE) on Samsung Galaxy and other Android devices **without requiring a PC, ADB cables, or root access**.

---

### 🚀 What's New in v1.1.0

- 🛠️ **Samsung One UI 6 (Android 14) APN Fix**:
  - Automatically queries the active SIM's subscription ID (`sub_id`) and attaches it to the APN settings intent. Prevents Samsung `ApnSettings` from closing immediately with `"Due to SIM absent"`.
- ⚡ **Enhanced Assistant Hijack (Method 3)**:
  - Directly triggers `input keyevent 219` (`KEYCODE_ASSIST`) for instant, 100% reliable launching of IMS Settings on One UI 6 without waiting for gestures.
- 📋 **Refined VoLTE Bypass Guidance**:
  - **Method 1 (Lockscreen Shortcut)**: Highlighted as the 100% working non-Shizuku method for Android 10–15 using standard user `WRITE_SETTINGS`.
  - **Method 2 (Side Key Double-Press)**: Clearly marked as Legacy (Android 10–12 only). Added technical notices explaining Samsung One UI 5 & 6 restrictions (`AdbCommandUtils` blacklist and `category.LAUNCHER` requirements).
  - Streamlined method numbering (Method 4 is now 1-Click IMS Package Repair; unexported direct launch removed).
- 🌐 **Separated Documentation**:
  - Dedicated, unmixed English and Arabic documentation with one-click language switcher navigation.
- 🔬 **Reverse Engineering Dossier**:
  - Included binary analysis proof and disassembly dumps of One UI 6.1 `SettingsProvider.apk` and `services.jar`.

---

### 📥 Download Assets

- **`app-release.apk`**: Signed production APK (Android 10.0+ / API 26+).

---

### 📋 Prerequisites

1. Android 10.0 to Android 14+ (Samsung Galaxy One UI 2.0 to 6.1+ recommended).
2. [Shizuku](https://github.com/RikkaApps/Shizuku/releases) installed and running (for Method 3, 4, and terminal commands).
3. Grant **VoLTE Enabler** permission in the Shizuku manager when prompted.
