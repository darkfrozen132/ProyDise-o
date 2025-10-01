# MoraPack Colonia v2 - Ant Colony Optimization

Sistema de optimización basado en algoritmos de colonias de hormigas para resolver problemas de planificación y ruteo logístico con soporte completo para **entregas parciales múltiples**.

## Estructura del Proyecto

```
morapack_coloniav2/
├── bin/                    # Archivos ejecutables compilados
├── datos/                  # Archivos de datos y configuración
│   ├── aeropuertos.csv     # 31 aeropuertos globales (3 sedes + 28 destinos)
│   ├── planes_de_vuelo.csv # Horarios y capacidades de vuelos
│   └── pedidos/
│       └── pedidos_01.csv  # Pedidos enero
├── src/                    # Código fuente
│   └── morapack/
│       ├── colonia/        # Algoritmos de colonia de hormigas
│       │   ├── algoritmo/  # AlgoritmoColoniaHormigas (optimizado para logística)
│       │   └── componentes/ # Hormiga, Heuristica, Feromona
│       ├── datos/          # Manejo de datos y persistencia
│       │   ├── cargadores/  # Cargadores CSV especializados
│       │   └── modelos/     # RedDistribucion, Aeropuerto, Vuelo, Pedido
│       ├── core/           # Componentes principales
│       │   ├── problema/    # ProblemaMoraPack (problema logístico)
│       │   └── solucion/    # SolucionMoraPack (entregas parciales)
│       ├── ejemplos/       # Demostraciones y ejemplos
│       └── main/           # Clase principal
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

## Características Principales

### ✨ Modelo Híbrido de Entregas Parciales
- **Entregas Múltiples**: Un pedido puede dividirse en varias entregas
- **Productos Fungibles**: Los productos son intercambiables entre pedidos
- **Entregas Asíncronas**: Cada entrega puede llegar en momentos diferentes
- **Optimización Flexible**: Mejor utilización de capacidades de vuelo

### 🧠 Algoritmo ACO Optimizado para Logística
- **Parámetros Especializados**: Configuración específica para problemas logísticos
- **Heurísticas Inteligentes**: Urgencia, eficiencia, capacidad, proximidad geográfica
- **Depositación Elite**: Top 30% de hormigas con bonus por cumplimiento
- **Estadísticas Avanzadas**: Métricas específicas de logística

### 🌍 Manejo Global de Husos Horarios
- **Operación Multi-Continental**: SAM, EUR, ASI con husos horarios precisos
- **Conversiones Automáticas**: UTC ↔ Local según necesidad
- **Validación Temporal**: Cumplimiento de plazos por zona horaria
- **Cálculos Precisos**: Duración real de vuelos considerando husos

### 📊 Detección de Colapso del Sistema
- **Criterios Múltiples**: Plazos, capacidades, conectividad
- **Métricas en Tiempo Real**: Utilización, retrasos, factibilidad
- **Validación Integral**: Sistema completo y componentes individuales

## Estado del Desarrollo

### ✅ Fase 1 - Completada
- ✅ Implementación básica del algoritmo ACO
- ✅ Componentes Hormiga, Feromona, Heurística

### ✅ Fase 2 - Completada
- ✅ Adaptación completa al problema MoraPack
- ✅ Cargadores de datos CSV especializados
- ✅ Manejo de husos horarios globales

### ✅ Fase 3 - Completada
- ✅ Modelo híbrido de entregas parciales
- ✅ Optimizaciones específicas para logística
- ✅ Sistema de detección de colapso
- ✅ Eliminación de métodos deprecated

### 🚧 Fase 4 - En Progreso
- 🚧 Interfaz y visualización (pendiente)
- 🚧 Escenarios de evaluación (pendiente)

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
# Usar siempre los scripts automáticos (recomendado)
# Los scripts manejan las dependencias correctamente

# Windows
compile.bat
run.bat

# Unix/Linux/Mac
./compile.sh
./run.sh

# Ejemplos específicos
java -cp bin morapack.ejemplos.EjemploEntregasParciales
java -cp bin morapack.main.Main
```

### Solución de Problemas

Si encuentras errores de compilación:
1. Verifica que Java esté instalado: `java -version`
2. Asegúrate de estar en el directorio correcto del proyecto
3. Elimina el directorio `bin` y vuelve a compilar
4. En Windows, usa `compile.bat`; en Unix/Linux/Mac, usa `./compile.sh`

## Ejemplos de Uso

### Demostrar Entregas Parciales
```bash
java -cp bin morapack.ejemplos.EjemploEntregasParciales
```
**Output esperado:**
```
=== DEMOSTRACIÓN: MODELO HÍBRIDO CON ENTREGAS PARCIALES ===
📦 PEDIDO ORIGINAL:
   ID: 12345
   Destino: SEQM (Quito, Ecuador)
   Cantidad: 145 productos MPE

🚚 PLAN DE ENTREGAS PARCIALES:
   📦 Entrega #1: 60/145 productos (41.4%) - SPIM -> SEQM
   📦 Entrega #2: 45/145 productos (31.0%) - SPIM -> SBBR -> SEQM
   📦 Entrega #3: 40/145 productos (27.6%) - EBCI -> SEQM
```

### Ejecutar Sistema Principal
```bash
java -cp bin morapack.main.Main
```

### Estructura de Archivos de Datos
```
datos/
├── aeropuertos.csv        # 31 aeropuertos (3 sedes + 28 destinos)
│   # SPIM,Lima,Peru,lima,-5,440,-12.0219,-77.1143,SAM
├── planes_de_vuelo.csv    # Horarios y capacidades
│   # SKBO,SEQM,03:34,05:21,300
└── pedidos/
    └── pedidos_01.csv     # Pedidos enero
        # 30-09-15-SEQM-145-0054321
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