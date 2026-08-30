# 太鼓タッチコントローラー (Taiko Touch Controller)

Android端末が太鼓コントローラーへ変身！  
Android端末上はもちろん、PCへの入力送信も可能です。

---

## 主な機能と特徴

- **直感的なタッチ操作と打感設定**
  - 大音符をDSやWii Uのように片手で叩ける(オンオフ切り替え可能)
  - 触覚フィードバック（バイブレーション）
  - 画面の向きに応じた太鼓サイズ・表示位置・大音符判定サイズのカスタマイズとプリセット保存

- **接続モード**
  - **この端末 (Shizuku)**: 同一端末上で動作し、Shizuku経由で他のゲームアプリへキーボードまたはゲームパッド入力を直接送信
  - **PC (USB有線接続)**: 端末とPCをUSB接続し、ADB経由でPC上のゲームへキー入力を送信
  - **別のAndroid端末 (有線 / 無線)**: 1台をコントローラー送信側、もう1台を受信側（ゲーム実行端末）としてUSBケーブルまたはWi-Fi経由で連携
  - **なし (ローカルデモ)**: 外部通信を行わず、画面上の叩き心地や設定の確認のみを行うモード

- **オーバーレイとフローティングバブル**
  - ゲーム画面の上にコントローラーや設定ボタンを重ねて表示するフローティングオーバーレイ機能

---

## 動作環境

- **アプリ対応バージョン**: Android 8.0 (API レベル 26) 以上
- **ターゲットSDK**: Android 14 (API レベル 34)
- **受信対応OS(PC)**: Windows, Linux, macOS (動作未確認)

---

## 動作確認環境

- **動作確認端末**:
  - Pixel 11 Pro XL (Android 17)
  - Pixel 7 Pro (Android 17)
  - Pixel 3a (Evolution X 10.9 Android 15)
  - Thor (Android 13)
- **動作確認OS(PC)**:
  - Windows 11 25H2
  - Ubuntu 26.04 LTS

---

## 開発環境・技術スタック

- **開発ツール**: Google AI Studio
- **言語**: Kotlin
- **UI フレームワーク**: Jetpack Compose (Material 3)
- **ビルドツール**: Gradle (Kotlin DSL)

### ビルドコマンド

```bash
# デバッグビルド (APKの生成)
./gradlew assembleDebug

# 単体テストの実行
./gradlew testDebugUnitTest
```

---

## ライセンス

本プロジェクトは MIT License のもとで公開されています。詳細は LICENSE ファイルをご確認ください。
