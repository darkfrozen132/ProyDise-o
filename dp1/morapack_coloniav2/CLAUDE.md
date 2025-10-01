# Claude Code Configuration

## Project Overview
MoraPack Colonia v2 - Sistema de optimización basado en algoritmos de colonias de hormigas (Ant Colony Optimization) para resolver problemas de planificación y ruteo logístico con soporte para entregas parciales múltiples.

## Build Commands

### Compilation
```bash
# Windows
compile.bat

# Unix/Linux/Mac
./compile.sh

# Using Makefile
make
```

### Execution
```bash
# Windows
run.bat

# Unix/Linux/Mac
./run.sh

# Using Makefile
make run
```

### Development Commands
```bash
# Clean and rebuild
make clean compile

# View all make options
make help
```

## Project Structure

### Source Code
- `src/morapack/colonia/` - Algoritmos de colonia de hormigas
- `src/morapack/datos/` - Manejo de datos y persistencia
- `src/morapack/core/` - Componentes principales
- `src/morapack/utils/` - Utilidades y helpers

### Key Files
- **Main class**: `src/morapack/main/Main.java`
- **Core ACO**: `src/morapack/colonia/algoritmo/AlgoritmoColoniaHormigas.java`
- **Problem definition**: `src/morapack/core/problema/ProblemaMoraPack.java`
- **Solution model**: `src/morapack/core/solucion/SolucionMoraPack.java`
- **Examples**: `src/morapack/ejemplos/EjemploEntregasParciales.java`
- **Build output**: `bin/` directory
- **Data files**: `datos/` directory

### Data Files Structure
```
datos/
├── aeropuertos.csv              # 31 aeropuertos (3 sedes + 28 destinos)
├── planes_de_vuelo.csv         # Horarios y capacidades de vuelos
└── pedidos/
    └── pedidos_01.csv          # Pedidos enero (formato: dd-hh-mm-dest-###-IdClien)
```

**Important Notes:**
- Pedidos exclude destinations to main headquarters (SPIM, EBCI, UBBB)
- Flight capacities: 300-360 products per flight
- Airport storage: 400-480 products per airport
- Time restrictions: 2 days same continent, 3 days different continent

## Modelo Híbrido de Entregas Parciales

### Concepto Principal
El sistema soporta **entregas parciales múltiples** para pedidos grandes que no pueden ser completados en un solo envío. Esto permite optimización flexible y mejor utilización de capacidades.

### Características del Modelo
- **Un pedido** puede tener **múltiples entregas**
- **Productos fungibles**: Los productos son intercambiables entre pedidos
- **Entregas asíncronas**: Cada entrega puede llegar en momentos diferentes
- **Cumplimiento flexible**: Todas las entregas deben cumplir el plazo individual

### Estructura de Datos
```java
Map<Integer, List<RutaProducto>> rutasPorPedido;
// Un pedido (ID) -> Lista de entregas parciales

class RutaProducto {
    int cantidadTransportada;     // Cantidad en esta entrega
    int cantidadTotalPedido;      // Cantidad total del pedido
    int numeroEntrega;            // 1, 2, 3...
    boolean esEntregaParcial;     // true si hay más entregas
    // ... campos de ruta y timing
}
```

### Ejemplos de Uso
```bash
# Ejecutar ejemplo de entregas parciales
java -cp bin morapack.ejemplos.EjemploEntregasParciales
```

### Métodos Clave
- `solucion.getRutasProducto(idPedido)` - Obtener todas las entregas
- `solucion.pedidoCompleto(idPedido)` - Verificar completitud
- `solucion.pedidoCumplePlazo(idPedido)` - Verificar cumplimiento
- `ruta.porcentajeCompletado()` - % de completitud de la entrega

## Coding Conventions

### Java Naming
- **Packages**: minúsculas.sin_espacios (e.g., `morapack.colonia.algoritmo`)
- **Classes**: PascalCase (e.g., `AlgoritmoColoniaHormigas`)
- **Methods/Variables**: camelCase (e.g., `calcularDistancia()`)
- **Constants**: MAYUSCULAS_CON_GUIONES (e.g., `ALPHA_FEROMONA`)
- **DAO Interfaces**: suffix DAO (e.g., `PedidoDAO`)
- **Special Characters**: No usar ñ en variables (usar 'anio' en lugar de 'año')

## Manejo de Husos Horarios

### Reglas Fundamentales

**Planes de Vuelo (`planes_de_vuelo.csv`):**
```
Origen,Destino,HoraSalida,HoraLlegada,Capacidad
SKBO,SEQM,03:34,05:21,300
```
- **HoraSalida**: Hora LOCAL del aeropuerto origen (03:34 en huso de SKBO)
- **HoraLlegada**: Hora LOCAL del aeropuerto destino (05:21 en huso de SEQM)
- **Conversión**: Para cálculos usar UTC, manejar cruces de día automáticamente

**Pedidos (`pedidos_XX.csv`):**
```
dd-hh-mm-dest-###-IdClien
30-09-15-SEQM-145-0054321
```
- **hh:mm**: Hora LOCAL del aeropuerto destino (09:15 en huso de SEQM)
- **Plazo**: Se mide desde esa hora EN EL HUSO DEL DESTINO
- **Sin conversiones**: El tiempo ya está en el huso correcto

### Métodos de Implementación

**Clase Vuelo:**
- `calcularDuracionReal(origen, destino)`: Duración considerando husos
- `cruzaMedianocheReal(origen, destino)`: Cruces de día en UTC
- `puedeConectarConReal(vuelo, aeropuerto, minutos)`: Conexiones precisas
- `getInformacionCompletaConHusos(origen, destino)`: Debugging temporal

**Clase Pedido:**
- `getTiempoPedidoUTC(destino)`: Conversión a UTC para cálculos
- `getTiempoLimiteEntregaUTC(destino)`: Límite en UTC
- `estaDentroPlazoUTC(tiempoUTC, destino)`: Validación precisa
- `horasRestantesUTC(tiempoUTC, destino)`: Urgencia para ACO

### Para Desarrollo ACO
- **Comparaciones temporales**: Siempre usar métodos `*UTC()`
- **Heurísticas**: Usar `horasRestantesUTC()` para calcular urgencia
- **Validación de rutas**: Usar `*Real()` para conexiones precisas
- **Debugging**: Usar `*ConHusos()` para análisis detallado

## Detección de Colapso del Sistema

### Criterios de Colapso
El sistema MoraPack **colapsa** cuando no puede cumplir los requerimientos establecidos:

1. **Violación de Plazos**: Pedidos retrasados (fuera del plazo 2-3 días)
2. **Capacidad de Vuelos Excedida**: Vuelos saturados sin alternativas
3. **Capacidad de Aeropuertos Excedida**: Almacenes saturados
4. **Rutas Inválidas**: No existe ruta física viable

### Clases de Validación

**ValidadorColapso:**
- `verificarRetrasopedido(pedido, tiempoUTC, destino)`: Detecta retrasos
- `verificarCapacidadVuelo(vuelo, cantidad)`: Valida capacidad vuelos
- `verificarCapacidadAeropuerto(aeropuerto, cantidad)`: Valida almacenes
- `verificarConectividadRuta(red, origen, destino)`: Valida conectividad
- `verificarSistemaCompleto(red, pedidos, tiempoUTC)`: Análisis integral

**MetricasSistema:**
- `calcularUtilizacionVuelos(vuelos)`: % utilización vuelos
- `calcularTasaRetrasos(pedidos, tiempoUTC, red)`: % pedidos retrasados
- `calcularConectividadRed(red)`: % rutas factibles
- `generarReporteCompleto(red, pedidos, tiempoUTC)`: Reporte detallado

### Ejemplo de Uso
```java
// Detectar condiciones de colapso
List<CondicionColapso> problemas = ValidadorColapso
    .verificarSistemaCompleto(red, pedidos, tiempoActual);

// Generar reporte del sistema
String reporte = MetricasSistema
    .generarReporteCompleto(red, pedidos, tiempoActual);

// Verificar si ha colapsado
boolean colapso = ValidadorColapso
    .sistemaHaColapsado(problemas, 0.3); // 30% umbral
```

### Ejecutar Ejemplo
```bash
java -cp bin morapack.ejemplos.EjemploValidacionColapso
```

## Algoritmo ACO Optimizado para Logística

### Parámetros Optimizados
El algoritmo ha sido configurado específicamente para problemas logísticos:

```java
// Configuración optimizada para MoraPack
NUMERO_HORMIGAS_DEFAULT = 15      // Más diversidad que TSP
MAX_ITERACIONES_DEFAULT = 150     // Más paciencia para convergencia
TASA_EVAPORACION_DEFAULT = 0.15   // Evita estancamiento local
MAX_ITERACIONES_SIN_MEJORA = 20   // Mayor tolerancia
```

### Depositación de Feromonas Inteligente
- **Estrategia Elite Diversificada**: Top 30% de hormigas depositan feromona
- **Bonus por Cumplimiento**: 1.2x feromona para entregas a tiempo
- **Bonus por Eficiencia**: Incremento basado en % de completitud
- **Refuerzo Conservador**: 5% probabilidad de refuerzo global

### Heurísticas Específicas para MoraPack
```java
// Tipos de heurística implementados
URGENCIA_PEDIDOS         // Basada en tiempo restante
EFICIENCIA_RUTAS         // Vuelos directos vs. escalas
CAPACIDAD_VUELOS         // Disponibilidad de espacio
PROXIMIDAD_GEOGRAFICA    // Preferencia por mismo continente
HIBRIDA                  // Combinación ponderada (default)
```

### Estadísticas Avanzadas
```java
// Métricas específicas de logística
totalPedidos, pedidosCompletos, entregasParciales
tasaCompletitud(), tasaEntregasParciales()
promedioEntregasPorPedido

// Ejemplo de salida
"Iter 10: fitness=1250.50 | Pedidos: 45/50 completos (90%) |
Parciales: 15 (30%) | Entregas/Pedido: 1.8"
```

## Dependencies
- Java 8 or higher
- No external dependencies (pure Java project)

## Testing
Currently no automated testing framework configured. Tests should be added as development progresses.

## Development Notes
- Academic project focused on learning ACO algorithms
- Iterative development with validation at each step
- Code clarity prioritized over premature optimization
- JavaDoc documentation required for public methods