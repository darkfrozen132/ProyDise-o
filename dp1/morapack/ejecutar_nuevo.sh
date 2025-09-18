#!/bin/bash

# Script para compilar y ejecutar MoraPack con arquitectura profesional com.morapack

echo "=== COMPILANDO MORAPACK (ARQUITECTURA PROFESIONAL) ==="

# Limpiar directorio bin
rm -rf bin/*

# Compilar con la nueva estructura com.morapack incluyendo el algoritmo genético
javac -d bin -cp src:../algoritmo_genetico/src src/com/morapack/MoraPackApplication.java

if [ $? -eq 0 ]; then
    echo "✓ Compilación exitosa"
    echo ""
    echo "=== EJECUTANDO APLICACIÓN MORAPACK ==="
    echo ""
    java -cp bin:../algoritmo_genetico/src com.morapack.MoraPackApplication
else
    echo "✗ Error en la compilación"
    exit 1
fi
