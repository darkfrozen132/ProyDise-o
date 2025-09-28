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
compile_with_check "src/morapack/core/problema/ProblemaTSP.java" "clase ProblemaTSP"

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