# Cambios en el Response del Endpoint de Planificacion

## Fecha
2025-11-10

## Resumen
Se modifico el endpoint `/api/planificacion/semanal` para retornar un formato de response simplificado que solo incluye la lista de vuelos con sus pedidos asignados.

## Estructura del Nuevo Response

```json
{
  "vuelos": [
    {
      "fechaInicial": "2025-01-15 08:30",
      "fechaFinal": "2025-01-15 14:45",
      "origenCodigoICAO": "SPIM",
      "destinoCodigoICAO": "KJFK",
      "pedidos": [
        {
          "idPedido": 123,
          "cantidad": 50
        },
        {
          "idPedido": 456,
          "cantidad": 30
        }
      ]
    }
  ]
}
```

## Formato de Fechas
- **Formato**: `yyyy-MM-dd HH:mm`
- **Ejemplo**: `"2025-01-15 08:30"`
- Incluye: ano, mes, dia, hora, minuto

## Archivos Creados

### 1. `PedidoEnVueloDTO.java`
**Ubicacion**: `com.proyecto.backend.planificador.semanal.dto.response`

Representa un pedido asignado a un vuelo.

**Campos**:
- `idPedido` (Long): Identificador del pedido
- `cantidad` (Integer): Cantidad de productos asignados al vuelo

### 2. `VueloSimplificadoDTO.java`
**Ubicacion**: `com.proyecto.backend.planificador.semanal.dto.response`

Representa un vuelo planificado en formato simplificado.

**Campos**:
- `fechaInicial` (String): Fecha y hora de salida en formato `yyyy-MM-dd HH:mm`
- `fechaFinal` (String): Fecha y hora de llegada en formato `yyyy-MM-dd HH:mm`
- `origenCodigoICAO` (String): Codigo ICAO del aeropuerto origen
- `destinoCodigoICAO` (String): Codigo ICAO del aeropuerto destino
- `pedidos` (List<PedidoEnVueloDTO>): Lista de pedidos asignados al vuelo

**Metodos utiles**:
- `agregarPedido(Long idPedido, Integer cantidad)`: Agrega un pedido al vuelo
- `getTotalPaquetes()`: Calcula el total de paquetes en el vuelo

### 3. `PlanificacionResponseSimple.java`
**Ubicacion**: `com.proyecto.backend.planificador.semanal.dto.response`

Response principal que contiene la lista de vuelos planificados.

**Campos**:
- `vuelos` (List<VueloSimplificadoDTO>): Lista de vuelos planificados

**Metodos utiles**:
- `conVuelos(List<VueloSimplificadoDTO> vuelos)`: Constructor estatico de conveniencia
- `agregarVuelo(VueloSimplificadoDTO vuelo)`: Agrega un vuelo a la lista
- `getTotalVuelos()`: Obtiene el numero total de vuelos
- `getTotalPedidos()`: Obtiene el numero total de pedidos asignados

## Archivos Modificados

### 1. `AlgoritmoGeneticoService.java`
**Ubicacion**: `com.proyecto.backend.planificador.semanal.service`

**Metodos agregados**:

#### `planificarSimple(PlanificacionRequest request)`
Ejecuta la planificacion de rutas y retorna el formato simplificado.
- **Lineas**: 105-165
- **Transaccional**: Si (readOnly = true)
- **Retorna**: `PlanificacionResponseSimple`

#### `convertirAResponseSimple(Solution solucion, WorldTemporal worldTemporal)`
Convierte la solucion del algoritmo genetico al formato simplificado.
- **Lineas**: 1061-1122
- **Publico**: Si
- **Retorna**: `PlanificacionResponseSimple`

#### `formatearFecha(LocalDateTime fecha)`
Formatea una fecha a string en formato `yyyy-MM-dd HH:mm`.
- **Lineas**: 1124-1137
- **Privado**: Si
- **Retorna**: String formateado

### 2. `PlanificacionController.java`
**Ubicacion**: `com.proyecto.backend.planificador.semanal.controller`

**Endpoints modificados**:

#### `POST /api/planificacion/semanal`
- **Antes**: Retornaba `PlanificacionResponse` (formato completo)
- **Ahora**: Retorna `PlanificacionResponseSimple` (formato simplificado)
- **Lineas**: 63-81

**Endpoints nuevos**:

#### `POST /api/planificacion/semanal/completo`
Endpoint legacy que retorna el formato completo original.
- **Retorna**: `PlanificacionResponse` (con metadata, aeropuertos, rutas completas)
- **Lineas**: 92-108
- **Uso**: Para mantener compatibilidad con sistemas que necesiten el formato completo

## Diferencias entre Formato Completo vs Simplificado

### Formato Completo (legacy - `/completo`)
```json
{
  "metadata": { ... },           // Metadata de ejecucion
  "pedidosProcesados": [ ... ],  // Lista de pedidos procesados
  "aeropuertos": [ ... ],        // Estado de aeropuertos con ocupacion
  "vuelos": [ ... ],             // Vuelos con coordenadas geograficas
  "rutas": [ ... ]               // Rutas completas por pedido
}
```

### Formato Simplificado (nuevo - endpoint principal)
```json
{
  "vuelos": [                    // Solo lista de vuelos
    {
      "fechaInicial": "...",
      "fechaFinal": "...",
      "origenCodigoICAO": "...",
      "destinoCodigoICAO": "...",
      "pedidos": [ ... ]
    }
  ]
}
```

## Ventajas del Nuevo Formato

1. **Simplicidad**: Estructura mas simple y facil de consumir
2. **Reduccion de tamano**: Response mas pequeno al eliminar informacion redundante
3. **Foco en lo esencial**: Solo incluye vuelos y pedidos asignados
4. **Formato de fechas legible**: Formato string facil de parsear y leer
5. **Retrocompatibilidad**: Endpoint legacy disponible en `/completo`

## Algoritmo de Conversion

El metodo `convertirAResponseSimple` realiza los siguientes pasos:

1. **Agrupa vuelos**: Crea un mapa de vuelos unicos usando el ID de vuelo
2. **Agrupa pedidos**: Para cada vuelo, agrega todos los pedidos asignados
3. **Obtiene fechas reales**: Consulta las instancias de vuelos del WorldTemporal
4. **Formatea fechas**: Convierte LocalDateTime a formato `yyyy-MM-dd HH:mm`
5. **Construye response**: Crea el objeto `PlanificacionResponseSimple` con la lista de vuelos

## Convenciones Seguidas

- **Paquetes**: `com.proyecto.backend.planificador.semanal.dto.response`
- **Clases**: PascalCase (ej: `VueloSimplificadoDTO`, `PedidoEnVueloDTO`)
- **Metodos**: camelCase (ej: `agregarPedido()`, `getTotalVuelos()`)
- **Comentarios**: Documentacion JavaDoc en todos los metodos publicos
- **Sin caracteres especiales**: No se usan tildes ni caracteres especiales

## Pruebas de Compilacion

Ejecutado: `mvn clean compile -DskipTests`
- **Resultado**: BUILD SUCCESS
- **Tiempo**: 3.464 s
- **Archivos compilados**: 68 archivos Java

## Endpoints Disponibles

### 1. Planificacion Simplificada (NUEVO)
```
POST /api/planificacion/semanal
Body: PlanificacionRequest
Response: PlanificacionResponseSimple
```

### 2. Planificacion Completa (LEGACY)
```
POST /api/planificacion/semanal/completo
Body: PlanificacionRequest
Response: PlanificacionResponse
```

### 3. Estado del World
```
GET /api/planificacion/semanal/world/estado
Response: Estado del cache World
```

### 4. Refrescar World
```
POST /api/planificacion/semanal/world/refrescar
Response: Confirmacion de refresco
```

### 5. Health Check
```
GET /api/planificacion/semanal/health
Response: Estado del servicio
```

## Notas Adicionales

- El algoritmo genetico subyacente NO ha cambiado
- La logica de planificacion es identica
- Solo cambio el formato de salida del response
- Los DTOs antiguos NO fueron eliminados para mantener compatibilidad
- El formato de fechas es UTC (mismo que antes)

## Proximos Pasos Recomendados

1. Actualizar el frontend para consumir el nuevo formato
2. Probar el endpoint con diferentes escenarios
3. Validar que las fechas se formateen correctamente en diferentes zonas horarias
4. Considerar agregar tests unitarios para los nuevos DTOs
5. Documentar el endpoint en Swagger/OpenAPI si esta disponible
