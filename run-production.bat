@echo off
REM Ejecuta el JAR empaquetado. Los cambios de frontend requieren reconstruirlo.
cd /d "%~dp0"

echo.
echo ========================================
echo   ObraTech - Version empaquetada
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

echo [INFO] Compilando frontend desde frontend\src...
pushd frontend
call npm.cmd run build
if errorlevel 1 (
    popd
    echo [ERROR] La compilacion del frontend fallo
    pause
    exit /b 1
)
popd

echo [INFO] Compilando backend y recursos desde cero...
if exist "backend\target" attrib -R "backend\target\*" /S /D
call mvnw.cmd clean package -DskipTests
if errorlevel 1 (
    echo [ERROR] La compilacion del backend fallo
    pause
    exit /b 1
)

echo [INFO] Iniciando aplicacion...
echo.

java -jar "backend\target\obratech-0.0.1-SNAPSHOT.jar"
if errorlevel 1 (
    echo [ERROR] La aplicacion fallo
    pause
    exit /b 1
)

echo [INFO] Aplicacion finalizada
pause
