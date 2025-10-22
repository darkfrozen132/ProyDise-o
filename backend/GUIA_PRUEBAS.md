# Guía de Pruebas - Algoritmo Genético

## 🚀 Paso 1: Iniciar el Backend

```bash
cd C:\Users\User\Documents\GitHub\ProyDise-o\backend
mvn spring-boot:run
```

Espera a que veas el mensaje:
```
Started BackendApplication in X.XXX seconds
```

---

## 📝 Paso 2: Actualizar Fechas de Pedidos Existentes

Hibernate ya creó las columnas `anio` y `mes` en la tabla `pedidos`, pero los registros existentes tienen valores `0`.

### Opción A: Usando el endpoint de admin (Recomendado)

**Request:**
```
POST http://localhost:8080/api/admin/pedidos/actualizar-fecha
```

**Respuesta esperada:**
```json
{
    "success": true,
    "mensaje": "Pedidos actualizados exitosamente",
    "totalPedidos": 100,
    "pedidosActualizados": 100
}
```

### Opción B: Ejecutar SQL manualmente

Si prefieres usar SQL directamente:

```sql
UPDATE pedidos
SET anio = 2025, mes = 1
WHERE anio = 0 OR mes = 0;
```

---

## ✅ Paso 3: Verificar que los Pedidos se Actualizaron

**Request:**
```
GET http://localhost:8080/api/admin/pedidos/estadisticas
```

**Respuesta esperada:**
```json
{
    "totalPedidos": 100,
    "pedidosPendientes": 50,
    "pedidosSinFecha": 0,   // ← Debe ser 0 después de actualizar
    "pedidosPorFecha": {
        "2025-01-01": 5,
        "2025-01-13": 8,
        "2025-01-14": 7,
        "2025-01-17": 10,
        "2025-01-19": 5
    }
}
```

**Verificar también con GET pedidos:**
```
GET http://localhost:8080/api/pedidos
```

Ahora deberías ver:
```json
[
    {
        "id": 902,
        "anio": 2025,     // ← Nuevo campo
        "mes": 1,         // ← Nuevo campo
        "dia": 17,
        "hora": 15,
        "minuto": 47,
        "aeropuertoDestinoId": "SKBO",
        "cantidadProductos": 324,
        "clienteId": "0000015",
        "fechaCreacion": "2025-10-19T13:37:17.746796",
        "estado": "PENDIENTE"
    }
]
```

---

## 🧬 Paso 4: Probar la Planificación

### Opción 1: Fecha con Pedidos Conocidos

Primero mira las estadísticas para ver qué fechas tienen pedidos:

```
GET http://localhost:8080/api/admin/pedidos/estadisticas
```

Luego usa una fecha que tenga pedidos, por ejemplo `2025-01-17`:

**Request:**
```
POST http://localhost:8080/api/planificacion
Content-Type: application/json

{
    "fecha": "2025-01-17",
    "factorK": 1
}
```

**Respuesta esperada:**
```json
{
    "metadata": {
        "fechaInicio": "2025-01-17T00:00:00",
        "fechaFin": "2025-01-17T00:05:00",
        "factorK": 1,
        "saltoConsumoMinutos": 5,
        "saltoAlgoritmoMinutos": 5,
        "pedidosProcesados": 10,    // ← Debería ser > 0
        "pedidosATiempo": 0,
        "pedidosTarde": 0,
        "pedidosNoEntregados": 10,  // ← Por ahora todos no entregados
        "objetivo": 0.0,
        "tiempoEjecucionMs": 150,
        "generacionesEjecutadas": 0
    },
    "aeropuertos": [
        {
            "code": "SPIM",
            "lat": -12.0219,
            "lng": -77.1143,
            "name": "Lima - Peru",
            "region": "America del Sur",
            "country": "Peru",
            "isSede": true,
            "capacity": "ILIMITADO",
            "packages": 0
        }
        // ... más aeropuertos
    ],
    "vuelos": [],   // Vacío por ahora
    "rutas": []     // Vacío por ahora
}
```

### Opción 2: Crear Pedidos de Prueba

Si quieres crear pedidos específicos para una fecha:

**Request:**
```
POST http://localhost:8080/api/pedidos
Content-Type: application/json

{
    "anio": 2025,
    "mes": 1,
    "dia": 20,
    "hora": 10,
    "minuto": 30,
    "aeropuertoDestinoId": "SKBO",
    "cantidadProductos": 100,
    "clienteId": "0000099"
}
```

Luego planificar para esa fecha:

```
POST http://localhost:8080/api/planificacion
Content-Type: application/json

{
    "fecha": "2025-01-20",
    "factorK": 1
}
```

---

## 📊 Paso 5: Verificar Logs del Backend

En la consola del backend deberías ver:

```
INFO WorldCacheService - Inicializando WorldCacheService...
INFO WorldCacheService - Cargando aeropuertos desde la base de datos...
INFO WorldCacheService - Cargados 45 aeropuertos
INFO WorldCacheService - Cargando planes de vuelo desde la base de datos...
INFO WorldCacheService - Cargados 120 planes de vuelo
INFO WorldCacheService - World creado exitosamente: World: 45 aeropuertos, 120 planes de vuelo, 3 hubs
INFO WorldCacheService - Hubs disponibles: [SPIM, EBCI, UBBB]

// Cuando ejecutes la planificación:
INFO AlgoritmoGeneticoService - Iniciando planificacion para fecha 2025-01-17 con K=1
INFO AlgoritmoGeneticoService - Cargando pedidos para fecha: 2025/1/17 (rango: 5 minutos)
INFO AlgoritmoGeneticoService - Encontrados 10 pedidos que cumplen los criterios
INFO AlgoritmoGeneticoService - Planificacion completada en 150 ms
```

---

## 🎯 Casos de Prueba

### Caso 1: Sin Pedidos
```
POST http://localhost:8080/api/planificacion

{
    "fecha": "2025-12-31",
    "factorK": 1
}
```

**Resultado esperado:**
```json
{
    "metadata": {
        "pedidosProcesados": 0
    },
    "aeropuertos": [],
    "vuelos": [],
    "rutas": []
}
```

### Caso 2: Con Pedidos (K=1)
```
{
    "fecha": "2025-01-17",
    "factorK": 1
}
```

**Resultado esperado:**
- `pedidosProcesados > 0`
- Lista de aeropuertos con datos
- Vuelos y rutas vacíos (porque el AG aún no está implementado)

### Caso 3: Simulación 3 Días (K=14)
```
{
    "fecha": "2025-01-17",
    "factorK": 14
}
```

**Resultado esperado:**
- `saltoConsumoMinutos = 70` (14 × 5)
- Mismo comportamiento que K=1 por ahora

---

## 🔍 Troubleshooting

### Error: "No se pudo inicializar el World"
**Causa:** No hay conexión a BD o faltan tablas.
**Solución:** Verifica `application.properties` y que las tablas existan.

### pedidosProcesados = 0 siempre
**Causa:** Los pedidos no tienen `anio=2025` y `mes=1`.
**Solución:** Ejecuta el endpoint de actualización del Paso 2.

### Error 500 al actualizar fechas
**Causa:** Hibernate aún no creó las columnas.
**Solución:**
1. Detén el backend
2. Inicia de nuevo (Hibernate creará las columnas automáticamente)
3. Ejecuta el endpoint de actualización

### pedidosSinFecha > 0 después de actualizar
**Causa:** Hay pedidos nuevos que se crearon sin especificar año/mes.
**Solución:** Ejecuta de nuevo el endpoint de actualización.

---

## 📌 Endpoints Disponibles

### Administración
```
POST   /api/admin/pedidos/actualizar-fecha    → Actualizar año/mes de pedidos
GET    /api/admin/pedidos/estadisticas        → Ver estadísticas de pedidos
```

### Planificación
```
POST   /api/planificacion                     → Ejecutar planificación
GET    /api/planificacion/health              → Health check
GET    /api/planificacion/world/estado        → Estado del cache
POST   /api/planificacion/world/refrescar     → Refrescar cache
```

### Pedidos
```
GET    /api/pedidos                           → Ver todos los pedidos
POST   /api/pedidos                           → Crear pedido nuevo
```

---

## ✅ Checklist de Verificación

- [ ] Backend inicia sin errores
- [ ] Endpoint `/api/admin/pedidos/actualizar-fecha` retorna success
- [ ] Endpoint `/api/admin/pedidos/estadisticas` muestra `pedidosSinFecha: 0`
- [ ] GET `/api/pedidos` muestra campos `anio` y `mes`
- [ ] Endpoint `/api/planificacion/world/estado` muestra aeropuertos y vuelos cargados
- [ ] POST `/api/planificacion` con fecha válida retorna `pedidosProcesados > 0`
- [ ] Los logs muestran "Encontrados X pedidos que cumplen los criterios"

---

## 🎉 Siguiente Paso

Una vez que todo funcione correctamente, el siguiente paso será **implementar el decodificador del algoritmo genético** para que realmente genere rutas en lugar de soluciones vacías.

Los pedidos ya están cargándose correctamente, solo falta la lógica del AG para asignarlos a vuelos y generar las rutas.
