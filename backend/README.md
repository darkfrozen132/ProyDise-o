# Backend Spring Boot - Sistema de Aeropuertos

Proyecto backend desarrollado con Spring Boot 3.2.0 y Java 17 para gestion de aeropuertos.

## Estructura del Proyecto

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/proyecto/backend/
│   │   │   ├── controller/       # Controladores REST
│   │   │   │   ├── AeropuertoController.java
│   │   │   │   └── HealthController.java
│   │   │   ├── service/          # Logica de negocio
│   │   │   │   └── AeropuertoService.java
│   │   │   ├── repository/       # Acceso a datos (JPA)
│   │   │   │   └── AeropuertoRepository.java
│   │   │   ├── model/            # Entidades JPA
│   │   │   │   ├── Aeropuerto.java
│   │   │   │   └── BaseEntity.java
│   │   │   ├── dto/              # Objetos de transferencia
│   │   │   ├── config/           # Configuraciones
│   │   │   │   ├── HibernateConfig.java
│   │   │   │   ├── WebConfig.java
│   │   │   │   └── DatabaseConnectionChecker.java
│   │   │   ├── exception/        # Manejo de excepciones
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   └── BackendApplication.java
│   │   └── resources/
│   │       ├── datos/
│   │       │   └── Aeropuertos.txt
│   │       └── application.properties
│   └── test/
└── pom.xml
```

## Tecnologias

- **Spring Boot 3.2.0**
- **Java 17**
- **Hibernate 6.x** (ORM)
- **Spring Data JPA**
- **Spring Web**
- **Lombok**
- **Spring Validation**
- **MySQL 8.0** (AWS RDS)

## Configuracion de Hibernate

### Caracteristicas implementadas

1. **Auditoria automatica**: Todas las entidades que extiendan `BaseEntity` tienen campos automaticos de `createdDate` y `lastModifiedDate`

2. **Configuracion optimizada**:
   - Batch processing para inserts/updates
   - SQL formateado para debugging
   - Estadisticas de Hibernate habilitadas (desarrollo)
   - Logging detallado de SQL y bindings

3. **Naming Strategy**: Configurado para usar nombres estandar de Java sin conversion automatica a snake_case

### Base de Datos

El proyecto esta configurado para trabajar con MySQL en AWS RDS:

**Configuracion en `application.properties`:**
```properties
spring.datasource.url=jdbc:mysql://[host]:3306/dp1
spring.datasource.username=admin
spring.datasource.password=[password]
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
```

**Estrategias DDL de Hibernate:**
- `create`: Crea el esquema, destruyendo datos previos
- `create-drop`: Crea al inicio, elimina al cerrar
- `update`: Actualiza el esquema (recomendado para desarrollo)
- `validate`: Solo valida el esquema
- `none`: No hace nada (recomendado para produccion)

## Ejecucion

### Con Maven
```bash
mvn spring-boot:run
```

### Con Maven Wrapper
```bash
./mvnw spring-boot:run
```

### Compilar JAR
```bash
mvn clean package
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

## API Endpoints

### Health Check
- `GET /api/health` - Verificar estado de la aplicacion
- `GET /api/health/db` - Verificar conexion a base de datos

**Ejemplo:**
```bash
curl http://localhost:8080/api/health
curl http://localhost:8080/api/health/db
```

### Aeropuertos

#### Obtener todos los aeropuertos
```bash
GET /api/aeropuertos
```

**Ejemplo:**
```bash
curl http://localhost:8080/api/aeropuertos
```

#### Buscar aeropuerto por codigo ICAO
```bash
GET /api/aeropuertos/{codigo}
```

**Ejemplo:**
```bash
curl http://localhost:8080/api/aeropuertos/SPIM
```

#### Cargar aeropuertos desde archivo
```bash
POST /api/aeropuertos/cargar
```

**Ejemplo:**
```bash
curl -X POST http://localhost:8080/api/aeropuertos/cargar
```

**Respuesta:**
```json
{
  "mensaje": "Aeropuertos cargados exitosamente",
  "cantidad": 30,
  "aeropuertos": [...]
}
```

## Modelo de Datos

### Aeropuerto

| Campo | Tipo | Descripcion |
|-------|------|-------------|
| codigoICAO | String (PK) | Codigo ICAO de 4 letras |
| ciudad | String | Nombre de la ciudad |
| pais | String | Nombre del pais |
| husoHorario | Integer | Huso horario GMT |
| capacidadAlmacen | Integer | Capacidad maxima del almacen |
| latitud | Double | Coordenada de latitud |
| longitud | Double | Coordenada de longitud |
| continente | String | Continente al que pertenece |
| createdDate | DateTime | Fecha de creacion (auto) |
| lastModifiedDate | DateTime | Fecha de modificacion (auto) |

### Metodos de negocio incluidos

- `esSedePrincipal()` - Verifica si es sede principal (Lima, Bruselas, Baku)
- `tieneStockIlimitado()` - Verifica capacidad ilimitada
- `puedeAlmacenar(cantidad)` - Verifica disponibilidad
- `reservarCapacidad(cantidad)` - Reserva espacio
- `liberarCapacidad(cantidad)` - Libera espacio
- `calcularDistancia(otro)` - Calcula distancia euclidiana
- `esMismoContinente(otro)` - Verifica mismo continente

## Desarrollo

El proyecto incluye:
- **CORS** configurado para permitir solicitudes desde cualquier origen
- **Validacion** de datos con Jakarta Validation
- **Manejo global de excepciones**
- **Lombok** para reducir codigo boilerplate
- **Spring DevTools** para recarga automatica
- **Auditoria JPA** con `@EnableJpaAuditing`

## Estructura de Capas

1. **Controller**: Recibe las peticiones HTTP
2. **Service**: Contiene la logica de negocio y carga de datos
3. **Repository**: Acceso a la base de datos con Spring Data JPA
4. **Model**: Entidades de la base de datos con Hibernate
5. **DTO**: Objetos para transferencia de datos

## Trabajando con Hibernate

### Crear una nueva entidad

```java
@Entity
@Table(name = "mi_entidad")
@Getter
@Setter
@NoArgsConstructor
public class MiEntidad extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    // Los campos createdDate y lastModifiedDate
    // se heredan automaticamente de BaseEntity
}
```

### Configuraciones utiles de Hibernate

#### Logging de SQL con valores
En `application.properties`:
```properties
# Ver SQL queries
logging.level.org.hibernate.SQL=DEBUG

# Ver valores de los parametros
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE

# Ver estadisticas de rendimiento
logging.level.org.hibernate.stat=DEBUG
```

#### Optimizacion de rendimiento
```properties
# Batch processing
spring.jpa.properties.hibernate.jdbc.batch_size=20
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
```

### BaseEntity
Todas las entidades deben extender `BaseEntity` para obtener:
- `createdDate`: Fecha de creacion (automatica)
- `lastModifiedDate`: Fecha de ultima modificacion (automatica)
- Listeners de JPA para auditoria

### Buenas practicas
1. Usar `@Transactional` en metodos de servicio que modifican datos
2. Usar `@Transactional(readOnly = true)` para consultas
3. Implementar paginacion para listados grandes
4. Usar DTOs para exponer solo los datos necesarios en la API
5. Configurar indices en campos que se usan frecuentemente en consultas
6. No usar tildes ni caracteres especiales en codigo

## Carga inicial de datos

Para cargar los aeropuertos desde el archivo `Aeropuertos.txt`:

1. Ejecutar la aplicacion
2. Llamar al endpoint POST:
   ```bash
   curl -X POST http://localhost:8080/api/aeropuertos/cargar
   ```

El servicio parseara el archivo UTF-16 y creara los registros en la base de datos.

## Verificacion de conexion

Al iniciar la aplicacion, el `DatabaseConnectionChecker` verifica automaticamente la conexion y muestra:
- URL de conexion
- Driver utilizado
- Version de la base de datos
- Usuario conectado

Ver los logs al iniciar para confirmar conexion exitosa.
