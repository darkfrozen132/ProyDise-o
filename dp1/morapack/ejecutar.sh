#!/bin/bash

# Script de ejecución para el proyecto MoraPack

echo "=== EJECUTANDO OPTIMIZACIÓN MORAPACK ==="

# Verificar que existan los archivos compilados
if [ ! -d "bin" ]; then
    echo "❌ No se encontró el directorio bin/. Ejecuta primero ./compilar.sh"
    exit 1
fi

# Ejecutar el programa principal
echo "Iniciando optimización..."
java -cp bin morapack.main.MainMoraPack

echo "=== EJECUCIÓN COMPLETADA ==="
