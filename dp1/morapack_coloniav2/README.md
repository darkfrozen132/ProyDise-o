# MoraPack Colonia v2 - Ant Colony Optimization

Sistema de optimización basado en algoritmos de colonias de hormigas para resolver problemas de planificación y ruteo.

## Estructura del Proyecto

```
morapack_coloniav2/
├── bin/                    # Archivos ejecutables compilados
├── datos/                  # Archivos de datos y configuración
├── src/                    # Código fuente
│   └── morapack/
│       ├── colonia/        # Algoritmos de colonia de hormigas
│       ├── datos/          # Manejo de datos y persistencia
│       ├── utils/          # Utilidades y helpers
│       └── core/           # Componentes principales
└── README.md
```

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

### React (para futuras integraciones)
- **Componentes**: PascalCase
  - Ejemplo: `MapaSimulacion.jsx`
- **Hooks**: prefijo use + camelCase
  - Ejemplo: `useMonitorWs`
- **Archivos**: camelCase
  - Ejemplo: `pedidoService.js`

## Algoritmo Ant Colony Optimization (ACO)

### Componentes Principales
- **Hormiga**: Agente que construye soluciones
- **Feromona**: Información persistente del entorno
- **Heurística**: Información local para guiar decisiones
- **Colonia**: Conjunto de hormigas y gestión del algoritmo

### Parámetros del Algoritmo
- `α (alfa)`: Influencia de la feromona
- `β (beta)`: Influencia de la información heurística
- `ρ (rho)`: Tasa de evaporación de feromonas
- `Q`: Constante para cálculo de feromonas

## Desarrollo Incremental

Este proyecto se desarrollará incrementalmente:

1. **Fase 1**: Implementación básica del algoritmo ACO
2. **Fase 2**: Adaptación al problema específico universitario
3. **Fase 3**: Optimizaciones y mejoras de rendimiento
4. **Fase 4**: Interfaz y visualización

## Instalación y Uso

### Requisitos
- Java 8 o superior
- Git (para clonar el repositorio)

### Compilación y Ejecución

#### Opción 1: Scripts Automáticos (Recomendado)

**En Windows:**
```bash
# Compilar
compile.bat

# Ejecutar
run.bat
```

**En Unix/Linux/Mac:**
```bash
# Dar permisos (solo la primera vez)
chmod +x compile.sh run.sh

# Compilar
./compile.sh

# Ejecutar
./run.sh
```

#### Opción 2: Makefile (Usuario Avanzado)
```bash
# Ver todas las opciones disponibles
make help

# Compilar
make

# Compilar y ejecutar
make run

# Limpiar y recompilar
make clean compile
```

#### Opción 3: Comandos Manuales
```bash
# Crear directorio bin
mkdir bin

# Compilar paso a paso
javac -d bin src/morapack/core/solucion/Solucion.java
javac -cp bin -d bin src/morapack/core/problema/Problema.java
javac -cp bin -d bin src/morapack/core/problema/ProblemaTSP.java
javac -cp bin -d bin src/morapack/colonia/componentes/*.java
javac -cp bin -d bin src/morapack/colonia/algoritmo/AlgoritmoColoniaHormigas.java
javac -cp bin -d bin src/morapack/main/Main.java

# Ejecutar
java -cp bin morapack.main.Main
```

### Solución de Problemas

Si encuentras errores de compilación:
1. Verifica que Java esté instalado: `java -version`
2. Asegúrate de estar en el directorio correcto del proyecto
3. Elimina el directorio `bin` y vuelve a compilar
4. En Windows, usa `compile.bat`; en Unix/Linux/Mac, usa `./compile.sh`

## Estructura de Paquetes Planificada

```
src/morapack/
├── colonia/
│   ├── algoritmo/          # Implementación del algoritmo ACO
│   ├── componentes/        # Hormigas, Feromonas, Heurísticas
│   └── utils/              # Utilidades específicas de ACO
├── datos/
│   ├── dao/                # Data Access Objects
│   ├── modelos/            # Clases de dominio
│   └── cargadores/         # Cargadores de datos
├── core/
│   ├── problema/           # Definición del problema a resolver
│   └── solucion/           # Representación de soluciones
└── utils/
    ├── matematicas/        # Funciones matemáticas
    └── configuracion/      # Gestión de configuración

datos/                      # Archivos de datos del sistema
├── aeropuertos.csv        # 31 aeropuertos (SAM, EUR, ASI)
├── planes_de_vuelo.csv    # Horarios y capacidades de vuelos
└── pedidos/
    └── pedidos_01.csv     # Pedidos mensuales (enero)
```

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

#### Métodos Implementados

**Para cálculos precisos (ACO):**
- `Vuelo.calcularDuracionReal(origen, destino)`: Duración considerando husos
- `Pedido.getTiempoPedidoUTC(destino)`: Conversión a UTC para algoritmos
- `Pedido.horasRestantesUTC(tiempoUTC, destino)`: Urgencia temporal

**Para debugging:**
- `Vuelo.getInformacionCompletaConHusos(origen, destino)`: Análisis detallado
- Conversiones automáticas UTC ↔ Local según necesidad

## Notas de Desarrollo

- Este es un proyecto académico enfocado en aprendizaje ACO
- Problema específico: Optimización de rutas de distribución logística
- Se priorizará claridad de código sobre optimización prematura
- Desarrollo iterativo con validación en cada paso