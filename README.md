# MoraPack Colonia v2 - Ant Colony Optimization

Sistema de optimización basado en algoritmo genetico para resolver problemas de planificación y ruteo logístico con soporte completo para 

## Convenciones de Nomenclatura

### Java
- **Paquetes**: minúsculas.sin_espacios
  - Ejemplo: `com.morapack.dao`, `morapack.colonia.algoritmo`
- **Clases**: PascalCase
  - Ejemplo: `PedidoDAO`, `PlanificadorService`, `AlgoritmoColoniaHormigas`
- **Interfaces DAO**: sufijo DAO
  - Ejemplo: `PedidoDAO`, `ClienteDAO`
- **Métodos/variables**: camelCase
  - Ejemplo: `registrarPedido()`, `fechaEntrega`, `calcularDistancia()`
- **Constantes**: MAYUSCULAS_CON_GUIONES
  - Ejemplo: `MAX_CAP_ALMACEN`, `ALPHA_FEROMONA`, `BETA_HEURISTICA`





## Contribución

- Seguir las convenciones de nomenclatura establecidas
- Documentar métodos públicos con JavaDoc
- Mantener código limpio y legible
- Incluir tests unitarios cuando sea apropiado


## Datos del Sistema

### Estructura de Datos
- **31 aeropuertos**: 3 sedes principales (SPIM, EBCI, UBBB) + 28 destinos
- **Continentes**: SAM (América del Sur), EUR (Europa), ASI (Asia)
- **Vuelos**: Capacidad 300-360 productos, múltiples frecuencias diarias
- **Pedidos**: Formato `dd-hh-mm-dest-###-IdClien` por archivos mensuales

### Restricciones del Problema
- **Plazos de entrega**: 2 días mismo continente, 3 días diferente continente
- **Capacidades**: Vuelos (300-360), almacenes (400-480), sedes (ilimitado)
- **Exclusiones**: No se procesan pedidos con destino a sedes principales

### Manejo Temporal y Husos Horarios

**Sistema Global**: Opera en 3 continentes (SAM, EUR, ASI) con múltiples husos horarios

#### Interpretación de Horarios

**Planes de Vuelo:**
```
SKBO,SEQM,03:34,05:21,300
├── HoraSalida: 03:34 en huso LOCAL del origen (SKBO = GMT-5)
└── HoraLlegada: 05:21 en huso LOCAL del destino (SEQM = GMT-5)
```

**Pedidos:**
```
30-09-15-SEQM-145-0054321
├── Día: 30
├── Hora: 09:15 en huso LOCAL del destino (SEQM = GMT-5)
└── Plazo: Se mide desde esa hora EN EL HUSO DEL DESTINO
```