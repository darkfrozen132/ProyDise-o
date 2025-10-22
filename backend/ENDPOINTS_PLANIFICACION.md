# API Endpoints - Algoritmo Genético de Planificación

## Base URL
```
http://localhost:8080/api/planificacion
```

---

## 1. Health Check

Verifica que el servicio de planificación esté funcionando.

**Endpoint:** `GET /api/planificacion/health`

**Ejemplo de Request (Postman):**
```
GET http://localhost:8080/api/planificacion/health
```

**Respuesta Esperada:**
```json
{
    "status": "UP",
    "servicio": "Algoritmo Genetico - Planificacion de Rutas",
    "worldInicializado": true,
    "ultimaActualizacion": "2025-10-21T23:15:30"
}
```

---

## 2. Estado del World

Obtiene información sobre el caché en memoria (aeropuertos, vuelos, hubs).

**Endpoint:** `GET /api/planificacion/world/estado`

**Ejemplo de Request (Postman):**
```
GET http://localhost:8080/api/planificacion/world/estado
```

**Respuesta Esperada:**
```json
{
    "inicializado": true,
    "estadisticas": "World: 45 aeropuertos, 120 planes de vuelo, 3 hubs | Ultima actualizacion: 2025-10-21T23:15:30",
    "ultimaActualizacion": "2025-10-21T23:15:30",
    "numeroAeropuertos": 45,
    "numeroVuelos": 120,
    "hubs": ["SPIM", "EBCI", "UBBB"]
}
```

---

## 3. Refrescar World

Recarga los datos de aeropuertos y vuelos desde la base de datos.

**Endpoint:** `POST /api/planificacion/world/refrescar`

**Ejemplo de Request (Postman):**
```
POST http://localhost:8080/api/planificacion/world/refrescar
```

**Respuesta Esperada:**
```json
{
    "success": true,
    "mensaje": "World refrescado exitosamente",
    "estadisticas": "World: 45 aeropuertos, 120 planes de vuelo, 3 hubs | Ultima actualizacion: 2025-10-21T23:20:00",
    "ultimaActualizacion": "2025-10-21T23:20:00"
}
```

---

## 4. Planificar Rutas (Principal)

Ejecuta el algoritmo genético para planificar rutas de pedidos.

**Endpoint:** `POST /api/planificacion`

**Headers:**
```
Content-Type: application/json
```

### Ejemplo 1: Planificación Día a Día (K=1)

**Request Body:**
```json
{
    "fecha": "2025-01-15",
    "factorK": 1,
    "parametrosGenetico": {
        "tamanioPoblacion": 50,
        "maxGeneraciones": 200,
        "probabilidadCruce": 0.8,
        "probabilidadMutacion": 0.05,
        "numeroElite": 4,
        "limiteGeneracionesSinMejora": 40,
        "saltoAlgoritmoMinutos": 5,
        "semilla": 12345
    }
}
```

### Ejemplo 2: Simulación 3 Días (K=14)

**Request Body:**
```json
{
    "fecha": "2025-01-15",
    "factorK": 14
}
```
*Nota: Los parámetros genéticos son opcionales y usarán valores por defecto si no se especifican.*

### Ejemplo 3: Simulación hasta Colapso (K=75)

**Request Body:**
```json
{
    "fecha": "2025-01-15",
    "factorK": 75,
    "parametrosGenetico": {
        "tamanioPoblacion": 100,
        "maxGeneraciones": 500,
        "saltoAlgoritmoMinutos": 5
    }
}
```

### Respuesta Esperada:

```json
{
    "metadata": {
        "fechaInicio": "2025-01-15T00:00:00",
        "fechaFin": "2025-01-15T01:10:00",
        "factorK": 14,
        "saltoConsumoMinutos": 70,
        "saltoAlgoritmoMinutos": 5,
        "pedidosProcesados": 45,
        "pedidosATiempo": 38,
        "pedidosTarde": 5,
        "pedidosNoEntregados": 2,
        "objetivo": 1234.56,
        "tiempoEjecucionMs": 3500,
        "generacionesEjecutadas": 150
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
            "packages": 980,
            "ocupacionPorMinuto": [
                {
                    "minutoUTC": 0,
                    "paquetes": 0,
                    "porcentajeOcupacion": 0.0
                },
                {
                    "minutoUTC": 30,
                    "paquetes": 150,
                    "porcentajeOcupacion": 25.0
                }
            ]
        }
    ],
    "vuelos": [
        {
            "id": "SPIM-SEQM-D1-0334",
            "originCode": "SPIM",
            "destinationCode": "SEQM",
            "salida": "2025-01-15T03:34:00",
            "llegada": "2025-01-15T05:21:00",
            "paquetes": 145,
            "capacidad": 300,
            "altitude": 35000,
            "speed": 500,
            "regionOrigin": "America del Sur",
            "regionDestination": "America del Sur",
            "ruta": {
                "origin": {
                    "lat": -12.0219,
                    "lng": -77.1143
                },
                "destination": {
                    "lat": -0.1295,
                    "lng": -78.3575
                }
            }
        }
    ],
    "rutas": [
        {
            "pedidoId": 123,
            "clienteId": "0054321",
            "destino": "SEQM",
            "cantidad": 145,
            "estado": "ENTREGADO_A_TIEMPO",
            "fechaPedido": "2025-01-15T00:15:00",
            "fechaLimite": "2025-01-17T00:15:00",
            "subrutas": [
                {
                    "hub": "SPIM",
                    "cantidad": 145,
                    "llegada": "2025-01-15T05:21:00",
                    "vuelos": ["SPIM-SEQM-D1-0334"],
                    "escalas": []
                }
            ]
        }
    ]
}
```

---

## Notas Importantes

### Factor K (Factor de Ampliación Temporal)

- **K = 1**: Operación día a día (tiempo real)
- **K = 14**: Simulación de 3 días
- **K = 75**: Simulación hasta colapso logístico

**Fórmula:** `Sc (Salto de Consumo) = K × Sa (Salto del Algoritmo)`

Ejemplo: Si K=14 y Sa=5 minutos → Sc=70 minutos de pedidos procesados por cada ejecución.

### Estados de Pedidos

- `ENTREGADO_A_TIEMPO`: Entregado dentro del plazo (2-3 días)
- `ENTREGADO_TARDE`: Entregado fuera de plazo
- `NO_ENTREGADO`: No se pudo planificar ruta
- `EN_PROCESO`: Aún en tránsito

### Plazos de Entrega

- **Mismo continente**: 2 días + 2 horas de recojo
- **Diferente continente**: 3 días + 2 horas de recojo

---

## Orden de Pruebas Sugerido

1. **Health Check** - Verificar que el servicio esté UP
2. **Estado del World** - Verificar que se cargaron aeropuertos y vuelos
3. **Planificación Simple (K=1)** - Probar con un día
4. **Planificación 3 Días (K=14)** - Simulación más amplia
5. **Refrescar World** - Si se agregan datos nuevos a la BD

---

## Troubleshooting

### Error: "World no ha sido inicializado"
**Solución:** El servicio acaba de iniciar. Espera unos segundos y vuelve a intentar, o llama a `/world/refrescar`.

### Error: "No hay pedidos para procesar"
**Solución:** Verifica que existan pedidos en la BD con estado `PENDIENTE` para la fecha especificada.

### Error 500
**Solución:** Revisa los logs del backend. Puede ser un problema de conexión a BD o datos faltantes.

---

## Colección Postman

Importa esta colección en Postman para probar todos los endpoints:

```json
{
    "info": {
        "name": "MoraPack - Algoritmo Genetico",
        "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
    },
    "item": [
        {
            "name": "Health Check",
            "request": {
                "method": "GET",
                "url": "{{baseUrl}}/api/planificacion/health"
            }
        },
        {
            "name": "Estado del World",
            "request": {
                "method": "GET",
                "url": "{{baseUrl}}/api/planificacion/world/estado"
            }
        },
        {
            "name": "Planificar (K=1)",
            "request": {
                "method": "POST",
                "header": [
                    {
                        "key": "Content-Type",
                        "value": "application/json"
                    }
                ],
                "body": {
                    "mode": "raw",
                    "raw": "{\n    \"fecha\": \"2025-01-15\",\n    \"factorK\": 1\n}"
                },
                "url": "{{baseUrl}}/api/planificacion"
            }
        },
        {
            "name": "Refrescar World",
            "request": {
                "method": "POST",
                "url": "{{baseUrl}}/api/planificacion/world/refrescar"
            }
        }
    ],
    "variable": [
        {
            "key": "baseUrl",
            "value": "http://localhost:8080"
        }
    ]
}
```

**Variable de entorno:**
- `baseUrl`: `http://localhost:8080`
