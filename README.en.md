<p align="right">
  <a href="README.md">日本語</a> | <b>English</b>
</p>

# Taiko Touch Controller

Transform your Android device into a responsive Taiko drum controller!  
Play games directly on your Android device, or send inputs over USB to your PC.

<p align="center">
  <a href="https://github.com/Sango916/TaikoTouchController-Android/releases/latest"><img src="assets/badges/get-it-on-github.png" alt="Get it on GitHub" height="48"></a>
  <a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22com.sango.taikocontroller%22%2C%22url%22%3A%22https%3A//github.com/Sango916/TaikoTouchController-Android%22%2C%22author%22%3A%22Sango916%22%2C%22name%22%3A%22%E5%A4%AA%E9%BC%93%E3%82%BF%E3%83%83%E3%83%81%E3%82%B3%E3%83%B3%E3%83%88%E3%83%AD%E3%83%BC%E3%83%A9%E3%83%BC%22%7D"><img src="assets/badges/get-it-on-obtainium.png" alt="Get it on Obtainium" height="48"></a>
</p>

---

## Key Features

- **Intuitive Touch Controls & Custom Hit Feel**
  - Hit big notes with a single hand (like in Nintendo DS or Wii U titles, toggleable)
  - Haptic feedback (vibration)
  - Customizable drum size, position, and big-note hit area per screen orientation (Portrait/Landscape) with preset saving

- **Connection Modes**
  - **On this device (Shizuku)**: Runs on the same device and directly dispatches keyboard or gamepad events to game apps via Shizuku
  - **PC (USB Wired)**: Connect your device to a PC via USB and send low-latency keystrokes to PC games via ADB (Receiver scripts can be downloaded from [Releases](https://github.com/Sango916/TaikoTouchController-Android/releases/latest))
  - **Another Android Device (USB Wired)**: Pair two Android devices over a USB cable (one acts as the touch controller, the other receives inputs as the game host)
  - **None (Local Demo)**: Practice and fine-tune touch layout/feel without external output

- **Floating Overlay & Quick Settings Bubble**
  - Overlay floating controller pads and a quick settings button right on top of your rhythm game screen

---

## System Requirements

- **Supported Android OS**: Android 8.0 (API Level 26) or higher
- **Target SDK**: Android 14 (API Level 34)
- **Supported PC OS (Receiver)**: Windows, Linux, macOS (untested)

---

## Tested Environments

- **Verified Android Devices**:
  - Pixel 11 Pro XL (Android 17)
  - Pixel 7 Pro (Android 17)
  - Pixel 3a (Evolution X 10.9 Android 15)
  - Thor (Android 13)
- **Verified PC OS**:
  - Windows 11 25H2
  - Ubuntu 26.04 LTS

---

## Tech Stack & Architecture

- **Development Platform**: Google AI Studio
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Build System**: Gradle (Kotlin DSL)

### Build Commands

```bash
# Debug build (Generate APK)
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest
```

---

## License

This project is licensed under the MIT License - see the LICENSE file for details.
