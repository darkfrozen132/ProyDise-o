#!/bin/bash

# ===================================================
# Script de compilacion para MoraPack Colonia v2
# ===================================================

echo "[INFO] Compilando MoraPack Colonia v2..."

# Crear directorio bin si no existe
if [ ! -d "bin" ]; then
    echo "[INFO] Creando directorio bin..."
    mkdir bin
fi

# Limpiar compilaciones anteriores
echo "[INFO] Limpiando compilaciones anteriores..."
rm -rf bin/*.class bin/**/*.class 2>/dev/null

# Función para compilar con verificación de errores
compile_with_check() {
    local file=$1
    local description=$2

    echo "[INFO] Compilando $description..."
    javac -cp bin -d bin "$file"

    if [ $? -ne 0 ]; then
        echo "[ERROR] Fallo al compilar $file"
        exit 1
    fi
}

# Compilar en orden de dependencias
compile_with_check "src/morapack/core/solucion/Solucion.java" "clase Solucion"
compile_with_check "src/morapack/core/problema/Problema.java" "clase Problema"

echo "[INFO] Compilando cargadores base..."
compile_with_check "src/morapack/datos/cargadores/CargadorException.java" "excepción de cargador"
compile_with_check "src/morapack/datos/cargadores/CargadorCSV.java" "cargador CSV base"

echo "[INFO] Compilando modelos base..."
compile_with_check "src/morapack/datos/modelos/Continente.java" "modelo Continente"
compile_with_check "src/morapack/datos/modelos/Aeropuerto.java" "modelo Aeropuerto"
compile_with_check "src/morapack/datos/modelos/Vuelo.java" "modelo Vuelo"
compile_with_check "src/morapack/datos/modelos/Pedido.java" "modelo Pedido"
compile_with_check "src/morapack/datos/modelos/Cliente.java" "modelo Cliente"

echo "[INFO] Compilando cargadores específicos..."
compile_with_check "src/morapack/datos/cargadores/CargadorAeropuertos.java" "cargador Aeropuertos"
compile_with_check "src/morapack/datos/cargadores/CargadorVuelos.java" "cargador Vuelos"
compile_with_check "src/morapack/datos/cargadores/CargadorPedidos.java" "cargador Pedidos"

echo "[INFO] Compilando modelos complejos..."
compile_with_check "src/morapack/datos/modelos/RedDistribucion.java" "red de distribución"
compile_with_check "src/morapack/datos/modelos/ValidadorColapso.java" "validador de colapso"
compile_with_check "src/morapack/datos/modelos/MetricasSistema.java" "métricas del sistema"

# Ahora compilar las clases específicas de MoraPack que dependen de los modelos
compile_with_check "src/morapack/core/solucion/SolucionMoraPack.java" "clase SolucionMoraPack"
compile_with_check "src/morapack/core/problema/ProblemaMoraPack.java" "clase ProblemaMoraPack"

echo "[INFO] Compilando componentes de colonia..."
javac -cp bin -d bin src/morapack/colonia/componentes/*.java
if [ $? -ne 0 ]; then
    echo "[ERROR] Fallo al compilar componentes de colonia"
    exit 1
fi

compile_with_check "src/morapack/colonia/algoritmo/AlgoritmoColoniaHormigas.java" "algoritmo principal"
compile_with_check "src/morapack/main/Main.java" "clase Main"

echo "[SUCCESS] Compilacion completada exitosamente!"
echo "[INFO] Para ejecutar: ./run.sh"