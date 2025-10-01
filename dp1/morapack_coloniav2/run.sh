#!/bin/bash

# ===================================================
# Script de ejecucion para MoraPack Colonia v2
# ===================================================

echo "[INFO] Ejecutando MoraPack Colonia v2..."

# Verificar que exista el directorio bin
if [ ! -d "bin" ]; then
    echo "[ERROR] Directorio 'bin' no encontrado. Ejecuta ./compile.sh primero."
    exit 1
fi

# Verificar que exista la clase Main compilada
if [ ! -f "bin/morapack/main/Main.class" ]; then
    echo "[ERROR] Clase Main no encontrada. Ejecuta ./compile.sh primero."
    exit 1
fi

# Ejecutar el programa
java -cp bin morapack.main.Main

if [ $? -ne 0 ]; then
    echo "[ERROR] Error durante la ejecucion"
    exit 1
fi

echo ""
echo "[INFO] Ejecucion completada"