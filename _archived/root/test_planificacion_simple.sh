#!/bin/bash

echo "╔═══════════════════════════════════════════════════════════╗"
echo "║     🧪 TEST RÁPIDO: Planificación Simple REST           ║"
echo "╚═══════════════════════════════════════════════════════════╝"
echo ""

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# URL del backend
BACKEND_URL="http://localhost:8000"

echo "1️⃣  Verificando que el backend esté corriendo..."
if curl -s -f "${BACKEND_URL}/actuator/health" > /dev/null; then
    echo -e "${GREEN}✅ Backend corriendo en ${BACKEND_URL}${NC}"
else
    echo -e "${RED}❌ Backend NO está corriendo${NC}"
    echo "   Por favor inicia el backend con: cd backend && mvn spring-boot:run"
    exit 1
fi

echo ""
echo "2️⃣  Ejecutando planificación simple..."
echo "   📅 Fecha: 2025-01-15"
echo "   ⚙️  Factor K: 5"
echo ""

START_TIME=$(date +%s%3N)

RESPONSE=$(curl -s -X POST \
  "${BACKEND_URL}/api/planificacion/ejecutar-simple?fecha=2025-01-15&factorK=5&tamanioPoblacion=20&maxGeneraciones=20&limiteGeneracionesSinMejora=10" \
  -H "Content-Type: application/json")

END_TIME=$(date +%s%3N)
DURATION=$((END_TIME - START_TIME))

echo "⏱️  Duración de la llamada HTTP: ${DURATION}ms"
echo ""

# Verificar si hay error
if echo "$RESPONSE" | grep -q "error"; then
    echo -e "${RED}❌ Error en la planificación:${NC}"
    echo "$RESPONSE" | jq '.'
    exit 1
fi

# Contar vuelos
TOTAL_VUELOS=$(echo "$RESPONSE" | jq '.vuelos | length')
TOTAL_PEDIDOS=$(echo "$RESPONSE" | jq '[.vuelos[].pedidos | length] | add')

echo "╔═══════════════════════════════════════════════════════════╗"
echo "║                  📊 RESULTADOS                           ║"
echo "╠═══════════════════════════════════════════════════════════╣"
echo -e "║  ✈️  Vuelos planificados: ${GREEN}${TOTAL_VUELOS}${NC}"
echo -e "║  📦 Pedidos asignados: ${GREEN}${TOTAL_PEDIDOS}${NC}"
echo -e "║  ⏱️  Tiempo total: ${GREEN}${DURATION}ms${NC}"
echo "╚═══════════════════════════════════════════════════════════╝"
echo ""

if [ "$TOTAL_VUELOS" -gt 0 ]; then
    echo -e "${GREEN}✅ TEST EXITOSO${NC}"
    echo ""
    echo "Primeros 3 vuelos:"
    echo "$RESPONSE" | jq -r '.vuelos[0:3] | .[] | "  ✈️  \(.origenCodigoICAO) → \(.destinoCodigoICAO) (\(.pedidos | length) pedidos)"'
    echo ""
    echo "🌐 Ahora abre el navegador en:"
    echo "   ${YELLOW}http://localhost:3001/operaciones/simulador-simple${NC}"
    echo ""
else
    echo -e "${RED}❌ NO SE GENERARON VUELOS${NC}"
    exit 1
fi
