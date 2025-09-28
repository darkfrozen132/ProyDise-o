@echo off
REM ===================================================
REM Script de ejecucion para MoraPack Colonia v2
REM ===================================================

echo [INFO] Ejecutando MoraPack Colonia v2...

REM Verificar que exista el directorio bin
if not exist "bin" (
    echo [ERROR] Directorio 'bin' no encontrado. Ejecuta compile.bat primero.
    pause
    exit /b 1
)

REM Verificar que exista la clase Main compilada
if not exist "bin\morapack\main\Main.class" (
    echo [ERROR] Clase Main no encontrada. Ejecuta compile.bat primero.
    pause
    exit /b 1
)

REM Ejecutar el programa
java -cp bin morapack.main.Main

if %ERRORLEVEL% neq 0 (
    echo [ERROR] Error durante la ejecucion
    pause
    exit /b 1
)

echo.
echo [INFO] Ejecucion completada
pause