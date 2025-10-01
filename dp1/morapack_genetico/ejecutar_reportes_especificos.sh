#!/bin/bash

echo "========== GENERADOR DE REPORTES ESPECÍFICOS =========="
echo "Procesando reportes por rutas y destinos"
echo "======================================================="

cd /home/leoncio/Documentos/GitHub/ProyDise-o/dp1/morapack_genetico

# Verificar que existe el reporte completo
if [ ! -f "REPORTE_COMPLETO_TODAS_LAS_RUTAS.txt" ]; then
    echo "❌ Error: No se encontró REPORTE_COMPLETO_TODAS_LAS_RUTAS.txt"
    echo "   Ejecuta primero: ./ejecutar_reporte_completo.sh"
    exit 1
fi

# Limpiar reportes específicos anteriores
echo "🗑️  Limpiando reportes específicos anteriores..."
rm -f REPORTE_POR_DESTINOS.txt
rm -f REPORTE_POR_RUTAS.txt  
rm -f REPORTE_POR_TIPO_RUTA.txt
rm -f REPORTE_ESTADISTICAS_SEDES.txt

# Compilar
echo ""
echo "🔨 Compilando GeneradorReportesEspecificos..."
javac -d bin -cp ".:src" src/morapack/main/GeneradorReportesEspecificos.java

if [ $? -eq 0 ]; then
    echo "✅ Compilación exitosa"
else
    echo "❌ Error en la compilación"
    exit 1
fi

# Ejecutar
echo ""
echo "🚀 Generando reportes específicos..."
echo "   Procesando datos desde REPORTE_COMPLETO_TODAS_LAS_RUTAS.txt..."
echo ""

java -cp ".:bin" morapack.main.GeneradorReportesEspecificos

echo ""
echo "📊 Verificando reportes generados..."

reportes=("REPORTE_POR_DESTINOS.txt" "REPORTE_POR_RUTAS.txt" "REPORTE_POR_TIPO_RUTA.txt" "REPORTE_ESTADISTICAS_SEDES.txt")

for reporte in "${reportes[@]}"; do
    if [ -f "$reporte" ]; then
        lineas=$(wc -l < "$reporte")
        echo "   ✅ $reporte ($lineas líneas)"
    else
        echo "   ❌ No se generó $reporte"
    fi
done

echo ""
echo "🔍 Vista rápida de los reportes:"

if [ -f "REPORTE_POR_DESTINOS.txt" ]; then
    echo ""
    echo "📍 DESTINOS MÁS FRECUENTES:"
    grep -A 10 "RESUMEN POR DESTINOS:" REPORTE_POR_DESTINOS.txt | tail -10
fi

if [ -f "REPORTE_POR_TIPO_RUTA.txt" ]; then
    echo ""
    echo "🛫 TIPOS DE RUTA:"
    grep -E "(DIRECTA:|CON ESCALAS:|Total pedidos:)" REPORTE_POR_TIPO_RUTA.txt
fi

echo ""
echo "✅ Generación de reportes específicos completada"
echo "======================================================="
