@echo off
echo 🧬 ================ SISTEMA GENÉTICO WINDOWS ================
echo 🎯 Ejecutando algoritmo genético MoraPack
echo.

REM Cambiar al directorio del sistema genético
cd /d "%~dp0..\morapack_genetico"

if not exist bin (
    echo 📦 Compilando sistema genético...
    mkdir bin
    
    REM Compilar modelos
    javac -d bin -cp src src\morapack\modelo\*.java
    if errorlevel 1 (
        echo ❌ Error compilando modelos
        pause
        exit /b 1
    )
    
    REM Compilar datos
    javac -d bin -cp "src;bin" src\morapack\datos\*.java
    if errorlevel 1 (
        echo ❌ Error compilando datos
        pause
        exit /b 1
    )
    
    REM Compilar planificación
    javac -d bin -cp "src;bin" src\morapack\planificacion\*.java
    if errorlevel 1 (
        echo ❌ Error compilando planificación
        pause
        exit /b 1
    )
    
    REM Compilar main
    javac -d bin -cp "src;bin" src\morapack\main\*.java
    if errorlevel 1 (
        echo ❌ Error compilando main
        pause
        exit /b 1
    )
    
    echo ✅ Compilación completada
)

echo 🎯 Ejecutando sistema genético...
echo.

java -cp "bin;src" morapack.main.MainMoraPackCorregido

echo.
echo ✅ ================ PROCESO COMPLETADO ================
pause
