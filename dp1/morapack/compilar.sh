#!/bin/bash

# Script de compilación para el proyecto MoraPack

echo "=== COMPILANDO PROYECTO MORAPACK ==="

# Crear directorio de salida
mkdir -p bin

# Compilar todas las clases de MoraPack
echo "Compilando clases MoraPack..."

# Compilar en orden de dependencias
javac -d bin -sourcepath src \
    src/morapack/genetico/*.java \
    src/morapack/modelo/*.java \
    src/morapack/datos/*.java \
    src/morapack/optimizacion/*.java \
    src/morapack/main/*.java

if [ $? -eq 0 ]; then
    echo "✅ Compilación exitosa"
    echo "Los archivos .class están en el directorio bin/"
else
    echo "❌ Error en la compilación"
    exit 1
fi

echo "================================="
