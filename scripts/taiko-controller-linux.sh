#!/usr/bin/env python3
# Linux PC-side Receiver for Taiko Controller
# File extension to save as: controller.sh (or controller.py)
# Usage: chmod +x controller.sh && ./controller.sh

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

    log(f"Setting up ADB port forwarding (tcp:{PORT})...")
    subprocess.run([adb_cmd, "forward", "--remove", f"tcp:{PORT}"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    fwd_proc = subprocess.run([adb_cmd, "forward", f"tcp:{PORT}", f"tcp:{PORT}"], capture_output=True, text=True)
    if fwd_proc.returncode != 0:
        log(f"ADB forward message: {fwd_proc.stderr.strip() or fwd_proc.stdout.strip()}")
        log("Ensure your Android device is USB connected with USB debugging enabled!")

    log(f"Connecting to Android Taiko controller on localhost:{PORT}...")

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
