# 🔧 Fix: Parseo de Coordenadas de Aeropuertos

## 🐛 Problema Detectado

El aeropuerto **OPKC (Karachi, Pakistan)** se cargaba con coordenadas incorrectas:
- ❌ **Latitud**: 0 (debería ser ~24.9°)
- ❌ **Longitud**: 67.15 (parcial, debería ser ~67.15° completo en formato DMS)

### Causa Raíz

El archivo `Aeropuertos.txt` está en **UTF-16 Big-Endian** y usa diferentes tipos de comillas Unicode:
- Comilla simple derecha: `'` (U+2019) 
- Comilla simple estándar: `'` (U+0027)
- Comilla doble: `"` (U+0022)

El regex original solo reconocía las comillas estándar ASCII, por lo que fallaba al parsear coordenadas con comillas Unicode.

---

## ✅ Solución Implementada

### Cambios en `AeropuertoService.java`

**Antes:**
```java
private static final Pattern LAT_PATTERN = Pattern.compile(
    "(?<d>\\d{1,2})[°º]\\s*(?<m>\\d{1,2})['']\\s*(?<s>\\d{1,2})[\"\"]\\s*(?<hem>[NS])"
);

private static final Pattern LON_PATTERN = Pattern.compile(
    "(?<d>\\d{1,3})[°º]\\s*(?<m>\\d{1,2})['']\\s*(?<s>\\d{1,2})[\"\"]\\s*(?<hem>[EW])"
);
```

**Después:**
```java
private static final Pattern LAT_PATTERN = Pattern.compile(
    "(?<d>\\d{1,2})[°º]\\s*(?<m>\\d{1,2})['''']\\s*(?<s>\\d{1,2})[\"\"\"']\\s*(?<hem>[NS])"
);

private static final Pattern LON_PATTERN = Pattern.compile(
    "(?<d>\\d{1,3})[°º]\\s*(?<m>\\d{1,2})['''']\\s*(?<s>\\d{1,2})[\"\"\"']\\s*(?<hem>[EW])"
);
```

### Explicación de los Cambios

#### Para Minutos (después de los grados):
- **Antes**: `['']` - Solo comillas simples ASCII
- **Ahora**: `['''']` - Acepta:
  - `'` - Comilla simple ASCII (U+0027)
  - `'` - Comilla simple izquierda (U+2018)
  - `'` - Comilla simple derecha (U+2019)

#### Para Segundos (después de los minutos):
- **Antes**: `[\"\"]` - Solo comillas dobles ASCII
- **Ahora**: `[\"\"\"']` - Acepta:
  - `"` - Comilla doble ASCII (U+0022)
  - `"` - Comilla doble izquierda (U+201C)
  - `"` - Comilla doble derecha (U+201D)
  - `'` - Comilla simple derecha (U+2019) como fallback

---

## 🧪 Casos de Prueba

### Formatos Soportados Ahora

✅ ASCII estándar:
```
24° 54' 00" N
```

✅ Unicode con comillas tipográficas:
```
24° 54' 00" N
```

✅ Mezcla de formatos:
```
24° 54' 00' N
```

---

## 📊 Resultado Esperado

Después de este fix, **OPKC (Karachi)** debería cargarse como:
- ✅ **Latitud**: 24.9° N (24° 54' 00" N)
- ✅ **Longitud**: 67.15° E (67° 09' 00" E)

---

## 🔍 Verificación

Para verificar que el fix funciona:

1. **Iniciar el backend**:
   ```bash
   mvn spring-boot:run
   ```

2. **Cargar aeropuertos**:
   ```bash
   curl -X POST http://localhost:8000/api/aeropuertos/cargar
   ```

3. **Verificar Karachi**:
   ```bash
   curl http://localhost:8000/api/aeropuertos/OPKC
   ```

4. **Comprobar en la BD**:
   ```sql
   SELECT codigo_icao, ciudad, latitud, longitud 
   FROM aeropuertos 
   WHERE codigo_icao = 'OPKC';
   ```

**Resultado esperado**:
```
codigo_icao | ciudad  | latitud | longitud
OPKC        | Karachi | 24.9    | 67.15
```

---

## 📝 Notas Adicionales

- El archivo `Aeropuertos.txt` está codificado en **UTF-16 Big-Endian**
- El `InputStreamReader` usa `UTF-16` para leer correctamente
- El método `normalizarCoordenada()` ayuda a limpiar espacios extras

---

**Fecha del Fix**: 3 de noviembre de 2025  
**Archivo Modificado**: `AeropuertoService.java`  
**Líneas Modificadas**: Regex patterns LAT_PATTERN y LON_PATTERN
