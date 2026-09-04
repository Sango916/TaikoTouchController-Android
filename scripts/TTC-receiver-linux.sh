#!/usr/bin/env python3
# Linux PC-side Receiver for Taiko Controller
# File name: TTC-receiver-linux.sh
# Usage: chmod +x TTC-receiver-linux.sh && ./TTC-receiver-linux.sh

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

def ensure_system_package(pkg_name_apt, pkg_name_dnf=None, pkg_name_pacman=None, pkg_name_zypper=None):
    # Attempt to install a system package using available package manager with sudo prompt
    if os.path.exists("/usr/bin/apt-get"):
        log(f"Installing {pkg_name_apt} via apt (administrator password may be requested)...")
        try:
            subprocess.run(["sudo", "apt-get", "update"], check=False)
            subprocess.run(["sudo", "apt-get", "install", "-y", pkg_name_apt], check=True)
            return True
        except Exception as e:
            log(f"apt-get install failed: {e}")
    elif os.path.exists("/usr/bin/dnf"):
        pkg = pkg_name_dnf or pkg_name_apt
        log(f"Installing {pkg} via dnf (administrator password may be requested)...")
        try:
            subprocess.run(["sudo", "dnf", "install", "-y", pkg], check=True)
            return True
        except Exception as e:
            log(f"dnf install failed: {e}")
    elif os.path.exists("/usr/bin/pacman"):
        pkg = pkg_name_pacman or pkg_name_apt
        log(f"Installing {pkg} via pacman (administrator password may be requested)...")
        try:
            subprocess.run(["sudo", "pacman", "-Sy", "--noconfirm", pkg], check=True)
            return True
        except Exception as e:
            log(f"pacman install failed: {e}")
    elif os.path.exists("/usr/bin/zypper"):
        pkg = pkg_name_zypper or pkg_name_apt
        log(f"Installing {pkg} via zypper (administrator password may be requested)...")
        try:
            subprocess.run(["sudo", "zypper", "--non-interactive", "in", pkg], check=True)
            return True
        except Exception as e:
            log(f"zypper install failed: {e}")
    return False

def main():
    log("=== Taiko Controller Receiver for Linux ===")
    
    adb_cmd = "adb"

    # 1. Verify and setup ADB
    try:
        subprocess.run(["adb", "version"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    except FileNotFoundError:
        if os.path.exists("./platform-tools/adb"):
            adb_cmd = "./platform-tools/adb"
        else:
            log("ADB not found in PATH. Attempting automatic installation...")
            installed = ensure_system_package(
                pkg_name_apt="adb",
                pkg_name_dnf="android-tools",
                pkg_name_pacman="android-tools",
                pkg_name_zypper="android-tools"
            )
            if installed:
                adb_cmd = "adb"
            else:
                log("Downloading official standalone Android SDK Platform Tools...")
                url = "https://dl.google.com/android/repository/platform-tools-latest-linux.zip"
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
                    log("Please install 'adb' or 'android-tools' via your package manager manually.")
                    return

    # 2. Key simulation library (pynput)
    try:
        from pynput.keyboard import Key, Controller
    except ImportError:
        log("pynput library not found. Installing system package...")
        installed = ensure_system_package(
            pkg_name_apt="python3-pynput",
            pkg_name_dnf="python3-pynput",
            pkg_name_pacman="python-pynput",
            pkg_name_zypper="python3-pynput"
        )
        if not installed:
            log("Trying pip install with --user flag...")
            try:
                subprocess.run([sys.executable, "-m", "pip", "install", "--user", "pynput"], check=True)
            except Exception as e:
                log(f"pip install failed: {e}")
                log("Please run: sudo apt install python3-pynput (or equivalent for your Linux distribution)")
                return
        try:
            from pynput.keyboard import Key, Controller
        except ImportError:
            log("Could not load pynput. Please restart the terminal or install python3-pynput.")
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
            log_bi(" Sending keys (D / F / J / K) to PC games in real-time.", "PCゲーム（太鼓ウェブ等）へキーをリアルタイム送信します。")
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
