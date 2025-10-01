# ESPECIFICACIÓN DEL CASO PROBLEMA UNIVERSITARIO
## Sistema de Distribución MoraPack con Algoritmos ACO

> **NOTA IMPORTANTE**: Este documento se enfoca en mejorar el entendimiento del problema y su solución con algoritmos de optimización (Secciones 3, 4 y 5)

---

## 📋 ÍNDICE DE CONTENIDO

**I. DEFINICIÓN DEL PROBLEMA**
- [1. Enunciado de la Situación Auténtica](#1-enunciado-de-la-situación-auténtica)
- [2. Notas y Aclaraciones del Profesor](#2-notas-y-aclaraciones-del-profesor)

**II. ANÁLISIS Y DISEÑO DE SOLUCIÓN**
- [3. Análisis del Problema desde Perspectiva ACO](#3-análisis-del-problema-desde-perspectiva-aco)
- [4. Estructura del Problema](#4-estructura-del-problema)
- [5. Estructura de la Solución](#5-estructura-de-la-solución)

**III. IMPLEMENTACIÓN**
- [6. Documentación de Archivos de Datos](#6-documentación-de-archivos-de-datos)
- [7. Arquitectura del Sistema](#7-arquitectura-del-sistema)
- [8. Manejo Temporal y Husos Horarios](#8-manejo-temporal-y-husos-horarios)

---

# I. DEFINICIÓN DEL PROBLEMA

## 1. ENUNCIADO DE LA SITUACIÓN AUTÉNTICA

**Semestre:** 2025-2
**Tipo:** Problema simplificado para propósitos del curso

La empresa MoraPack se dedica a la venta y distribución de su producto estrella MPE a las
principales ciudades de América, Asia y Europa. MoraPack ha tenido un relativo éxito en el
cumplimiento de sus plazos de entrega, lo que le ha permitido crecer considerablemente en clientes y
ventas (envíos). La Empresa tiene como política que cada cliente debe recoger sus productos (uno o
más), directamente en sus oficinas en los aeropuertos. Se debe considerar que sólo se trabaja con un
aeropuerto en cada ciudad y el cliente tiene como plazo de recojo dos horas como máximo. Además,
los productos pueden llegar en distintos momentos siempre que todos lleguen dentro del plazo
establecido.
El cliente recibe, al inicio y luego a demanda (a pedido), el plan de viaje y ubicación (en
tiempo real) de sus productos comprados en cualquier momento. El servicio de monitoreo, indica en
qué ciudad está en ese momento, según el plan de viaje del paquete (actividad manual que hacían en
cada destino por donde pasaba el paquete).
MoraPack tiene sede en Lima (Perú), Bruselas (Bélgica) y Baku (Azerbaiyan) y stock
ilimitado en esas sedes. El plazo de entrega de los productos establecido en MoraPack es de dos días
como máximo para ciudades del mismo continente y de tres días para distinto continente. La
Empresa tiene un acuerdo de negocios con la Aerolínea PACK por la cual tiene un tiempo para el
traslado de paquete entre dos ciudades del mismo continente de medio día y diferente continente de
un día. Los vuelos de PACK se realizan una o más veces al día entre ciudades del mismo continente
y al menos una vez al día entre algunas ciudades de distinto continente. La capacidad máxima actual
de traslado para vuelos dentro del mismo continente varía entre de 200 y 300 paquetes según el
vuelo; y para distinto continente varía entre 250 y 400 paquetes según el vuelo. La capacidad de
almacenamiento en cada almacén en el aeropuerto varía entre 600 y 1000 paquetes, según la ciudad.
La empresa MoraPack ha contratado a usted y su equipo para que desarrolle una solución
informática para sus principales necesidades. Dichas necesidades se resumen en: (i) registrar la
cantidad de productos (MPE) a ser enviados a los clientes; (ii) planificar -y replanificar- las rutas
de los productos cumpliendo los plazos comprometidos (componente planificador); y (iii) presentar
gráficamente el monitoreo de las operaciones de la Empresa, en un mapa (componente visualizador).
Para la evaluación del curso, se manejará 3 escenarios: las operaciones día a día (en tiempo real), la
simulación semanal del traslado de los productos MPE y la simulación hasta el colapso de las
operaciones de la MoraPack. Para ello se requiere que: (a) el componente planificador resuelva
mediante parámetros los 3 escenarios; y (b) presente de manera gráfica información relevante del
desempeño de las operaciones (en los 3 escenarios). El primer escenario en resolverse debe ser el
de la simulación semanal que debe tomar en ejecutarse entre 30 y 90 minutos.
Requisitos No Funcionales:
Para este proyecto se establecen los siguientes requisitos no funcionales:
a. Presentar dos soluciones algorítmicas en Lenguaje Java y evaluadas por experimentación
numérica.
b. Los dos algoritmos de la experimentación numérica deben ser del tipo metaheurísticos.
c. La solución debe funcionar en el equipamiento provisto por el laboratorio de Ingeniería
Informática.
d. Se evaluará el proceso seguido utilizando NTP-ISO/IEC 29110-5-1-2 (VSE)
e. Se entregará el video de la presentación final del equipo (exposiciones).
f. Se entregará videos (avances o final) sobre los 3 escenarios requeridos.


## 2. NOTAS Y ACLARACIONES DEL PROFESOR

### 2.1 Consideraciones Generales de Comunicación

#### **Nota 1: Terminología Estándar**
**Fecha:** 28-ago-2025 12:15

**Sinónimos aceptados:**
- **Pedido** (perspectiva del cliente) ≡ **Envío** (perspectiva del proveedor)
- **Entregado** (al cliente) ≡ **Recibido** (por el cliente)

#### **Nota 2: Eliminación del Término "Paquete"**
**Fecha:** 28-ago-2025 12:20

⚠️ **No usar el término "paquete"** - causa confusión

**Clarificación:** Un pedido/envío está compuesto de 1 o más **productos**.

### 2.2 Características de los Pedidos

#### **Nota 3: Destinos por Pedido**
**Fecha:** 28-ago-2025 12:21
**Pregunta:** ¿Los pedidos de un cliente pueden ir a distintos destinos?
**Respuesta:** **NO.** Cada pedido del cliente tiene un único destino.

#### **Nota 4: Cantidad Máxima por Pedido**
**Fecha:** 28-ago-2025 12:22
**Pregunta:** ¿Los pedidos tienen una cantidad máxima definida?
**Respuesta:** **NO.** Cada pedido tiene uno o más productos (sin límite superior).

#### **Nota 5: Reasignación de Productos**
**Fecha:** 28-ago-2025 12:23
**Pregunta:** ¿Los productos planificados o en ruta pueden ser reasignados?

**Respuesta:** Los productos son intercambiables entre clientes.

**Reglas de reasignación:**
- ✅ **En tránsito** (en vuelo): SÍ puede ser reasignado
- ✅ **En tierra** (almacén de escala): SÍ puede ser reasignado
- ❌ **En tierra** (almacén destino final): NO debe ser reasignado

#### **Nota 6: Sincronización de Entregas**
**Fecha:** 19-sept-2025 18:00
**Pregunta:** ¿Los productos de un pedido deben llegar simultáneamente?

**Respuesta:** **NO.** Cada producto puede llegar individualmente, cumpliendo el plazo establecido.

**Reglas de plazo actualizadas (28/08/2025):**
- **Mismo continente**: Si todos los productos son del mismo continente que el destino → **2 días**
- **Diferente continente**: Si al menos un producto es de continente diferente al destino → **3 días**

> **Nota:** Esta condición es independiente de replanificaciones.

### 2.3 Gestión de Vuelos

#### **Nota 7: Cancelación de Vuelos**
**Fecha:** 28-ago-2025 12:35
**Pregunta:** ¿Un vuelo puede ser cancelado?
**Respuesta:** **SÍ.** Un vuelo puede ser cancelado manualmente.

**Condiciones de cancelación:**
- ✅ **Disponible en los 3 escenarios**: día a día, simulación semanal, simulación colapso
- ✅ **Métodos de cancelación**: desde mapa (vía aeropuerto) o panel de selección
- ❌ **Restricción**: NO se puede cancelar una vez que ha despegado

**Archivos de cancelación programada:**
- Se generan para simulación semanal y simulación colapso
- 📝 **Nota académica:** Uso de archivos no es realista, pero necesario para el curso

#### **Nota 8: Demoras de Vuelos**
**Fecha:** 28-ago-2025 12:39
**Pregunta:** ¿Un vuelo puede ser demorado?
**Respuesta:** **SÍ.** Demora fija de **3 horas**.

> ⚠️ **Nota:** Este ítem aún está en evaluación.

### 2.4 Formato de Datos

#### **Nota 9: Estructura de Pedidos**
**Fecha:** 17-sept-2025 19:00
**Pregunta:** ¿Cuál es la plantilla de pedidos?

**Formato:** `dd-hh-mm-dest-###-IdClien`

**Componentes:**
- **dd**: Días (2 posiciones): 01, 04, 12, 24
- **hh**: Horas (2 posiciones): 01, 08, 14...23 (máximo 23)
- **mm**: Minutos (2 posiciones): 01, 08, 14, 25...59 (máximo 59)
- **dest**: Código ICAO destino (4 caracteres): SVMI, SBBR, etc.
- **###**: Cantidad productos (3 posiciones): 001, 002, 089, 999
- **IdClien**: ID cliente (7 posiciones): completando con 000 a la izquierda

**Características de los archivos:**
- **Organización**: Archivos mensuales
- **Contenido**: Cada línea = 1 pedido único
- **Rango de productos**: 1 a 999 productos por pedido

**Exclusiones importantes:**
❌ **Pedidos con destino a sedes principales NO se procesan:**
- Lima, Bruselas, Baku

**Parámetros de crecimiento:**
- **Fórmula**: y = 900 + x^n, donde n ∈ [1.119, 1.229]
- **Base**: 10% de la capacidad total de flota

> 📝 **Configuración**: Las sedes principales son parámetros configurables
#### **Nota 10: Reprogramación de Vuelos**
**Fecha:** 17-sept-2025 19:15
**Pregunta:** ¿Los vuelos se pueden reprogramar?
**Respuesta:** **NO.**

#### **Nota 11: Archivos de Cancelación de Vuelos**
**Fecha:** 17-sept-2025 19:20
**Pregunta:** ¿Los vuelos se pueden cancelar?
**Respuesta:** **SÍ.** Se generan archivos mensuales de cancelación.

**Formato de cancelación:** `dd.id-vuelo`
- **dd**: Días (2 posiciones): 01, 04, 12, 24
- **id-vuelo**: ORIGEN-DESTINO-HoraOrigen

**Restricciones:**
- ✅ Se pueden cancelar en tierra hasta el último minuto
- ❌ NO se pueden cancelar una vez iniciado el vuelo

### 2.5 Operaciones y Tiempos

#### **Nota 12: Tiempos de Carga y Descarga**
**Fecha:** 19-sept-2025 15:00
**Pregunta:** ¿Tiempos de carga, descarga y estancia de productos?

**Respuesta:**
- **Carga y descarga**: Despreciables (instantáneos)
- **Estancia mínima en tránsito**: **1 hora** (destino intermedio)

#### **Nota 13: Tiempo de Procesamiento en Destino**
**Fecha:** 19-sept-2025 15:30
**Pregunta:** ¿El plazo incluye el tiempo de espera en aeropuerto destino?

**Respuesta:** Las **2 horas** son necesarias para:
- Limpieza y envasado final
- Trámites administrativos de internamiento (aduanas)

**Proceso:** Cliente recibe productos DESPUÉS del procesamiento completo.

### 2.6 Escenarios de Evaluación

#### **Nota 14: Operaciones Día a Día**
**Fecha:** 19-sept-2025 18:20
**Pregunta:** ¿Cómo será la prueba de Operaciones día a día?

**Características del escenario:**
- ❌ Sin data histórica
- ❌ Sin data proyectada

**Estructura de la prueba:**
1. **Primera parte**: 3-4 estudiantes registran pedidos simultáneamente
2. **Segunda parte**: Carga de archivo con 4-36 registros

#### **Nota 15: Simulación Semanal**
**Fecha:** 19-sept-2025 18:25
**Pregunta:** ¿Cómo debe ser la simulación semanal?

**Requisitos:**
- **Inicio**: Desde un punto central (PC/Dispositivo/Web)
- **Acceso**: Múltiples dispositivos simultáneos vía URL
- **Duración**: Simulación de una semana completa

### 2.7 Criterios de Colapso del Sistema

#### **Definición de Colapso del Sistema MoraPack**

El sistema MoraPack **colapsa** cuando los requerimientos establecidos por la empresa no se pueden cumplir. Esto ocurre en cualquiera de las siguientes condiciones:

#### **🚨 Condiciones Críticas de Colapso:**

1. **Violación de Plazos de Entrega**
   - ❌ **Pedidos retrasados**: Productos no entregados dentro del plazo (2-3 días)
   - ❌ **Imposibilidad temporal**: No existe ruta que permita cumplir el plazo

2. **Exceso de Capacidad en Vuelos**
   - ❌ **Vuelos saturados**: Capacidad insuficiente (300-360 productos/vuelo)
   - ❌ **Sin alternativas**: No hay vuelos alternativos disponibles

3. **Exceso de Capacidad en Aeropuertos**
   - ❌ **Almacenes saturados**: Capacidad excedida (400-480 productos/aeropuerto)
   - ❌ **Congestión**: Acumulación de productos sin posibilidad de reenvío

4. **Desconexión de Red**
   - ❌ **Rutas inválidas**: No existe ruta física entre sede y destino
   - ❌ **Aislamiento**: Aeropuertos sin conexiones viables

#### **🎯 Implementación de Detección:**

```java
// Métodos existentes para detectar colapso:
pedido.estaDentroPlazoUTC(tiempoActual, destino)     // Validación temporal
vuelo.puedeTransportar(cantidad)                      // Validación capacidad vuelo
aeropuerto.puedeAlmacenar(cantidad)                   // Validación capacidad almacén
red.buscarRutaMinima(origen, destino) != null        // Validación conectividad
```

#### **📊 Métricas de Colapso:**
- **Tasa de retraso**: % de pedidos fuera de plazo
- **Utilización de capacidad**: % de uso promedio de vuelos/almacenes
- **Conectividad**: % de rutas factibles disponibles
- **Tiempo hasta colapso**: Duración antes de saturación completa

#### **Nota 16: Referencia Temporal para Plazos**
**Fecha:** 19-sept-2025 18:30
**Pregunta:** ¿Respecto a qué ubicación se mide el plazo de entrega?

**Respuesta:** El plazo se mide respecto a la **hora del envío/pedido** en el **huso horario del destino**.

> 🕐 **Importante:** Base temporal para el sistema de husos horarios implementado

---

# II. ANÁLISIS Y DISEÑO DE SOLUCIÓN

## 3. ANÁLISIS DEL PROBLEMA DESDE PERSPECTIVA ACO

**Problema Principal**: Optimización de rutas de distribución de productos MPE desde 3 sedes principales (Lima, Bruselas, Baku) hacia aeropuertos de destino en América, Asia y Europa, cumpliendo restricciones de tiempo y capacidad.

**Características Clave del Problema:**
- **Multi-origen**: 3 sedes con stock ilimitado
- **Restricciones temporales**: 2 días mismo continente, 3 días diferente continente
- **Capacidades limitadas**: Vuelos (200-400 paquetes) y almacenes (600-1000 paquetes)
- **Replanificación dinámica**: Cancelaciones y reasignaciones en tiempo real
- **Escalas permitidas**: Los productos pueden hacer escalas (tiempo mínimo 1 hora)

**Mapeo a ACO:**
- **Nodos**: Aeropuertos (sedes + destinos + escalas)
- **Aristas**: Vuelos disponibles entre aeropuertos
- **Feromona**: Rutas exitosas que cumplen plazos
- **Heurística**: Combinación de tiempo, capacidad disponible y distancia
- **Restricciones**: Capacidades de vuelos/almacenes y plazos de entrega

**Escenarios de Evaluación:**
1. **Operaciones día a día**: Tiempo real con registro manual + archivo batch
2. **Simulación semanal**: 30-90 minutos de ejecución
3. **Simulación colapso**: Hasta saturación del sistema

## 4. ESTRUCTURA DEL PROBLEMA

### Definición Formal del Problema de Optimización

**Problema**: Vehicle Routing Problem with Time Windows and Capacity Constraints (VRPTWCC) Multi-Depot

**Variables de Decisión:**
- `x_ijk`: Binaria, 1 si el producto i usa el vuelo del aeropuerto j al aeropuerto k
- `t_i`: Tiempo de llegada del producto i a su destino final

**Función Objetivo:**
```
Minimizar: Σ(penalización_tiempo * retraso_i) + Σ(costo_operacional * x_ijk)
```

**Restricciones:**

1. **Conservación de flujo**: Cada producto debe llegar a su destino
   ```
   Σ_k x_ijk - Σ_m x_ikm = demanda_destino_i  ∀i,j
   ```

2. **Capacidad de vuelos**: No exceder capacidad por vuelo
   ```
   Σ_i x_ijk ≤ capacidad_vuelo_jk  ∀j,k
   ```

3. **Capacidad de almacenes**: No exceder capacidad de almacenamiento
   ```
   Σ_i productos_en_almacen_j ≤ capacidad_almacen_j  ∀j
   ```

4. **Restricciones temporales**: Cumplir plazos de entrega
   ```
   t_i ≤ tiempo_pedido_i + plazo_maximo_i  ∀i
   ```

5. **Tiempo mínimo de escala**: 1 hora mínima en aeropuertos intermedios
   ```
   tiempo_llegada_jk + 1_hora ≤ tiempo_salida_km  ∀j,k,m
   ```

**Parámetros del Problema:**
- Sedes principales: {Lima, Bruselas, Baku}
- Plazos: 2 días (mismo continente), 3 días (diferente continente)
- Capacidades vuelos: [200, 400] productos
- Capacidades almacenes: [600, 1000] productos
- Tiempo vuelo: 0.5 días (mismo continente), 1 día (diferente continente)

## 5. ESTRUCTURA DE LA SOLUCIÓN

### Representación de Soluciones para ACO

**Estructura de una Solución:**
Una solución representa el plan completo de rutas para todos los productos en un período dado.

**Representación Implementada:**
```java
class SolucionMoraPack {
    // MODELO HÍBRIDO: Soporte para entregas parciales múltiples
    Map<Integer, List<RutaProducto>> rutasPorPedido;  // ID_pedido -> lista de entregas
    double fitness;                                    // MAYOR = MEJOR solución
    boolean cumplePlazos;                             // Factibilidad temporal
    LocalDateTime tiempoCreacion;                     // Timestamp de creación
    boolean validacionRealizada;                      // Estado de validación
}

class RutaProducto {
    // Identificación del pedido y entrega
    int idPedido;                               // ID del pedido original
    int cantidadTransportada;                   // Cantidad en esta entrega
    int cantidadTotalPedido;                    // Cantidad total del pedido
    int numeroEntrega;                          // 1, 2, 3... para entregas múltiples
    boolean esEntregaParcial;                   // true si hay más entregas

    // Información de ruta
    String aeropuertoOrigen;                    // Lima/Bruselas/Baku
    String aeropuertoDestino;                   // Destino final
    List<SegmentoVuelo> segmentos;              // Vuelos que componen la ruta
    LocalDateTime tiempoSalida;                 // Inicio de la ruta
    LocalDateTime tiempoLlegada;                // Fin de la ruta
    boolean cumplePlazo;                        // Validación temporal

    // Métodos auxiliares
    double porcentajeCompletado();              // % de completitud de la entrega
    RutaProducto clonar();                      // Copia profunda
}

class SegmentoVuelo {
    String idVuelo;                             // ID único del vuelo
    String aeropuertoOrigen;                    // Aeropuerto de salida
    String aeropuertoDestino;                   // Aeropuerto de llegada
    LocalDateTime horaSalida;                   // Hora de salida
    LocalDateTime horaLlegada;                  // Hora de llegada
}
```

**Construcción de Soluciones por Hormigas (Implementación Real):**

1. **Inicialización**: Cada hormiga recibe lista de pedidos pendientes ordenados aleatoriamente
2. **Para cada pedido**:
   - Seleccionar pedido siguiente usando probabilidades basadas en urgencia y heurística
   - Construir rutas múltiples para el pedido (entregas parciales):
     ```java
     while (cantidadRestante > 0 && numeroEntrega <= 3) {
         String sedeOrigen = seleccionarSedeOrigen(destino, heuristica);
         int cantidadEntrega = determinarCantidadEntrega(cantidadRestante, capacidades);
         List<SegmentoVuelo> ruta = construirRuta(sedeOrigen, destino);
         // Crear entrega y agregar a la solución
     }
     ```
3. **Heurísticas Implementadas**:
   - **Urgencia**: horasRestantesUTC(), factorCantidad
   - **Eficiencia**: vuelos directos vs. escalas, capacidadPromedio
   - **Proximidad**: mismo continente, conexiones disponibles
   - **Capacidad**: ratio cantidadPedido/capacidadDisponible

**Función de Evaluación (Fitness) - MAYOR = MEJOR:**
```java
// NUEVA FUNCIÓN OBJETIVO: MAYOR FITNESS = MEJOR SOLUCIÓN
double fitness = costoOperacional + bonificacionTotal + bonificacionCompletitud +
                eficienciaEntregas + bonificacionEficienciaGeneral - penalizacionTotal;

Donde:
- costoOperacional: Costo base de todos los vuelos utilizados
- bonificacionTotal: Bonus por entregas que cumplen plazo (500.0 por entrega)
- bonificacionCompletitud: Bonus por pedidos 100% completados (200.0 por pedido)
- eficienciaEntregas: Bonus por eficiencia de entregas parciales
- penalizacionTotal: Penalización por retrasos (200.0 por día de retraso)

return Math.max(1.0, fitness); // Fitness mínimo = 1.0
```

**Validación de Solución:**
- ✅ Todos los productos tienen ruta completa
- ✅ Capacidades de vuelos no excedidas
- ✅ Capacidades de almacenes no excedidas
- ✅ Tiempos de escala ≥ 1 hora
- ✅ Sincronización temporal de vuelos

**Operadores de Mejora:**
- **2-opt en rutas**: Cambiar orden de escalas
- **Reasignación de sede**: Cambiar origen Lima/Bruselas/Baku
- **Intercambio de vuelos**: Mover productos entre vuelos con capacidad

---

# III. IMPLEMENTACIÓN

## 6. DOCUMENTACIÓN DE ARCHIVOS DE DATOS

### 6.1 Estructura de Datos del Sistema

El sistema utiliza tres archivos principales de datos ubicados en la carpeta `datos/`:

```
datos/
├── aeropuertos.csv              # Información de aeropuertos globales
├── planes_de_vuelo.csv         # Horarios y capacidades de vuelos
└── pedidos/
    └── pedidos_01.csv          # Pedidos mensuales (01=enero)
```

### 6.2 Archivo de Pedidos (`pedidos/pedidos_XX.csv`)

**Nomenclatura**: `pedidos_XX.csv` donde XX representa el mes (01=enero, 02=febrero, etc.)

**Formato de ID de Pedido**: `dd-hh-mm-dest-###-IdClien`

| Campo | Descripción | Formato | Rango/Ejemplo |
|-------|-------------|---------|---------------|
| `dd` | Día del mes | 2 dígitos | 01-31 |
| `hh` | Hora del pedido | 2 dígitos | 00-23 |
| `mm` | Minutos del pedido | 2 dígitos | 00-59 |
| `dest` | Código ICAO destino | 4 caracteres | SVMI, SEQM, OERK |
| `###` | Cantidad de productos | 3 dígitos | 001-999 |
| `IdClien` | ID del cliente | 7 dígitos | 0000001-9999999 |

**Ejemplo de Pedidos:**
```
30-09-15-SEQM-145-0054321  → Día 30, 09:15, Quito, 145 productos, cliente 0054321
16-11-55-SVMI-063-0012345  → Día 16, 11:55, Caracas, 63 productos, cliente 0012345
```

**❌ Restricción Importante**: Los pedidos con destino a sedes principales NO se procesan:
- **SPIM** (Lima, Perú)
- **EBCI** (Bruselas, Bélgica)
- **UBBB** (Baku, Azerbaiyán)

### 6.3 Archivo de Aeropuertos (`aeropuertos.csv`)

**Estructura**: `ICAO,Ciudad,Pais,Codigo,Huso,Capacidad,Latitud,Longitud,Continente`

| Campo | Tipo | Descripción | Ejemplo |
|-------|------|-------------|---------|
| `ICAO` | String(4) | Código ICAO internacional | SKBO, SEQM |
| `Ciudad` | String | Nombre de la ciudad | Bogota, Quito |
| `Pais` | String | País del aeropuerto | Colombia, Ecuador |
| `Codigo` | String(4) | Código interno abreviado | bogo, quit |
| `Huso` | Integer | Huso horario GMT | -5, 2, 3 |
| `Capacidad` | Integer | Capacidad almacén (productos) | 430, 410, 480 |
| `Latitud` | Double | Coordenada geográfica | 4.7014, -15.8647 |
| `Longitud` | Double | Coordenada geográfica | -74.1469, -47.9181 |
| `Continente` | String(3) | SAM/EUR/ASI | SAM, EUR, ASI |

**🏢 Sedes Principales MoraPack (Stock Ilimitado):**
- **SPIM** (Lima, Perú) - Capacidad: 440, Huso: -5
- **EBCI** (Bruselas, Bélgica) - Capacidad: 440, Huso: +2
- **UBBB** (Baku, Azerbaiyán) - Capacidad: 400, Huso: +2

**Distribución por Continente:**
- **SAM (América del Sur)**: 10 aeropuertos (SKBO, SEQM, SVMI, SBBR, SPIM, SLLP, SCEL, SABE, SGAS, SUAA)
- **EUR (Europa)**: 10 aeropuertos (LATI, EDDI, LOWW, EBCI, UMMS, LBSF, LKPR, LDZA, EKCH, EHAM)
- **ASI (Asia)**: 11 aeropuertos (VIDP, OSDI, OERK, OMDB, OAKB, OOMS, OYSN, OPKC, UBBB, OJAI)

### 6.4 Archivo de Planes de Vuelo (`planes_de_vuelo.csv`)

**Estructura**: `Origen,Destino,HoraSalida,HoraLlegada,Capacidad`

| Campo | Tipo | Descripción | Ejemplo |
|-------|------|-------------|---------|
| `Origen` | String(4) | Código ICAO aeropuerto origen | SKBO, SEQM |
| `Destino` | String(4) | Código ICAO aeropuerto destino | SEQM, SKBO |
| `HoraSalida` | Time | Hora salida formato HH:MM | 03:34, 14:22 |
| `HoraLlegada` | Time | Hora llegada formato HH:MM | 05:21, 16:09 |
| `Capacidad` | Integer | Capacidad máxima productos | 300, 340, 360 |

**Características Observadas:**
- **Vuelos bidireccionales**: Cada ruta tiene vuelos en ambas direcciones
- **Múltiples frecuencias**: Varios vuelos diarios en rutas principales
- **Capacidades variables**: Rango de 300-360 productos por vuelo
- **Cobertura global**: Conexiones entre SAM, EUR y ASI

**Ejemplos de Rutas:**
```
SKBO → SEQM: 3 vuelos diarios (03:34, 14:22, 19:01)
SEQM → SKBO: 3 vuelos diarios (04:29, 08:05, 19:55)
SKBO → SPIM: 3 vuelos diarios hacia sede Lima
```

### 6.5 Consideraciones para Implementación ACO

**Nodos del Grafo:**
- Total: 31 aeropuertos (3 sedes + 28 destinos)
- Sedes origen: SPIM, EBCI, UBBB
- Destinos válidos: 28 aeropuertos (excluyendo sedes)

**Restricciones Temporales:**
- **Mismo continente**: Máximo 2 días (48 horas)
- **Diferente continente**: Máximo 3 días (72 horas)
- **Tiempo mínimo escala**: 1 hora entre vuelos

**Restricciones de Capacidad:**
- **Vuelos**: 300-360 productos por vuelo
- **Almacenes**: 400-480 productos por aeropuerto
- **Sedes principales**: Capacidad ilimitada

**Función Objetivo Propuesta:**
```
Minimizar: Σ(costo_vuelo) + PENALIZACIÓN_RETRASO * Σ(productos_retrasados)
         + PENALIZACIÓN_CAPACIDAD * Σ(violaciones_capacidad)
```

## 7. ARQUITECTURA DEL SISTEMA

### 7.1 Clase RedDistribucion - Componente Central

La clase `RedDistribucion` es el **integrador principal** del sistema que conecta todos los componentes de datos y proporciona servicios al algoritmo ACO.

#### 🎯 **Propósito:**
- **NO es un algoritmo** de optimización
- **ES el "mundo/entorno"** que representa el estado completo del sistema MoraPack
- **Provee servicios** para que el algoritmo ACO explore y tome decisiones

#### 🏗️ **Funciones Principales:**

**1. Cargador e Integrador:**
```java
red.inicializar(1, 2025); // Carga todos los CSV automáticamente
// → aeropuertos.csv + planes_de_vuelo.csv + pedidos_01.csv
```

**2. Constructor de Grafos:**
- **Grafo de conectividad**: Aeropuertos conectados por vuelos
- **Índices de búsqueda**: Acceso O(1) a vuelos por ruta
- **Validación cruzada**: Integridad entre todos los datos

**3. Proveedor de Servicios para ACO:**
```java
List<Vuelo> opciones = red.buscarVuelosDirectos("SPIM", "SEQM");
List<String> ruta = red.buscarRutaMinima("SPIM", "SEQM");
List<Pedido> prioritarios = red.obtenerPedidosPrioritarios();
```

**4. Gestor de Estado del Sistema:**
- **Tiempo de referencia**: Base temporal para cálculos consistentes
- **Capacidades dinámicas**: Estado actual de vuelos y almacenes
- **Reinicio de estado**: Entre ejecuciones del algoritmo

#### ⏰ **Gestión Temporal:**
```java
// Tiempo fijo para simulaciones consistentes
red.setTiempoReferencia(LocalDateTime.of(2025, 1, 1, 0, 0));

// Evita inconsistencias del tiempo real (LocalDateTime.now())
// Permite simulaciones reproducibles y debugging
```

#### 🔄 **Flujo de Integración:**
```
1. Carga de Datos → 2. Construcción de Grafos → 3. Generación de Clientes
      ↓                         ↓                        ↓
4. Validación Cruzada → 5. Cálculo de Plazos → 6. Servicios para ACO
```

#### 🎯 **Relación con ACO:**
| RedDistribucion | AlgoritmoColoniaHormigas |
|-----------------|--------------------------|
| Provee el "mundo" | Explora el mundo |
| Carga y organiza datos | Busca soluciones óptimas |
| Calcula datos base | Optimiza rutas |
| Mantiene estado | Construye soluciones |

La `RedDistribucion` actúa como la **infraestructura base** que permite al algoritmo ACO concentrarse únicamente en la optimización, sin preocuparse por la carga de datos, validaciones o servicios de consulta.

## 8. MANEJO TEMPORAL Y HUSOS HORARIOS

### 8.1 Fundamentos del Sistema Temporal

El sistema MoraPack opera globalmente en tres continentes (SAM, EUR, ASI) con múltiples husos horarios. Para garantizar precisión en los cálculos temporales y cumplimiento de plazos, se han establecido reglas específicas de interpretación temporal.

### 8.2 Reglas de Interpretación Temporal

#### **📅 Planes de Vuelo**
```
Archivo: planes_de_vuelo.csv
Formato: Origen,Destino,HoraSalida,HoraLlegada,Capacidad
Ejemplo: SKBO,SEQM,03:34,05:21,300
```

**Interpretación:**
- **HoraSalida (03:34)**: Hora LOCAL del aeropuerto origen (SKBO = GMT-5)
- **HoraLlegada (05:21)**: Hora LOCAL del aeropuerto destino (SEQM = GMT-5)
- **Base temporal**: Ambas horas del mismo día calendario
- **Para cálculos**: Convertir a UTC y manejar cruces de día automáticamente

**Ejemplo de conversión:**
```
SPIM → EBCI: 00:58 → 22:02
• Salida: 00:58 Lima (GMT-5) = 05:58 UTC
• Llegada: 22:02 Bruselas (GMT+2) = 20:02 UTC del día siguiente
• Duración real: 38h 04min (vuelo intercontinental)
```

#### **📦 Pedidos**
```
Archivo: pedidos_XX.csv (XX = mes)
Formato: dd-hh-mm-dest-###-IdClien
Ejemplo: 30-09-15-SEQM-145-0054321
```

**Interpretación:**
- **dd**: Día del mes (30)
- **hh:mm (09:15)**: Hora LOCAL del aeropuerto destino (SEQM = GMT-5)
- **dest**: Código ICAO del destino (SEQM)
- **Plazo**: Se mide desde 09:15 EN EL HUSO HORARIO DEL DESTINO

**Fundamento (Nota Profesor #16):**
> "El plazo de entrega se mide respecto de la hora minuto en que se hizo el envio/pedido en el **uso horario del destino**."

**Ventajas de esta interpretación:**
1. **Consistente**: El cliente registra el pedido en su hora local
2. **Lógico**: El plazo se cumple en la hora local del cliente
3. **Simplificado**: No requiere conversiones adicionales para calcular plazos

### 8.3 Implementación en las Clases

#### **Clase Vuelo - Métodos Temporales:**

```java
// Duración real considerando husos horarios
Duration duracion = vuelo.calcularDuracionReal(aeropuertoOrigen, aeropuertoDestino);

// Verificar cruces de medianoche en UTC
boolean cruzaDia = vuelo.cruzaMedianocheReal(aeropuertoOrigen, aeropuertoDestino);

// Validar conexiones precisas
boolean puedeConectar = vuelo1.puedeConectarConReal(vuelo2, aeropuertoIntermedio, 60);

// Debugging temporal completo
String analisis = vuelo.getInformacionCompletaConHusos(origen, destino);
```

#### **Clase Pedido - Métodos Temporales:**

```java
// Conversión a UTC para cálculos del algoritmo
LocalDateTime pedidoUTC = pedido.getTiempoPedidoUTC(aeropuertoDestino);
LocalDateTime limiteUTC = pedido.getTiempoLimiteEntregaUTC(aeropuertoDestino);

// Validaciones temporales precisas
boolean dentroPlazo = pedido.estaDentroPlazoUTC(tiempoActualUTC, aeropuertoDestino);
long horasRestantes = pedido.horasRestantesUTC(tiempoActualUTC, aeropuertoDestino);
```

### 8.4 Aplicación en el Algoritmo ACO

#### **Función Heurística Temporal:**
```java
// Calcular urgencia de pedidos para heurística ACO
for (Pedido pedido : pedidosPendientes) {
    Aeropuerto destino = red.getAeropuerto(pedido.getCodigoDestino());
    long horasRestantes = pedido.horasRestantesUTC(tiempoActualUTC, destino);

    // Urgencia normalizada (0 = no urgente, 1 = muy urgente)
    double urgencia = Math.max(0, 72.0 - horasRestantes) / 72.0;

    // Usar en función heurística: η(i,j) = f(capacidad, distancia, urgencia)
}
```

#### **Validación de Rutas Temporales:**
```java
// Verificar factibilidad temporal de rutas
public boolean rutaEsFactible(List<Vuelo> ruta, Pedido pedido) {
    if (ruta.isEmpty()) return false;

    // Calcular tiempo total de la ruta
    LocalDateTime tiempoLlegada = calcularTiempoLlegadaRuta(ruta);
    LocalDateTime tiempoLimite = pedido.getTiempoLimiteEntregaUTC(aeropuertoDestino);

    return tiempoLlegada.isBefore(tiempoLimite) || tiempoLlegada.isEqual(tiempoLimite);
}
```

### 8.5 Beneficios del Sistema Temporal

1. **Precisión Global**: Manejo correcto de husos horarios en operaciones multinacionales
2. **Consistencia**: Reglas claras y uniformes para toda la aplicación
3. **Flexibilidad**: Métodos tanto locales como UTC según la necesidad
4. **Debugging**: Información detallada para análisis temporal
5. **Optimización ACO**: Heurísticas basadas en urgencia temporal real

Este sistema temporal robusto garantiza que el algoritmo ACO pueda tomar decisiones precisas considerando las restricciones temporales reales del problema de distribución global de MoraPack.

---

# IV. ESTADO ACTUAL DE IMPLEMENTACIÓN

## Estado del Proyecto (Enero 2025)

### ✅ COMPLETAMENTE IMPLEMENTADO

#### 1. **Modelo Híbrido de Entregas Parciales**
- ✅ **SolucionMoraPack**: Soporte completo para múltiples entregas por pedido
- ✅ **RutaProducto**: Tracking de cantidad, número de entrega, completitud
- ✅ **Métodos de gestión**: `getRutasProducto()`, `pedidoCompleto()`, `pedidoCumplePlazo()`
- ✅ **Estadísticas**: Eficiencia de entregas parciales, tasas de completitud

#### 2. **Algoritmo ACO Optimizado para Logística**
- ✅ **Hormiga**: Construcción basada en pedidos con entregas parciales
- ✅ **Heurística**: 5 tipos especializados (urgencia, eficiencia, capacidad, proximidad, híbrida)
- ✅ **Feromona**: Depositación elite diversificada con bonus por cumplimiento
- ✅ **AlgoritmoColoniaHormigas**: Parámetros optimizados para logística

#### 3. **Sistema de Datos Robusto**
- ✅ **RedDistribucion**: Integrador principal con servicios para ACO
- ✅ **Cargadores CSV**: Aeropuertos, vuelos, pedidos con validación cruzada
- ✅ **Modelos de dominio**: Aeropuerto, Vuelo, Pedido con métodos UTC
- ✅ **Manejo de husos horarios**: Conversiones automáticas y cálculos precisos

#### 4. **Función de Fitness Balanceada**
- ✅ **Convención moderna**: MAYOR fitness = MEJOR solución
- ✅ **Parámetros calibrados**: Penalizaciones 200.0, bonificaciones 500.0
- ✅ **Evaluación integral**: Costo + cumplimiento + eficiencia + completitud

#### 5. **Sistema de Validación y Detección de Colapso**
- ✅ **ValidadorColapso**: Detección de condiciones críticas
- ✅ **MetricasSistema**: Métricas de utilización y eficiencia
- ✅ **Criterios múltiples**: Plazos, capacidades, conectividad

#### 6. **Ejemplos y Demostraciones**
- ✅ **EjemploEntregasParciales**: Demostración completa del modelo híbrido
- ✅ **Output detallado**: Estadísticas, rutas, cumplimiento de plazos
- ✅ **API limpia**: Sin métodos deprecated, solo versiones modernas

### 🚧 PENDIENTE DE IMPLEMENTACIÓN

#### 1. **Escenarios de Evaluación**
- 🚧 **Operaciones día a día**: Registro manual + carga de archivos
- 🚧 **Simulación semanal**: Ejecución 30-90 minutos
- 🚧 **Simulación colapso**: Hasta saturación del sistema

#### 2. **Interfaz Gráfica**
- 🚧 **Componente visualizador**: Mapa con monitoreo en tiempo real
- 🚧 **Dashboard**: Métricas y estadísticas en vivo
- 🚧 **Panel de control**: Cancelaciones manuales de vuelos

#### 3. **Funcionalidades Avanzadas**
- 🚧 **Cancelación de vuelos**: Manual y programada por archivos
- 🚧 **Demoras de vuelos**: 3 horas fijas (en evaluación)
- 🚧 **Replanificación dinámica**: Reasignación automática

### 📊 Métricas de Completitud

| Componente | Estado | Completitud |
|------------|--------|-------------|
| **Core ACO** | ✅ Completo | 100% |
| **Modelo de Datos** | ✅ Completo | 100% |
| **Entregas Parciales** | ✅ Completo | 100% |
| **Evaluación de Fitness** | ✅ Completo | 100% |
| **Detección de Colapso** | ✅ Completo | 100% |
| **Manejo Temporal** | ✅ Completo | 100% |
| **Escenarios de Evaluación** | 🚧 Pendiente | 0% |
| **Interfaz Gráfica** | 🚧 Pendiente | 0% |
| **Funciones Avanzadas** | 🚧 Pendiente | 30% |

### 🎯 **Próximos Pasos Recomendados**

1. **Implementar escenarios de evaluación** (prioridad alta)
2. **Desarrollar interfaz básica de visualización**
3. **Integrar funcionalidades de cancelación de vuelos**
4. **Crear simulaciones de carga para pruebas de colapso**

El proyecto ha alcanzado un **estado altamente funcional** con todas las funcionalidades core implementadas y optimizadas para el dominio logístico específico de MoraPack.