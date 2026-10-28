@echo off
setlocal
cd /d "%~dp0"

echo.
echo ========================================
echo   ObraTech - Modo desarrollo (Integrado)
echo ========================================
echo.

where node >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Node.js no esta instalado o no esta en PATH.
    pause
    exit /b 1
)

if not exist "frontend\node_modules\vite\bin\vite.js" (
    echo [ERROR] Faltan dependencias del frontend en frontend\node_modules.
    echo Abre una terminal en frontend y ejecuta npm install una sola vez.
    pause
    exit /b 1
)

echo [INFO] Iniciando el backend Spring Boot en http://localhost:9999
:: El parametro /b evita que se abra una ventana externa
start /b "" cmd /c "cd /d ""%~dp0"" && call mvnw.cmd -pl backend spring-boot:run"

echo [INFO] Iniciando React + Vite con recarga al guardar en http://localhost:5173
:: El parametro /b mantiene a Vite corriendo dentro de VS Code
start /b "" cmd /c "cd /d ""%~dp0frontend"" && call npm.cmd run dev -- --host 127.0.0.1"

echo.
echo ambos servidores se estan ejecutando dentro de esta terminal.
echo Abre http://localhost:5173 y edita archivos en frontend\src.
echo.
timeout /t 5 /nobreak >nul
start "" "http://localhost:5173"
endlocal
