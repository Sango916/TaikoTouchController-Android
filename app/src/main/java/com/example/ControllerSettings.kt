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
    var verticalPositionPercent: Int = 50,
    var donBigPercent: Int = 40, // 判定の広さ (面: 10%～100%)
    var katBigPercent: Int = 50  // 判定の広さ (フチ: 10%～100%)
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
    // 大音符DS判定の広さ (面: 10%～100%, デフォルト40% -> 面の内側40%が大音符判定)
    var donBigNotePercent: Int = 40,
    // 大音符DS判定の広さ (フチ: 10%～100%, デフォルト50% -> フチの外側50%が大音符判定)
    var katBigNotePercent: Int = 50,

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
    var anotherAndroidPort: String = "60002",
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
    var portraitPreset1: DrumPreset = DrumPreset(100, 50, 40, 50),
    var portraitPreset2: DrumPreset = DrumPreset(85, 60, 40, 50),

    // Layout adjustment settings (Landscape / 横画面)
    var landscapeSizePercent: Int = 100,
    var landscapeVerticalPosPercent: Int = 55,
    var landscapePreset1: DrumPreset = DrumPreset(100, 55, 40, 50),
    var landscapePreset2: DrumPreset = DrumPreset(80, 50, 40, 50),

    // Overlay display settings (オーバーレイ表示用設定)
    var overlayAlphaPercent: Int = 80 // 10% to 100%
) {
    val activeEmulationMode: String
        get() = when (connectionMode) {
            "usb-wired" -> "keyboard"
            "another_android" -> if (anotherAndroidRole == "sender") "keyboard" else shizukuEmulationMode
            else -> shizukuEmulationMode
        }
}

