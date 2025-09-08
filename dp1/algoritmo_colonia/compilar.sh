#!/bin/bash

# Script de compilación para el framework Algoritmo de Colonia de Hormigas

echo "=== COMPILANDO FRAMEWORK ALGORITMO COLONIA DE HORMIGAS ==="

# Crear directorio de salida
mkdir -p bin

# Compilar todas las clases del framework ACO
echo "Compilando framework ACO..."

# Compilar en orden de dependencias
javac -d bin -sourcepath src \
    src/colonia/*.java \
    src/colonia/algoritmo/*.java \
    src/colonia/componentes/*.java

if [ $? -eq 0 ]; then
    echo "✅ Compilación exitosa"
    echo "Los archivos .class están en el directorio bin/"
    echo ""
    echo "📚 Para usar el framework:"
    echo "  1. Extiende la clase Hormiga para tu problema específico"
    echo "  2. Implementa la clase Heuristica para tu dominio"
    echo "  3. Usa AlgoritmoColoniaHormigas para ejecutar"
    echo ""
    echo "🐜 Framework ACO listo para usar!"
else
    echo "❌ Error en la compilación"
    exit 1
fi

echo "=============================================="
