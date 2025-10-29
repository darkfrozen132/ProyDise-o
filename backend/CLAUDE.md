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
│   │   │   ├── ControladorAlmacenes.java # Control temporal de almacenes (slots)
│   │   │   ├── CalculadorPlazos.java   # Calculo de plazos de entrega
│   │   │   ├── EstadoEntrega.java      # Enum (A_TIEMPO, TARDE, NO_ENTREGADO)
│   │   │   ├── BuscadorRutas.java      # Busqueda BFS de rutas (reutilizable)
│   │   │   ├── Chromosome.java         # Cromosoma del AG
│   │   │   ├── Solution.java           # Solucion planificada
│   │   │   ├── SubRuta.java            # Subruta de un pedido
│   │   │   ├── VueloUso.java           # Uso concreto de un vuelo
│   │   │   ├── DecodificadorBasico.java    # Greedy simple (v3)
│   │   │   └── DecodificadorGenetico.java  # Greedy con prioridades del AG
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

### Version Implementada: v4 - Algoritmo Genetico Completo

**Funcionando:**
- ✅ WorldTemporal: Expande templates de vuelos por horizonte temporal (7 dias)
- ✅ VueloInstancia: Conversiones UTC correctas, control de capacidad individual
- ✅ DecodificadorBasico: Busqueda BFS para rutas con hasta 3 escalas
  - Busqueda generalizada con Queue (BFS)
  - Configurable con MAX_ESCALAS = 3
  - Verifica capacidades de vuelos y almacenes en cada paso
  - Retorna primera ruta válida (greedy)
- ✅ IDs con fecha completa: ORIGEN-DESTINO-YYYYMMDD-HHMM
- ✅ API REST funcional con DTOs correctos
- ✅ ControladorAlmacenes: Control de capacidad temporal con difference arrays
  - Slots de 60 minutos para rastrear ocupacion
  - Verifica almacenes intermedios y destino
  - Hubs con capacidad ilimitada
- ✅ CalculadorPlazos: Calculo de plazos de entrega (2/3 dias segun continente)
- ✅ Clasificacion de pedidos: EstadoEntrega (A_TIEMPO, TARDE, NO_ENTREGADO)
- ✅ Solution.calcularMetricas(): Metricas automaticas de entrega

- ✅ Funcion fitness completa con pesos configurables
  - Pedidos a tiempo: +100 puntos
  - Pedidos tarde: -50 puntos
  - Pedidos no entregados: -200 puntos
  - Violaciones capacidad: -1000 puntos
- ✅ BuscadorRutas: Logica BFS encapsulada y reutilizable
- ✅ DecodificadorGenetico: Greedy con prioridades de cromosoma
  - Interpreta genes como prioridades de pedidos
  - Ordena pedidos por prioridad descendente
  - Asigna rutas greedy respetando orden
  - Calcula fitness automáticamente
- ✅ Loop Evolutivo Completo (AlgoritmoGeneticoService)
  - Población: 50 individuos
  - Generaciones: hasta 200 (o 40 sin mejora)
  - Selección: Torneo de 3 individuos
  - Cruce: Uniforme (80% probabilidad)
  - Mutación: Gaussiana (5% probabilidad)
  - Elitismo: 4 mejores preservados
  - Logs detallados de evolución

**🎉 Algoritmo Genetico 100% Implementado**

**Mejoras Futuras Opcionales:**
- Operadores avanzados (cruce de dos puntos, mutación adaptativa)
- Paralelización de evaluación de población
- Diversidad poblacional (prevenir convergencia prematura)
- Algoritmos híbridos (búsqueda local post-AG)

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

### Parametros del Algoritmo Genetico (Configurados)

```java
// Poblacion y generaciones
TAMANIO_POBLACION = 50        // Individuos por generacion
MAX_GENERACIONES = 200        // Generaciones maximas
NO_MEJORA_LIMITE = 40         // Parar si 40 gen sin mejora

// Operadores geneticos
PROB_CRUCE = 0.8              // Probabilidad de cruce (80%)
PROB_MUTACION = 0.05          // Probabilidad de mutacion (5%)
ELITE_K = 4                   // Mejores preservados (elitismo)
TAMANIO_TORNEO = 3            // Individuos en seleccion por torneo

// Representacion cromosoma
1 gen por pedido              // Gen = prioridad del pedido [0, 1]
Mayor valor = mayor prioridad // Se procesa primero greedy
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
