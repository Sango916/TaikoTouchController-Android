#!/usr/bin/env python3
# macOS PC-side Receiver for Taiko Controller
# File name: TTC-receiver-macos.command
# Usage: chmod +x TTC-receiver-macos.command && ./TTC-receiver-macos.command

import socket
import subprocess
import time
import sys
import os
import urllib.request
import zipfile
import traceback

PORT = 60001

def log(msg):
    print(msg, flush=True)

def main():
    log("=== Taiko Controller Receiver for macOS ===")
    
    adb_cmd = "adb"

    # Verify and check ADB updates
    try:
        subprocess.run(["adb", "version"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    except FileNotFoundError:
        if os.path.exists("./platform-tools/adb"):
            adb_cmd = "./platform-tools/adb"
        else:
            log("ADB not found in PATH. Checking Homebrew or standalone tools...")
            installed = False
            try:
                log("Trying to install android-platform-tools via Homebrew...")
                res = subprocess.run(["brew", "install", "android-platform-tools"], check=False)
                if res.returncode == 0:
                    installed = True
                    adb_cmd = "adb"
            except Exception:
                pass
            
            if not installed:
                log("Downloading official Android SDK Platform Tools...")
                url = "https://dl.google.com/android/repository/platform-tools-latest-darwin.zip"
                zip_path = "./platform-tools.zip"
                try:
                    urllib.request.urlretrieve(url, zip_path)
                    with zipfile.ZipFile(zip_path, "r") as zip_ref:
                        zip_ref.extractall(".")
                    if os.path.exists(zip_path):
                        os.remove(zip_path)
                    os.chmod("./platform-tools/adb", 0o755)
                    adb_cmd = "./platform-tools/adb"
                    log("ADB downloaded and extracted successfully to ./platform-tools/")
                except Exception as e:
                    log(f"Error downloading platform-tools: {e}")
                    log("Please install ADB manually.")
                    return

    # Check for ADB updates
    try:
        log("[ADB] Checking for ADB updates...")
        if os.path.exists("./platform-tools/adb"):
            url = "https://dl.google.com/android/repository/platform-tools-latest-darwin.zip"
            etag_file = "./platform-tools/.etag"
            saved_etag = ""
            if os.path.exists(etag_file):
                try:
                    with open(etag_file, "r", encoding="utf-8") as f:
                        saved_etag = f.read().strip()
                except Exception:
                    pass
            req = urllib.request.Request(url, method="HEAD")
            with urllib.request.urlopen(req, timeout=4) as resp:
                remote_etag = resp.headers.get("ETag") or resp.headers.get("Last-Modified")
            if remote_etag and saved_etag and remote_etag == saved_etag:
                log("[ADB] Local platform-tools is already up to date.")
            elif remote_etag:
                log("[ADB] New version available. Updating ADB platform-tools...")
                subprocess.run([adb_cmd, "kill-server"], check=False, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
                zip_path = "./platform-tools-update.zip"
                urllib.request.urlretrieve(url, zip_path)
                with zipfile.ZipFile(zip_path, "r") as zip_ref:
                    zip_ref.extractall(".")
                if os.path.exists(zip_path):
                    os.remove(zip_path)
                os.chmod("./platform-tools/adb", 0o755)
                with open(etag_file, "w", encoding="utf-8") as f:
                    f.write(remote_etag)
                log("[ADB] Platform-tools updated successfully!")
                adb_cmd = "./platform-tools/adb"
        else:
            try:
                # If installed via Homebrew, check outdated
                out = subprocess.run(["brew", "outdated", "android-platform-tools"], capture_output=True, text=True, check=False)
                if out.returncode == 0 and "android-platform-tools" in out.stdout:
                    log("[ADB] Updating android-platform-tools via Homebrew...")
                    subprocess.run([adb_cmd, "kill-server"], check=False, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
                    subprocess.run(["brew", "upgrade", "android-platform-tools"], check=False)
                    log("[ADB] Updated via Homebrew!")
                else:
                    log("[ADB] ADB is up to date.")
            except Exception:
                log("[ADB] ADB is active and ready.")
    except Exception:
        log("[ADB] Update check finished.")

    # Key simulation libraries
    try:
        from pynput.keyboard import Key, Controller
    except ImportError:
        log("Installing pynput library for keyboard simulation...")
        installed = False
        try:
            subprocess.run([sys.executable, "-m", "pip", "install", "--user", "pynput"], check=True)
            installed = True
        except Exception:
            try:
                subprocess.run([sys.executable, "-m", "pip", "install", "pynput"], check=True)
                installed = True
            except Exception:
                pass
        try:
            from pynput.keyboard import Key, Controller
        except ImportError:
            log("Could not load pynput. Please run: pip3 install --user pynput")
            return

    keyboard = Controller()

    def send_down(key_char):
        try:
            keyboard.press(key_char)
        except Exception as e:
            log(f"Key press error: {e}")

    def send_up(key_char):
        try:
            keyboard.release(key_char)
        except Exception as e:
            log(f"Key release error: {e}")

    def log_bi(en, ja=None):
        log(en)
        if ja:
            log(f"  -> {ja}")

    def reset_adb_server(reason=""):
        if reason:
            log_bi(f"[ADB] Resetting ADB server: {reason} (adb kill-server)...", f"ADBサーバーを再起動中: {reason} (adb kill-server)...")
        try:
            subprocess.run([adb_cmd, "kill-server"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            time.sleep(0.4)
            subprocess.run([adb_cmd, "start-server"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            time.sleep(0.4)
        except Exception:
            pass

    log_bi("[ADB] Initializing clean ADB server state (adb kill-server)...", "クリーンなADBサーバーを初期化中 (adb kill-server)...")
    reset_adb_server("Startup initialization")

    consecutive_waits = 0

    def ensure_adb_forward():
        nonlocal consecutive_waits
        try:
            # Check devices
            res = subprocess.run([adb_cmd, "devices", "-l"], capture_output=True, text=True)
            lines = res.stdout.strip().splitlines()
            online_serials = []
            unauthorized = False
            offline = False
            for line in lines[1:]:
                line = line.strip()
                if not line:
                    continue
                if "unauthorized" in line:
                    unauthorized = True
                elif "offline" in line:
                    offline = True
                elif "device" in line:
                    parts = line.split()
                    if len(parts) > 0:
                        online_serials.append(parts[0])

            if offline and not online_serials:
                log_bi("[ADB] Device in offline state. Resetting ADB server...", "オフライン端末を検出。ADBサーバーをリフレッシュ中...")
                reset_adb_server("Offline device recovery")
                return False

            if not online_serials:
                consecutive_waits += 1
                if consecutive_waits >= 3:
                    log_bi("[ADB] Device not detected after reconnect. Resetting ADB (adb kill-server)...", "端末が認識されないか切断されました。adb kill-server を実行して再試行中...")
                    reset_adb_server("Reconnect retry")
                    consecutive_waits = 0
                    return False

                if unauthorized:
                    log_bi("[WAIT] Android device detected, but unauthorized.", "Android端末が検出されましたが、未許可です。画面ロックを解除して「USBデバッグを許可」をタップしてください。")
                else:
                    log_bi("[WAIT] No Android device detected.", "Android端末が見つかりません。USBケーブル接続と「USBデバッグ」の有効化を確認してください。")
                return False

            consecutive_waits = 0
            chosen_serial = online_serials[0]
            subprocess.run([adb_cmd, "-s", chosen_serial, "forward", "--remove", f"tcp:{PORT}"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            fwd = subprocess.run([adb_cmd, "-s", chosen_serial, "forward", f"tcp:{PORT}", f"tcp:{PORT}"], capture_output=True, text=True)
            return fwd.returncode == 0
        except Exception:
            return False

    log_bi(f"Ready. Starting auto-connection loop (target port: {PORT})...", f"準備完了。自動接続ループを開始します (ポート: {PORT})...")
    log_bi("Note: Ensure Terminal/App has Accessibility permission in System Settings -> Privacy & Security -> Accessibility.", "注意: 初回は「システム設定 -> プライバシーとセキュリティ -> アクセシビリティ」でターミナルのキー送信許可が必要です。")
    last_fwd_ok = False

    while True:
        try:
            if not ensure_adb_forward():
                last_fwd_ok = False
                time.sleep(2)
                continue

            if not last_fwd_ok:
                log_bi(f"[OK] ADB port forwarding active (port {PORT}).", f"ADBポートフォワード確立 (ポート {PORT})。スマホアプリで「PC接続 (USB)」モードを開いてください。")
                last_fwd_ok = True

            s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            s.settimeout(2.0)
            s.connect(("127.0.0.1", PORT))
            
            f = s.makefile("r", encoding="utf-8")
            banner = f.readline()
            if not banner:
                s.close()
                time.sleep(2)
                continue

            s.settimeout(None)
            log("==========================================================")
            log_bi(" *** Taiko Controller Connected Successfully! ***", "★★★ 太鼓コントローラー (アプリ) と接続完了！ ★★★")
            log_bi(" Sending keys (D / F / J / K) to PC games in real-time.", "PCゲームへキーをリアルタイム送信します。")
            log("==========================================================")
            
            for line in f:
                line = line.strip()
                if not line or line == "PING":
                    continue
                parts = line.split(" ")
                if len(parts) >= 2:
                    action = parts[0]
                    for key_str in parts[1:]:
                        key_char = key_str.lower()
                        if not key_char:
                            continue
                        log(f"[KEY] {action} -> {key_char}")
                        if action == "DOWN":
                            send_down(key_char)
                        elif action == "UP":
                            send_up(key_char)

            log_bi("[INFO] Connection closed by Android app. Waiting to reconnect...", "アプリとの接続が切断されました。再接続待機中...")
            s.close()
            reset_adb_server("Reconnection cleanup")
            time.sleep(2)
        except (socket.timeout, ConnectionRefusedError, OSError):
            time.sleep(2)
        except KeyboardInterrupt:
            log("Exiting...")
            break
        except Exception as e:
            time.sleep(2)

if __name__ == "__main__":
    try:
        main()
    except Exception as err:
        print(f"Fatal Error: {err}", flush=True)
        traceback.print_exc()
        input("Press Enter to exit...")
