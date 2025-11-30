#!/bin/bash

# 🎯 Script de Prueba - Sistema de Simulación SSE
# Ejecuta este script para probar el sistema completo

echo "🚀 Sistema de Simulación SSE - Prueba Completa"
echo "=============================================="
echo ""

# Colores para output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# URL base
BASE_URL="http://localhost:8000"

# Función para imprimir con color
print_step() {
    echo -e "${BLUE}📌 $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ️  $1${NC}"
}

# 1. Health check
print_step "Paso 1: Verificando que el servidor esté activo..."
response=$(curl -s -o /dev/null -w "%{http_code}" $BASE_URL/api/simulacion/health)

if [ "$response" -eq 200 ]; then
    print_success "Servidor activo y funcionando"
    curl -s $BASE_URL/api/simulacion/health | python3 -m json.tool
else
    print_error "Servidor no responde. ¿Está corriendo el backend?"
    print_info "Ejecuta: mvn spring-boot:run"
    exit 1
fi

echo ""
echo "─────────────────────────────────────────────"
echo ""

# 2. Verificar estado inicial
print_step "Paso 2: Verificando estado inicial de la simulación..."
curl -s $BASE_URL/api/simulacion/estado | python3 -m json.tool

echo ""
echo "─────────────────────────────────────────────"
echo ""

# 3. Iniciar simulación
print_step "Paso 3: Iniciando simulación..."
print_info "Parámetros: fecha=2025-01-15, salto=5 min, población=50, generaciones=200"

init_response=$(curl -s -X POST $BASE_URL/api/simulacion/iniciar \
  -H "Content-Type: application/json" \
  -d '{
    "fecha": "2025-01-15",
    "saltoMinutos": 5,
    "tamanioPoblacion": 50,
    "maxGeneraciones": 200
  }')

echo "$init_response" | python3 -m json.tool

# Verificar si fue exitoso
success=$(echo "$init_response" | grep -o '"success"[[:space:]]*:[[:space:]]*true')
if [ -z "$success" ]; then
    print_error "No se pudo iniciar la simulación"
    echo "$init_response"
    exit 1
fi

print_success "Simulación iniciada correctamente"

echo ""
echo "─────────────────────────────────────────────"
echo ""

# 4. Conectar a SSE stream
print_step "Paso 4: Conectando al stream SSE..."
print_info "Recibirás eventos cada segundo. Presiona Ctrl+C para detener."
print_info "Cada evento muestra el progreso de la planificación incremental"

echo ""
echo "📡 EVENTOS EN TIEMPO REAL:"
echo "══════════════════════════"

# Conectar al stream y mostrar eventos
curl -N $BASE_URL/api/simulacion/stream 2>/dev/null | while IFS= read -r line; do
    if [[ $line == event:* ]]; then
        # Extraer tipo de evento
        event_type=$(echo "$line" | sed 's/event: //')
        
        if [ "$event_type" == "estado" ]; then
            echo -e "${BLUE}🔵 Estado Inicial${NC}"
        elif [ "$event_type" == "tick" ]; then
            echo -e "${GREEN}⏱️  Tick${NC}"
        elif [ "$event_type" == "finalizado" ]; then
            echo -e "${YELLOW}✨ Finalizado${NC}"
        fi
    elif [[ $line == data:* ]]; then
        # Extraer y formatear JSON
        json_data=$(echo "$line" | sed 's/data: //')
        echo "$json_data" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    if 'tick' in data:
        print(f\"   Tick: {data['tick']}, Minutos: {data['minutoActual']}, Progreso: {data['progreso']*100:.1f}%\")
        if 'planificacion' in data and 'metadata' in data['planificacion']:
            meta = data['planificacion']['metadata']
            print(f\"   Pedidos: {meta.get('pedidosProcesados', 0)}, Objetivo: {meta.get('objetivo', 0):.2f}\")
    elif 'activa' in data:
        print(f\"   Activa: {data['activa']}, Clientes: {data.get('clientesConectados', 0)}\")
    elif 'mensaje' in data:
        print(f\"   {data['mensaje']}\")
    else:
        print(f\"   {json.dumps(data, indent=2)}\")
except:
    print(f\"   {sys.stdin.read()}\")
" 2>/dev/null || echo "   $json_data"
        echo ""
    fi
done

echo ""
print_success "Stream finalizado"
