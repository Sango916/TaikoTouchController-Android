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

    # Verify ADB
    try:
        subprocess.run(["adb", "version"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    except FileNotFoundError:
        if os.path.exists("./platform-tools/adb"):
            adb_cmd = "./platform-tools/adb"
        else:
            log("ADB not found in PATH. Checking Homebrew or standalone tools...")
            installed = False
            try:
                # Homebrew does not require sudo
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

    log(f"Setting up ADB port forwarding (tcp:{PORT})...")
    subprocess.run([adb_cmd, "forward", "--remove", f"tcp:{PORT}"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    fwd_proc = subprocess.run([adb_cmd, "forward", f"tcp:{PORT}", f"tcp:{PORT}"], capture_output=True, text=True)
    if fwd_proc.returncode != 0:
        log(f"ADB forward message: {fwd_proc.stderr.strip() or fwd_proc.stdout.strip()}")
        log("Ensure your Android device is USB connected with USB debugging enabled.")

    log(f"Connecting to Android Taiko controller on localhost:{PORT}...")
    log("Note: Ensure Terminal/App has Accessibility permission in System Settings -> Privacy & Security -> Accessibility.")

    while True:
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            s.settimeout(3.0)
            s.connect(("127.0.0.1", PORT))
            
            f = s.makefile("r", encoding="utf-8")
            banner = f.readline()
            if not banner:
                s.close()
                log("Waiting for Android app response... (retrying in 2s)")
                time.sleep(2)
                continue

            s.settimeout(None)
            log("Connected successfully to Taiko App! Start your game now!")
            
            for line in f:
                line = line.strip()
                if not line:
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

            log("Disconnected by Android app. Reconnecting in 2 seconds...")
            s.close()
            time.sleep(2)
        except (socket.timeout, ConnectionRefusedError, OSError):
            log("Waiting for Android app connection... (retrying in 2s)")
            time.sleep(2)
        except KeyboardInterrupt:
            log("Exiting...")
            break
        except Exception as e:
            log(f"Error: {e}. Reconnecting in 2 seconds...")
            time.sleep(2)

if __name__ == "__main__":
    try:
        main()
    except Exception as err:
        print(f"Fatal Error: {err}", flush=True)
        traceback.print_exc()
        input("Press Enter to exit...")
