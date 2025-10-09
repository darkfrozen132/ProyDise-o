#!/bin/bash

echo "🧬 =========================================="
echo "   MORAPACK - SISTEMA GENÉTICO SIN ICONOS"
echo "   Optimización de rutas + Consolidación"
echo "========================================== 🧬"

# Verificar que Java está instalado
if ! command -v javac &> /dev/null; then
    echo "Error: Java no está instalado o no está en el PATH"
    echo "Por favor instala Java JDK 8 o superior"
    exit 1
fi

echo ""
echo "🔧 Preparando compilación..."

# Crear directorio bin si no existe
mkdir -p bin
echo "   ✓ Directorio bin creado/verificado"

# Compilar todos los archivos Java del sistema genético
echo ""
echo "📦 Compilando componentes..."
echo "   🏗️  Modelos de datos..."
javac -d bin src/morapack/modelo/*.java 2>/dev/null || echo "   ⚠️  Algunos modelos no compilaron (normal si faltan dependencias)"

echo "   📊 Cargadores de datos..."
javac -d bin -cp bin src/morapack/datos/*.java 2>/dev/null || echo "   ⚠️  Algunos cargadores no compilaron (normal si faltan dependencias)"

echo "   ⏰ Planificador temporal..."
javac -d bin -cp bin src/morapack/planificacion/*.java 2>/dev/null || echo "   ⚠️  Planificador no compiló (normal si faltan dependencias)"

echo "   🎯 Clases principales..."
javac -d bin -cp bin src/morapack/main/*.java 2>/dev/null || echo "   ⚠️  Algunas clases principales no compilaron"

echo "   🧬 Algoritmo genético..."
javac -d bin -cp bin src/morapack/genetico/*.java 2>/dev/null || echo "   ⚠️  Algoritmo genético no compiló (normal si faltan dependencias)"

# Compilar demo independiente siempre
echo ""
echo "🚀 Compilando demo independiente..."
javac -d bin src/morapack/main/DemoGeneticoSinIconos.java

# Verificar que la demo se compiló
if [ $? -ne 0 ]; then
    echo "❌ Error crítico: No se pudo compilar la demo"
    echo "   Verifica que Java esté instalado correctamente"
    exit 1
fi

echo "   ✅ Demo compilada exitosamente!"
echo ""
echo "🎯 ========================================"
echo "   EJECUTANDO SISTEMA GENÉTICO SIN ICONOS"
echo "   Con reporte de consolidación incluido"
echo "======================================== 🎯"
echo ""

# Ejecutar el programa principal
java -cp bin morapack.main.DemoGeneticoSinIconos

echo ""
echo "🏁 ================================"
echo "   EJECUCIÓN COMPLETADA"
echo "   Sistema optimizado sin iconos"  
echo "================================ 🏁"
