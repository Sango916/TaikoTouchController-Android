# 太鼓タッチコントローラー (Taiko Touch Controller)

Android端末（AYN Thor等の2画面デバイスや一般的なスマートフォン・タブレット）を、PCや他のエミュレータ・ゲーム向けの超低遅延な「太鼓コントローラー」として利用できるようにするAndroidアプリケーションです。

---

## 🥁 主な特徴・機能

- **直感的なタッチ操作 & 打感カスタマイズ**
  - ドン（面）/ カッ（フチ）の左右叩き分けに対応
  - 大音符の片手同時押しアシスト機能
  - 打撃時の触覚フィードバック（バイブレーション）および内蔵サウンドエフェクト
  - ドラムのサイズ・位置・角度・左右分離幅の自由なレイアウト調整
  - 低スペック端末向けの「描画軽量モード」搭載（波紋エフェクトOFFでCPU/GPU負荷を削減）

- **多彩な接続モード（PC・ローカル・外部端末対応）**
  - ⚡ **USB有線接続 (ADB Port Forwarding)**: root化不要でPC（Windows / macOS / Linux）へ超低遅延にキーボード入力を送信
  - 🌐 **ネットワーク接続 (WebSocket / TCP)**: Wi-Fi経由でPCやブラウザ版シミュレータへ入力を送信
  - 🛡️ **Shizuku / root 連携**: Android端末単体でバックグラウンドから他アプリへタッチ・キー入力をインジェクション
  - 💬 **フローティングバブル（オーバーレイ）**: 他のゲーム画面の上に最小化バブルや設定メニューを常駐表示

- **充実したPC受信側スクリプト（全OS自動セットアップ対応）**
  - アプリ内の「USB PC接続設定」からワンクリックでスクリプトをコピー可能
  - 必要なツール（ADB、pynput等）の自動ダウンロード・導入機能を内蔵

---

## 🖥️ PC連携・セットアップ手順 (USB有線接続)

PC上の太鼓ゲーム（シミュレータや各種PCゲーム）でコントローラーとして使用する場合の手順です。

### 1. 端末側の準備
1. Android端末の「開発者向けオプション」で **USBデバッグ** を有効化します。
2. USBケーブルでAndroid端末とPCを接続します。
3. 本アプリを起動し、接続モードで **「USB有線接続」** を選択します。

### 2. PC側スクリプトの実行

アプリ内の「💻 USB PC接続設定」カードから、お使いのOSに合わせてスクリプトをコピーして保存・実行します。

#### 🪟 Windows の場合
1. アプリ内の「Windows (.ps1)」タブからスクリプトをコピーします。
2. PC上で `controller.ps1` というファイル名で保存します。
3. `controller.ps1` を右クリックして **「PowerShell で実行」** を選択します。

#### 🍎 macOS の場合
1. アプリ内の「macOS (.command)」タブからスクリプトをコピーします。
2. PC上で `controller.command` というファイル名で保存します。
3. 初回のみターミナルで実行権限を付与し、起動します：
   ```bash
   chmod +x controller.command
   ./controller.command
   ```
   > **※ macOSの権限について**: キー入力を送信するため、初回実行時にシステム設定の「プライバシーとセキュリティ」→「アクセシビリティ」からターミナルをONに許可してください。

#### 🐧 Linux の場合
1. アプリ内の「Linux (.sh)」タブからスクリプトをコピーします。
2. PC上で `controller.sh` というファイル名で保存します。
3. 実行権限を付与して起動します：
   ```bash
   chmod +x controller.sh
   ./controller.sh
   ```
   > ※ `python3-pynput` や `adb` が未導入の場合、スクリプト実行中にパッケージマネージャ用の管理者パスワード（sudo）が求められ、自動インストールされます。

---

## 🛠️ 開発・ビルド環境

- **言語**: Kotlin
- **UI フレームワーク**: Jetpack Compose (Material 3)
- **ビルドツール**: Gradle (Kotlin DSL)
- **推奨開発環境**: Android Studio Ladybug 以降

### ビルド手順

```bash
# デバッグビルド (APK生成)
./gradlew assembleDebug

# ユニットテスト実行
./gradlew testDebugUnitTest
```

---

## 📄 ライセンス
This project is licensed under the MIT License - see the LICENSE file for details.
