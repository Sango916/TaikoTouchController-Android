import base64

py_script = """#!/usr/bin/env python3
# macOS / Linux PC-side Receiver for Taiko Controller
# File extension to save as: controller.command (or controller.py / controller.sh)

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
    log("=== Taiko Controller Receiver for macOS / Linux ===")
    
    adb_cmd = "adb"

    # Verify ADB
    try:
        subprocess.run(["adb", "version"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    except FileNotFoundError:
        if os.path.exists("./platform-tools/adb"):
            adb_cmd = "./platform-tools/adb"
        else:
            log("ADB not found in PATH. Checking local platform-tools...")
            try:
                log("Trying to install android-platform-tools via Homebrew/apt...")
                if sys.platform == "darwin":
                    subprocess.run(["brew", "install", "android-platform-tools"], check=True)
                    adb_cmd = "adb"
                elif os.path.exists("/usr/bin/apt-get"):
                    subprocess.run(["sudo", "apt-get", "install", "-y", "adb"], check=True)
                    adb_cmd = "adb"
                else:
                    raise FileNotFoundError()
            except Exception:
                log("Downloading official Android SDK Platform Tools...")
                if sys.platform == "darwin":
                    url = "https://dl.google.com/android/repository/platform-tools-latest-darwin.zip"
                else:
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
                    log("Please install ADB manually.")
                    return

    # Key simulation libraries
    try:
        from pynput.keyboard import Key, Controller
    except ImportError:
        log("Installing pynput library for keyboard simulation...")
        subprocess.check_call([sys.executable, "-m", "pip", "install", "pynput"])
        from pynput.keyboard import Key, Controller

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
    if sys.platform == "darwin":
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
"""

ps_script = """# Windows PC-side Receiver for Taiko Controller
# File extension to save as: controller.ps1
# Usage: Right-click the saved file and select "Run with PowerShell"

$port = 60001
$adbCmd = "adb"

function Add-PathToUserEnvironment($dirToAdd) {
    try {
        $resolvedDir = (Resolve-Path $dirToAdd).Path
        $userPath = [System.Environment]::GetEnvironmentVariable("Path", "User")
        if ($null -eq $userPath) { $userPath = "" }
        $paths = $userPath.Split(';', [System.StringSplitOptions]::RemoveEmptyEntries)
        if ($paths -notcontains $resolvedDir) {
            Write-Host "Registering ADB to User PATH: $resolvedDir" -ForegroundColor Cyan
            $newPath = if ($userPath.Trim().Length -gt 0) { "$userPath;$resolvedDir" } else { $resolvedDir }
            [System.Environment]::SetEnvironmentVariable("Path", $newPath, "User")
            $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
            Write-Host "ADB successfully added to PATH!" -ForegroundColor Green
        }
    } catch {
        Write-Host "Note: Could not automatically update User PATH: $_" -ForegroundColor Yellow
    }
}

# Check if adb is in PATH
if (!(Get-Command adb -ErrorAction SilentlyContinue)) {
    Write-Host "ADB is not in PATH. Checking local platform-tools..." -ForegroundColor Yellow
    
    if (Test-Path ".\\platform-tools\\adb.exe") {
        $adbCmd = ".\\platform-tools\\adb.exe"
        Write-Host "Found local ADB in platform-tools folder." -ForegroundColor Green
        Add-PathToUserEnvironment ".\\platform-tools"
    } else {
        Write-Host "Trying to install ADB via winget..." -ForegroundColor Cyan
        if (Get-Command winget -ErrorAction SilentlyContinue) {
            try {
                winget install Google.Adb --silent --accept-source-agreements --accept-package-agreements | Out-Null
                $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
            } catch {}
        }

        if (Get-Command adb -ErrorAction SilentlyContinue) {
            $adbCmd = "adb"
            Write-Host "ADB installed via winget successfully!" -ForegroundColor Green
        } else {
            Write-Host "winget unavailable or pending. Downloading official Android SDK Platform Tools..." -ForegroundColor Yellow
            $url = "https://dl.google.com/android/repository/platform-tools-latest-windows.zip"
            $output = ".\\platform-tools.zip"
            try {
                Invoke-WebRequest -Uri $url -OutFile $output
                Expand-Archive -Path $output -DestinationPath "." -Force
                Remove-Item $output
                if (Test-Path ".\\platform-tools\\adb.exe") {
                    $adbCmd = ".\\platform-tools\\adb.exe"
                    Write-Host "ADB downloaded and extracted successfully!" -ForegroundColor Green
                    Add-PathToUserEnvironment ".\\platform-tools"
                } else {
                    Write-Host "Error: Failed to extract platform-tools." -ForegroundColor Red
                    Pause
                    Exit
                }
            } catch {
                Write-Host "Error: Could not download ADB. Please install ADB or platform-tools manually." -ForegroundColor Red
                Pause
                Exit
            }
        }
    }
} else {
    if (Test-Path ".\\platform-tools\\adb.exe") {
        Add-PathToUserEnvironment ".\\platform-tools"
    }
}

# Add C# helper for Win32 low-latency key events with DirectX/DirectInput Hardware ScanCode support
if (!("TaikoKeyboard" -as [type])) {
    $Signature = @"
using System;
using System.Runtime.InteropServices;

public class TaikoKeyboard {
    [DllImport("user32.dll")]
    public static extern void keybd_event(byte bVk, byte bScan, uint dwFlags, UIntPtr dwExtraInfo);

    [DllImport("user32.dll")]
    public static extern uint MapVirtualKey(uint uCode, uint uMapType);

    [DllImport("user32.dll", SetLastError = true)]
    public static extern uint SendInput(uint nInputs, IntPtr pInputs, int cbSize);

    private const uint KEYEVENTF_KEYUP = 0x0002;

    private static void SendDownRaw(byte vkey, ushort scanCode) {
        keybd_event(vkey, (byte)scanCode, 0, UIntPtr.Zero);

        try {
            int cbSize = (IntPtr.Size == 8) ? 40 : 28;
            byte[] inputBytes = new byte[cbSize];
            BitConverter.GetBytes((uint)1).CopyTo(inputBytes, 0); // INPUT_KEYBOARD
            int kiOffset = (IntPtr.Size == 8) ? 8 : 4;
            BitConverter.GetBytes(scanCode).CopyTo(inputBytes, kiOffset + 2); // wScan
            BitConverter.GetBytes((uint)0x0008).CopyTo(inputBytes, kiOffset + 4); // KEYEVENTF_SCANCODE
            GCHandle handle = GCHandle.Alloc(inputBytes, GCHandleType.Pinned);
            SendInput(1, handle.AddrOfPinnedObject(), cbSize);
            handle.Free();
        } catch {}
    }

    private static void SendUpRaw(byte vkey, ushort scanCode) {
        keybd_event(vkey, (byte)scanCode, KEYEVENTF_KEYUP, UIntPtr.Zero);

        try {
            int cbSize = (IntPtr.Size == 8) ? 40 : 28;
            byte[] inputBytes = new byte[cbSize];
            BitConverter.GetBytes((uint)1).CopyTo(inputBytes, 0); // INPUT_KEYBOARD
            int kiOffset = (IntPtr.Size == 8) ? 8 : 4;
            BitConverter.GetBytes(scanCode).CopyTo(inputBytes, kiOffset + 2); // wScan
            BitConverter.GetBytes((uint)0x000a).CopyTo(inputBytes, kiOffset + 4); // KEYEVENTF_SCANCODE | KEYEVENTF_KEYUP
            GCHandle handle = GCHandle.Alloc(inputBytes, GCHandleType.Pinned);
            SendInput(1, handle.AddrOfPinnedObject(), cbSize);
            handle.Free();
        } catch {}
    }

    public static void Down(byte vkey) {
        ushort scanCode = (ushort)MapVirtualKey(vkey, 0);
        SendDownRaw(vkey, scanCode);
    }

    public static void Up(byte vkey) {
        ushort scanCode = (ushort)MapVirtualKey(vkey, 0);
        SendUpRaw(vkey, scanCode);
    }
}
"@
    try {
        Add-Type -TypeDefinition $Signature -ErrorAction Stop
    } catch {
        Write-Host "Failed to compile keyboard helper: $_" -ForegroundColor Red
    }
}

Write-Host "=== Taiko Controller Receiver for Windows ===" -ForegroundColor Green
Write-Host "Setting up ADB port forwarding (tcp:$port)..." -ForegroundColor Cyan
try { & $adbCmd forward --remove tcp:$port 2>$null } catch {}

$fwdOut = & $adbCmd forward tcp:$port tcp:$port 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "Notice from ADB: $fwdOut" -ForegroundColor Yellow
    Write-Host "Ensure Android device is connected via USB and USB debugging is enabled!" -ForegroundColor Yellow
}

Write-Host "Connecting to Android Taiko controller on localhost:$port..." -ForegroundColor Cyan

while ($true) {
    $client = $null
    try {
        $client = New-Object System.Net.Sockets.TcpClient
        $connectResult = $client.BeginConnect("127.0.0.1", $port, $null, $null)
        $success = $connectResult.AsyncWaitHandle.WaitOne(3000, $false)
        if (!$success) {
            $client.Close()
            throw "Connection timeout"
        }
        $client.EndConnect($connectResult)

        $stream = $client.GetStream()
        $stream.ReadTimeout = 3000
        $reader = New-Object System.IO.StreamReader($stream)

        # Read banner to verify real app connection
        $banner = $reader.ReadLine()
        if ($null -eq $banner) {
            $client.Close()
            Write-Host "Waiting for Android app connection... (retrying in 2 seconds)" -ForegroundColor Yellow
            Start-Sleep -Seconds 2
            continue
        }

        $stream.ReadTimeout = -1
        Write-Host "Connected successfully to Taiko App! Start your game now!" -ForegroundColor Green
        
        while ($client.Connected) {
            $line = $reader.ReadLine()
            if ($null -eq $line) {
                Write-Host "Disconnected by Android app." -ForegroundColor Yellow
                break
            }
            
            $line = $line.Trim()
            if ($line.Length -eq 0) { continue }

            $parts = $line.Split(' ')
            if ($parts.Length -ge 2) {
                $action = $parts[0]
                for ($i = 1; $i -lt $parts.Length; $i++) {
                    $key = $parts[$i].ToUpper()
                    if ($key.Length -gt 0) {
                        Write-Host "[KEY] $action -> $key" -ForegroundColor Cyan
                        $vkey = [byte][char]$key[0]
                        
                        if ($action -eq "DOWN") {
                            [TaikoKeyboard]::Down($vkey)
                        } elseif ($action -eq "UP") {
                            [TaikoKeyboard]::Up($vkey)
                        }
                    }
                }
            }
        }
    } catch {
        Write-Host "Waiting for Android app connection... (retrying in 2 seconds)" -ForegroundColor Yellow
    } finally {
        if ($null -ne $client) {
            try { $client.Close() } catch {}
        }
    }
    Start-Sleep -Seconds 2
}
"""

with open("py_b64.txt", "w") as f:
    f.write(base64.b64encode(py_script.encode("utf-8")).decode("ascii"))

with open("ps_b64.txt", "w") as f:
    f.write(base64.b64encode(ps_script.encode("utf-8")).decode("ascii"))

print("SUCCESS")
