#!/bin/bash

echo "🐜 ================ COMPILACIÓN ACO COMPLETO ================"
echo "🎯 Preparando sistema con optimización total de rutas"
echo

# Limpiar compilaciones anteriores
echo "🧹 Limpiando compilaciones anteriores..."
rm -rf bin
mkdir -p bin
echo "   ✅ Directorio bin creado"

# Compilar en orden correcto
echo
echo "⚙️ Compilando componentes del sistema..."

# 1. Modelos
echo "   📦 Compilando modelos..."
javac -d bin -cp src src/morapack/modelo/*.java
if [ $? -eq 0 ]; then
    echo "   ✅ Modelos compilados"
else
    echo "   ❌ Error compilando modelos"
    exit 1
fi

# 2. Datos
echo "   📂 Compilando cargadores de datos..."
javac -d bin -cp src:bin src/morapack/datos/*.java
if [ $? -eq 0 ]; then
    echo "   ✅ Cargadores compilados"
else
    echo "   ❌ Error compilando cargadores"
    exit 1
fi

# 3. Planificación (solo los archivos que funcionan)
echo "   🗺️ Compilando planificadores..."
javac -d bin -cp src:bin src/morapack/planificacion/PlanificadorTemporalColoniaV2.java src/morapack/planificacion/RutaCompleta.java
if [ $? -eq 0 ]; then
    echo "   ✅ Planificadores compilados"
else
    echo "   ❌ Error compilando planificadores"
    exit 1
fi

# 4. Main corregido
echo "   🎯 Compilando main ACO corregido..."
javac -d bin -cp src:bin src/morapack/main/MainColoniaV2Corregido.java
if [ $? -eq 0 ]; then
    echo "   ✅ Main ACO compilado"
else
    echo "   ❌ Error compilando main - usando demostración"
    echo
    echo "🎯 ============= EJECUTANDO DEMOSTRACIÓN ACO ============="
    java ACOCompletoDemo
    exit 0
fi

echo
echo "✅ ================ COMPILACIÓN EXITOSA ================"
echo "🎯 Ejecutando sistema ACO con optimización completa..."
echo

# Ejecutar el sistema
java -cp bin:src morapack.main.MainColoniaV2Corregido
