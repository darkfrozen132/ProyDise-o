# 🔍 Guía Rápida: Encontrar Origen de Errores PNG (400)

## 🎯 Objetivo
Localizar qué componente está intentando cargar archivos `0.png`, `1.png`, `2.png`, etc. que no existen o el servidor rechaza.

---

## 🚀 Método 1: Buscar en el Código Frontend

### Paso 1: Buscar referencias directas
```bash
cd front
grep -r "\.png" src/ --include="*.js" --include="*.jsx" | grep -E "[0-9]+"
```

### Paso 2: Buscar construcción dinámica de URLs
```bash
grep -r "\${.*}\.png" src/ --include="*.js" --include="*.jsx"
grep -r "`.*\.png`" src/ --include="*.js" --include="*.jsx"
grep -r "\.png'" src/ --include="*.js" --include="*.jsx"
```

### Paso 3: Buscar importaciones de imágenes
```bash
grep -r "import.*\.png" src/ --include="*.js" --include="*.jsx"
grep -r "require.*\.png" src/ --include="*.js" --include="*.jsx"
```

---

## 🔬 Método 2: Usar Chrome DevTools

### Paso 1: Ver Stack Trace de la petición
1. Abrir DevTools (F12)
2. Ir a pestaña **Network**
3. Filtrar por "png"
4. Hacer clic en cualquier petición fallida (e.g., `3.png`)
5. Ver pestaña **Initiator** → muestra qué archivo JS hizo la petición

### Paso 2: Ver el Request Headers
```
URL completa: http://localhost:8000/3.png
Referer: http://localhost:3000/simulacion
```
Esto te dice desde qué página se está haciendo la petición.

### Paso 3: Pausar en peticiones XHR
1. DevTools → Sources
2. Clic derecho en panel izquierdo → Add folder to workspace
3. XHR/fetch breakpoints → Add breakpoint: `*.png`
4. Cuando se dispare, ver el call stack

---

## 🎯 Método 3: Buscar en el Backend

### Paso 1: Ver logs del servidor
```bash
cd backend
tail -f info/backend.log | grep -E "400|png"
```

### Paso 2: Buscar endpoints que sirvan imágenes
```bash
grep -r "@GetMapping.*png" src/ --include="*.java"
grep -r "ResponseEntity.*png" src/ --include="*.java"
grep -r "/images/" src/ --include="*.java"
```

### Paso 3: Verificar configuración de recursos estáticos
```bash
grep -r "addResourceHandlers" src/ --include="*.java"
grep -r "ResourceHandler" src/ --include="*.java"
```

---

## 🧪 Método 4: Análisis por Contexto

### Pistas desde los errores
Los archivos son:
```
0.png, 1.png, 2.png, 3.png, 4.png, 5.png, 6.png
```

**Patrón**: Números secuenciales → Probablemente se está iterando sobre un array

### Componentes Sospechosos

#### 1. **Avatares de Usuarios**
```javascript
// Ejemplo común:
users.map((user, index) => (
    <img src={`${index}.png`} alt="avatar" />
))
```

#### 2. **Iconos de Paquetes/Pedidos**
```javascript
// Podría ser:
packages.map(pkg => (
    <img src={`${pkg.priority}.png`} alt="priority" />
))
```

#### 3. **Indicadores de Estado**
```javascript
// Quizás:
<img src={`${statusCode}.png`} alt="status" />
```

#### 4. **Galería de Imágenes**
```javascript
// Tal vez:
Array.from({length: 7}, (_, i) => (
    <img src={`${i}.png`} key={i} />
))
```

---

## 🔎 Método 5: Buscar en Archivos Específicos

### Componentes Comunes que Usan Imágenes

```bash
# Dashboard/Panel Principal
cat src/pages/dashboard/*.js | grep -i png

# Componentes de Pedidos
cat src/pages/pedidos/*.js | grep -i png

# Componentes de Usuarios
cat src/pages/users/*.js | grep -i png
cat src/components/user/*.js | grep -i png

# Layouts
cat src/layouts/*.js | grep -i png

# Headers/Footers
cat src/components/header/*.js | grep -i png
cat src/components/footer/*.js | grep -i png
```

---

## 💡 Soluciones Comunes

### Solución 1: Imagen No Encontrada
```javascript
// ❌ PROBLEMA
<img src="/3.png" alt="icon" />

// ✅ SOLUCIÓN: Verificar ruta correcta
<img src="/images/icons/3.png" alt="icon" />
```

### Solución 2: Construcción Dinámica Incorrecta
```javascript
// ❌ PROBLEMA
<img src={`${index}.png`} alt="icon" />

// ✅ SOLUCIÓN: Ruta completa
<img src={`/assets/icons/${index}.png`} alt="icon" />
```

### Solución 3: Usar Placeholder si No Existe
```javascript
// ✅ SOLUCIÓN: Fallback
<img 
    src={`/icons/${index}.png`} 
    onError={(e) => {
        e.target.src = '/icons/default.png';
    }}
    alt="icon" 
/>
```

### Solución 4: Usar SVG en Lugar de PNG
```javascript
// ✅ MEJOR: Usar SVG o icon font
<i className={`fas fa-icon-${index}`} />
```

---

## 📋 Checklist de Investigación

- [ ] Buscar `*.png` en todo el código frontend
- [ ] Revisar Network tab en DevTools (Initiator)
- [ ] Ver logs del backend
- [ ] Buscar construcción dinámica de URLs con template literals
- [ ] Revisar componentes de Dashboard
- [ ] Revisar componentes de Pedidos
- [ ] Revisar componentes de Usuarios
- [ ] Verificar configuración de recursos estáticos en backend

---

## 🎯 Comando Rápido: Encontrar TODOS los PNG

```bash
cd front
# Buscar TODAS las referencias a PNG en el frontend
find src -type f \( -name "*.js" -o -name "*.jsx" \) -exec grep -l "\.png" {} \; | \
while read file; do
    echo "=== $file ==="
    grep -n "\.png" "$file"
    echo ""
done
```

---

## 🚨 Si No Encuentras Nada

### Posibilidad 1: Librería de Terceros
Alguna librería podría estar intentando cargar imágenes:
```bash
# Buscar en node_modules (cuidado, es lento)
grep -r "\.png" node_modules/*/dist/*.js | grep -E "[0-9]+"
```

### Posibilidad 2: CSS Background Images
```bash
grep -r "background.*url.*png" src/
grep -r "background-image.*png" src/
```

### Posibilidad 3: HTML Directo
```bash
grep -r "<img.*src.*png" src/
grep -r "srcset.*png" src/
```

---

## ✅ Resultado Esperado

Una vez encontrado el archivo, deberías ver algo como:

```javascript
// Archivo: src/components/PackageStatus.js
// Línea: 45
{packages.map((pkg, index) => (
    <img src={`${index}.png`} alt="package icon" />
    // ^^^ AQUÍ ESTÁ EL PROBLEMA
))}
```

---

## 🎉 Próximos Pasos

1. **Ejecutar comandos de búsqueda** en orden
2. **Documentar hallazgos** en un archivo RESULTADOS_PNG.md
3. **Corregir el componente** encontrado
4. **Verificar en navegador** que desaparecen los errores

---

**Nota**: Los errores PNG **NO afectan** la visualización de aviones (ya usa SVG).  
Esta guía es para **limpiar la consola** y mejorar la calidad del código.

**Fecha**: 26 de noviembre de 2025  
**Prioridad**: 🟡 BAJA (no afecta funcionalidad crítica)
