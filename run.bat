@echo off
setlocal
cd /d "%~dp0"

echo.
echo ========================================
echo   ObraTech - Modo desarrollo
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
start "ObraTech - Backend" cmd /k "cd /d ""%~dp0"" && call mvnw.cmd -pl backend spring-boot:run"

echo [INFO] Iniciando React + Vite con recarga al guardar en http://localhost:5173
start "ObraTech - Frontend (Vite)" cmd /k "cd /d ""%~dp0frontend"" && call npm.cmd run dev -- --host 127.0.0.1"

echo.
echo Espera a que las ventanas indiquen que ambos servidores estan listos.
echo Abre http://localhost:5173 y edita archivos en frontend\src.
echo Al guardar, Vite actualizara los cambios automaticamente.
echo Para detenerlos, cierra las ventanas Backend y Frontend.
echo.
timeout /t 5 /nobreak >nul
start "" "http://localhost:5173"
endlocal
