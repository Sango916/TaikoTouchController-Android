# Windows PC-side Receiver for Taiko Controller
# File name: TTC-receiver-windows.ps1
# Usage: Right-click the saved file and select "Run with PowerShell"

param(
    [int]$port = 60001,
    [string]$adbTarget = ""
)

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

function Reset-AdbServer([string]$reason = "") {
    if ($reason -ne "") {
        Log-Bi Yellow "[ADB] Resetting ADB server: $reason (adb kill-server)..." "44Kv44Oq44O844Oz44GqQURC54q25oWL44KS5L2c5oiQ44GZ44KL44Gf44KB44CBQURC44K144O844OQ44O844KS5YaN6LW35YuV44GX44G+44GZLi4u"
    }
    try {
        & $adbCmd kill-server 2>$null
        Start-Sleep -Milliseconds 400
        & $adbCmd start-server 2>$null
        Start-Sleep -Milliseconds 400
    } catch {}
}

# 1. Clean up any rogue or locked ADB state at startup
Log-Bi Cyan "[ADB] Initializing clean ADB server state (adb kill-server)..." "44Kv44Oq44O844Oz44GqQURC44K144O844OQ44O844KS5Yid5pyf5YyW5LitIChhZGIga2lsbC1zZXJ2ZXIpLi4u"
Reset-AdbServer "Initial startup"

$script:consecutiveWaitCount = 0

function Ensure-AdbForward($targetPort) {
    # Check connected devices
    $rawDevices = & $adbCmd devices -l 2>&1
    $onlineSerials = @()
    $unauthorizedFound = $false
    $offlineFound = $false

    foreach ($line in $rawDevices) {
        $trimmed = $line.Trim()
        if ($trimmed.Length -eq 0 -or $trimmed.StartsWith("List of devices")) { continue }
        if ($trimmed -match "^([^\s]+)\s+unauthorized") {
            $unauthorizedFound = $true
        } elseif ($trimmed -match "^([^\s]+)\s+offline") {
            $offlineFound = $true
        } elseif ($trimmed -match "^([^\s]+)\s+device") {
            $onlineSerials += $matches[1]
        }
    }

    # If offline device detected, reset ADB connection
    if ($offlineFound -and $onlineSerials.Count -eq 0) {
        Log-Bi Yellow "[ADB] Device in offline state. Resetting ADB server..." "44Kq44OV44Op44Kk44Oz56uv5pyr44KS5qSc5Ye644CCQURC44K144O844OQ44O844KS44Oq44OV44Os44OD44K344Ol5LitLi4u"
        Reset-AdbServer "Offline device recovery"
        return $false
    }

    # If user passed an explicit adb target (e.g. -adbTarget "127.0.0.1:58526")
    if ($adbTarget -ne "" -and $onlineSerials -notcontains $adbTarget) {
        $connOut = & $adbCmd connect $adbTarget 2>&1
        if ($connOut -match "connected to") {
            $onlineSerials += $adbTarget
        }
    }

    # If no devices found, attempt auto-connecting to WSA (Windows Subsystem for Android)
    if ($onlineSerials.Count -eq 0) {
        $wsaRunning = Get-Process -Name "WsaClient","WsaService","vmmemWSA" -ErrorAction SilentlyContinue

        # Standard WSA ports: 58526 (standard), 5555 (default ADB)
        $candidateWsaPorts = @(58526, 5555)
        try {
            $listeners = Get-NetTCPConnection -State Listen -LocalAddress "127.0.0.1" -ErrorAction SilentlyContinue |
                Where-Object { ($_.LocalPort -ge 58520 -and $_.LocalPort -le 58535) } |
                Select-Object -ExpandProperty LocalPort -Unique
            foreach ($p in $listeners) {
                if ($candidateWsaPorts -notcontains $p) {
                    $candidateWsaPorts = @($p) + $candidateWsaPorts
                }
            }
        } catch {}

        # Fast TCP pre-check (80ms): only call 'adb connect' if the port is actually open
        # This completely avoids blocking for 5-10 seconds per port!
        $wsaConnected = $false
        foreach ($wsaPort in $candidateWsaPorts) {
            $isOpen = $false
            try {
                $tcpTest = New-Object System.Net.Sockets.TcpClient
                $iar = $tcpTest.BeginConnect("127.0.0.1", $wsaPort, $null, $null)
                if ($iar.AsyncWaitHandle.WaitOne(80, $false)) {
                    $tcpTest.EndConnect($iar)
                    $isOpen = $true
                }
                $tcpTest.Close()
            } catch {}

            if ($isOpen) {
                $targetEndpoint = "127.0.0.1:$wsaPort"
                $connOut = & $adbCmd connect $targetEndpoint 2>&1
                if ($connOut -match "connected to" -and $connOut -notmatch "cannot connect" -and $connOut -notmatch "failed") {
                    $onlineSerials += $targetEndpoint
                    $wsaConnected = $true
                    Log-Bi Green "[WSA] Successfully connected to WSA ADB ($targetEndpoint)." "V1NBIEFEQuOBqOOBruaOpee2muOBq+aIkOWKn+OBl+OBvuOBl+OBn+OAgiA="
                    break
                }
            }
        }

        if (!$wsaConnected -and $null -ne $wsaRunning -and $wsaRunning.Count -gt 0) {
            Log-Bi Yellow "[WSA] WSA (Windows Subsystem for Android) detected, but ADB port is not open." "V2luZG93cyBTdWJzeXN0ZW0gZm9yIEFuZHJvaWQgKFdTQSkg44KS5qSc5Ye644GX44G+44GX44Gf44GM44CBQURC44Od44O844OI44GM6ZaL44GE44Gm44GE44G+44Gb44KT44CC"
            Log-Bi Yellow "      Open 'Windows Subsystem for Android Settings' -> 'Developer' -> enable 'Developer mode'." "V1NB6Kit5a6a44Ki44OX44Oq44Gu44CM6ZaL55m66ICF44CN44K/44OW44Gn44CM6ZaL55m66ICF44Oi44OD44OJ44CN44KS44Kq44Oz44Gr44GX44Gm44GP44Gg44GV44GE44CC"
        }
    }

    if ($onlineSerials.Count -eq 0) {
        $script:consecutiveWaitCount++
        # If waiting repeatedly without finding device (e.g. after USB reconnect when another program grabbed ADB):
        # Run adb kill-server to reset ADB state
        if ($script:consecutiveWaitCount -ge 3) {
            Log-Bi Yellow "[ADB] Device not detected after reconnect. Resetting ADB (adb kill-server)..." "56uv5pyr44GM6KqN6K2Y44GV44KM44Gq44GE44GL5YiH5pat44GV44KM44G+44GX44Gf44CCYWRiIGtpbGwtc2VydmVyIOOCkuWun+ihjOOBl+OBpuWGjeippuihjOS4rS4uLg=="
            Reset-AdbServer "Reconnect retry"
            $script:consecutiveWaitCount = 0
            return $false
        }

        if ($unauthorizedFound) {
            Log-Bi Yellow "[WAIT] Android device detected, but unauthorized." "QW5kcm9pZOerr+acq+OBjOaknOWHuuOBleOCjOOBvuOBl+OBn+OBjOOAgeacquioseWPr+OBp+OBmeOAgg=="
            Log-Bi Yellow "       Please unlock phone screen and tap 'Allow USB debugging'." "44K544Oe44Ob55S76Z2i44Gu44Ot44OD44Kv44KS6Kej6Zmk44GX44CB44CMVVNC44OH44OQ44OD44Kw44KS6Kix5Y+v44CN44KS44K/44OD44OX44GX44Gm44GP44Gg44GV44GE44CC"
        } else {
            Log-Bi Yellow "[WAIT] No Android device detected." "QW5kcm9pZOerr+acq+OBjOimi+OBpOOBi+OCiuOBvuOBm+OCk+OAgg=="
            Log-Bi Yellow "       1. Connect phone to PC via USB cable (or open WSA)." "MS4g44K544Oe44Ob44KSVVNC44Kx44O844OW44Or44GnUEPjgavmjqXntprjgZfjgabjgY/jgaDjgZXjgYQgKOWQiOaIkFdTQSk="
            Log-Bi Yellow "       2. Enable 'USB debugging' in Developer options." "Mi4g56uv5pyr44Gu6ZaL55m66ICF5ZCR44GR44Kq44OX44K344On44Oz44Gn44CMVVNC44OH44OQ44OD44Kw44CN44KST07jgavjgZfjgabjgY/jgaDjgZXjgYTjgII="
        }
        return $false
    }

    $script:consecutiveWaitCount = 0

    # Pick best target serial
    $chosenSerial = $onlineSerials[0]
    
    # Check if port forward is already active for chosen device and target port
    $existingFwd = & $adbCmd forward --list 2>&1
    $isAlreadyForwarded = $false
    foreach ($fwdLine in $existingFwd) {
        if ($fwdLine -match [regex]::Escape($chosenSerial) -and $fwdLine -match "tcp:$targetPort\s+tcp:$targetPort") {
            $isAlreadyForwarded = $true
            break
        }
    }

    if (!$isAlreadyForwarded) {
        try { & $adbCmd -s $chosenSerial forward --remove "tcp:$targetPort" 2>$null } catch {}
        $fwdOut = & $adbCmd -s $chosenSerial forward "tcp:$targetPort" "tcp:$targetPort" 2>&1
        if ($LASTEXITCODE -ne 0) {
            Write-Host "[NOTICE] ADB Port Forwarding notice: $fwdOut" -ForegroundColor Yellow
            return $false
        }
    }
    return $true
}

Log-Bi Cyan "Ready. Starting auto-connection loop (target port: $port)..." "5rqW5YKZ5a6M5LqG44CC6Ieq5YuV5o6l57aa44Or44O844OX44KS6ZaL5aeL44GX44G+44GZ"

$lastForwardOk = $false

while ($true) {
    $client = $null
    $stream = $null
    $reader = $null
    $connectedAnnounced = $false
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
        $client.NoDelay = $true
        $connectResult = $client.BeginConnect("127.0.0.1", $port, $null, $null)
        $connectSuccess = $connectResult.AsyncWaitHandle.WaitOne(4000, $false)
        if (!$connectSuccess) {
            $client.Close()
            Start-Sleep -Seconds 1
            continue
        }
        $client.EndConnect($connectResult)

        $stream = $client.GetStream()
        $reader = New-Object System.IO.StreamReader($stream, [System.Text.Encoding]::UTF8)

        while ($true) {
            $line = $reader.ReadLine()
            if ($null -eq $line) {
                if ($connectedAnnounced) {
                    Log-Bi Yellow "[INFO] Connection closed by Android app. Waiting to reconnect..." "44Ki44OX44Oq44Go44Gu5o6l57aa44GM5YiH5pat44GV44KM44G+44GX44Gf44CC5YaN5o6l57aa5b6F5qmf5LitLi4u"
                }
                break
            }
            
            $line = $line.Trim()
            if ($line.Length -eq 0) { continue }

            if ($line -eq "OK" -or $line -eq "PING") {
                if (!$connectedAnnounced) {
                    Write-Host ""
                    Write-Host "==========================================================" -ForegroundColor Green
                    Log-Bi Green " *** Taiko Controller Connected Successfully! ***" "4piF4piF4piFIOWkqum8k+OCs+ODs+ODiOODreODvOODqeODvCAo44Ki44OX44OqKSDjgajmjqXntprlrozkuobvvIEg4piF4piF4piF"
                    Log-Bi Green " Sending keys (D / F / J / K) to PC games in real-time." "UEPjgrLjg7zjg6DvvIjlpKrpvJPjgqbjgqfjg5bnrYnvvInjgbjjgq3jg7zjgpLjg6rjgqLjg6vjgr/jgqTjg6DpgIHkv6HjgZfjgb7jgZnjgII="
                    Write-Host "==========================================================" -ForegroundColor Green
                    Write-Host ""
                    $connectedAnnounced = $true
                }
                continue
            }

            $parts = $line.Split(' ')
            if ($parts.Length -ge 2) {
                if (!$connectedAnnounced) {
                    Write-Host ""
                    Write-Host "==========================================================" -ForegroundColor Green
                    Log-Bi Green " *** Taiko Controller Connected Successfully! ***" "4piF4piF4piFIOWkqum8k+OCs+ODs+ODiOODreODvOODqeODvCAo44Ki44OX44OqKSDjgajmjqXntprlrozkuobvvIEg4piF4piF4piF"
                    Log-Bi Green " Sending keys (D / F / J / K) to PC games in real-time." "UEPjgrLjg7zjg6DvvIjlpKrpvJPjgqbjgqfjg5bnrYnvvInjgbjjgq3jg7zjgpLjg6rjgqLjg6vjgr/jgqTjg6DpgIHkv6HjgZfjgb7jgZnjgII="
                    Write-Host "==========================================================" -ForegroundColor Green
                    Write-Host ""
                    $connectedAnnounced = $true
                }

                $action = $parts[0]
                for ($i = 1; $i -lt $parts.Length; $i++) {
                    $key = $parts[$i].ToUpper()
                    if ($key.Length -gt 0) {
                        $vkey = [byte][char]$key[0]
                        
                        # Instant injection first for sub-millisecond game response
                        if ($action -eq "DOWN") {
                            [TaikoKeyboard]::Down($vkey)
                        } elseif ($action -eq "UP") {
                            [TaikoKeyboard]::Up($vkey)
                        }

                        Write-Host "[KEY] $action -> $key" -ForegroundColor Cyan
                    }
                }
            }
        }
    } catch {
        if ($connectedAnnounced) {
            $errMsg = $_.Exception.Message
            Log-Bi Yellow "[INFO] Connection dropped ($errMsg). Reconnecting..." "44K744OD44K344On44Oz44GM5YiH5pat44GV44KM44G+44GX44Gf44CC5YaN5o6l57aa44GX44G+44GZLi4u"
        }
    } finally {
        $lastForwardOk = $false
        if ($null -ne $reader) { try { $reader.Close() } catch {} }
        if ($null -ne $stream) { try { $stream.Close() } catch {} }
        if ($null -ne $client) { try { $client.Close() } catch {} }
    }
    Start-Sleep -Seconds 1
}
