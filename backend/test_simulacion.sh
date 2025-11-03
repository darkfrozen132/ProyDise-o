#!/bin/bash

# Script de prueba para el Sistema de Simulación Incremental SSE
# Asegúrate de que el backend esté corriendo en http://localhost:8080

BASE_URL="http://localhost:8080/api/simulacion"

echo "🧪 PRUEBA DEL SISTEMA DE SIMULACIÓN INCREMENTAL SSE"
echo "=================================================="
echo ""

# 1. Health Check
echo "1️⃣ Health Check..."
curl -s "${BASE_URL}/health" | jq '.'
echo ""
echo ""

# 2. Verificar estado inicial
echo "2️⃣ Estado inicial..."
curl -s "${BASE_URL}/estado" | jq '.'
echo ""
echo ""

# 3. Iniciar simulación
echo "3️⃣ Iniciando simulación..."
curl -s -X POST "${BASE_URL}/iniciar" \
  -H "Content-Type: application/json" \
  -d '{
    "fecha": "2025-01-15",
    "saltoMinutos": 5,
    "tamanioPoblacion": 50,
    "maxGeneraciones": 200
  }' | jq '.'
echo ""
echo ""

# 4. Esperar un poco
echo "4️⃣ Esperando 2 segundos..."
sleep 2
echo ""

# 5. Verificar estado activo
echo "5️⃣ Estado durante la simulación..."
curl -s "${BASE_URL}/estado" | jq '.'
echo ""
echo ""

# 6. Conectar al stream SSE (solo 10 segundos)
echo "6️⃣ Conectando al stream SSE (mostrando primeros 10 segundos)..."
echo "   Presiona Ctrl+C para detener antes"
echo ""
timeout 10 curl -N "${BASE_URL}/stream"
echo ""
echo ""

# 7. Detener simulación
echo "7️⃣ Deteniendo simulación..."
curl -s -X POST "${BASE_URL}/detener" | jq '.'
echo ""
echo ""

# 8. Verificar estado final
echo "8️⃣ Estado final..."
curl -s "${BASE_URL}/estado" | jq '.'
echo ""
echo ""

echo "✅ Prueba completada!"
