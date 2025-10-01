#!/bin/bash

echo "============ EJECUTANDO GENERADOR DE REPORTE COMPLETO ============"
echo "Sistema de rutas inteligentes SIN fallos aleatorios"
echo "=================================================================="

cd /home/leoncio/Documentos/GitHub/ProyDise-o/dp1/morapack_genetico

# Limpiar archivos anteriores
echo "🗑️  Limpiando reportes anteriores..."
rm -f REPORTE_*.txt RESUMEN_*.txt

# Compilar
echo ""
echo "🔨 Compilando GeneradorReporteCompleto..."
javac -d bin -cp ".:src" src/morapack/main/GeneradorReporteCompleto.java src/morapack/datos/CargadorPedidosUltrafinal.java src/morapack/modelo/*.java

if [ $? -eq 0 ]; then
    echo "✅ Compilación exitosa"
else
    echo "❌ Error en la compilación"
    exit 1
fi

# Ejecutar
echo ""
echo "🚀 Ejecutando generación de reportes..."
echo "   Procesando 211 pedidos con planificación inteligente..."
echo ""

java -cp ".:bin" morapack.main.GeneradorReporteCompleto

echo ""
echo "📊 Verificando archivos generados..."

# Verificar los 4 reportes nuevos
declare -a REPORTES=("REPORTE_GENERAL.txt" "REPORTE_RUTAS_POR_PEDIDOS.txt" "REPORTE_PEDIDOS_POR_VUELOS.txt" "REPORTE_PEDIDOS_DIVIDIDOS.txt")

for reporte in "${REPORTES[@]}"; do
    if [ -f "$reporte" ]; then
        echo "✅ $reporte generado"
        echo "   Tamaño: $(wc -l < "$reporte") líneas"
    else
        echo "❌ ERROR: No se generó $reporte"
    fi
done

echo ""
echo "🔍 Resumen rápido de resultados:"
echo "   Analizando datos desde REPORTE_GENERAL.txt..."

if [ -f "REPORTE_GENERAL.txt" ]; then
    # Extraer estadísticas del reporte general
    exitosos=$(grep "Total pedidos exitosos" REPORTE_GENERAL.txt | grep -o '[0-9]\+' | head -1 2>/dev/null || echo "0")
    fallidos=$(grep "Total pedidos fallidos" REPORTE_GENERAL.txt | grep -o '[0-9]\+' | head -1 2>/dev/null || echo "0")
    eficiencia=$(grep "Tasa de éxito global" REPORTE_GENERAL.txt | grep -o '[0-9.]\+' | head -1 2>/dev/null || echo "0")
    
    echo "   ✅ Pedidos EXITOSOS: $exitosos"
    echo "   ❌ Pedidos FALLIDOS: $fallidos"  
    echo "   🎯 Eficiencia del sistema: ${eficiencia}%"
    
    if [ "$fallidos" -eq 0 ]; then
        echo "   🎉 ¡PERFECTO! Cero fallos - Todas las rutas planificadas correctamente"
    else
        echo "   ⚠️  Se encontraron $fallidos fallos en la planificación"
    fi
else
    echo "   ⚠️  No se puede verificar - REPORTE_GENERAL.txt no encontrado"
fi

echo ""
echo "✅ Generación de reportes completada"
echo "=================================================================="
