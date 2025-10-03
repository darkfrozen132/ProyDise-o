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

REM Las clases específicas se compilarán después de los modelos de datos

echo [INFO] Compilando cargadores base...
javac -cp bin -d bin src\morapack\datos\cargadores\CargadorException.java
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Fallo al compilar CargadorException.java
    pause
    exit /b 1
)

javac -cp bin -d bin src\morapack\datos\cargadores\CargadorCSV.java
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Fallo al compilar CargadorCSV.java
    pause
    exit /b 1
)

echo [INFO] Compilando modelos base...
javac -cp bin -d bin src\morapack\datos\modelos\Continente.java
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Fallo al compilar Continente.java
    pause
    exit /b 1
)

javac -cp bin -d bin src\morapack\datos\modelos\Aeropuerto.java
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Fallo al compilar Aeropuerto.java
    pause
    exit /b 1
)

javac -cp bin -d bin src\morapack\datos\modelos\Vuelo.java
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Fallo al compilar Vuelo.java
    pause
    exit /b 1
)

javac -cp bin -d bin src\morapack\datos\modelos\Pedido.java
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Fallo al compilar Pedido.java
    pause
    exit /b 1
)

javac -cp bin -d bin src\morapack\datos\modelos\Cliente.java
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Fallo al compilar Cliente.java
    pause
    exit /b 1
)

javac -cp bin -d bin src\morapack\datos\modelos\VueloInstancia.java
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Fallo al compilar VueloInstancia.java
    pause
    exit /b 1
)

echo [INFO] Compilando cargadores especificos...
javac -cp bin -d bin src\morapack\datos\cargadores\CargadorAeropuertos.java
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Fallo al compilar CargadorAeropuertos.java
    pause
    exit /b 1
)

javac -cp bin -d bin src\morapack\datos\cargadores\CargadorVuelos.java
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Fallo al compilar CargadorVuelos.java
    pause
    exit /b 1
)

javac -cp bin -d bin src\morapack\datos\cargadores\CargadorPedidos.java
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Fallo al compilar CargadorPedidos.java
    pause
    exit /b 1
)

echo [INFO] Compilando modelos complejos...
javac -cp bin -d bin src\morapack\datos\modelos\RedDistribucion.java
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Fallo al compilar RedDistribucion.java
    pause
    exit /b 1
)

javac -cp bin -d bin src\morapack\datos\modelos\ValidadorColapso.java
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Fallo al compilar ValidadorColapso.java
    pause
    exit /b 1
)

javac -cp bin -d bin src\morapack\datos\modelos\MetricasSistema.java
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Fallo al compilar MetricasSistema.java
    pause
    exit /b 1
)

echo [INFO] Compilando clases específicas MoraPack...
javac -cp bin -d bin src\morapack\core\solucion\SolucionMoraPack.java
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Fallo al compilar SolucionMoraPack.java
    pause
    exit /b 1
)

javac -cp bin -d bin src\morapack\core\problema\ProblemaMoraPack.java
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Fallo al compilar ProblemaMoraPack.java
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