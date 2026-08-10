package com.example

import kotlinx.serialization.Serializable

@Serializable
data class KeyConfig(
    var leftKat: String = "D",
    var leftDon: String = "F",
    var rightDon: String = "J",
    var rightKat: String = "K"
)

@Serializable
data class DrumPreset(
    var sizePercent: Int = 100,
    var verticalPositionPercent: Int = 50
)

@Serializable
data class GamepadKeyConfig(
    var leftKat: String = "L1",
    var leftDon: String = "DPAD_LEFT",
    var rightDon: String = "B",
    var rightKat: String = "R1"
)

@Serializable
data class ControllerSettings(
    var singleHandBigNotes: Boolean = true,
    var soundEffects: Boolean = false,
    var vibration: Boolean = true,
    var vibrationStrengthPercent: Int = 100,
    var soundVolume: Float = 0.7f,
    var keyConfig: KeyConfig = KeyConfig(),
    var gamepadKeyConfig: GamepadKeyConfig = GamepadKeyConfig(),
    var connectionMode: String = "shizuku", // "shizuku" (この端末), "usb-wired" (PC), "another_android" (別のAndroid), "local-demo" (なし)
    var anotherAndroidRole: String = "sender", // "sender" (送信側: 太鼓) / "receiver" (受信側: ゲーム)
    var anotherAndroidConnectionType: String = "wired", // "wired" (有線 USB通信) / "wireless" (無線 Wi-Fi/ネットワーク)
    var anotherAndroidTargetIp: String = "192.168.1.100",
    var anotherAndroidPort: String = "60001",
    var webAdbHost: String = "127.0.0.1",
    var webAdbPort: String = "5555",
    var webAdbPairingPort: String = "",
    var webAdbPairingCode: String = "",
    var roomId: String = "7530",
    var isTurboEnabled: Boolean = false,
    var turboIntervalMs: Int = 30, // 15ms to 100ms
    var minPressDurationMs: Int = 25, // 10ms to 300ms
    var shizukuEmulationMode: String = "gamepad", // "keyboard", "gamepad"
    var usbEmulationMode: String = "keyboard",     // "keyboard", "gamepad"
    var emulationMode: String = "gamepad", // "keyboard", "gamepad" (legacy fallback)
    var injectionMethod: String = "inject", // "uinput", "inject", "keyevent"
    var simultaneousGroupingMs: Int = 0, // 0ms (disabled/instant) to 40ms
    var showLogConsole: Boolean = false,
    var isDarkTheme: Boolean = false,
    var themeMode: String = "light", // "system", "light", "dark"

    // Layout adjustment settings (Portrait / 縦画面)
    var portraitSizePercent: Int = 100,
    var portraitVerticalPosPercent: Int = 50,
    var portraitPreset1: DrumPreset = DrumPreset(100, 50),
    var portraitPreset2: DrumPreset = DrumPreset(85, 60),

    // Layout adjustment settings (Landscape / 横画面)
    var landscapeSizePercent: Int = 100,
    var landscapeVerticalPosPercent: Int = 55,
    var landscapePreset1: DrumPreset = DrumPreset(100, 55),
    var landscapePreset2: DrumPreset = DrumPreset(80, 50)
) {
    val activeEmulationMode: String
        get() = when (connectionMode) {
            "usb-wired" -> "keyboard"
            "another_android" -> if (anotherAndroidRole == "sender") "keyboard" else shizukuEmulationMode
            else -> shizukuEmulationMode
        }
}
