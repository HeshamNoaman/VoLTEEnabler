# AGENTS.md — Guidelines for AI Coding Agents

Welcome to the **enable-volte-app** repository. This document defines the architecture, project structure, operational guidelines, and specialized agent skills to follow when developing, inspecting, or testing this project.

---

## 📱 Project Identity & Purpose

The primary active project in this repository is **VoLTEEnabler** (`/VoLTEEnabler`), a native Android application built with **Kotlin** and **Jetpack Compose (Material 3)**.

### Core Problem & Solution
- **The Problem**: On Samsung Galaxy devices running modern One UI (Android 13, 14, 15), Samsung unexported the IMS Settings activity (`com.samsung.advp.imssettings/.MainActivity`). Direct ADB `am start` fails with `SecurityException: Permission Denial: not exported`.
- **The Solution**: VoLTE Enabler connects directly to **[Shizuku](https://shizuku.rikka.app/)** on-device (`uid=2000` / `WRITE_SECURE_SETTINGS`) and system setting providers to provide non-root, PC-less bypass methods:
  1. **Lock Screen Shortcut (SystemUI Keyguard Trick - 100% Works on Android 10–15)**: Modifies `Settings.System.lock_application_shortcut` using standard `WRITE_SETTINGS` (no Shizuku or PC needed!) so SystemUI Keyguard launches IMS Settings with elevated privileges upon swipe.
  2. **Power Double-Tap (Side Key - Android 10–12 Only)**: Rebinds `function_key_config_doublepress_value`. *Note: Blocked on One UI 5 & 6 (Android 13/14) by Samsung's `AdbCommandUtils` blacklist and `KeyCustomizationInfoManager` launcher requirement.*
  3. **Digital Assistant Remap (100% Works via Shizuku)**: Sets `Settings.Secure.assistant` and triggers `input keyevent 219` (`KEYCODE_ASSIST`) or Home long-press to launch IMS Settings instantly.
  4. **IMS Package Repair**: Re-enables and installs debloated carrier IMS packages via `cmd package install-existing`.
  5. **Network Tools & Hidden Menus**: RadioInfo (`*#*#4636#*#*`), APN carrier profiles with automatic `sub_id` detection on Android 14, QS Mobile Data tile toggle, and ADB terminal console.

---

## 📂 Repository Directory Map

```text
d:\github_project\enable-volte-app\
├── VoLTEEnabler/                 # Primary native Android project
│   ├── app/                      # Main application module
│   │   ├── src/main/java/        # Kotlin Compose UI, ViewModels, Shizuku bridge
│   │   ├── src/main/res/         # Drawables, layouts, strings (AR/EN bilingual)
│   │   └── build.gradle.kts      # App-level build config (compileSdk 34, minSdk 26)
│   ├── build.gradle.kts          # Root project build config
│   ├── settings.gradle.kts       # Gradle settings
│   └── docs/                     # Technical specifications & methods reference
├── skills/                       # Installed Android agent skills (see below)
│   ├── android-cli/              # Android CLI specialist skill
│   ├── edge-to-edge/             # Adaptive edge-to-edge & insets handling
│   ├── jetpack-compose-performance/ # Compose performance & recomposition optimization
│   ├── play-policy-insights/     # Play Store compliance & permissions auditor
│   ├── r8-analyzer/              # R8 / ProGuard rules & keep-rules optimization
│   └── testing-setup/            # Testing infrastructure (Unit, Compose UI, E2E)
├── docs/                         # Repository-wide documentation, plans & screenshots
│   ├── volte_enabler_plan.md     # Architectural implementation plan
│   └── METHODS_AND_ADB_COMMANDS.md # Full reference of ADB & Shizuku commands
├── Tweaker/                      # Reference SystemUI Tuner app codebase
├── lesson_11/                    # Reference C# WinForms desktop tool (telephony commands & APNs)
└── enable VoLTE without PC required Shizuku/ # Reference APKs & tools
```

---

## 🛠️ Installed Android Skills & When to Use Them

This repository has been configured with official Android agent skills in the `/skills` directory:

| Skill | Path | When to Use |
|---|---|---|
| **`android-cli`** | `skills/android-cli` | Primary tool for building, running APKs, interacting with devices/emulators (`android layout`, `android screen`), searching Android documentation (`android docs search`), and managing the Android SDK. |
| **`edge-to-edge`** | `skills/edge-to-edge` | **Always use** when modifying or creating Compose screens. Ensures navigation bar, status bar, and IME (keyboard) padding/insets are handled cleanly without overlapping interactive components. |
| **`jetpack-compose-performance`** | `skills/jetpack-compose-performance` | Use when diagnosing recomposition overhead, lag, or optimizing UI state structures (`remember`, `derivedStateOf`, stable parameters, lazy list keys). |
| **`testing-setup`** | `skills/testing-setup` | Use when writing or configuring unit tests, Compose UI test rules, Robolectric tests, or Shizuku mock tests. |
| **`r8-analyzer`** | `skills/r8-analyzer` | **Critical for Release builds**: Shizuku uses AIDL / Binder IPC and reflection. Use this skill to audit `proguard-rules.pro` to ensure Shizuku classes and system model classes aren't stripped by R8. |
| **`play-policy-insights`** | `skills/play-policy-insights` | Use when auditing manifest permissions (`WRITE_SECURE_SETTINGS`, telephony queries) against Google Play Store policies and security disclosures. |

---

## ⚙️ Development Guidelines & Rules for Agents

### 1. Android CLI Commands
- When inspecting or interacting with connected devices:
  ```powershell
  # Check environment status
  android info

  # Inspect UI layout hierarchy as JSON
  android layout

  # Capture screen for visual confirmation
  android screen capture --output=screen.png

  # Query official Android documentation
  android docs search "<query>"
  ```

### 2. Gradle Build & Run
- Always run Gradle commands from inside the `VoLTEEnabler` directory:
  ```powershell
  cd VoLTEEnabler
  .\gradlew assembleDebug
  ```

### 3. Safety & Non-Destructive Operations
- **Always preserve original state**: When altering system settings (`lock_application_shortcut`, `function_key_config_*`, `assistant`), the app must first read and store the user's current values.
- **Provide 1-click restore**: Every method screen in the UI must have a prominent, unambiguous restore button (e.g. *Restore Camera Shortcut*, *Restore Power Double-Tap*).

### 4. Shizuku API Conventions
- Check `Shizuku.pingBinder()` and `Shizuku.checkSelfPermission()` before dispatching privileged processes.
- Bind `Shizuku.OnRequestPermissionResultListener` to handle user responses dynamically.
- Handle `DeadObjectException` and unexpected binder disconnection gracefully with in-app error feedback.
- For shell execution, use `Shizuku.newProcess` with proper thread dispatching (Dispatchers.IO) and standard error stream reading.

### 5. UI & Localization (Bilingual AR / EN)
- All user-facing strings must reside in `res/values/strings.xml` and `res/values-ar/strings.xml`.
- Maintain complete Right-to-Left (RTL) support for Arabic. Use start/end padding instead of left/right.
- Follow Material 3 color tokens (`MaterialTheme.colorScheme.*`) and adapt dynamically to system dark mode.

---

## 🔒 Security & Privacy Standard
- **No Internet Permission**: The app operates exclusively offline. Never add `android.permission.INTERNET` to `AndroidManifest.xml`.
- **Privilege Separation**: Only invoke Shizuku / UID 2000 for commands that require privileged shell permissions. Standard activities must be launched via standard Android Intents.
