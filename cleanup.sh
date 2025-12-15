#!/bin/bash

# 🧹 Script de Limpieza Automática del Proyecto
# Archiva archivos obsoletos en _archived/ preservando el historial

# No salir en error, continuar archivando
set +e
shopt -s nullglob

echo "🧹 =========================================="
echo "   LIMPIEZA AUTOMÁTICA DEL PROYECTO"
echo "   Archivando archivos obsoletos..."
echo "=========================================="
echo ""

# Colores
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Directorio base
BASE_DIR="/home/leoncio/Documentos/GitHub/ProyDise-o"
cd "$BASE_DIR"

echo -e "${BLUE}📂 Creando estructura de archivos...${NC}"
mkdir -p _archived/backend/{tests,docs}
mkdir -p _archived/frontend/docs
mkdir -p _archived/root

echo ""
echo -e "${GREEN}✅ Estructura creada${NC}"
echo ""

# Contador
TOTAL=0

# ==================== BACKEND ====================
echo -e "${YELLOW}🔧 BACKEND - Archivando archivos de prueba HTML...${NC}"

# Tests HTML (excepto visualizador-ag.html)
shopt -s nullglob
for file in backend/test-*.html backend/info/test-*.html backend/info/ejemplo_*.html backend/info/websocket-test.html; do
    if [ -f "$file" ] && [[ "$file" != *"visualizador-ag.html"* ]]; then
        mv "$file" _archived/backend/tests/ && echo "  📦 $file" && ((TOTAL++)) || true
    fi
done

echo ""
echo -e "${YELLOW}🔧 BACKEND - Archivando documentación obsoleta...${NC}"

# Docs de migración SSE
for file in backend/*SSE*.md backend/*MIGRACION*.md; do
    if [ -f "$file" ]; then
        mv "$file" _archived/backend/docs/ && echo "  📦 $file" && ((TOTAL++)) || true
    fi
done

# Prompts y guías de desarrollo
for file in backend/PROMPT_*.md backend/CAMBIOS_JSON_WEBSOCKET_FRONTEND.md backend/GUIA_TEST_WEBSOCKET.md backend/INSTRUCCIONES_FRONTEND_MAPA.md; do
    if [ -f "$file" ]; then
        mv "$file" _archived/backend/docs/ && echo "  📦 $file" && ((TOTAL++)) || true
    fi
done

# Docs en info/
for file in backend/info/*SSE*.md backend/info/*FIX_*.md backend/info/*SOLUCION_*.md backend/info/*OPTIMIZACION*.md backend/info/ALGORITMO_GENETICO_OLD.md backend/info/planificaidor.md backend/info/especificacionCaso.md backend/info/CLAUDE.md; do
    if [ -f "$file" ]; then
        mv "$file" _archived/backend/docs/ && echo "  📦 $file" && ((TOTAL++)) || true
    fi
done

echo ""

# ==================== FRONTEND ====================
echo -e "${YELLOW}🎨 FRONTEND - Archivando documentación obsoleta...${NC}"

# Guías de integración/migración
for file in front/ADAPTACION_*.md front/INTEGRACION_*.md front/GUIA_*_WEBSOCKET*.md front/DEBUG_WEBSOCKET.md front/BACKEND_SPRING_BOOT_WEBSOCKET.md; do
    if [ -f "$file" ]; then
        mv "$file" _archived/frontend/docs/ && echo "  📦 $file" && ((TOTAL++)) || true
    fi
done

# Sistema híbrido y animación
for file in front/SISTEMA_HIBRIDO_*.md front/*ANIMACION*.md front/MEJORAS_ANIMACION_INTEGRADAS.md; do
    if [ -f "$file" ]; then
        mv "$file" _archived/frontend/docs/ && echo "  📦 $file" && ((TOTAL++)) || true
    fi
done

# Otros docs obsoletos
for file in front/webscoket.md front/SOLUCION_VUELOS_WEBSOCKET.md front/EJEMPLOS_WEBSOCKET.md front/DOCUMENTACION_WEBSOCKET.md front/CHECKLIST_WEBSOCKET_STOMP.md; do
    if [ -f "$file" ]; then
        mv "$file" _archived/frontend/docs/ && echo "  📦 $file" && ((TOTAL++)) || true
    fi
done

echo ""

# ==================== RAÍZ ====================
echo -e "${YELLOW}📁 RAÍZ - Archivando archivos temporales...${NC}"

# Archivos de debugging y soluciones temporales
for file in CAMBIOS_WEBSOCKET_PLANIFICACION.md CHECKLIST_WEBSOCKET_PLANIFICACION.md CORRECCIONES_FINALES_DEBUG.md DIAGNOSTICO_RAPIDO.md INSTRUCCIONES_RAPIDAS.md README_SIMULADOR_SIMPLE.md RESUMEN_CORRECCIONES_WEBSOCKET.md SOLUCION_SIMPLE_REST.md TEST_CARGA_AEROPUERTOS.md test_planificacion_simple.sh test_websocket_planificacion.html; do
    if [ -f "$file" ]; then
        mv "$file" _archived/root/ && echo "  📦 $file" && ((TOTAL++)) || true
    fi
done

echo ""
echo -e "${GREEN}=========================================="
echo -e "✅ LIMPIEZA COMPLETADA"
echo -e "=========================================="
echo -e "${NC}"
echo -e "📊 ${GREEN}Total de archivos archivados: $TOTAL${NC}"
echo ""
echo -e "${BLUE}📂 Archivos archivados en:${NC}"
echo "   - _archived/backend/tests/     (Tests HTML)"
echo "   - _archived/backend/docs/      (Docs obsoletos backend)"
echo "   - _archived/frontend/docs/     (Docs obsoletos frontend)"
echo "   - _archived/root/              (Archivos temporales raíz)"
echo ""
echo -e "${YELLOW}⚠️  IMPORTANTE:${NC}"
echo "   1. Verifica que todo funciona correctamente"
echo "   2. Si necesitas un archivo archivado, búscalo en _archived/"
echo "   3. Haz commit: git add -A && git commit -m 'chore: archive obsolete files'"
echo ""
echo -e "${GREEN}✨ Proyecto limpio y organizado!${NC}"
