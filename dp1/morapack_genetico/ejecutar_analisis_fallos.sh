#!/bin/bash

echo "============================================"
echo "      ANÁLISIS DE PEDIDOS FALLIDOS"  
echo "============================================"

# Navegar al directorio del proyecto
cd /home/leoncio/Documentos/GitHub/ProyDise-o/dp1/morapack_genetico

# Compilar el analizador
echo "Compilando AnalizadorFallos..."
javac -cp "src" -d "bin" src/morapack/main/AnalizadorFallos.java src/morapack/datos/CargadorPedidosUltrafinal.java src/morapack/modelo/Pedido.java

if [ $? -eq 0 ]; then
    echo "✅ Compilación exitosa"
    echo ""
    echo "Ejecutando análisis de fallos..."
    echo "=================================="
    
    # Ejecutar el analizador
    java -cp "bin" morapack.main.AnalizadorFallos
    
    echo ""
    echo "============================================"
    echo "        ANÁLISIS COMPLETADO"
    echo "============================================"
else
    echo "❌ Error en la compilación"
    exit 1
fi
