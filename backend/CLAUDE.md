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
│   ├── controller/          # REST Controllers
│   │   ├── AeropuertoController.java
│   │   └── HealthController.java
│   ├── service/             # Business Logic
│   │   └── AeropuertoService.java
│   ├── repository/          # Data Access (JPA)
│   │   └── AeropuertoRepository.java
│   ├── model/               # JPA Entities
│   │   ├── Aeropuerto.java
│   │   └── BaseEntity.java
│   ├── dto/                 # Data Transfer Objects
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

## Contribucion

- Seguir las convenciones de nomenclatura establecidas
- Documentar metodos publicos con JavaDoc
- Mantener codigo limpio y legible
- Incluir tests unitarios cuando sea apropiado
- NO USAR TILDES ni caracteres especiales
- Usar @Transactional en metodos de servicio
- Usar @Transactional(readOnly = true) para consultas
- Extender BaseEntity para auditoria automatica
