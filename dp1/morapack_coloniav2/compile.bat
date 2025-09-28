@echo off
REM ===================================================
REM Script de compilacion para MoraPack Colonia v2
REM ===================================================

echo [INFO] Compilando MoraPack Colonia v2...

REM Crear directorio bin si no existe
if not exist "bin" (
    echo [INFO] Creando directorio bin...
    mkdir bin
)

REM Limpiar compilaciones anteriores
echo [INFO] Limpiando compilaciones anteriores...
del /Q /S bin\*.class 2>nul

REM Compilar en orden de dependencias
echo [INFO] Compilando clases core...
javac -d bin src\morapack\core\solucion\Solucion.java
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Fallo al compilar Solucion.java
    pause
    exit /b 1
)

javac -cp bin -d bin src\morapack\core\problema\Problema.java
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Fallo al compilar Problema.java
    pause
    exit /b 1
)

javac -cp bin -d bin src\morapack\core\problema\ProblemaTSP.java
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Fallo al compilar ProblemaTSP.java
    pause
    exit /b 1
)

echo [INFO] Compilando componentes de colonia...
javac -cp bin -d bin src\morapack\colonia\componentes\*.java
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Fallo al compilar componentes de colonia
    pause
    exit /b 1
)

echo [INFO] Compilando algoritmo principal...
javac -cp bin -d bin src\morapack\colonia\algoritmo\AlgoritmoColoniaHormigas.java
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Fallo al compilar AlgoritmoColoniaHormigas.java
    pause
    exit /b 1
)

echo [INFO] Compilando clase Main...
javac -cp bin -d bin src\morapack\main\Main.java
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Fallo al compilar Main.java
    pause
    exit /b 1
)

echo [SUCCESS] Compilacion completada exitosamente!
echo [INFO] Para ejecutar: run.bat
pause