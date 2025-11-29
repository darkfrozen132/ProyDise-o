# 📦 Archivos Archivados

Este directorio contiene archivos obsoletos del proyecto que fueron archivados el **26 de noviembre de 2025**.

## 📂 Estructura

```
_archived/
├── backend/
│   ├── tests/        # Tests HTML obsoletos (SSE, WebSocket nativos, etc.)
│   └── docs/         # Documentación obsoleta del backend
├── frontend/
│   └── docs/         # Documentación obsoleta del frontend
└── root/             # Archivos temporales de la raíz del proyecto
```

## 🗂️ Contenido

### Backend Tests (7 archivos)
- Tests de SSE (obsoletos, ahora usamos STOMP)
- Tests de WebSocket nativos
- Ejemplos de consola

### Backend Docs (24 archivos)
- Documentación de migración SSE → STOMP
- Prompts de desarrollo
- Guías obsoletas de integración
- Fixes y soluciones de bugs antiguos
- Versiones antiguas de algoritmos

### Frontend Docs (21 archivos)
- Guías de integración WebSocket obsoletas
- Documentación del sistema híbrido (ya implementado)
- Guías de migración
- Debug y checklists temporales
- Documentación duplicada

### Root (11 archivos)
- Scripts de prueba temporales
- Archivos de debugging
- Checklists de desarrollo
- Soluciones temporales

## ⚠️ Importante

- **NO ELIMINES** esta carpeta sin revisar su contenido
- Si necesitas un archivo, búscalo aquí antes de recrearlo
- Estos archivos se mantienen por si necesitas consultar algo del historial
- No están en uso activo pero pueden tener información valiosa

## 🔍 Buscar un Archivo

```bash
# Buscar por nombre
find _archived/ -name "*nombre*"

# Ver estructura completa
tree _archived/

# Buscar contenido
grep -r "texto a buscar" _archived/
```

## 📝 Notas

- Total archivado: **63 archivos**
- Reducción de ruido en workspace: **~91%**
- Archivos importantes se mantuvieron en sus ubicaciones originales

---

**Fecha de archivo:** 26 de noviembre de 2025  
**Razón:** Limpieza y organización del proyecto  
**Script:** `cleanup.sh`
