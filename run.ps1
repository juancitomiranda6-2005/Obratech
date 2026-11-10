$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$processes = [System.Collections.Generic.List[System.Diagnostics.Process]]::new()
$reportedExit = [System.Collections.Generic.HashSet[int]]::new()

function Start-ManagedProcess {
    param(
        [string]$FilePath,
        [string[]]$ArgumentList,
        [string]$WorkingDirectory,
        [string]$Name
    )

    $process = Start-Process -FilePath $FilePath `
        -ArgumentList $ArgumentList `
        -WorkingDirectory $WorkingDirectory `
        -NoNewWindow `
        -PassThru
    $processes.Add($process)
    Write-Host "[INFO] $Name iniciado (PID $($process.Id))"
    return $process
}

function Stop-ProcessTree {
    param([int]$ProcessId)

    $children = Get-CimInstance Win32_Process -Filter "ParentProcessId = $ProcessId" `
        -ErrorAction SilentlyContinue
    foreach ($child in $children) {
        Stop-ProcessTree -ProcessId $child.ProcessId
    }
    Stop-Process -Id $ProcessId -Force -ErrorAction SilentlyContinue
}

try {
    Set-Location $root

    if (-not (Get-Command node.exe -ErrorAction SilentlyContinue)) {
        throw 'Node.js no esta instalado o no esta en PATH.'
    }
    if (-not (Test-Path (Join-Path $root 'frontend\node_modules\vite\bin\vite.js'))) {
        throw 'Faltan dependencias del frontend. Ejecuta npm install en frontend.'
    }
    if (-not (Test-Path (Join-Path $root 'ml-service\.venv\Scripts\python.exe'))) {
        throw 'Falta ml-service\.venv. Crea el entorno e instala requirements.txt.'
    }

    Write-Host ''
    Write-Host '========================================'
    Write-Host '  ObraTech - Modo desarrollo'
    Write-Host '========================================'
    Write-Host ''

    Start-ManagedProcess 'cmd.exe' @('/d', '/c', 'call mvnw.cmd -pl backend spring-boot:run') `
        $root 'Backend Spring Boot (http://localhost:9999)' | Out-Null
    Start-ManagedProcess 'cmd.exe' @('/d', '/c', 'call npm.cmd run dev -- --host 127.0.0.1') `
        (Join-Path $root 'frontend') 'Frontend Vite (http://localhost:5173)' | Out-Null
    Start-ManagedProcess (Join-Path $root 'ml-service\.venv\Scripts\python.exe') `
        @('-m', 'uvicorn', 'app.main:app', '--host', '127.0.0.1', '--port', '8000') `
        (Join-Path $root 'ml-service') 'ML service (http://localhost:8000)' | Out-Null

    Start-Sleep -Seconds 5
    Start-Process 'http://localhost:5173' | Out-Null
    Write-Host ''
    Write-Host '[INFO] Servicios activos. Presiona Ctrl+C para detenerlos todos.'
    while ($true) {
        Start-Sleep -Seconds 1
        foreach ($process in $processes) {
            if ($process.HasExited -and $reportedExit.Add($process.Id)) {
                Write-Host "[WARN] Un proceso termino (PID $($process.Id))."
            }
        }
    }
}
finally {
    Write-Host ''
    Write-Host '[INFO] Deteniendo backend, frontend y ML...'
    foreach ($process in $processes) {
        if (-not $process.HasExited) {
            Stop-ProcessTree -ProcessId $process.Id
        }
    }
    Write-Host '[INFO] Todos los servicios fueron detenidos.'
}
