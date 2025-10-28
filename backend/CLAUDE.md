## Coding Conventions

### Java Naming
- **Packages**: minusculas.sin_espacios (e.g., `com.proyecto.backend.model`)
- **Classes**: PascalCase (e.g., `Aeropuerto`, `AeropuertoService`)
- **Methods/Variables**: camelCase (e.g., `calcularDistancia()`, `husoHorario`)
- **Constants**: MAYUSCULAS_CON_GUIONES (e.g., `SEDES_PRINCIPALES`)
- **Repository Interfaces**: suffix Repository (e.g., `AeropuertoRepository`)
- **Special Characters**: No usar ñ ni tildes en variables (usar 'anio' en lugar de 'año')

### Spring Boot Structure
- **@Entity**: Clases de modelo con anotaciones JPA
- **@Repository**: Interfaces que extienden JpaRepository
- **@Service**: Clases de logica de negocio con @Transactional
- **@RestController**: Controladores REST con @RequestMapping

### Database Conventions
- **Table names**: minusculas con underscore (e.g., `aeropuertos`)
- **Column names**: minusculas con underscore (e.g., `codigo_icao`, `huso_horario`)
- **Primary Keys**: Usar el campo natural cuando sea apropiado (e.g., codigoICAO)

## Project Structure

```
backend/
├── src/main/java/com/proyecto/backend/
│   ├── algoritmo/           # Algoritmo Genetico
│   │   ├── core/           # Clases core del AG
│   │   │   ├── World.java              # Cache inmutable (templates)
│   │   │   ├── WorldTemporal.java      # Expansion temporal por request
│   │   │   ├── VueloInstancia.java     # Instancia concreta con fecha/UTC
│   │   │   ├── Chromosome.java         # Cromosoma del AG
│   │   │   ├── Solution.java           # Solucion planificada
│   │   │   ├── SubRuta.java            # Subruta de un pedido
│   │   │   ├── VueloUso.java           # Uso concreto de un vuelo
│   │   │   └── DecodificadorBasico.java # Greedy simple (v1)
│   │   ├── dto/            # DTOs del algoritmo
│   │   │   ├── request/
│   │   │   │   └── PlanificacionRequest.java
│   │   │   └── response/
│   │   │       ├── PlanificacionResponse.java
│   │   │       ├── AeropuertoEstadoDTO.java
│   │   │       ├── VueloEnRutaDTO.java
│   │   │       └── RutaPlanificadaDTO.java
│   │   ├── service/        # Servicios del algoritmo
│   │   │   ├── WorldCacheService.java
│   │   │   └── AlgoritmoGeneticoService.java
│   │   └── controller/     # Controladores del algoritmo
│   │       └── PlanificacionController.java
│   ├── controller/          # REST Controllers
│   │   ├── AeropuertoController.java
│   │   ├── PedidoController.java
│   │   ├── PlanDeVueloController.java
│   │   └── HealthController.java
│   ├── service/             # Business Logic
│   │   ├── AeropuertoService.java
│   │   ├── PedidoService.java
│   │   └── PlanDeVueloService.java
│   ├── repository/          # Data Access (JPA)
│   │   ├── AeropuertoRepository.java
│   │   ├── PedidoRepository.java
│   │   └── PlanDeVueloRepository.java
│   ├── model/               # JPA Entities
│   │   ├── Aeropuerto.java
│   │   ├── Pedido.java
│   │   └── PlanDeVuelo.java
│   ├── config/              # Configuration Classes
│   │   ├── HibernateConfig.java
│   │   ├── WebConfig.java
│   │   └── DatabaseConnectionChecker.java
│   ├── exception/           # Exception Handlers
│   │   └── GlobalExceptionHandler.java
│   └── BackendApplication.java
├── src/main/resources/
│   ├── datos/               # Data files
│   │   └── Aeropuertos.txt
│   └── application.properties
├── ENDPOINTS_PLANIFICACION.md  # Documentacion de endpoints del AG
└── pom.xml
```

## API Endpoints

### Health Check
- `GET /api/health` - Status de la aplicacion
- `GET /api/health/db` - Verificar conexion a base de datos

### Aeropuertos
- `GET /api/aeropuertos` - Obtener todos los aeropuertos
- `GET /api/aeropuertos/{codigo}` - Buscar por codigo ICAO
- `POST /api/aeropuertos/cargar` - Cargar aeropuertos desde archivo

### Pedidos
- `GET /api/pedidos` - Obtener todos los pedidos
- `POST /api/pedidos` - Crear un nuevo pedido
- `GET /api/pedidos/{id}` - Obtener pedido por ID

### Planes de Vuelo
- `GET /api/planes-vuelo` - Obtener todos los planes de vuelo
- `POST /api/planes-vuelo` - Crear un nuevo plan de vuelo

### Algoritmo Genetico - Planificacion
- `POST /api/planificacion` - Ejecutar planificacion de rutas
- `GET /api/planificacion/health` - Health check del servicio
- `GET /api/planificacion/world/estado` - Estado del cache en memoria
- `POST /api/planificacion/world/refrescar` - Refrescar cache desde BD

Ver `ENDPOINTS_PLANIFICACION.md` para detalles y ejemplos de Postman.

## Database Configuration

### MySQL (AWS RDS)
```properties
spring.datasource.url=jdbc:mysql://[host]:3306/dp1
spring.datasource.username=admin
spring.datasource.password=[password]
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
```

### Hibernate DDL
- **Development**: `spring.jpa.hibernate.ddl-auto=update`
- **Production**: `spring.jpa.hibernate.ddl-auto=validate`

## Estado Actual del Algoritmo Genetico

### Version Implementada: v1 - Greedy Basico

**Funcionando:**
- ✅ WorldTemporal: Expande templates de vuelos por horizonte temporal (7 dias)
- ✅ VueloInstancia: Conversiones UTC correctas, control de capacidad individual
- ✅ DecodificadorBasico: Greedy simple (directo + 1 escala)
- ✅ IDs con fecha completa: ORIGEN-DESTINO-YYYYMMDD-HHMM
- ✅ API REST funcional con DTOs correctos

**Pendiente de Implementar:**
- ❌ StockTracker: Control de capacidad de almacenes (difference arrays)
- ❌ Calculo de plazos de entrega (2/3 dias segun continente)
- ❌ Clasificacion de pedidos (a tiempo/tarde/no entregado)
- ❌ DecodificadorGenetico: Expansion greedy con heuristica de pesos
- ❌ Funcion fitness completa
- ❌ Operadores geneticos (cruce, mutacion, elitismo)
- ❌ Loop evolutivo (poblacion, generaciones)

### Reglas de Negocio

```java
// Plazos de entrega
PLAZO_MISMO_CONTINENTE = 2 dias
PLAZO_DIFERENTE_CONTINENTE = 3 dias
VENTANA_RECOJO = 2 horas

// Conexiones entre vuelos
MIN_CONEXION = 30 minutos
MIN_ESPERA_TRANSITO = 1 hora  // Aeropuertos intermedios

// Capacidades
Vuelos: 200-400 productos (segun vuelo)
Almacenes: 600-1000 productos (segun aeropuerto)
Hubs: ILIMITADO (SPIM, EBCI, UBBB)

// Horizonte temporal
Por defecto: 7 dias (calculado dinamicamente)
```

### Parametros del Algoritmo Genetico (Futuros)

```java
MAX_GEN = 200                 // Generaciones maximas
NO_IMPROV_LIMIT = 40          // Parar si 40 gen sin mejora
TAMANIO_POBLACION = 50-100    // Cromosomas por generacion
ELITE_K = 4                   // Mejores preservados
PCROSS = 0.8                  // Probabilidad de cruce
PMUT = 0.05                   // Probabilidad de mutacion
```

## Contribucion

- Seguir las convenciones de nomenclatura establecidas
- Documentar metodos publicos con JavaDoc
- Mantener codigo limpio y legible
- Incluir tests unitarios cuando sea apropiado
- NO USAR TILDES ni caracteres especiales
- Usar @Transactional en metodos de servicio
- Usar @Transactional(readOnly = true) para consultas
- Extender BaseEntity para auditoria automatica
- Español
