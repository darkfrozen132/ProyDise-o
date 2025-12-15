# 📚 Carpeta de Información y Documentación del Backend

Esta carpeta contiene **toda la documentación, archivos de prueba y scripts de utilidad** del proyecto Package Planner. Ninguno de estos archivos es necesario para la ejecución del backend en producción.

---

## 📁 Contenido de esta Carpeta (62 archivos)

### 📖 **Documentación Principal del Nuevo Sistema**

#### **🚀 Sistema de Simulación con WebSockets (NUEVA IMPLEMENTACIÓN)**
- ✅ **`README_SIMULATION.md`** - **LEER PRIMERO** - Documentación completa del sistema
- ✅ **`ARQUITECTURA_SIMULACION.md`** - Diagramas y arquitectura detallada  
- ✅ **`GUIA_INTEGRACION_AG.md`** - Guía para integrar el Algoritmo Genético real
- `WEBSOCKET_API.md` - API WebSocket y protocolo STOMP
- `WEBSOCKET_IMPLEMENTACION.md` - Detalles de implementación WebSocket
- `WEBSOCKET_PLANIFICACION.md` - Planificación con WebSockets
- `WEBSOCKET_CONTROL.md` - Control y gestión de WebSockets

#### **🧬 Algoritmo Genético**
- `ALGORITMO_GENETICO_ESPAÑOL.md` - Documentación del AG en español
- `ALGORITMO_GENETICO_OLD.md` - Versión antigua del AG (histórico)
- `PARAMETROS_TIEMPO_AG.md` - Configuración de parámetros de tiempo

#### **📡 Server-Sent Events (SSE) para Tracking**
- `IMPLEMENTACION_SSE_SIMULACION.md` - Implementación SSE para tracking en tiempo real
- `SSE_SIMULACION_CON_RUTAS.md` - SSE con rutas completas
- `TRACKING_VUELOS_SSE.md` - Tracking de vuelos en tiempo real
- `INTEGRACION_SIMULACION_TRACKING.md` - Integración simulación + tracking
- `RESUMEN_SSE_SIMPLE.md` - Resumen simple de SSE
- `GUIA_USO_SSE.md` - Guía de uso de SSE
- `QUE_DEVUELVE_SSE.md` - Estructura de mensajes SSE
- `PRUEBA_SSE.md` - Pruebas de SSE
- `CICLO_VIDA_TRACKING.md` - Ciclo de vida del tracking

---

### 🔄 **Migraciones y Cambios del Sistema**
- `MIGRACION_ANIO_MES_PEDIDO.md` - Migración de campos año/mes en pedidos
- `MIGRACION_SEQUENCE.md` - Migración de sequences
- `CAMBIOS_GENERACION_IDS.md` - Cambios en generación de IDs
- `CAMBIO_IDENTITY_GENERACION_IDS.md` - Identity para generación de IDs
- `CAMBIOS_MODELO_RUTA_SOLUCION.md` - Cambios en modelo de rutas
- `CAMBIOS_PLANIFICACION.md` - Cambios en sistema de planificación
- `REORGANIZACION_PLANIFICADOR.md` - Reorganización del planificador

---

### ⚡ **Optimizaciones y Fixes**
- `MEJORAS_RENDIMIENTO.md` - Mejoras generales de rendimiento
- `OPTIMIZACIONES_VELOCIDAD.md` - Optimizaciones de velocidad
- `OPTIMIZACION_FINAL_DELETE.md` - Optimización de operaciones DELETE
- `PROBLEMA_LENTITUD_PLANES_VUELO.md` - Fix de lentitud en planes de vuelo
- `SOLUCION_TIMEOUT.md` - Solución a problemas de timeout
- `SOLUCION_COORDENADAS_NO_ACTUALIZAN.md` - Fix de actualización de coordenadas
- `FIX_PARSEO_COORDENADAS.md` - Fix de parseo de coordenadas
- `FIX_ORDEN_COLUMNAS_AEROPUERTOS.md` - Fix de orden de columnas

---

### 🗄️ **Base de Datos y Configuración**
- `AUTO_LIMPIEZA_BD_AL_INICIAR.md` - Limpieza automática de BD al iniciar
- `SINCRONIZACION_TIEMPO_VUELOS.md` - Sincronización de tiempos en vuelos

---

### 📚 **Otros Documentos**
- `planificaidor.md` - Documentación del sistema planificador
- `solucion_orquestador.md` - Solución de orquestación
- `especificacionCaso.md` - Especificaciones del caso de uso
- `DIFERENCIA_SISTEMAS.md` - Diferencias entre sistemas
- `API_PEDIDOS.md` - Documentación de API de pedidos
- `GUIA_PRUEBAS_SIMULACION.md` - Guía completa de pruebas
- `CLAUDE.md` - Notas de desarrollo con Claude AI

---

### 🧪 **Archivos de Prueba y Testing**

#### **Clientes HTML para Testing**
- ✅ **`test-simulation-client.html`** - Cliente de prueba profesional con STOMP (RECOMENDADO)
- `test-simulacion-sse.html` - Cliente SSE de prueba
- `test-sse-simulacion.html` - Otro cliente SSE alternativo
- `websocket-test.html` - Cliente WebSocket básico
- `ejemplo_sse_consola.html` - Ejemplo SSE en consola

#### **Scripts Shell de Prueba**
- `test_simulacion.sh` - Script bash para probar simulación
- `test-simulacion-sse.sh` - Script bash para probar SSE

---

### 🗃️ **Scripts SQL**

#### **Gestión y Mantenimiento de BD**
- `agregar_pedidos_prueba.sql` - Agregar pedidos de prueba
- `eliminar_todas_tablas.sql` - Eliminar todas las tablas
- `limpiar_base_datos.sql` - Limpiar completamente la BD
- `recrear_tabla_aeropuertos.sql` - Recrear tabla de aeropuertos
- `verificar_tabla.sql` - Verificar estructura de tablas

#### **Scripts de Migración**
- `migracion_agregar_anio_mes.sql` - Agregar campos año/mes
- `migracion_modelo_ruta_solucion_sse.sql` - Migración modelo de rutas
- `inicializar_secuencias.sql` - Inicializar sequences de BD

---

### ⚙️ **Archivos de Configuración**
- `configuracion_mysql_aws.properties` - Configuración MySQL para AWS RDS

---

### 📝 **Logs**
- `backend.log` - Archivo de logs de ejecución (si existe)

---

## 🎯 **Para Desarrolladores - Quick Start**

### **📚 Documentación Esencial (en orden de lectura):**

1. ✅ **`README_SIMULATION.md`** → Sistema completo de simulación con WebSockets
2. ✅ **`ARQUITECTURA_SIMULACION.md`** → Entender la arquitectura y flujo de datos
3. ✅ **`GUIA_INTEGRACION_AG.md`** → Cómo integrar el Algoritmo Genético real

### **🧪 Para Testing Rápido:**

1. **Iniciar el backend:**
   ```bash
   cd ../
   mvn spring-boot:run
   ```

2. **Abrir cliente de prueba:**
   ```bash
   # Desde la carpeta info
   open test-simulation-client.html
   ```

3. **Conectar y simular:**
   - URL: `http://localhost:8000`
   - Preset recomendado: "Balanceado"
   - Click en "Iniciar Simulación"

### **🔧 Para Troubleshooting:**
- `PROBLEMA_LENTITUD_PLANES_VUELO.md` - Si hay lentitud
- `SOLUCION_TIMEOUT.md` - Si hay timeouts
- `SOLUCION_COORDENADAS_NO_ACTUALIZAN.md` - Problemas de coordenadas

---

## 📦 **Estructura del Proyecto Backend**

```
backend/
├── src/                          # ← CÓDIGO FUENTE (NECESARIO)
│   ├── main/java/com/proyecto/backend/
│   │   ├── simulation/           # Sistema de simulación WebSocket
│   │   │   ├── controller/       # SimulationController (REST API)
│   │   │   ├── service/          # SimulationService (lógica核)
│   │   │   ├── session/          # SimulationSession (gestión estado)
│   │   │   └── dto/              # DTOs (Request, Snapshot, Messages)
│   │   ├── config/               # Configuraciones
│   │   │   ├── WebSocketConfig   # STOMP WebSocket
│   │   │   └── SchedulingConfig  # Tareas programadas
│   │   ├── model/                # Entidades JPA
│   │   ├── repository/           # Spring Data JPA
│   │   └── BackendApplication    # Main class
│   └── resources/
│       └── application.properties
├── pom.xml                       # ← DEPENDENCIAS (NECESARIO)
├── target/                       # ← COMPILADO (generado)
└── info/                         # ← DOCUMENTACIÓN (esta carpeta)
    ├── README.md                 # Este archivo
    ├── README_SIMULATION.md      # ⭐ Documentación principal
    ├── *.md                      # Toda la documentación técnica
    ├── *.html                    # Clientes de prueba
    ├── *.sh                      # Scripts de testing
    ├── *.sql                     # Scripts de BD
    └── *.properties              # Configuraciones de ejemplo
```

---

## 🚀 **Tecnologías Utilizadas**

### **Backend Core**
- **Spring Boot 3.5.6**
- **Java 21** (Virtual Threads)
- **Spring Data JPA**
- **Hibernate 6.x**

### **Comunicación en Tiempo Real**
- **WebSocket** con protocolo **STOMP**
- **SockJS** (fallback HTTP Long-Polling)
- **Server-Sent Events (SSE)** para tracking

### **Concurrencia Moderna**
- **Virtual Threads** (Project Loom - Java 21)
- **ConcurrentHashMap** para gestión de sesiones
- **Atomic primitives** para thread-safety

### **Base de Datos**
- **MySQL 8.0** (AWS RDS compatible)
- **Spring Data JPA Repositories**

### **Testing y Desarrollo**
- **JUnit 5** para tests unitarios
- **Mockito** para mocking
- **Spring Boot DevTools**
- **Lombok** para reducir boilerplate

---

## ⚠️ **Nota Importante**

**Ninguno de los archivos en esta carpeta es necesario para que el backend funcione en producción.**

### **El backend solo necesita:**
- ✅ `../src/` - Código fuente Java
- ✅ `../pom.xml` - Dependencias Maven  
- ✅ `../src/main/resources/application.properties` - Configuración

### **Todo lo demás en esta carpeta es:**
- 📚 Documentación técnica
- 🧪 Archivos de prueba y testing
- 🗃️ Scripts SQL de desarrollo
- 📝 Notas históricas y de desarrollo

---

## 🔗 **Enlaces Rápidos**

### **API REST del Sistema de Simulación:**
```
Base URL: http://localhost:8000/api/simulations

POST   /start              → Iniciar simulación
POST   /{id}/cancel        → Cancelar simulación
POST   /{id}/pause         → Pausar simulación  
POST   /{id}/resume        → Reanudar simulación
GET    /{id}               → Estado actual
GET    /                   → Listar sesiones activas
GET    /health             → Health check
```

### **WebSocket STOMP:**
```
Endpoint: ws://localhost:8000/ws
Canal:    /topic/simulations/{sessionId}
```

---

## 📊 **Características del Nuevo Sistema**

### ✅ **Arquitectura Moderna**
- Java 21 Virtual Threads para alta concurrencia
- WebSocket/STOMP con SockJS fallback automático
- Records de Java para DTOs inmutables
- Sin @Transactional en simulación (no bloquea BD)

### ✅ **Gestión de Simulaciones**
- Múltiples simulaciones simultáneas (100+)
- Control de ciclo de vida completo
- Estado thread-safe con primitivas atómicas
- Limpieza automática cada hora (previene Memory Leaks)

### ✅ **Comunicación en Tiempo Real**
- STOMP sobre WebSocket con protocolo estándar
- Throttling inteligente (500ms) para no saturar frontend
- Canales dinámicos por sesión
- Mensajes tipados con enums

### ✅ **Testing Incluido**
- Tests unitarios con JUnit 5 + Mockito
- Cliente HTML profesional con interfaz moderna
- Scripts bash para testing automatizado
- Ejemplos de integración React y Vue.js

---

## 🎓 **Recursos de Aprendizaje**

### **Para entender WebSockets:**
- `WEBSOCKET_API.md` - API completa documentada
- `WEBSOCKET_IMPLEMENTACION.md` - Detalles técnicos
- `test-simulation-client.html` - Código de ejemplo funcional

### **Para entender SSE (Server-Sent Events):**
- `IMPLEMENTACION_SSE_SIMULACION.md` - Implementación completa
- `GUIA_USO_SSE.md` - Cómo usar SSE
- `TRACKING_VUELOS_SSE.md` - Tracking en tiempo real

### **Para optimizar rendimiento:**
- `MEJORAS_RENDIMIENTO.md` - Técnicas aplicadas
- `OPTIMIZACIONES_VELOCIDAD.md` - Optimizaciones específicas

---

## 🛠️ **Herramientas de Desarrollo**

### **Scripts SQL útiles:**
```bash
# Limpiar BD completamente
mysql < limpiar_base_datos.sql

# Agregar datos de prueba
mysql < agregar_pedidos_prueba.sql

# Verificar estructura
mysql < verificar_tabla.sql
```

### **Testing rápido:**
```bash
# Test de simulación
./test_simulacion.sh

# Test de SSE
./test-simulacion-sse.sh
```

---

## 📈 **Métricas del Sistema**

### **Rendimiento Esperado:**
- **Simulaciones concurrentes**: 100+ simultáneas
- **Iteraciones por segundo**: 2-10 (según configuración AG)
- **Latencia WebSocket**: < 10ms
- **Memoria por sesión**: ~1-5 MB

### **Escalabilidad:**
- Virtual Threads permiten miles de conexiones simultáneas
- Sin locks ni bloqueos en código crítico
- Pool de threads configurable para tareas programadas

---

## ✨ **Siguientes Pasos para Integración**

1. **Leer `README_SIMULATION.md`** - Entender el sistema completo
2. **Leer `GUIA_INTEGRACION_AG.md`** - Preparar integración del AG
3. **Implementar `GeneticAlgorithm` interface** - Según guía
4. **Inyectar en `SimulationService`** - Reemplazar stub
5. **Probar con `test-simulation-client.html`** - Verificar funcionamiento
6. **Optimizar parámetros** - Ajustar según resultados

---

## 📞 **Soporte y Referencias**

### **Documentación Externa:**
- Spring WebSocket: https://docs.spring.io/spring-framework/reference/web/websocket.html
- STOMP Protocol: https://stomp.github.io/
- Java Virtual Threads: https://openjdk.org/jeps/444

### **Código de Ejemplo:**
- Cliente React: Ver `README_SIMULATION.md` sección "Integración con Frontend"
- Cliente Vue.js: Ver `README_SIMULATION.md` sección "Vue.js Composable"
- Cliente Vanilla JS: `test-simulation-client.html`

---

## 📝 **Historial de Cambios Importantes**

### **v2.0 - Sistema de Simulación con WebSockets (26/11/2025)**
- ✅ Implementación completa con Virtual Threads
- ✅ WebSocket/STOMP con SockJS fallback
- ✅ Tests unitarios completos
- ✅ Cliente HTML profesional
- ✅ Documentación extensa

### **v1.x - Sistema Original**
- Sistema de planificación básico
- SSE para tracking de vuelos
- Algoritmo Genético inicial

---

## 🎯 **Conclusión**

Esta carpeta `info/` es tu **centro de conocimiento** del proyecto. Contiene:

✅ **62 archivos** de documentación, pruebas y scripts  
✅ **3 documentos esenciales** para el nuevo sistema  
✅ **5 clientes HTML** de prueba funcionales  
✅ **10+ scripts SQL** para gestión de BD  
✅ **40+ documentos técnicos** históricos y actuales  

**Directorio raíz del backend ahora limpio:**
```
backend/
├── info/          # ← Toda la documentación (aquí)
├── src/           # ← Código fuente
├── pom.xml        # ← Dependencias
└── target/        # ← Compilado
```

---

**Última actualización:** 26 de noviembre de 2025  
**Versión del Sistema:** 2.0 (WebSocket + Virtual Threads)  
**Estado:** ✅ Producción - Listo para integrar AG real

---

## 📌 **Configuracion de Hibernate (Histórico)**

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
