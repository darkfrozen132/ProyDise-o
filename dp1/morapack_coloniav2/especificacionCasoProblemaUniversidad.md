NOTA: ESTE DOCUMENTO DEBE SER SOLO EDITADO PARA MEJORAR EL ENTENDIMIENTO DEL PROBLEMA Y COMO SOLUCIONARLO CON EL ALGORITMO (Punto 3, 4 y 5)

Este es la especificación de mi caso universitario, para explicarlo tendremos una estructura, y luego explicaremos su contenido:
(El contenido puede ir actualizandose con el tiempo)
1. ENUNCIADO DE LA SITUACIÓN AUTÉNTICA: Se explica el caso general por parte del profesor.
2. NOTAS DEL PROFESOR: Se incluye notas del profesor en forma de preguntas y respuestas.
3. NOTAS DE CLAUDE: Se incluye comentarios de CLAUDE, como instrucciones o estructura para ayudarlo con el caso.
4. ESTRUCTURA DEL PROBLEMA.
5. ESTRUCTURA DE LA SOLUCION.

1. ENUNCIADO DE LA SITUACIÓN AUTÉNTICA

(Semestre 2025-2)

Problema simplificado para propósitos del curso

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


2. NOTAS DEL PROFESOR
	Consideraciones especiales para toda comunicación. Parte 1	
jue 28-ago-2025//12:15		Se tomará como sinónimos (en el contexto coloquial) lo siguiente:
- Pedido (perspectiva del cliente) y envío (perspectiva del proveedor).
- Entregado (al cliente) y Recibido (por el cliente).	
2	Consideraciones especiales para toda comunicación. Parte 2	
jue 28-ago-2025//12:20		No se debe hablar, de ahora en adelante, sobre el término paquete pues está causando confusión.

Un pedido/envío está compuesto de 1 o más productos.	
3	¿Los pedidos/envíos que realiza un cliente pueden ir a distintos destinos?	
jue 28-ago-2025//12:21		No. Cada pedido del cliente sólo tiene un único destino.	
4	¿Los pedidos/envíos que realiza un cliente tiene una cantidad máxima definida?	
jue 28-ago-2025//12:22		No. Cada pedido del cliente tiene uno o más productos a ser enviados.	
5	Los productos de los pedidos/envíos una vez que han sido planifcados o estén en ruta, ¿pueden ser reasignados?	
jue 28-ago-2025//12:23		Se trata de un tipo de producto que puede ser entregado a cualquier cliente de manera indistinta.
Cuando un producto está en tránsito (en un vuelo), sí puede ser reasignado.
Cuando un producto está en tierra (en un almacen), sí puede ser reasignado si el almacén es de paso (o de escala).
Cuando un producto está en tierra (en un almacen), no debe ser reasignado si el almacén es el de entrega (punto final).	
6	¿Los productos de los pedidos/envíos deben llegar todos en simultáneo?	
vie 19-sept-2025//18:00		No. Cada producto puede llegar de manera individual (o colectiva), siendo la única condición que se cumpla el plazo de entrega establecido.
(28/08/2025 12:29:00)
En el caso de reasignaciones, puede ocurrir que se combinen productos de distintos origenes, por tanto el plazo máximo queda en 3 días que corresponde a vuelo entre dos distintos continentes.	
Se reescribe la regla para mayor precisión sobre un pedido/envío:
- Cuando se tiene todos los productos entregados y si todos los productos son del mismo continente que el destino entonces el plaso es de 2 dias.
- Cuando se tiene todos los produtos entregados y si al menos un producto es de un continente destino que el destino entonces el plazo es de 3 días.
* Esta condición es independiente de si un pedido fue o no planificado varias veces.

7	¿Un vuelo puede ser cancelado?	
jue 28-ago-2025//12:35		Sí, Un vuelo puede ser cancelado.

Durante la interacción con el software en los tres escenarios (día a día, sim. semanal y sim. colapso) se puede cancelar manualmente un avión (no importa la causa). La cancelación puede ser desde el mapa vía el aeropuerto o desde un panel de seleccion.

Un vuelo no puede ser cancelado una vez que ha despegado.

Además, se va a generar un grupo de archivos para cancelaciones "programadas", para el caso de sim. semanal y sim.colapso.


Nota. Este hecho de usar una archivo de cancelaciones programadas no es resal pero se hace para los fines del curso.	
8	¿Un vuelo puede ser demorado?	
jue 28-ago-2025//12:39		Sí. La demora que se va a fijar es de 3 horas.

NOTA: Este ítem aún está en evaluación.	
9	¿Plantilla de pedidos?	
mié 17-sept-2025//19:00		La estructuta de los pedidos es mediante archivos mensuales como se presenta a continuación:
dd-hh-mm-dest-###-IdClien

Donde
dd: días en dos posiciones 01, 04, 12, 24
hh: horas en dos posiciones 01, 08, 14..23 (máximo de 23)
mm: minutos en dos posiciones 01, 08, 14, 25..59 (máximo de 59)
dest: codigo del aeropuerto destino considerado SVMI, SBBR, etc (basado en la lista de aeropuertos)
###: cantidad como cadena de 3 posiciones 001, 002, 089, 999
IdClien: 7 posiciones numéricas, completando 000 en todas las posiciones a la izquierda.

Sí un pedido de la lista tiene como "dest" uno de los almacenes principales: Lima, Bruselas, Baku

Cada línea del archivo, representa un único pedido. Cada pedido tiene entre 1 y 999 productos.

Nota. Se usa una tasa de crecimiento polinomial de los productos que se piden ese día siendo de la forma
y = 900 + x ^ n, donde n varia entre 1.119 a 1.229
La base de cantidad de productos suele ser el 10% (de la capacidad de toda flota en simultáneo).	Los pedidos que se registren o estén como data histórica o data proyectada que tengan como destino Lima, Bruselas o Baku, no serán consideradas para la planificación.

Esos datos de almacences principales deben ser abordados como parámetros y el número de ciudades puede cambiar en el futuro (aumentar o reducir).
10	¿los vuelos se pueden reprogramar?	
mié 17-sept-2025//19:15		No.	
11	¿los vuelos se pueden cancelar?	
mié 17-sept-2025//19:20		Sí
Se van a generar archivos mensuales de cancelación de vuelos.
dd.id-vuelo

Donde
dd: días en dos posiciones 01, 04, 12, 24
id-vuelo : ORIGEN-DESTINO-HoraOrigen	Los vuelos solo se pueden cancelar en tierra hasta el último minuto.
Los vuelos no se pueden cancelar una vez que han iniciado el vuelo.
12	¿Tiempos de carga, descarga y estancia de los paquetes?	
vie 19-sept-2025//15:00		Los tiempos de carga y descarga serán considerados despreciables (instantaneos).
Los tiempos de estancia mínima para los productos en tránsito (destino intermedio) es de 1 hora.	
13	¿El plazo total de entrega límite considera dentro o fuera el tiempo de espera en el aerpuerto destino?	
vie 19-sept-2025//15:30		La mejor forma de responder es indicar que las dos horas son necesarias para que la empresa MoraPack completar la limpieza, envasado final y los trámites administrativos de internamiento en el Pais (aduanas).
Luego, el cliente recién podrá recibir su paquete después de que la empresa MoraPack haya completado todo lo previsto.	
14	¿Cómo va a ser la prueba de Operaciones dia a día?	
vie 19-sept-2025//18:20		En el escenario de Operaciones se tienen lo siguiente:
- No existe data histórica
- No existe data proyectada.
- En la primera parte de la prueba, los 4 o 3 estudiantes registran pedidos/envíos de manera simultánea.
- En la segunda parte de la prueba, cargarán un archivo con un conjunto de registros (entre 4 y 36) diseñado para fal fin.	
15	¿Cómo debe ser la simulación semanal?	
vie 19-sept-2025//18:25		Lo esperado es que la simulación semanal se inicie desde un punto (una PC/Dispositivo/Web), esta se pueda ver también desde otras PC/Dispositivos de manera simultánea con una URL de acceso.	
16	¿Con respecto a que ubicación se mide el plazo de entrega?	
vie 19-sept-2025//18:30		El plazo de entrega se mide respecto de la hora minuto en que se hizó el envio/pedido en el uso horario del destino.	

3. NOTAS DE CLAUDE

### Análisis del Problema desde la perspectiva ACO

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

4. ESTRUCTURA DEL PROBLEMA

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

5. ESTRUCTURA DE LA SOLUCION

### Representación de Soluciones para ACO

**Estructura de una Solución:**
Una solución representa el plan completo de rutas para todos los productos en un período dado.

**Representación:**
```java
class SolucionMoraPack {
    Map<Integer, RutaProducto> rutasProductos;  // ID_producto -> ruta completa
    double fitness;                             // Costo total + penalizaciones
    boolean cumplePlazos;                       // Factibilidad temporal
    Map<String, Integer> usageVuelos;          // Uso de capacidad por vuelo
    Map<String, Integer> usageAlmacenes;       // Uso de capacidad por almacén
}

class RutaProducto {
    int idProducto;
    String aeropuertoOrigen;                    // Lima/Bruselas/Baku
    String aeropuertoDestino;                   // Destino final
    List<Escalas> escalas;                      // Aeropuertos intermedios
    LocalDateTime tiempoSalida;
    LocalDateTime tiempoLlegada;
    boolean cumplePlazo;
}

class Escala {
    String aeropuerto;
    LocalDateTime llegada;
    LocalDateTime salida;
    String vueloEntrada;  // ID del vuelo
    String vueloSalida;   // ID del vuelo
}
```

**Construcción de Soluciones por Hormigas:**

1. **Inicialización**: Cada hormiga recibe lista de productos pendientes
2. **Para cada producto**:
   - Seleccionar sede origen (Lima/Bruselas/Baku) basado en disponibilidad
   - Construir ruta paso a paso usando probabilidades ACO:
     ```
     P(aeropuerto_j) = [τ(i,j)]^α × [η(i,j)]^β / Σ[...]
     ```
3. **Heurística η(i,j)**:
   - Tiempo restante hasta deadline
   - Capacidad disponible en vuelo
   - Capacidad disponible en almacén destino
   - Costo operacional del vuelo

**Función de Evaluación (Fitness):**
```java
fitness = Σ(costo_vuelos_usados)
        + PENALIZACION_RETRASO * Σ(productos_retrasados)
        + PENALIZACION_CAPACIDAD * Σ(violaciones_capacidad)
        + BONUS_EFICIENCIA * (productos_entregados_temprano)
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