#!/bin/bash

# Script para compilar y ejecutar MoraPack con DAO y Algoritmo Genético

echo "=== COMPILANDO MORAPACK CON DAO Y ALGORITMO GENÉTICO ==="

# Limpiar directorio bin
rm -rf bin/*

# Compilar MoraPack incluyendo el algoritmo genético
javac -d bin -cp src:../algoritmo_genetico/src src/morapack/main/MainMoraPackDAO.java

if [ $? -eq 0 ]; then
    echo "✓ Compilación exitosa"
    echo ""
    echo "=== EJECUTANDO SISTEMA MORAPACK ==="
    echo ""
    java -cp bin:../algoritmo_genetico/src morapack.main.MainMoraPackDAO
else
    echo "✗ Error en la compilación"
    exit 1
fi
