package com.example

import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

object I18n {
    val isEnglish: Boolean
        @Composable
        @ReadOnlyComposable
        get() {
            val locale = LocalConfiguration.current.locales[0]
            val lang = locale?.language ?: Locale.getDefault().language
            return lang != "ja"
        }

    fun isEnglish(context: Context? = null): Boolean {
        val lang = context?.resources?.configuration?.locales?.get(0)?.language
            ?: Locale.getDefault().language
        return lang != "ja"
    }

    @Composable
    @ReadOnlyComposable
    fun tr(ja: String, en: String): String = if (isEnglish) en else ja

    fun tr(context: Context?, ja: String, en: String): String = if (isEnglish(context)) en else ja

    @Composable
    @ReadOnlyComposable
    fun t(ja: String): String = if (isEnglish) translate(ja) else ja

    fun t(context: Context?, ja: String): String = if (isEnglish(context)) translate(ja) else ja

    fun getCurrentAppLocaleTag(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            val locales = localeManager?.applicationLocales
            if (locales != null && !locales.isEmpty) {
                return locales.get(0)?.language ?: ""
            }
        }
        return ""
    }

    fun openAppLanguageSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            } catch (e: Exception) {
                // Fallback to app details
            }
        }
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                tr(context, "設定画面を開けませんでした", "Could not open settings"),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    val translations: Map<String, String> = mapOf(
        "太鼓タッチコントローラー" to "Taiko Touch Controller",
        "🥁 太鼓コントローラー" to "🥁 Taiko Controller",
        "⚙️ 設定" to "⚙️ Settings",
        "⚙️ コントローラー設定項目" to "⚙️ Controller Settings",
        "すべて折りたたむ" to "Collapse All",
        "すべて展開" to "Expand All",
        "📱 表示モード切り替え" to "📱 Display Mode",
        "全画面表示または他アプリの上に重ねて表示" to "Display in fullscreen or overlay over other apps",
        "📱 全画面" to "📱 Fullscreen",
        "🪟 オーバーレイ" to "🪟 Overlay",
        "長押しで全画面を終了" to "Long press to exit fullscreen",
        "設定を開く" to "Open Settings",
        "キャンセル" to "Cancel",
        "閉じる" to "Close",
        "更新" to "Refresh",
        "クリア" to "Clear",
        "保存" to "Save",
        "読み込み" to "Load",
        "削除" to "Delete",
        "初期化" to "Reset",
        "適用" to "Apply",
        "接続" to "Connect",
        "切断" to "Disconnect",
        "接続中" to "Connecting",
        "待機中" to "Waiting",
        "稼働中" to "Running",
        "要確認" to "Needs Check",
        "展開する" to "Expand",
        "折りたたむ" to "Collapse",
        "拡大" to "Expand",
        "縮小" to "Collapse",
        "長押し" to "Hold",
        "🌐 接続先設定" to "🌐 Connection Mode",
        "この端末 (Shizuku)" to "This Device (Shizuku)",
        "この端末" to "This Device",
        "PC (USB)" to "PC (USB)",
        "PC" to "PC",
        "別のAndroid" to "Another Android",
        "なし (ローカル)" to "None (Local)",
        "なし" to "None",
        "未接続" to "Disconnected",
        "送信側" to "Sender",
        "受信側" to "Receiver",
        "送信側 (太鼓)" to "Sender (Taiko)",
        "受信側 (ゲーム)" to "Receiver (Game)",
        "有線 USB通信" to "Wired USB",
        "無線 Wi-Fi" to "Wireless Wi-Fi",
        "無線 Bluetooth" to "Wireless Bluetooth",
        "🔌 USB有線" to "🔌 Wired USB",
        "接続方式を選択:" to "Select Connection Type:",
        "🥁 太鼓オーバーレイ" to "🥁 Taiko Overlay",
        "太鼓オーバーレイ" to "Taiko Overlay",
        "メニューを閉じる" to "Close Menu",
        "太鼓アプリを開く" to "Open Taiko App",
        "オーバーレイ終了" to "Exit Overlay",
        "太鼓オーバーレイ表示" to "Taiko Overlay Display",
        "太鼓コントローラー (オーバーレイ表示中)" to "Taiko Controller (Overlay Active)",
        "バブルメニューから判定のON/OFFやアプリ復帰が可能です" to "Toggle hit detection or return to app via bubble menu",
        "太鼓コントローラーのオーバーレイ表示を維持します" to "Keeps Taiko controller overlay active over other apps",
        "判定: ON (プレイ中)" to "Hit: ON (Playing)",
        "判定: OFF (透過中)" to "Hit: OFF (Pass-through)",
        "判定ON (タッチ有効)" to "Hit ON (Touch Active)",
        "判定OFF (透過中)" to "Hit OFF (Pass-through)",
        "🥁 太鼓の判定: ON (プレイ中)" to "🥁 Drum Hit: ON (Playing)",
        "🛡️ 太鼓の判定: OFF (タッチ透過中)" to "🛡️ Drum Hit: OFF (Pass-through)",
        "判定ON中: メニューを開くには長押ししてください" to "Hit is ON: Long press bubble to open menu",
        "※ プレイ中の誤動作防止のため、判定ON時はバブル長押しでメニューを開きます" to "※ To prevent accidental touches while playing, long press the bubble when Hit is ON",
        "太鼓オーバーレイを終了しました" to "Taiko overlay closed",
        "🖥️ 上画面 (メイン) へ移動" to "🖥️ Move to Top (Main) Screen",
        "📱 下画面 (サブ) へ移動" to "📱 Move to Bottom (Sub) Screen",
        "上画面 (メイン画面)" to "Top Screen (Main)",
        "下画面 (サブ画面)" to "Bottom Screen (Sub)",
        "上画面" to "Top Screen",
        "下画面" to "Bottom Screen",
        "画面" to "Screen",
        "🪟 「他のアプリの上に重ねて表示」の許可" to "🪟 'Display over other apps' Permission",
        "太鼓コントローラーを他のアプリの画面上にオーバーレイ表示するため、システム設定で「他のアプリの上に重ねて表示」を許可してください。\n\n許可後に再度「オーバーレイ」ボタンを押すと起動します。" to "Please allow 'Display over other apps' in System Settings to display the Taiko Controller over other apps.\n\nAfter granting permission, tap 'Overlay' again to start.",
        "オーバーレイ設定画面を開けませんでした" to "Could not open overlay settings screen",
        "Bluetooth権限が必要です" to "Bluetooth permission required",
        "Shizuku権限: 承認されました" to "Shizuku permission: Granted",
        "Shizuku権限: 拒否されました" to "Shizuku permission: Denied",
        "Shizuku権限は既に承認されています" to "Shizuku permission already granted",
        "Shizukuサービスが起動していません" to "Shizuku service is not running",
        "📋 ログを表示" to "📋 Show Logs",
        "ログコンソールを表示" to "Show Log Console",
        "入力履歴やデバイス接続状態のリアルタイムログを上部にオーバーレイ表示します" to "Overlay real-time logs of input events and connection states at the top",
        "🔌 接続中の入力デバイス一覧 " to "🔌 Connected Input Devices ",
        "🔌 接続中の入力デバイス一覧" to "🔌 Connected Input Devices",
        "📋 リアルタイム入力・接続ログ " to "📋 Real-time Input & Connection Logs ",
        "📋 リアルタイム入力・接続ログ" to "📋 Real-time Input & Connection Logs",
        "← ログ表示" to "← Logs",
        "デバイス一覧" to "Device List",
        "デバイス情報を取得中..." to "Retrieving device info...",
        "接続されている入力デバイスはありません。" to "No input devices connected.",
        "ログは空です。太鼓を叩くか、接続を行うとリアルタイムでここに出力されます。" to "Logs are empty. Tapping the drum or connecting will output logs here.",
        "🎨 カラーテーマ設定" to "🎨 Color Theme",
        "ライト" to "Light",
        "ダーク" to "Dark",
        "🌙 ダーク" to "🌙 Dark",
        "☀️ ライト" to "☀️ Light",
        "⚙️ 自動 (OS)" to "⚙️ Auto (OS)",
        "テーマ: 自動" to "Theme: Auto",
        "テーマ: ライト" to "Theme: Light",
        "テーマ: ダーク" to "Theme: Dark",
        "🥁 太鼓の動作設定 (振動・大音符・ログ)" to "🥁 Drum Behavior (Haptics, Big Notes, Logs)",
        "バイブレーション" to "Vibration",
        "バイブレーションの強さ" to "Vibration Strength",
        "デフォルト" to "Default",
        "入力:" to "Input:",
        "大音符DS風モード" to "DS-style Big Notes",
        "太鼓の中心/フチ端をタップ時、自動で両手同時押しに変換" to "Automatically triggers simultaneous 2-hand hits when tapping center or rim edge",
        "太鼓を叩くと振動し、迫力のある演奏体験を実現します" to "Vibrates when hitting the drum for realistic tactile feedback",
        "描画軽量モード (エフェクトOFF)" to "Lightweight Mode (Effects OFF)",
        "波紋エフェクトや毎フレームの回転アニメーション描画を省略し、高速連打時のCPU負荷とフレーム落ちを最小化します" to "Disables ripple effects and animations to minimize CPU load and frame drops during fast rolls",
        "縦画面" to "Portrait",
        "横画面" to "Landscape",
        "右ドン" to "Right Don",
        "左ドン" to "Left Don",
        "右カッ" to "Right Kat",
        "左カッ" to "Left Kat",
        "🎯 大音符DS 判定の広さ設定" to "🎯 Big Note Detection Area",
        "初期値 (40%)" to "Default (40%)",
        "初期値 (100%)" to "Default (100%)",
        "※ 面は中心から指定%内、フチは内側(面との境界)から指定%内をタップすると大音符(両手)になります (100%超えも設定可能)" to "※ Tapping within specified % from center (Don) or rim boundary (Kat) triggers big notes (Can exceed 100%)",
        "配置・サイズ・大音符範囲の保存と一括切り替え" to "Save and batch switch layouts, sizes, and big note areas",
        "🎮 キーマッピング設定 (ゲームパッド)" to "🎮 Key Mapping (Gamepad)",
        "⌨️ キーマッピング設定 (キーボード)" to "⌨️ Key Mapping (Keyboard)",
        "💻 USB PC接続設定" to "💻 USB PC Connection",
        "USB接続時入力形式: ⌨️ キーボード (固定)" to "Input format for USB PC: ⌨️ Keyboard (Fixed)",
        "※ USB PC接続時はPC側スクリプトへキーボード入力のみ送信されます。" to "※ Only keyboard inputs are sent to the PC script during USB PC connection.",
        "📖 接続手順 (推奨: GitHub Releases からダウンロード):" to "📖 Setup Guide (Recommended: Download from GitHub Releases):",
        "📥 GitHub Releases からスクリプトをダウンロード" to "📥 Download Script from GitHub Releases",
        "📋 テキストからスクリプトを手動コピーして作成する" to "📋 Copy script text manually",
        "📱 AndroidのBluetooth設定を開く (新規ペアリング)" to "📱 Open Bluetooth Settings (New Pairing)",
        "📱 AndroidのBluetooth設定を開く (ペアリング用)" to "📱 Open Bluetooth Settings (Pairing)",
        "📱 AndroidのUSB設定を開く" to "📱 Open Android USB Settings",
        "📱 AndroidのUSB設定を開く (制御元の切替)" to "📱 Open Android USB Settings (Switch Control)",
        "📱 ペアリング済み端末一覧 (タップして接続)" to "📱 Paired Devices (Tap to Connect)",
        "🔍 受信機 (ゲーム) を自動検出して接続" to "🔍 Auto-detect and Connect to Receiver",
        "🔍 探索中..." to "🔍 Searching...",
        "🔍 ゲーム側 (受信機) を自動検出中..." to "🔍 Auto-detecting receiver (game)...",
        "ペアリング成功" to "Pairing Successful",
        "ペアリングに成功しました！次に接続ボタンを押してください。" to "Pairing succeeded! Next, tap the Connect button.",
        "ペアリングに失敗しました" to "Pairing Failed",
        "Bluetooth設定画面を開けませんでした" to "Could not open Bluetooth settings screen",
        "再確認する" to "Recheck Status",
        "ポート番号" to "Port Number",
        "受信側 (ゲーム) AndroidのIPアドレス" to "Receiver (Game) Android IP Address",
        "待受ポート" to "Listening Port",
        "太鼓のサイズ・位置設定" to "Drum Size & Position",
        "📏 太鼓のサイズ・位置設定" to "📏 Drum Size & Position",
        "✥ 太鼓のサイズ・位置・透明度調整" to "✥ Drum Size, Position & Opacity",
        "⚡ Shizuku 直接入力設定" to "⚡ Shizuku Direct Input Settings",
        "⚡ この端末 (Shizuku) 設定" to "⚡ This Device (Shizuku) Settings",
        "📱 別のAndroid連携設定" to "📱 Another Android Connection Settings",
        "💻 PC有線/ADB接続設定" to "💻 PC Wired / ADB Settings",
        "🥁 打感・判定カスタマイズ" to "🥁 Hit Feel & Detection Customization",
        "💾 プリセット管理" to "💾 Preset Management",
        "⌨️ キー割り当て・入力設定" to "⌨️ Key Mapping & Input Settings",
        "🎨 テーマ・UI設定" to "🎨 Theme & UI Settings",
        "アプリアイコン" to "App Icon",
        "コピー" to "Copy",
        "IPコピー" to "Copy IP",
        "検出されたすべてのネットワーク:" to "All detected network interfaces:",
        "この端末のIPアドレス (接続先)" to "This Device IP Address (Target)",
        "この端末のBluetooth名" to "This Device Bluetooth Name",
        "この端末の役割を選択:" to "Select This Device Role:",
        "インジェクション方式:" to "Injection Method:",
        "デバイス種別 (エミュレーション):" to "Emulation Device Type:",
        "Direct API (推奨)" to "Direct API (Recommended)",
        "uinput (仮想デバイス)" to "uinput (Virtual Device)",
        "ゲームパッド (推奨)" to "Gamepad (Recommended)",
        "ゲームパッド" to "Gamepad",
        "キーボード" to "Keyboard",
        "DPad 上" to "DPad Up",
        "DPad 下" to "DPad Down",
        "DPad 左" to "DPad Left",
        "DPad 右" to "DPad Right",
        "A ボタン" to "A Button",
        "B ボタン" to "B Button",
        "X ボタン" to "X Button",
        "Y ボタン" to "Y Button",
        "⚙️ インジェクション設定" to "⚙️ Injection Settings",
        "🎮 ゲームパッドのポイント" to "🎮 Gamepad Tips",
        "🐬 Dolphinエミュレータ設定のポイント" to "🐬 Dolphin Emulator Setup Tips",
        "💡 Bluetooth接続の簡単3ステップ" to "💡 3 Easy Steps for Bluetooth Connection",
        "💡 受信側 (ゲーム端末) の準備手順" to "💡 Setup Steps for Receiver (Game Device)",
        "🚀 Wi-Fiテザリング直接接続の手順" to "🚀 Steps for Direct Wi-Fi Tethering",
        "🔒 無線通信モードを非表示にしました (USB有線固定)" to "🔒 Wireless modes hidden (USB Wired Only)",
        "🔓 隠し設定解放: 無線通信モード (Wi-Fi / Bluetooth) を出現させました！" to "🔓 Secret Unlocked: Wireless modes (Wi-Fi / Bluetooth) revealed!",
        "無線通信モード: 解放" to "Wireless Mode: Unlocked",
        "無線通信モード: 非表示" to "Wireless Mode: Hidden",
        "Shizukuアプリを起動" to "Launch Shizuku App",
        "Shizukuの使用を許可" to "Allow Shizuku Access",
        "Shizuku 未起動 / 権限なし" to "Shizuku Not Running / No Permission",
        "Shizuku: 停止中 (Shizukuアプリを起動してください)" to "Shizuku: Stopped (Please launch Shizuku app)",
        "Shizuku: 実行中・許可保留中" to "Shizuku: Running / Permission Pending",
        "Shizuku: 実行中・許可済み (接続成功)" to "Shizuku: Running / Authorized (Connected)",
        "(オーバーレイ専用)" to "(Overlay Only)",
        "※ オーバーレイ表示モード時の透け具合を設定します (通常アプリ画面には影響しません)" to "※ Adjusts transparency in overlay mode (Does not affect main app screen)",
        "Bluetooth受信: 待機を停止しました" to "Bluetooth Receiver: Stopped listening",
        "Bluetooth送信: 接続を解除しました" to "Bluetooth Sender: Disconnected",
        "Bluetooth送信: 切断されました" to "Bluetooth Sender: Disconnected",
        "Bluetooth送信: ペアリング済みデバイスを選択してください" to "Bluetooth Sender: Please select a paired device",
        "Wi-Fi 無線通信: 切断されました" to "Wi-Fi Wireless: Disconnected",
        "USB有線モード: TCP サーバー再起動完了" to "USB Wired Mode: TCP server restarted",
        "⚡ 通信・ポート再初期化 (1タップ再接続)" to "⚡ Reinitialize Ports & Comm (1-Tap Reconnect)",
        "⚡ 通信・ポート再初期化を開始します..." to "⚡ Starting port & communication reinitialization...",
        "⚡ 通信・ポートを再初期化しました" to "⚡ Reinitialized communication and ports",
        "⚡ USB 有線通信: 接続完了" to "⚡ USB Wired: Connected",
        "⚡ USB Direct (AOA) 超極小遅延通信 接続完了 (<1ms Latency)" to "⚡ USB Direct (AOA) Ultra-Low Latency Connected (<1ms)",
        "🔌 USB ケーブル接続を待機中..." to "🔌 Waiting for USB cable connection...",
        "🔌 USBケーブルを繋ぐだけで自動認識されます (テザリング設定は不要です)" to "🔌 Auto-recognized simply by plugging in USB cable (No tethering setup required)",
        "🔌 接続方式: USB有線 (Type-C直結)" to "🔌 Connection Type: USB Wired (Direct Type-C)",
        "接続試行中..." to "Attempting to connect...",
        "接続完了: 受信側 (ゲーム) へ入力を送信可能です" to "Connected: Ready to send inputs to receiver (game)",
        "接続エラー: 受信側IP・ポート番号を確認してください" to "Connection error: Please check receiver IP and port",
        "送信側Androidからの接続を待機中..." to "Waiting for connection from sender Android...",
        "⚪ Bluetooth未接続 (下の端末一覧からゲーム端末をタップして接続)" to "⚪ Bluetooth Disconnected (Tap game device below to connect)",
        "🟡 ゲーム端末にBluetooth接続中..." to "🟡 Connecting to game device via Bluetooth...",
        "🟡 太鼓側 (送信側) からのBluetooth接続を待機中..." to "🟡 Waiting for Bluetooth connection from sender (Taiko)...",
        "🔴 Bluetooth接続エラー (相手端末でアプリが起動しているか確認してください)" to "🔴 Bluetooth error (Please check if app is running on the other device)",
        "🟢 接続中" to "🟢 Connected",
        "🔥 [テザリング直接]" to "🔥 [Direct Tethering]",
        "(🌐 Wi-Fi経由)" to "(🌐 via Wi-Fi)",
        "(🔥 テザリング直接接続)" to "(🔥 Direct Tethering)",
        "⚙️ 手動設定 (IP/ポート指定) を表示 ▾" to "⚙️ Show Manual Settings (IP/Port) ▾",
        "⚙️ 手動設定 (IP/ポート指定) を隠す ▴" to "⚙️ Hide Manual Settings (IP/Port) ▴",
        "192.168.43.1 または 192.168.1.100" to "192.168.43.1 or 192.168.1.100",
        "Shizuku（ADB権限実行環境）を使用して、端末ローカルでダイレクトにキーボード/ゲームパッド信号を注入します。Root化不要で動作します。" to "Uses Shizuku (ADB privileged environment) to directly inject keyboard/gamepad inputs locally on this device. No root required.",
        "ARMSX2などデバイスの種類を識別しないエミュレータでは、本アプリのデバイス種別を「ゲームパッド (推奨)」に設定することで、物理コントローラーと本アプリの入力を同じボタン入力として共有でき便利です。" to "For emulators like ARMSX2 that do not distinguish device types, setting device type to 'Gamepad (Recommended)' lets you share inputs between physical controllers and this app.",
        "端末をUSBケーブルでPCに接続し、PC側からADBポートフォワーディングを行うことで、PC上のゲームへ超低遅延・root化不要で入力を送信します。" to "Connect this device to PC via USB cable and use ADB port forwarding to send inputs to games on PC with ultra-low latency and no root required.",
        "2台のAndroid端末をType-C - Type-C ケーブル（またはUSB OTGケーブル）で繋ぐだけ！USB AOAダイレクト通信により、ネットワーク遅延ゼロ・1ms未満の最高速入力レスポンスを実現します。" to "Just connect two Android devices with a Type-C to Type-C cable (or USB OTG cable)! Direct USB AOA gives zero network latency and sub-1ms fastest input response.",
        "2台のAndroid端末を同じWi-Fi（またはネットワーク）に接続し、一方を「送信側（太鼓）」、もう一方を「受信側（ゲーム）」として通信させます。" to "Connect two Android devices to the same Wi-Fi (or network), setting one as 'Sender (Taiko)' and the other as 'Receiver (Game)'.",
        "📶 Bluetoothで2台のAndroidを直接ワイヤレス接続します。外部Wi-Fiルーターやテザリング、IPアドレスの入力は不要！端末同士をペアリングするだけで接続できます。" to "Directly connect two Android devices wirelessly via Bluetooth. No external Wi-Fi router, tethering, or IP address entry needed! Just pair the devices.",
        "スマートフォンの無線通信は、Android OSの省電力制御（Sniff Mode/スリープ）、パケットバッファリング、電波干渉等の影響を受けるため、入力の遅延（レイテンシ）や打鍵の欠落（抜け）が発生しやすくなります。\n\n高精度な判定や高速連打の安定性を求める場合は、Type-Cケーブル直結の「🔌 USB有線」接続を推奨します。" to "Smartphone wireless communication is affected by Android OS power saving (Sniff Mode/sleep), packet buffering, and radio interference, which can cause input latency or missed hits.\n\nFor high-precision timing and stable fast rolls, direct Type-C '🔌 Wired USB' connection is recommended.",
        "USB Type-Cケーブルで太鼓側 (送信側) Androidと繋いでおくだけで受信準備完了です。接続許可ダイアログが出たら「許可」を選択してください。" to "Simply connect with the Taiko (Sender) Android using a USB Type-C cable to be ready. If a permission dialog appears, select 'Allow'.",
        "この端末のIPアドレスを太鼓側 (送信側) に入力するか、太鼓側で「自動検出」を実行してください。" to "Enter this device's IP address on the Taiko (Sender) device, or run 'Auto-detect' on the sender.",
        "「自動検出」を押すか、受信側（ゲーム）画面に表示されているIPアドレスを入力して接続してください。" to "Tap 'Auto-detect' or enter the IP address displayed on the receiver (game) screen to connect.",
        "ペアリング済みのBluetooth端末が見つかりません。\n下のボタンからゲーム端末とBluetoothペアリングしてください。" to "No paired Bluetooth devices found.\nPlease pair with the game device using the button below.",
        "PC側スクリプト(ポート60001)と正常に接続中です。太鼓を叩くとPCへ入力が送信されます。" to "Successfully connected to PC script (port 60001). Tapping the drum will send inputs to the PC.",
        "❌ 受信機が見つかりませんでした。受信側（ゲーム）でアプリを起動しているか確認してください。" to "❌ Receiver not found. Please verify the app is running on the receiver (game) device.",
        "接続に失敗しました。ワイヤレスデバッグを有効にしてください。" to "Connection failed. Please enable Wireless Debugging.",
        "① AndroidのBluetoothがONになっていることを確認します。\n" to "① Ensure Android Bluetooth is turned ON.\n",
        "② 太鼓側端末と一度ペアリングします（設定ボタンから可能）。\n" to "② Pair once with the Taiko device (via the Settings button).\n",
        "③ このアプリでShizukuを起動した状態で太鼓側から接続すれば、受信した打鍵がゲームに即座に入力されます！" to "③ With Shizuku running in this app, connecting from the Taiko device instantly inputs hits into the game!",
        "① 受信側（ゲーム端末）とこの端末（太鼓端末）の両方でBluetoothをONにしてペアリングします。\n" to "① Turn ON Bluetooth on both receiver (game) and this device (Taiko), and pair them.\n",
        "② ゲーム端末でこのアプリを起動し、「受信側 (ゲーム)」にして待機します。\n" to "② Launch this app on the game device, set to 'Receiver (Game)', and wait.\n",
        "③ 上の一覧に表示されたゲーム端末の名前をタップするだけで即接続完了！\n" to "③ Tap the game device name in the list above to connect instantly!\n",
        "※ルーターを介さず端末同士が直接通信するため、安定してプレイできます。" to "※ Devices communicate directly without a router for stable gameplay.",
        "※Wi-Fiルーターやテザリング、IPアドレス指定が一切不要で、最速・超低遅延で快適に遊べます！" to "※ No Wi-Fi router, tethering, or IP entry required, offering fast, ultra-low latency play!",
        "① 受信側（ゲーム端末）で「Wi-Fiテザリング（アクセスポイント）」をONにします。\n" to "① Turn ON 'Wi-Fi Tethering (Hotspot)' on the receiver (game device).\n",
        "② 送信側（太鼓端末）のWi-Fi設定を開き、受信側のWi-Fiスポットに接続します。\n" to "② Open Wi-Fi settings on the sender (Taiko device) and connect to the receiver's hotspot.\n",
        "③ この画面で「🔍 受信機 (ゲーム) を自動検出して接続」を押します。（または手動で 192.168.43.1 を入力）\n" to "③ Tap '🔍 Auto-detect and Connect to Receiver' on this screen (or manually enter 192.168.43.1).\n",
        "1. 端末の「USBデバッグ」を有効にしてPCにUSB接続します。\n" to "1. Enable 'USB Debugging' on device and connect to PC via USB.\n",
        "2. 下のボタンからGitHub Releasesを開き、お使いのPC環境に合わせたスクリプトをダウンロードします：\n" to "2. Open GitHub Releases via button below and download script for your PC:\n",
        "   ・Windows用: TTC-receiver-windows.ps1\n" to "   - For Windows: TTC-receiver-windows.ps1\n",
        "   ・macOS用: TTC-receiver-macos.command\n" to "   - For macOS: TTC-receiver-macos.command\n",
        "   ・Linux用: TTC-receiver-linux.sh\n" to "   - For Linux: TTC-receiver-linux.sh\n",
        "3. ダウンロードしたスクリプトを実行すると、ADB環境の自動構築・ポート転送・キー入力ツールの準備・接続まで全自動で行われます！\n" to "3. Run the downloaded script to automatically set up ADB, port forwarding, input tools, and connection!\n",
        "   ※ Windows版: スクリプトを右クリックして「PowerShell で実行」を選択します。\n" to "   ※ Windows: Right-click script and select 'Run with PowerShell'.\n",
        "   ※ macOS / Linux版: 初回実行時にファイルの実行権限（chmod +x）の設定が必要です。\n" to "   ※ macOS / Linux: Set execution permissions (chmod +x) on first run.\n",
        "   ※ macOS版: 初回実行時にキー入力送信のためアクセシビリティ権限の許可が必要です。\n\n" to "   ※ macOS: Grant Accessibility permission for key simulation on first run.\n\n",
        "※ テキストから手動でコピーして作成する場合は、下の「手動作成用スクリプトを表示」を展開してください。" to "※ To create manually from text, expand 'Show scripts for manual creation' below.",
        "【基本設定】\n" to "【Basic Setup】\n",
        "1. インジェクション方式: 「Direct API (推奨)」を選択（動作不安定時は「uinput」）\n" to "1. Injection method: Select 'Direct API (Recommended)' (or 'uinput' if unstable)\n",
        "2. デバイス種別: 「ゲームパッド (推奨)」を選択\n" to "2. Device type: Select 'Gamepad (Recommended)'\n",
        "3. Dolphin設定: 『コントローラー設定』の「Create Mappings for Other Devices」をオンにしてボタンを割り当てます。\n\n" to "3. Dolphin: Turn ON 'Create Mappings for Other Devices' in Controller Settings and map buttons.\n\n",
        "【タタコン拡張設定 (Extension)】\n" to "【TaTaCon Extension Setup】\n",
        "• Dolphin Android版でタタコンをExtensionに設定するには、設定プロファイル (.ini) を直接編集する必要があります。\n" to "• To set TaTaCon as an Extension in Dolphin Android, edit the profile (.ini) directly.\n",
        "• Dolphinの『Wii Input』各コントローラー設定内にある『Profiles』から設定を保存すると.iniファイルが作成されます。\n" to "• Save a profile from 'Profiles' under 'Wii Input' controller settings to create the .ini file.\n",
        "• 保存先: Dolphinユーザーフォルダ内の『Config/Profiles/Wiimote/』配下\n" to "• Location: Under 'Config/Profiles/Wiimote/' in Dolphin user directory\n",
        "• .iniファイル内に「Extension = TaTaCon」と記入してください（元からExtensionの行があれば書き換え、無ければ追加）。\n\n" to "• Add or replace line 'Extension = TaTaCon' in the .ini file.\n\n",
        "【Android 11以降のフォルダ制限と回避策】\n" to "【Android 11+ Folder Access & Workarounds】\n",
        "• Android 11以降でDolphinのユーザーフォルダにアクセスするには、特殊なファイルマネージャーを使用するか、Dolphinの機能で毎回「エクスポート / インポート」を行う必要があります。\n" to "• To access Dolphin user folder on Android 11+, use a specialized file manager or Dolphin's Export/Import feature.\n",
        "• 【回避方法】旧来の「dolphin-emu」フォルダを使用したい場合は、一度バージョン 5.0-15341 以前のDolphinをインストールして起動（フォルダ生成）させてから、最新版にアップデートすることで制限を回避できます。\n" to "• 【Workaround】To use legacy 'dolphin-emu' folder, install Dolphin v5.0-15341 or earlier once to create the folder, then update to latest.\n",
        "※ 既にDolphinをインストールして使用している場合は、作業前に必ずDolphinの機能でエクスポートしてバックアップを取ってください。\n\n" to "※ If Dolphin is already installed, always export a backup before proceeding.\n\n",
        "詳しく知りたい方は以下の公式ガイドをご覧ください:" to "For details, please see the official guide below:",
        "【受信側 (ゲーム) の設定 (有線 USB通信)】" to "【Receiver (Game) Setup (Wired USB)】",
        "【送信側の設定 (有線 USB通信)】" to "【Sender Setup (Wired USB)】",
        "【受信側 (ゲーム) の設定 (無線 Wi-Fi)】" to "【Receiver (Game) Setup (Wireless Wi-Fi)】",
        "【送信側の設定 (無線 Wi-Fi)】" to "【Sender Setup (Wireless Wi-Fi)】",
        "【受信側 (ゲーム) の設定 (Bluetooth直接接続)】" to "【Receiver (Game) Setup (Bluetooth Direct)】",
        "【送信側の設定 (Bluetooth直接接続)】" to "【Sender Setup (Bluetooth Direct)】",
        "USB 有線直接通信 (AOA / USB Direct): 接続完了! (<1ms)" to "USB Direct (AOA / USB Direct): Connected! (<1ms)",
        "USB 有線通信: AOA / USBテザリング接続の待機中..." to "USB Wired: Waiting for AOA / USB tethering connection...",
        "無線 (Wi-Fi) モード: 受信機 (ゲーム) を自動探索中..." to "Wireless (Wi-Fi): Auto-discovering receiver (game)...",
        "無線 (Wi-Fi) モード: 受信機が見つかりませんでした。受信側IPを手動入力するか、受信側のアプリが起動しているか確認してください。" to "Wireless (Wi-Fi): Receiver not found. Please enter receiver IP manually or check if app is running on receiver.",
        "全画面モード: 終了ボタンを長押しで閉じます" to "Fullscreen mode: Long press exit button to close",
        "設定画面を開けませんでした" to "Could not open settings screen",
        "テザリング / AP" to "Tethering / AP",
        "USB テザリング" to "USB Tethering",
        "有線 LAN" to "Wired LAN",
        "⚡ USB 有線通信: 送信側から接続中 (入力受信待機中)" to "⚡ USB Wired: Connected from sender (Waiting for input)",
        "🔌 太鼓側 (送信側) からのUSB接続を待機中..." to "🔌 Waiting for USB connection from sender (Taiko)...",
        "接続待機中 (ポート60001)" to "Waiting for connection (Port 60001)",
        "PC接続待機中 (ポート60001)" to "Waiting for PC connection (Port 60001)",
        "USB有線モード: TCP サーバー起動中 (ポート 60001)" to "USB Wired Mode: TCP Server running (Port 60001)",
        "USB Direct Driver (AOA / Direct) 起動: 自動検出開始" to "USB Direct Driver (AOA / Direct) started: Auto-detection begun",
        "USB Direct Driver 停止しました" to "USB Direct Driver stopped",
        "USB Direct Driver を完全リセット・再起動中..." to "USB Direct Driver: Fully resetting and restarting...",
        "USB ケーブルが抜かれました - リセット" to "USB cable unplugged - Reset",
        "USB ケーブル挿入検知" to "USB cable plugged in detected",
        "台" to " units",
        "件" to " items",
        "秒後" to "s later",
        "倍" to "x",
        "自動再接続をスケジュール中 (2秒後)..." to "Scheduling auto-reconnect (in 2s)...",
        "Bluetooth権限が許可されていません。設定で許可してください。" to "Bluetooth permission not granted. Please allow in Settings.",
        "Bluetoothがオフになっています。Bluetoothをオンにしてください。" to "Bluetooth is turned off. Please turn on Bluetooth.",
        "Bluetooth接続に失敗しました" to "Bluetooth connection failed",
        "Bluetooth受信: Bluetooth権限が必要です" to "Bluetooth Receiver: Bluetooth permission required",
        "Bluetooth受信: Bluetoothがオフになっています" to "Bluetooth Receiver: Bluetooth is turned off",
        "Bluetooth受信: 太鼓側端末が切断されました" to "Bluetooth Receiver: Sender device disconnected",
        "送信側 (太鼓) Androidが切断されました" to "Sender (Taiko) Android disconnected",
        "受信待機サーバー (ゲーム側) を停止しました" to "Receiver Server (Game) stopped",
        "=== [デバイス一覧] Dolphin設定用 ===" to "=== [Device List] For Dolphin Setup ===",
        "ジョイスティック " to "Joystick ",
        "タッチスクリーン " to "Touchscreen ",
        "仮想" to "Virtual",
        "物理" to "Physical",
    )

    fun translate(ja: String): String {
        if (ja.isEmpty()) return ja
        val exact = translations[ja]
        if (exact != null) return exact

        // Parameterized patterns
        val p1 = Regex("""^上下位置:\s*(-?\d+)%\s*\(0%=上端,\s*50%=中央,\s*100%=下端\s*/\s*-50%〜150%\)$""")
        p1.matchEntire(ja)?.let {
            return "Vertical Position: ${it.groupValues[1]}% (0%=Top, 50%=Center, 100%=Bottom / -50%~150%)"
        }

        val p2 = Regex("""^太鼓サイズ:\s*(\d+)%$""")
        p2.matchEntire(ja)?.let {
            return "Drum Size: ${it.groupValues[1]}%"
        }

        val p3 = Regex("""^🪟\s*オーバーレイ時の不透明度:\s*(\d+)%$""")
        p3.matchEntire(ja)?.let {
            return "🪟 Overlay Opacity: ${it.groupValues[1]}%"
        }

        val p4 = Regex("""^🔴\s*面\s*\(ドン\)\s*の判定範囲:\s*(\d+)%$""")
        p4.matchEntire(ja)?.let {
            return "🔴 Center (Don) Area: ${it.groupValues[1]}%"
        }

        val p5 = Regex("""^🔵\s*フチ\s*\(カッ\)\s*の判定範囲:\s*(\d+)%$""")
        p5.matchEntire(ja)?.let {
            return "🔵 Rim (Kat) Area: ${it.groupValues[1]}%"
        }

        val p6 = Regex("""^バイブレーションの強さ:\s*(\d+)%$""")
        p6.matchEntire(ja)?.let {
            return "Vibration Strength: ${it.groupValues[1]}%"
        }

        val p7 = Regex("""^バイブ:\s*(.+?)\s*/\s*大音符DS:\s*(.+)$""")
        p7.matchEntire(ja)?.let {
            return "Vibe: ${translate(it.groupValues[1])} / Big DS: ${translate(it.groupValues[2])}"
        }

        val p8 = Regex("""^横画面\s*\((\d+)%\s*/\s*位置(-?\d+)%\s*/\s*透過(\d+)%\)$""")
        p8.matchEntire(ja)?.let {
            return "Landscape (${it.groupValues[1]}% / Pos ${it.groupValues[2]}% / Opacity ${it.groupValues[3]}%)"
        }

        val p9 = Regex("""^縦画面\s*\((\d+)%\s*/\s*位置(-?\d+)%\s*/\s*透過(\d+)%\)$""")
        p9.matchEntire(ja)?.let {
            return "Portrait (${it.groupValues[1]}% / Pos ${it.groupValues[2]}% / Opacity ${it.groupValues[3]}%)"
        }

        val p10 = Regex("""^横画面\s*プリセット\s*(\d+)$""")
        p10.matchEntire(ja)?.let {
            return "Landscape Preset ${it.groupValues[1]}"
        }

        val p11 = Regex("""^縦画面\s*プリセット\s*(\d+)$""")
        p11.matchEntire(ja)?.let {
            return "Portrait Preset ${it.groupValues[1]}"
        }

        val p12 = Regex("""^📍\s*(横画面|縦画面)用\s*プリセット\s*\(サイズ・位置・面/フチ大音符\)$""")
        p12.matchEntire(ja)?.let {
            val orient = if (it.groupValues[1] == "横画面") "Landscape" else "Portrait"
            return "📍 $orient Preset (Size, Position, Big Notes)"
        }

        val p13 = Regex("""^サイズ:\s*(\d+)%\s*/\s*位置:\s*(-?\d+)%\s*/\s*面大音符:\s*(\d+)%\s*/\s*フチ大音符:\s*(\d+)%$""")
        p13.matchEntire(ja)?.let {
            return "Size: ${it.groupValues[1]}% / Pos: ${it.groupValues[2]}% / Center Big: ${it.groupValues[3]}% / Rim Big: ${it.groupValues[4]}%"
        }

        val p14 = Regex("""^待受ポート:\s*(\d+)$""")
        p14.matchEntire(ja)?.let {
            return "Listening Port: ${it.groupValues[1]}"
        }

        val p15 = Regex("""^接続中の送信側Android:\s*(\d+)台\s*\(入力受信待機中\)$""")
        p15.matchEntire(ja)?.let {
            return "Connected Sender Androids: ${it.groupValues[1]} (Waiting for input)"
        }

        val p16 = Regex("""^太鼓側端末「(.+?)」が接続中\s*\(入力受信待機中\)$""")
        p16.matchEntire(ja)?.let {
            return "Sender device '${it.groupValues[1]}' connected (Waiting for input)"
        }

        val p17 = Regex("""^🟢\s*太鼓側端末「(.+?)」が接続中\s*\(入力受信待機中\)$""")
        p17.matchEntire(ja)?.let {
            return "🟢 Sender device '${it.groupValues[1]}' connected (Waiting for input)"
        }

        val p18 = Regex("""^🟢\s*Bluetooth接続中:\s*(.+)$""")
        p18.matchEntire(ja)?.let {
            return "🟢 Bluetooth Connected: ${it.groupValues[1]}"
        }

        val p19 = Regex("""^💻\s*PC接続中\s*\((\d+)台\)$""")
        p19.matchEntire(ja)?.let {
            return "💻 PC Connected (${it.groupValues[1]})"
        }

        val p20 = Regex("""^💻\s*PCとの接続完了\s*\((\d+)台接続中\)$""")
        p20.matchEntire(ja)?.let {
            return "💻 Connected to PC (${it.groupValues[1]} connected)"
        }

        val p21 = Regex("""^(\d+)台のPC接続中$""")
        p21.matchEntire(ja)?.let {
            return "${it.groupValues[1]} PC(s) connected"
        }

        val p22 = Regex("""^PC接続状態:\s*接続完了\s*\((\d+)台のPCが接続中\)$""")
        p22.matchEntire(ja)?.let {
            return "PC Connection: Connected (${it.groupValues[1]} PC(s) connected)"
        }

        val p23 = Regex("""^PC接続状態:\s*接続待機中\s*\(TCPサーバー起動中:\s*ポート\s*60001\)$""")
        p23.matchEntire(ja)?.let {
            return "PC Connection: Waiting (TCP Server running on port 60001)"
        }

        val p24 = Regex("""^(.+?)\s*の内容をコピー$""")
        p24.matchEntire(ja)?.let {
            return "Copy content of ${it.groupValues[1]}"
        }

        val p25 = Regex("""^📱\s*別のAndroid連携設定\s*\((.+?)\)$""")
        p25.matchEntire(ja)?.let {
            return "📱 Another Android Settings (${translate(it.groupValues[1])})"
        }

        val p26 = Regex("""^役割:\s*受信側\s*\(ゲーム\)\s*\|\s*方式:\s*(.+)$""")
        p26.matchEntire(ja)?.let {
            return "Role: Receiver (Game) | Mode: ${translate(it.groupValues[1])}"
        }

        val p27 = Regex("""^役割:\s*送信側\s*\(太鼓\)\s*\|\s*方式:\s*(.+)$""")
        p27.matchEntire(ja)?.let {
            return "Role: Sender (Taiko) | Mode: ${translate(it.groupValues[1])}"
        }

        val p28 = Regex("""^左カッ:(.+?)\s*/\s*左ドン:(.+?)\s*/\s*右ドン:(.+?)\s*/\s*右カッ:(.+)$""")
        p28.matchEntire(ja)?.let {
            return "Left Kat:${it.groupValues[1]} / Left Don:${it.groupValues[2]} / Right Don:${it.groupValues[3]} / Right Kat:${it.groupValues[4]}"
        }

        val p29 = Regex("""^USB\s*有線ネットワーク:\s*受信側\s*\((.+?)\)\s*に接続完了$""")
        p29.matchEntire(ja)?.let {
            return "USB Wired Network: Connected to receiver (${it.groupValues[1]})"
        }

        val p30 = Regex("""^Wi-Fi\s*無線通信:\s*受信側\s*\((.+?)\)\s*に接続完了$""")
        p30.matchEntire(ja)?.let {
            return "Wi-Fi Wireless: Connected to receiver (${it.groupValues[1]})"
        }

        val p31 = Regex("""^Bluetooth送信:\s*受信側「(.+?)」に接続完了\s*\(<2ms\)$""")
        p31.matchEntire(ja)?.let {
            return "Bluetooth Sender: Connected to receiver '${it.groupValues[1]}' (<2ms)"
        }

        val p32 = Regex("""^太鼓オーバーレイを\s*(.+?)\s*に移動しました\n\[(.+?)\]$""")
        p32.matchEntire(ja)?.let {
            return "Moved Taiko overlay to ${translate(it.groupValues[1])}\n[${translate(it.groupValues[2])}]"
        }

        val p33 = Regex("""^太鼓オーバーレイ起動\s*\((.+?)\s*/\s*初期状態:\s*判定OFF\)\nバブルメニューから判定をONにできます$""")
        p33.matchEntire(ja)?.let {
            return "Taiko overlay launched (${translate(it.groupValues[1])} / Initial: Hit OFF)\nToggle Hit ON via bubble menu"
        }

        val p34 = Regex("""^オーバーレイの追加に失敗しました:\s*(.+)$""")
        p34.matchEntire(ja)?.let {
            return "Failed to add overlay: ${it.groupValues[1]}"
        }

        val p35 = Regex("""^Shizuku権限の要求に失敗しました:\s*(.+)$""")
        p35.matchEntire(ja)?.let {
            return "Failed to request Shizuku permission: ${it.groupValues[1]}"
        }

        val p36 = Regex("""^デバイス一覧取得エラー:\s*(.+)$""")
        p36.matchEntire(ja)?.let {
            return "Device list error: ${it.groupValues[1]}"
        }

        val p37 = Regex("""^Wi-Fi\s*無線通信エラー:\s*(.+)$""")
        p37.matchEntire(ja)?.let {
            return "Wi-Fi error: ${it.groupValues[1]}"
        }

        val p38 = Regex("""^Bluetooth送信エラー:\s*(.+)$""")
        p38.matchEntire(ja)?.let {
            return "Bluetooth error: ${it.groupValues[1]}"
        }

        val p39 = Regex("""^USB\s*有線通信:\s*(.+)$""")
        p39.matchEntire(ja)?.let {
            return "USB Wired: ${it.groupValues[1]}"
        }

        val p40 = Regex("""^Bluetooth:\s*ペアリング済みデバイス\s*(\d+)\s*件を取得しました$""")
        p40.matchEntire(ja)?.let {
            return "Bluetooth: Found ${it.groupValues[1]} paired device(s)"
        }

        val p41 = Regex("""^Another\s*Android\s*モード\s*\((.+?)\)\s*再接続完了$""")
        p41.matchEntire(ja)?.let {
            return "Another Android mode (${translate(it.groupValues[1])}) reconnected"
        }

        val p42 = Regex("""^ID:\s*(.+?)\s*\|\s*\(デバイス詳細取得失敗\)$""")
        p42.matchEntire(ja)?.let {
            return "ID: ${it.groupValues[1]} | (Failed to retrieve device details)"
        }

        val p43 = Regex("""^✅\s*発見しました:\s*(.+)$""")
        p43.matchEntire(ja)?.let {
            return "✅ Found: ${it.groupValues[1]}"
        }

        val p44 = Regex("""^💾\s*プリセット保存\s*\((.+?)\)$""")
        p44.matchEntire(ja)?.let {
            return "💾 Save Preset (${translate(it.groupValues[1])})"
        }

        val p45 = Regex("""^無線通信（(.+?)）使用時の注意$""")
        p45.matchEntire(ja)?.let {
            return "Notes when using wireless (${translate(it.groupValues[1])})"
        }

        val p46 = Regex("""^別のAndroid\s*\((.+?)\)$""")
        p46.matchEntire(ja)?.let {
            return "Another Android (${translate(it.groupValues[1])})"
        }

        val p47 = Regex("""^動作形態:\s*(.+)$""")
        p47.matchEntire(ja)?.let {
            return "Mode: ${translate(it.groupValues[1])}"
        }

        val p48 = Regex("""^太鼓側端末（送信機）の画面で、この名前「(.+?)」をタップして接続してください。$""")
        p48.matchEntire(ja)?.let {
            return "On the Taiko (Sender) device screen, tap this name '${it.groupValues[1]}' to connect."
        }

        val p49 = Regex("""^\((\d+)台\)$""")
        p49.matchEntire(ja)?.let {
            return "(${it.groupValues[1]} units)"
        }

        val p50 = Regex("""^\((\d+)件\)$""")
        p50.matchEntire(ja)?.let {
            return "(${it.groupValues[1]} items)"
        }

        // Substring fallback replacement
        var res = ja
        val subWords = listOf(
            "横画面" to "Landscape",
            "縦画面" to "Portrait",
            "プリセット" to "Preset",
            "大音符DS風モード" to "DS Big Notes",
            "大音符DS" to "Big Notes DS",
            "大音符" to "Big Notes",
            "面 (ドン)" to "Center (Don)",
            "フチ (カッ)" to "Rim (Kat)",
            "面" to "Center",
            "フチ" to "Rim",
            "ドン" to "Don",
            "カッ" to "Kat",
            "判定" to "Hit",
            "透過" to "Opacity",
            "初期化" to "Reset",
            "初期値" to "Default",
            "保存" to "Save",
            "適用" to "Apply",
            "接続中" to "Connecting",
            "接続完了" to "Connected",
            "接続待機中" to "Waiting for connection",
            "未接続" to "Disconnected",
            "待機中" to "Waiting",
            "稼働中" to "Running",
            "受信側" to "Receiver",
            "送信側" to "Sender",
            "太鼓" to "Taiko",
            "ゲーム" to "Game",
            "有線" to "Wired",
            "無線" to "Wireless",
            "端末" to "Device",
            "設定" to "Settings"
        )
        for ((jw, ew) in subWords) {
            if (res.contains(jw)) {
                res = res.replace(jw, ew)
            }
        }
        return res
    }
}

@Composable
@ReadOnlyComposable
fun tr(ja: String, en: String): String = I18n.tr(ja, en)

fun tr(context: Context?, ja: String, en: String): String = I18n.tr(context, ja, en)

@Composable
@ReadOnlyComposable
fun t(ja: String): String = I18n.t(ja)

fun t(context: Context?, ja: String): String = I18n.t(context, ja)

@Composable
@ReadOnlyComposable
@JvmName("translateExt")
fun String.t(): String = I18n.t(this)
