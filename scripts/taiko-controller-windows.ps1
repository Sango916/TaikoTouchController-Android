# Windows PC-side Receiver for Taiko Controller
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
    
    if (Test-Path ".\platform-tools\adb.exe") {
        $adbCmd = ".\platform-tools\adb.exe"
        Write-Host "Found local ADB in platform-tools folder." -ForegroundColor Green
        Add-PathToUserEnvironment ".\platform-tools"
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
            $output = ".\platform-tools.zip"
            try {
                Invoke-WebRequest -Uri $url -OutFile $output
                Expand-Archive -Path $output -DestinationPath "." -Force
                Remove-Item $output
                if (Test-Path ".\platform-tools\adb.exe") {
                    $adbCmd = ".\platform-tools\adb.exe"
                    Write-Host "ADB downloaded and extracted successfully!" -ForegroundColor Green
                    Add-PathToUserEnvironment ".\platform-tools"
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
    if (Test-Path ".\platform-tools\adb.exe") {
        Add-PathToUserEnvironment ".\platform-tools"
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
