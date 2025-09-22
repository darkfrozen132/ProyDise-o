# MoraPack Genético - Nuevo Formato de Pedidos

## Estructura del Nuevo Sistema

### Formato de ID de Pedidos: `dd-hh-mm-dest-###-IdClien`

**Componentes:**
- `dd`: Días en dos posiciones (01-31)
- `hh`: Horas en dos posiciones (01-23) 
- `mm`: Minutos en dos posiciones (01-59)
- `dest`: Código del aeropuerto destino (4 letras, ej: SVMI, SBBR)
- `###`: Cantidad de productos (001-999)
- `IdClien`: ID del cliente (7 dígitos con ceros a la izquierda, ej: 0001234)

### Ejemplos de IDs Válidos:
- `21-11-03-SVMI-045-0001234` - Pedido del día 21 a las 11:03 hacia SVMI con 45 productos del cliente 0001234
- `15-14-30-SBBR-125-9876543` - Pedido del día 15 a las 14:30 hacia SBBR con 125 productos del cliente 9876543
- `01-08-15-KJFK-001-0000001` - Pedido del día 1 a las 08:15 hacia KJFK con 1 producto del cliente 0000001

## Implementación Técnica

### 1. Clase Pedido.java Actualizada
```java
// Nuevos métodos de validación
public static boolean esFormatoValido(String id)
public static String crearId(int dia, int hora, int minuto, String aeropuerto, int cantidad, String clienteId)

// Extracción de componentes
public static String extraerAeropuertoDestino(String id)
public static int extraerCantidad(String id)
public static String extraerClienteId(String id)

// Getters para componentes del ID
public int getDia()
public int getHora() 
public int getMinuto()
```

### 2. DAO Extendido con Nuevas Consultas
```java
// Consultas basadas en tiempo
List<Pedido> obtenerPorDia(int dia)
List<Pedido> obtenerPorHora(int hora)
List<Pedido> obtenerPorRangoHoras(int horaInicio, int horaFin)

// Consultas por cliente y cantidad
List<Pedido> obtenerPorCliente(String clienteId)
List<Pedido> obtenerPorCantidadMinima(int cantidadMinima)
List<Pedido> obtenerPorCantidadRango(int cantidadMin, int cantidadMax)

// Estadísticas avanzadas
long contarPorDia(int dia)
long contarPorHora(int hora)
double calcularPromedioProductosPorDia(int dia)
int obtenerCantidadMaximaPorDia(int dia)
```

### 3. Arquitectura del Sistema

```
morapack_genetico/
├── src/morapack/
│   ├── modelo/               # Entidades del dominio
│   │   ├── Pedido.java      # ✅ Nuevo formato implementado
│   │   ├── Aeropuerto.java  # ✅ Modelo existente
│   │   └── Sede.java        # ✅ Modelo existente
│   ├── dao/                 # Interfaces de acceso a datos
│   │   ├── PedidoDAO.java   # ✅ Interfaz extendida
│   │   ├── AeropuertoDAO.java
│   │   └── SedeDAO.java
│   ├── dao/impl/            # Implementaciones DAO
│   │   ├── PedidoDAOImpl.java   # ✅ Nuevos métodos implementados
│   │   ├── AeropuertoDAOImpl.java
│   │   └── SedeDAOImpl.java
│   ├── service/             # Lógica de negocio
│   │   └── OptimizacionService.java # ⏳ En desarrollo
│   ├── genetico/core/       # Algoritmo genético copiado
│   │   ├── AlgoritmoGenetico.java
│   │   ├── Poblacion.java
│   │   └── Individuo.java
│   ├── test/                # Pruebas
│   │   └── TestPedido.java  # ✅ Demostración del formato
│   └── demo/                # Demostraciones
│       └── DemoMoraPackGenetico.java # ✅ Demo completa
```

## Resultados de la Demostración

### Pedidos Generados
- **Total**: 75 pedidos con formato válido
- **Distribución temporal**: 15 pedidos por día (días 1-5)
- **Horarios**: De 8:00 a 16:00 en intervalos de 2 horas
- **Destinos**: SBBR, KJFK, SCFA, SVMG
- **Clientes**: 5 clientes diferentes con IDs válidos

### Estadísticas del Sistema
```
Día 01: 15 pedidos, promedio 116.2 productos, máximo 292 productos
Día 02: 15 pedidos, promedio 127.9 productos, máximo 290 productos
Día 03: 15 pedidos, promedio 171.7 productos, máximo 298 productos
Día 04: 15 pedidos, promedio 172.7 productos, máximo 296 productos
Día 05: 15 pedidos, promedio 150.7 productos, máximo 284 productos

Distribución por hora:
08:xx - 15 pedidos | 10:xx - 15 pedidos | 12:xx - 15 pedidos
14:xx - 15 pedidos | 16:xx - 15 pedidos

Consultas especializadas:
- Pedidos con 200+ productos: 22
- Pedidos del cliente 0001234: 10  
- Pedidos matutinos (8-12h): 45
```

## Integración con Algoritmo Genético

El sistema está preparado para integrar el algoritmo genético:

1. **Datos estructurados**: Pedidos con formato estándar para procesamiento
2. **DAOs implementados**: Acceso eficiente a datos para optimización
3. **Servicio de optimización**: Listo para combinar DAO + algoritmo genético
4. **Flexibilidad de consultas**: Filtros específicos para diferentes escenarios

## Próximos Pasos

1. ✅ **Completar implementaciones DAO** - HECHO
2. ⏳ **Finalizar servicio de optimización** - EN PROGRESO
3. 🔄 **Crear IndividuoAsignacion** para algoritmo genético
4. 🔄 **Integrar optimización completa** con asignación de pedidos a sedes
5. 🔄 **Pruebas de rendimiento** del algoritmo genético

El nuevo formato de pedidos proporciona una base sólida y organizada para la optimización mediante algoritmos genéticos en el sistema MoraPack.
