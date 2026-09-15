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

/**
 * Localization helper for Taiko Touch Controller.
 * Supports:
 * - System default language matching (Japanese when ja, English otherwise)
 * - Android 13+ Per-App Language Preferences (App info -> Language)
 * - Dynamic Composable and Context-based translation
 */
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
    fun t(ja: String): String = if (isEnglish) (translations[ja] ?: ja) else ja

    fun t(context: Context?, ja: String): String = if (isEnglish(context)) (translations[ja] ?: ja) else ja

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
        "🪟 「他のアプリの上に重ねて表示」の許可" to "🪟 'Display over other apps' Permission",
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
        "📋 リアルタイム入力・接続ログ " to "📋 Real-time Input & Connection Logs ",
        "← ログ表示" to "← Logs",
        "デバイス一覧" to "Device List",
        "デバイス情報を取得中..." to "Retrieving device info...",
        "接続されている入力デバイスはありません。" to "No input devices connected.",
        "ログは空です。太鼓を叩くか、接続を行うとリアルタイムでここに出力されます。" to "Logs are empty. Tapping the drum or connecting will show logs here.",
        "🎨 カラーテーマ設定" to "🎨 Color Theme",
        "ライト" to "Light",
        "🌙 ダーク" to "🌙 Dark",
        "🥁 太鼓の動作設定 (振動・大音符・ログ)" to "🥁 Drum Behavior (Haptics, Big Notes, Logs)",
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
        "配置・サイズ・大音符範囲の保存と一括切り替え" to "Save and batch switch layouts, sizes, and big note areas",
        "🎮 キーマッピング設定 (ゲームパッド)" to "🎮 Key Mapping (Gamepad)",
        "💻 USB PC接続設定" to "💻 USB PC Connection",
        "📥 GitHub Releases からスクリプトをダウンロード" to "📥 Download Script from GitHub Releases",
        "📋 テキストからスクリプトを手動コピーして作成する" to "📋 Copy script text manually",
        "📱 AndroidのBluetooth設定を開く (新規ペアリング)" to "📱 Open Bluetooth Settings (New Pairing)",
        "📱 AndroidのBluetooth設定を開く (ペアリング用)" to "📱 Open Bluetooth Settings (Pairing)",
        "📱 AndroidのUSB設定を開く" to "📱 Open Android USB Settings",
        "📱 ペアリング済み端末一覧 (タップして接続)" to "📱 Paired Devices (Tap to Connect)",
        "🔍 受信機 (ゲーム) を自動検出して接続" to "🔍 Auto-detect and Connect to Receiver",
        "🔍 探索中..." to "🔍 Searching...",
        "ペアリング成功" to "Pairing Successful",
        "ペアリングに失敗しました" to "Pairing Failed",
        "Bluetooth設定画面を開けませんでした" to "Could not open Bluetooth settings screen",
        "再確認する" to "Recheck Status",
        "ポート番号" to "Port Number",
        "受信側 (ゲーム) AndroidのIPアドレス" to "Receiver (Game) Android IP Address",
        "待受ポート" to "Listening Port",
        "太鼓のサイズ・位置設定" to "Drum Size & Position",
        "📏 太鼓のサイズ・位置設定" to "📏 Drum Size & Position",
        "⚡ Shizuku 直接入力設定" to "⚡ Shizuku Direct Input Settings",
        "📱 別のAndroid連携設定" to "📱 Another Android Connection Settings",
        "💻 PC有線/ADB接続設定" to "💻 PC Wired / ADB Settings",
        "🥁 打感・判定カスタマイズ" to "🥁 Hit Feel & Detection Customization",
        "💾 プリセット管理" to "💾 Preset Management",
        "⌨️ キー割り当て・入力設定" to "⌨️ Key Mapping & Input Settings",
        "🎨 テーマ・UI設定" to "🎨 Theme & UI Settings",
        "🌐 言語設定 (Language)" to "🌐 Language Settings",
        "言語 / Language" to "Language / 言語",
        "Androidのアプリ設定で言語を変更" to "Change Language in Android Settings",
        "デフォルトはシステムの言語設定に従います。Androidの「アプリ情報」>「言語」からいつでも切り替えられます。" to "Follows the system language by default. You can change this app's language anytime in Android's 'App Info' > 'Language'."
    )
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
