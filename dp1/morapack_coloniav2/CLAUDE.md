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