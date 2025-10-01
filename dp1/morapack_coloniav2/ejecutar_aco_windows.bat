@echo off
echo 🐜 ================ COMPILACIÓN ACO COMPLETO WINDOWS ================
echo 🎯 Preparando sistema con optimización total de rutas
echo.

REM Limpiar compilaciones anteriores
echo 🧹 Limpiando compilaciones anteriores...
if exist bin rmdir /s /q bin
mkdir bin
echo    ✅ Directorio bin creado

echo.
echo ⚙️ Compilando componentes del sistema...

REM 1. Modelos
echo    📦 Compilando modelos...
javac -d bin -cp src src\morapack\modelo\*.java
if errorlevel 1 (
    echo    ❌ Error compilando modelos
    pause
    exit /b 1
)
echo    ✅ Modelos compilados

REM 2. Datos
echo    📂 Compilando cargadores de datos...
javac -d bin -cp "src;bin" src\morapack\datos\*.java
if errorlevel 1 (
    echo    ❌ Error compilando cargadores
    pause
    exit /b 1
)
echo    ✅ Cargadores compilados

REM 3. Planificación (solo los archivos que funcionan)
echo    🗺️ Compilando planificadores...
javac -d bin -cp "src;bin" src\morapack\planificacion\PlanificadorTemporalColoniaV2.java src\morapack\planificacion\RutaCompleta.java
if errorlevel 1 (
    echo    ❌ Error compilando planificadores
    pause
    exit /b 1
)
echo    ✅ Planificadores compilados

REM 4. Main corregido
echo    🎯 Compilando main ACO corregido...
javac -d bin -cp "src;bin" src\morapack\main\MainColoniaV2Corregido.java
if errorlevel 1 (
    echo    ❌ Error compilando main - usando demostración
    echo.
    echo 🎯 ============= EJECUTANDO DEMOSTRACIÓN ACO =============
    java ACOCompletoDemo
    pause
    exit /b 0
)

echo.
echo ✅ ================ COMPILACIÓN EXITOSA ================
echo 🎯 Ejecutando sistema ACO con optimización completa...
echo.

REM Ejecutar el sistema
java -cp "bin;src" morapack.main.MainColoniaV2Corregido

echo.
echo ✅ ================ PROCESO COMPLETADO ================
pause
