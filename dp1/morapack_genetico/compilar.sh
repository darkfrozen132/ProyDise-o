#!/bin/bash

echo "🔧 Compilando sistema MoraPack Optimizado Fitness..."

# Crear directorio bin si no existe
mkdir -p bin

# Compilar todo el código fuente
javac -d bin src/morapack/*/*.java

# Verificar si la compilación fue exitosa
if [ $? -eq 0 ]; then
    echo "✅ Compilación exitosa!"
    echo "📁 Clases compiladas en directorio bin/"
else
    echo "❌ Error en la compilación"
fi
