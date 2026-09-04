# Windows PC-side Receiver for Taiko Controller
# File name: TTC-receiver-windows.ps1
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

try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch {}

function Log-Bi($color, [string]$en, [string]$jaB64 = "") {
    Write-Host $en -ForegroundColor $color
    if ($jaB64 -ne "") {
        try {
            $bytes = [System.Convert]::FromBase64String($jaB64)
            $ja = [System.Text.Encoding]::UTF8.GetString($bytes)
            Write-Host "  -> $ja" -ForegroundColor $color
        } catch {}
    }
}

Log-Bi Green "=== Taiko Controller Receiver for Windows ===" "5aSq6byT44Kz44Oz44OI44Ot44O844Op44O8IFdpbmRvd3PnlKjlj5fkv6Hjgrnjgq/jg6rjg5fjg4g="
Log-Bi Cyan "Initializing connection helper..." "5o6l57aa44OY44Or44OR44O844KS5Yid5pyf5YyW5LitLi4u"

function Ensure-AdbForward($targetPort) {
    # Check connected devices
    $rawDevices = & $adbCmd devices -l 2>&1
    $onlineSerials = @()
    $unauthorizedFound = $false

    foreach ($line in $rawDevices) {
        $trimmed = $line.Trim()
        if ($trimmed.Length -eq 0 -or $trimmed.StartsWith("List of devices")) { continue }
        if ($trimmed -match "^([^\s]+)\s+unauthorized") {
            $unauthorizedFound = $true
        } elseif ($trimmed -match "^([^\s]+)\s+device") {
            $onlineSerials += $matches[1]
        }
    }

    # If no devices found, attempt auto-connecting to WSA (Windows Subsystem for Android)
    if ($onlineSerials.Count -eq 0) {
        foreach ($wsaPort in @(58526, 5555)) {
            $connOut = & $adbCmd connect "127.0.0.1:$wsaPort" 2>&1
            if ($connOut -match "connected") {
                $onlineSerials += "127.0.0.1:$wsaPort"
                break
            }
        }
    }

    if ($onlineSerials.Count -eq 0) {
        if ($unauthorizedFound) {
            Log-Bi Yellow "[WAIT] Android device detected, but unauthorized." "QW5kcm9pZOerr+acq+OBjOaknOWHuuOBleOCjOOBvuOBl+OBn+OBjOOAgeacquioseWPr+OBp+OBmeOAgg=="
            Log-Bi Yellow "       Please unlock phone screen and tap 'Allow USB debugging'." "44K544Oe44Ob55S76Z2i44Gu44Ot44OD44Kv44KS6Kej6Zmk44GX44CB44CMVVNC44OH44OQ44OD44Kw44KS6Kix5Y+v44CN44KS44K/44OD44OX44GX44Gm44GP44Gg44GV44GE44CC"
        } else {
            Log-Bi Yellow "[WAIT] No Android device detected." "QW5kcm9pZOerr+acq+OBjOimi+OBpOOBi+OCiuOBvuOBm+OCk+OAgg=="
            Log-Bi Yellow "       1. Connect phone to PC via USB cable." "MS4g44K544Oe44Ob44KSVVNC44Kx44O844OW44Or44GnUEPjgavmjqXntprjgZfjgabjgY/jgaDjgZXjgYTjgII="
            Log-Bi Yellow "       2. Enable 'USB debugging' in Developer options." "Mi4g56uv5pyr44Gu6ZaL55m66ICF5ZCR44GR44Kq44OX44K344On44Oz44Gn44CMVVNC44OH44OQ44OD44Kw44CN44KST07jgavjgZfjgabjgY/jgaDjgZXjgYTjgII="
        }
        return $false
    }

    # Pick best target serial
    $chosenSerial = $onlineSerials[0]
    
    # Remove existing forward cleanly
    try { & $adbCmd -s $chosenSerial forward --remove "tcp:$targetPort" 2>$null } catch {}

    # Forward port to chosen device
    $fwdOut = & $adbCmd -s $chosenSerial forward "tcp:$targetPort" "tcp:$targetPort" 2>&1
    if ($LASTEXITCODE -eq 0) {
        return $true
    } else {
        Write-Host "[NOTICE] ADB Port Forwarding notice: $fwdOut" -ForegroundColor Yellow
        return $false
    }
}

Log-Bi Cyan "Ready. Starting auto-connection loop (target port: $port)..." "5rqW5YKZ5a6M5LqG44CC6Ieq5YuV5o6l57aa44Or44O844OX44KS6ZaL5aeL44GX44G+44GZ"

$hasAnnouncedWaiting = $false
$lastForwardOk = $false

while ($true) {
    $client = $null
    try {
        # Check and ensure ADB port forward is active
        $forwardOk = Ensure-AdbForward -targetPort $port
        if (!$forwardOk) {
            $lastForwardOk = $false
            Start-Sleep -Seconds 2
            continue
        }

        if (!$lastForwardOk) {
            Log-Bi Green "[OK] ADB port forwarding active (port $port)." "QURC44Od44O844OI44OV44Kp44Ov44O844OJ56K656uLICjjg53jg7zjg4ggJHBvcnQp"
            Log-Bi Cyan "[INFO] In Android app, select 'PC Connection (USB)' mode..." "44K544Oe44Ob44Ki44OX44Oq44Gn44CMUEPmjqXntpogKFVTQinjgI3jg6Ljg7zjg4njgpLpgbjmip7jgZfjgabjgY/jgaDjgZXjgYQuLi4="
            $lastForwardOk = $true
        }

        # Attempt TCP connection to Android app via forwarded localhost port
        $client = New-Object System.Net.Sockets.TcpClient
        $connectResult = $client.BeginConnect("127.0.0.1", $port, $null, $null)
        $success = $connectResult.AsyncWaitHandle.WaitOne(2000, $false)
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
            Start-Sleep -Seconds 2
            continue
        }

        $stream.ReadTimeout = -1
        Write-Host ""
        Write-Host "==========================================================" -ForegroundColor Green
        Log-Bi Green " *** Taiko Controller Connected Successfully! ***" "4piF4piF4piFIOWkqum8k+OCs+ODs+ODiOODreODvOODqeODvCAo44Ki44OX44OqKSDjgajmjqXntprlrozkuobvvIEg4piF4piF4piF"
        Log-Bi Green " Sending keys (D / F / J / K) to PC games in real-time." "UEPjgrLjg7zjg6DvvIjlpKrpvJPjgqbjgqfjg5bnrYnvvInjgbjjgq3jg7zjgpLjg6rjgqLjg6vjgr/jgqTjg6DpgIHkv6HjgZfjgb7jgZnjgII="
        Write-Host "==========================================================" -ForegroundColor Green
        Write-Host ""
        
        while ($client.Connected) {
            $line = $reader.ReadLine()
            if ($null -eq $line) {
                Log-Bi Yellow "[INFO] Connection closed by Android app. Waiting to reconnect..." "44Ki44OX44Oq44Go44Gu5o6l57aa44GM5YiH5pat44GV44KM44G+44GX44Gf44CC5YaN5o6l57aa5b6F5qmf5LitLi4u"
                break
            }
            
            $line = $line.Trim()
            if ($line.Length -eq 0 -or $line -eq "PING") { continue }

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
        # Silent wait or periodic status
    } finally {
        if ($null -ne $client) {
            try { $client.Close() } catch {}
        }
    }
    Start-Sleep -Seconds 2
}
