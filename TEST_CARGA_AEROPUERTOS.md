# Test de Carga de Aeropuertos

Este archivo contiene instrucciones simples para probar la funcionalidad de carga de aeropuertos.

## Cambios Realizados

### 1. Servicio AeropuertoService - Parseo con Regex

El método `cargarDesdeArchivo()` ahora implementa:

- **Expresiones regulares** para parsear las líneas del archivo con mayor robustez
- **Detección automática de continentes** leyendo los encabezados del archivo
- **Normalización de coordenadas** para manejar diferentes variantes de símbolos (°, º, ', ", etc.)
- **Conversión DMS a decimal** usando regex para extraer grados, minutos y segundos
- **Manejo de errores mejorado** con logs informativos

### 2. Nuevo Endpoint de Prueba

Se agregó el endpoint `POST /api/aeropuertos/test-carga` que:

- Carga los aeropuertos desde el archivo
- Retorna un resumen con:
  - Total de aeropuertos cargados
  - Conteo por continente
  - Muestra de los primeros 3 aeropuertos con sus coordenadas

## Instrucciones de Prueba

### Paso 1: Iniciar la aplicación

```bash
mvn spring-boot:run
```

### Paso 2: Probar el endpoint de carga

**Opción A: Usar curl (Git Bash o WSL)**
```bash
curl -X POST http://localhost:8000/api/aeropuertos/test-carga
```

**Opción B: Usar PowerShell**
```powershell
Invoke-WebRequest -Uri http://localhost:8000/api/aeropuertos/test-carga -Method POST
```

**Opción C: Usar navegador o Postman**
- URL: `http://localhost:8000/api/aeropuertos/test-carga`
- Método: POST

### Paso 3: Verificar la respuesta

La respuesta debe incluir:

```json
{
  "exitoso": true,
  "totalCargados": 30,
  "aeropuertosPorContinente": {
    "America del Sur": 10,
    "Europa": 10,
    "Asia": 10
  },
  "muestra": [
    {
      "codigo": "SKBO",
      "ciudad": "Bogota",
      "pais": "Colombia",
      "continente": "America del Sur",
      "coordenadas": "4.7014, -74.1469"
    },
    {
      "codigo": "SEQM",
      "ciudad": "Quito",
      "pais": "Ecuador",
      "continente": "America del Sur",
      "coordenadas": "0.1133, -78.3586"
    },
    {
      "codigo": "SVMI",
      "ciudad": "Caracas",
      "pais": "Venezuela",
      "continente": "America del Sur",
      "coordenadas": "10.6031, -66.9906"
    }
  ]
}
```

### Paso 4: Verificar todos los aeropuertos

```bash
curl http://localhost:8000/api/aeropuertos
```

## Detalles Técnicos del Parseo

### Regex Principal
```regex
^\s*\d+\s+(?<icao>[A-Z]{4})\s+(?<ciudad>.+?)\s{2,}(?<pais>[A-Za-zÁÉÍÓÚÜÑáéíóúüñ\.]+)\s+\S+\s+(?<gmt>[+\-]?\d+)\s+(?<cap>\d+)\s+Latitude:\s+(?<lat>[^L]+?)\s+Longitude:\s+(?<lon>.+)$
```

### Extracción de Coordenadas DMS

**Latitud:**
```regex
(?<d>\d{1,2})[°º]\s*(?<m>\d{1,2})['′]\s*(?<s>\d{1,2})["″]\s*(?<hem>[NS])
```

**Longitud:**
```regex
(?<d>\d{1,3})[°º]\s*(?<m>\d{1,2})['′]\s*(?<s>\d{1,2})["″]\s*(?<hem>[EW])
```

### Conversión DMS → Decimal
```
decimal = grados + (minutos / 60) + (segundos / 3600)
Si hemisferio es S o W → decimal = -decimal
```

## Validación de Resultados

Para verificar que el parseo funciona correctamente:

1. **Total de aeropuertos**: Debe ser 30 (10 por continente)
2. **Coordenadas**: Deben ser valores decimales negativos para Sur y Oeste
3. **Continentes**: Deben estar correctamente asignados según el encabezado del archivo

## Solución de Problemas

Si encuentras errores:

1. **Revisa los logs** para ver qué líneas están fallando
2. **Verifica el encoding** del archivo (debe ser UTF-16)
3. **Comprueba la base de datos** que esté conectada correctamente

## Importante: Caché y Base de Datos

**Si modificas el archivo Aeropuertos.txt y no ves los cambios:**

El problema NO es caché del servidor, sino que los aeropuertos ya están en la base de datos. El método `cargarDesdeArchivo()` solo carga aeropuertos que NO existen (revisa por código ICAO).

**Soluciones:**

### Opción 1: Recargar (limpia y carga de nuevo) - RECOMENDADO
```bash
curl -X POST http://localhost:8000/api/aeropuertos/recargar
```

### Opción 2: Limpiar manualmente y luego cargar
```bash
# 1. Limpiar
curl -X DELETE http://localhost:8000/api/aeropuertos/limpiar

# 2. Cargar
curl -X POST http://localhost:8000/api/aeropuertos/cargar
```

### Opción 3: Modificar directamente en la base de datos
Conéctate a MySQL y actualiza los registros manualmente.

## Sobre el Archivo de Texto

**Importante:** El archivo se lee desde `src/main/resources/datos/Aeropuertos.txt`

- Si modificas el archivo pero la aplicación ya está corriendo, necesitas **reiniciar la aplicación** para que Maven copie la nueva versión al classpath.
- Si no reinicias, Spring Boot seguirá usando la versión antigua que se compiló al iniciar.

**Para aplicar cambios en el archivo:**

1. **Modifica** `src/main/resources/datos/Aeropuertos.txt`
2. **Reinicia** la aplicación (`Ctrl+C` y luego `mvn spring-boot:run`)
3. **Recarga** usando el endpoint `/api/aeropuertos/recargar`

## Endpoints Disponibles

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/aeropuertos/cargar` | Carga aeropuertos (solo los que NO existen) |
| POST | `/api/aeropuertos/recargar` | **LIMPIA la BD y carga todo de nuevo** |
| POST | `/api/aeropuertos/test-carga` | Carga y muestra resumen (para pruebas) |
| DELETE | `/api/aeropuertos/limpiar` | Elimina todos los aeropuertos de la BD |
| GET | `/api/aeropuertos` | Lista todos los aeropuertos |
| GET | `/api/aeropuertos/{codigo}` | Busca por código ICAO |
