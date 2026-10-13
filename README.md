# VoLTE Enabler (مفعل VoLTE)

A modern Android application built with **Jetpack Compose** and **Material 3** that connects directly to **Shizuku** on-device to execute privileged ADB commands and system configurations without requiring a PC or root access.

## Features

- **Shizuku Privileged Bridge**: Direct binder link to execute privileged shell commands (UID 2000) and manage `WRITE_SECURE_SETTINGS` without root.
- **Multiple VoLTE Enablement Methods**:
  - **Lock Screen Shortcut**: Configures Samsung lockscreen right shortcut to IMS Settings (`com.samsung.advp.imssettings/.MainActivity`) to bypass One UI security restrictions.
  - **Side Key / Double-Tap**: Remaps power button double-press to launch IMS Settings directly.
  - **Assistant Button**: Maps device assistant gesture to IMS Settings.
  - **Direct Launch**: Launches IMS Settings and Band Selection directly (supported on Android 10–12).
  - **IMS Package Repair**: One-tap repair and enable for core Samsung IMS packages (`com.sec.ims`, `com.sec.imsservice`, etc.).
- **Network Settings & Tools**:
  - Radio Info (`*#*#4636#*#*`)
  - Samsung Hidden Network Band Selection
  - APN Editor & Preset Profiles
  - Mobile Data QS Tile Toggle
  - Reboot to Recovery / Download mode
- **Privileged ADB Terminal**: Built-in interactive console with preloaded command snippets.
- **Bilingual Interface**: Full English and Arabic localization with Material 3 dynamic theming.

## Requirements

- Android 10.0+ (API level 29 or higher)
- [Shizuku](https://shizuku.rikka.app/) installed and running on the target device

## Building from Source

```bash
./gradlew assembleDebug
```

The output APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.
