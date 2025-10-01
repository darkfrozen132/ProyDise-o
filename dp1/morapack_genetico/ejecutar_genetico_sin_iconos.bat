@echo off
echo.
echo ==========================================
echo    MORAPACK - SISTEMA GENETICO SIN ICONOS
echo    Optimizacion de rutas + Consolidacion
echo ==========================================

:: Verificar que Java esta instalado
where javac >nul 2>&1
if errorlevel 1 (
    echo Error: Java no esta instalado o no esta en el PATH
    echo Por favor instala Java JDK 8 o superior
    pause
    exit /b 1
)

echo.
echo Preparando compilacion...

:: Crear directorio bin si no existe  
if not exist bin mkdir bin
echo    Directorio bin creado/verificado

:: Compilar todos los archivos Java del sistema genetico
echo.
echo Compilando componentes...
echo    Modelos de datos...
javac -d bin src\morapack\modelo\*.java >nul 2>&1

echo    Cargadores de datos...
javac -d bin -cp bin src\morapack\datos\*.java >nul 2>&1

echo    Planificador temporal...
javac -d bin -cp bin src\morapack\planificacion\*.java >nul 2>&1

echo    Clases principales...
javac -d bin -cp bin src\morapack\main\*.java >nul 2>&1

echo    Algoritmo genetico...
javac -d bin -cp bin src\morapack\genetico\*.java >nul 2>&1

:: Compilar demo independiente siempre
echo.
echo Compilando demo independiente...
javac -d bin src\morapack\main\DemoGeneticoSinIconos.java

:: Verificar que la demo se compilo
if errorlevel 1 (
    echo Error critico: No se pudo compilar la demo
    echo    Verifica que Java este instalado correctamente
    pause
    exit /b 1
)

echo    Demo compilada exitosamente!
echo.
echo ========================================
echo    EJECUTANDO SISTEMA GENETICO SIN ICONOS
echo    Con reporte de consolidacion incluido
echo ========================================
echo.

:: Ejecutar el programa principal
java -cp bin morapack.main.DemoGeneticoSinIconos

echo.
echo ================================
echo    EJECUCION COMPLETADA
echo    Sistema optimizado sin iconos
echo ================================
pause
