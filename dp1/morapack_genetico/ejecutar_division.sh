#!/bin/bash

# Compilar y ejecutar el sistema con división de pedidos
echo "====== COMPILANDO SISTEMA CON DIVISIÓN DE PEDIDOS ======"

cd /home/leoncio/Documentos/GitHub/ProyDise-o/dp1/morapack_genetico

# Compilar
javac -d bin -cp bin src/morapack/main/MainGeneticoConDivision.java

if [ $? -eq 0 ]; then
    echo "✅ Compilación exitosa"
    echo ""
    echo "====== EJECUTANDO SISTEMA CON DIVISIÓN ======"
    
    # Ejecutar
    java -cp bin morapack.main.MainGeneticoConDivision
    
    echo ""
    echo "====== ARCHIVOS GENERADOS ======"
    ls -la REPORTE_DIVISION_PEDIDOS.txt 2>/dev/null && echo "📁 REPORTE_DIVISION_PEDIDOS.txt" || echo "⚠️ No se generó reporte de división"
    
else
    echo "❌ Error de compilación"
    exit 1
fi
