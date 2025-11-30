# 🧹 Plan de Limpieza del Proyecto

## 📋 Resumen
Este documento detalla los archivos innecesarios que se pueden archivar o eliminar para mejorar la organización del proyecto.

---

## 🗂️ **BACKEND**

### ❌ Archivos HTML de Prueba (NO SE USAN - Eliminables)
Estos archivos fueron utilizados para pruebas durante el desarrollo pero ya NO se necesitan:

```bash
backend/test-simulation-stomp.html
backend/test-simulation-client.html
backend/test-planificacion-websocket.html
backend/info/ejemplo_sse_consola.html
backend/info/test-sse-simulacion.html
backend/info/test-simulacion-sse.html
backend/info/websocket-test.html
backend/info/test-simulation-client.html
```

**✅ MANTENER:**
- `backend/visualizador-ag.html` - Visualizador funcional actual

### 📚 Archivos de Documentación Obsoleta (Archivables)

**❌ Documentación de SSE (Ya migrado a STOMP):**
```bash
backend/RESUMEN_MIGRACION_SSE_A_STOMP.md
backend/MIGRACION_SSE_A_STOMP.md
backend/info/IMPLEMENTACION_SSE_SIMULACION.md
backend/info/RESUMEN_SSE_SIMPLE.md
backend/info/TRACKING_VUELOS_SSE.md
backend/info/SSE_SIMULACION_CON_RUTAS.md
backend/info/PRUEBA_SSE.md
backend/info/GUIA_USO_SSE.md
backend/info/CICLO_VIDA_TRACKING.md
```

**❌ Guías/Prompts de desarrollo obsoletos:**
```bash
backend/PROMPT_INTEGRACION_FRONTEND.md
backend/PROMPT_VISUALIZADOR_AG.md
backend/CAMBIOS_JSON_WEBSOCKET_FRONTEND.md
backend/GUIA_TEST_WEBSOCKET.md
backend/INSTRUCCIONES_FRONTEND_MAPA.md
backend/info/ALGORITMO_GENETICO_OLD.md
backend/info/planificaidor.md (typo en nombre)
backend/info/especificacionCaso.md
backend/info/CLAUDE.md
```

**❌ Documentación de bugs/fixes antiguos:**
```bash
backend/info/FIX_PARSEO_COORDENADAS.md
backend/info/SOLUCION_TIMEOUT.md
backend/info/PROBLEMA_LENTITUD_PLANES_VUELO.md
backend/info/SOLUCION_COORDENADAS_NO_ACTUALIZAN.md
backend/info/FIX_ORDEN_COLUMNAS_AEROPUERTOS.md
backend/info/OPTIMIZACION_FINAL_DELETE.md
backend/info/OPTIMIZACIONES_VELOCIDAD.md
```

**✅ MANTENER (Documentación importante):**
```bash
backend/README_SIMULATION.md
backend/ARQUITECTURA_SIMULACION.md
backend/GUIA_INTEGRACION_AG.md
backend/ESTRUCTURA_JSON_WEBSOCKET_REAL.md
backend/info/ALGORITMO_GENETICO_ESPAÑOL.md
backend/info/API_PEDIDOS.md
backend/info/README.md
backend/info/ARQUITECTURA_SIMULACION.md
```

---

## 🎨 **FRONTEND**

### 📚 Archivos de Documentación Obsoleta (Archivables)

**❌ Guías de migración/integración antiguas:**
```bash
front/ADAPTACION_NUEVA_ESTRUCTURA_JSON.md
front/ADAPTACION_VUELOS_DIRECTOS.md
front/INTEGRACION_SSE.md
front/INTEGRACION_WEBSOCKET.md
front/INTEGRACION_WEBSOCKET_COMPLETA.md
front/INTEGRACION_WEBSOCKET_PLANIFICACION.md
front/GUIA_INTEGRACION_WEBSOCKET_STOMP.md
front/GUIA_RAPIDA_WEBSOCKET.md
front/GUIA_MINIMALISTA_WEBSOCKET.md
front/BACKEND_SPRING_BOOT_WEBSOCKET.md
front/DEBUG_WEBSOCKET.md
front/CHECKLIST_WEBSOCKET_STOMP.md
```

**❌ Guías duplicadas o versiones antiguas:**
```bash
front/EJEMPLOS_WEBSOCKET.md
front/DOCUMENTACION_WEBSOCKET.md
front/GUIA_WEBSOCKET_STOMP_SIMULADOR.md
front/webscoket.md (typo - mismo contenido que otros)
front/SOLUCION_VUELOS_WEBSOCKET.md
```

**❌ Documentación del sistema híbrido (ya implementado):**
```bash
front/SISTEMA_HIBRIDO_ANIMACION.md
front/SISTEMA_HIBRIDO_DIAGRAMA.md
front/SISTEMA_HIBRIDO_RESUMEN.md
front/MEJORAS_ANIMACION_INTEGRADAS.md
front/ANIMACION_AVIONES.md
```

**✅ MANTENER (Documentación importante):**
```bash
front/README.md - Principal del proyecto
front/RESUMEN_IMPLEMENTACION_STOMP.md - Implementación actual
front/ENTREGA_FINAL_WEBSOCKET.md - Documentación de entrega
front/GUIA_SIMULADOR_LOGISTICO.md - Guía de usuario
front/EJEMPLO_USO_PLANIFICACION_MAPA.md - Ejemplos útiles
front/src/config/README.md - Configuración
```

---

## 📦 **RAÍZ DEL PROYECTO**

**❌ Archivos de debugging/soluciones temporales:**
```bash
CAMBIOS_WEBSOCKET_PLANIFICACION.md
CHECKLIST_WEBSOCKET_PLANIFICACION.md
CORRECCIONES_FINALES_DEBUG.md
DIAGNOSTICO_RAPIDO.md
INSTRUCCIONES_RAPIDAS.md
README_SIMULADOR_SIMPLE.md
RESUMEN_CORRECCIONES_WEBSOCKET.md
SOLUCION_SIMPLE_REST.md
TEST_CARGA_AEROPUERTOS.md
test_planificacion_simple.sh
test_websocket_planificacion.html
```

**✅ MANTENER:**
```bash
README.md - Documentación principal
```

---

## 🚀 **Comandos de Limpieza**

### Opción 1: Archivar (Recomendado - mantiene historial)
```bash
# Crear directorios de archivo
mkdir -p _archived/backend/{tests,docs}
mkdir -p _archived/frontend/docs
mkdir -p _archived/root

# Backend - Tests HTML
mv backend/*.html _archived/backend/tests/ 2>/dev/null || true
mv backend/info/*.html _archived/backend/tests/ 2>/dev/null || true
# Excepto visualizador-ag.html
mv _archived/backend/tests/visualizador-ag.html backend/ 2>/dev/null || true

# Backend - Docs obsoletos
mv backend/RESUMEN_MIGRACION_SSE_A_STOMP.md _archived/backend/docs/
mv backend/MIGRACION_SSE_A_STOMP.md _archived/backend/docs/
mv backend/PROMPT_*.md _archived/backend/docs/
mv backend/CAMBIOS_JSON_WEBSOCKET_FRONTEND.md _archived/backend/docs/
mv backend/GUIA_TEST_WEBSOCKET.md _archived/backend/docs/
mv backend/INSTRUCCIONES_FRONTEND_MAPA.md _archived/backend/docs/

# Backend/info - Docs obsoletos
find backend/info -name "*SSE*" -o -name "*FIX_*" -o -name "*SOLUCION_*" | xargs -I {} mv {} _archived/backend/docs/

# Frontend - Docs obsoletos
mv front/ADAPTACION_*.md _archived/frontend/docs/
mv front/INTEGRACION_*.md _archived/frontend/docs/
mv front/GUIA_*_WEBSOCKET*.md _archived/frontend/docs/
mv front/DEBUG_WEBSOCKET.md _archived/frontend/docs/
mv front/SISTEMA_HIBRIDO_*.md _archived/frontend/docs/
mv front/*ANIMACION*.md _archived/frontend/docs/
mv front/webscoket.md _archived/frontend/docs/
mv front/SOLUCION_VUELOS_WEBSOCKET.md _archived/frontend/docs/
mv front/EJEMPLOS_WEBSOCKET.md _archived/frontend/docs/
mv front/DOCUMENTACION_WEBSOCKET.md _archived/frontend/docs/
mv front/CHECKLIST_WEBSOCKET_STOMP.md _archived/frontend/docs/

# Raíz - Archivos temporales
mv CAMBIOS_WEBSOCKET_PLANIFICACION.md _archived/root/
mv CHECKLIST_WEBSOCKET_PLANIFICACION.md _archived/root/
mv CORRECCIONES_FINALES_DEBUG.md _archived/root/
mv DIAGNOSTICO_RAPIDO.md _archived/root/
mv INSTRUCCIONES_RAPIDAS.md _archived/root/
mv README_SIMULADOR_SIMPLE.md _archived/root/
mv RESUMEN_CORRECCIONES_WEBSOCKET.md _archived/root/
mv SOLUCION_SIMPLE_REST.md _archived/root/
mv TEST_CARGA_AEROPUERTOS.md _archived/root/
mv test_planificacion_simple.sh _archived/root/
mv test_websocket_planificacion.html _archived/root/
```

### Opción 2: Eliminar permanentemente (NO recomendado)
```bash
# ⚠️ CUIDADO: Esto elimina los archivos permanentemente
# Solo ejecutar si estás 100% seguro

# Backend tests HTML
rm backend/test-*.html
rm backend/info/test-*.html
rm backend/info/ejemplo_*.html
rm backend/info/websocket-test.html

# Docs obsoletos
rm backend/*SSE*.md
rm backend/PROMPT_*.md
rm backend/info/*FIX_*.md
rm backend/info/*SOLUCION_*.md

# Frontend docs
rm front/*ADAPTACION*.md
rm front/*INTEGRACION*.md
rm front/*WEBSOCKET*.md
rm front/*SISTEMA_*.md

# Raíz
rm CAMBIOS_*.md
rm CORRECCIONES_*.md
rm DIAGNOSTICO_*.md
rm test_*.sh
rm test_*.html
```

---

## 📊 **Estadísticas de Limpieza**

### Antes:
- **Backend:** ~110 archivos MD + 18 HTML
- **Frontend:** ~58 archivos MD
- **Raíz:** ~11 archivos obsoletos
- **Total:** ~197 archivos innecesarios

### Después (archivando):
- **Archivos eliminados del workspace activo:** ~180
- **Archivos mantenidos en _archived:** ~180 (accesibles si se necesitan)
- **Reducción de ruido:** ~91%

### Beneficios:
✅ Navegación más rápida en el IDE
✅ Búsquedas más precisas
✅ Menor confusión sobre qué documentación usar
✅ Historial preservado en _archived
✅ Mejor rendimiento de Git
✅ Carga más rápida del proyecto

---

## ⚠️ **Importante**

1. **Antes de ejecutar:** Haz un commit de tu trabajo actual
2. **Revisa los archivos:** Asegúrate de que no hay nada crítico
3. **Usa opción 1 (archivar):** Más seguro, puedes recuperar archivos si los necesitas
4. **Después de limpiar:** Haz commit con mensaje: `chore: archive obsolete documentation and test files`

---

## 🔍 **Verificación Post-Limpieza**

```bash
# Verificar archivos importantes aún existen
ls backend/README_SIMULATION.md
ls backend/visualizador-ag.html
ls front/README.md
ls README.md

# Verificar archivos archivados
ls -R _archived/
```

---

**Generado:** 26 de noviembre de 2025
**Versión:** 1.0
