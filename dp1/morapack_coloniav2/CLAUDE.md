# Claude Code Configuration

## Project Overview
MoraPack Colonia v2 - Sistema de optimización basado en algoritmos de colonias de hormigas (Ant Colony Optimization) para resolver problemas de planificación y ruteo.

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