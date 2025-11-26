# 📁 Reorganización del Planificador - Resumen

## ✅ Cambios Realizados

### 1. Nueva Estructura de Carpetas

**ANTES:**
```
src/main/java/com/proyecto/backend/algoritmo/
├── controller/
├── service/
├── core/
└── dto/
    ├── request/
    └── response/
```

**DESPUÉS:**
```
src/main/java/com/proyecto/backend/planificador/semanal/
├── controller/       # PlanificacionController.java
├── service/          # AlgoritmoGeneticoService, WorldCacheService
├── model/            # World, Chromosome, Solution, etc. (antes en core/)
├── dto/
│   ├── request/      # PlanificacionRequest
│   ├── response/     # PlanificacionResponse, etc.
│   └── sse/          # (vacío, listo para DTOs de simulación)
├── config/           # (vacío, listo para SemanalConfig)
├── util/             # (vacío, listo para utilidades)
└── logs/             # Logs del algoritmo genético
```

### 2. Packages Actualizados

Todos los archivos fueron actualizados de:
- `com.proyecto.backend.algoritmo.controller` → `com.proyecto.backend.planificador.semanal.controller`
- `com.proyecto.backend.algoritmo.service` → `com.proyecto.backend.planificador.semanal.service`
- `com.proyecto.backend.algoritmo.core` → `com.proyecto.backend.planificador.semanal.model`
- `com.proyecto.backend.algoritmo.dto.*` → `com.proyecto.backend.planificador.semanal.dto.*`

### 3. Endpoints Actualizados

**ANTES:**
```
POST /api/planificacion
GET  /api/planificacion/world/estado
POST /api/planificacion/world/refrescar
GET  /api/planificacion/health
```

**DESPUÉS:**
```
POST /api/planificacion/semanal
GET  /api/planificacion/semanal/world/estado
POST /api/planificacion/semanal/world/refrescar
GET  /api/planificacion/semanal/health
```

### 4. Archivos Movidos

**Controllers (1 archivo):**
- `PlanificacionController.java`

**Services (2 archivos):**
- `AlgoritmoGeneticoService.java`
- `WorldCacheService.java`

**Models (14 archivos):**
- `World.java`
- `WorldTemporal.java`
- `Chromosome.java`
- `Solution.java`
- `BuscadorRutas.java`
- `CalculadorPlazos.java`
- `ControladorAlmacenes.java`
- `DecodificadorBasico.java`
- `DecodificadorGenetico.java`
- `EstadoEntrega.java`
- `SubRuta.java`
- `VueloInstancia.java`
- `VueloUso.java`

**DTOs (5 archivos):**
- `PlanificacionRequest.java` (request)
- `PlanificacionResponse.java` (response)
- `AeropuertoEstadoDTO.java` (response)
- `RutaPlanificadaDTO.java` (response)
- `VueloEnRutaDTO.java` (response)

**Logs (3 archivos):**
- `genetico-ag.log`
- `genetico-rutas.txt`
- `morapack-ga.log`

---

## ⚠️ Problema Detectado: Incompatibilidad con Modelo `Pedido`

### Error de Compilación

El algoritmo genético espera que `Pedido` tenga:
```java
pedido.getAnio()  // ❌ NO EXISTE
pedido.getMes()   // ❌ NO EXISTE
pedido.getDia()   // ✅ EXISTE
```

Pero el modelo actual solo tiene:
```java
private int dia;
private int hora;
private int minuto;
```

### Archivos Afectados (8 ocurrencias):
1. `AlgoritmoGeneticoService.java` (líneas 301, 357, 358, 582, 678)
2. `DecodificadorGenetico.java` (línea 139)
3. `DecodificadorBasico.java` (línea 139)
4. `CalculadorPlazos.java` (líneas 80, 184)

### Uso en el código:
```java
LocalDate fechaPedido = LocalDate.of(pedido.getAnio(), pedido.getMes(), pedido.getDia());
```

---

## 🔧 Soluciones Propuestas

### **Opción 1: Agregar campos al modelo `Pedido` (RECOMENDADO)**

**Ventaja:** Solución completa y correcta
**Desventaja:** Requiere migración de BD

```java
@Entity
@Table(name = "pedidos")
public class Pedido {
    // ... campos existentes ...
    
    @Column(nullable = false)
    private int anio;  // 🆕 NUEVO
    
    @Column(nullable = false)
    private int mes;   // 🆕 NUEVO
    
    @Column(nullable = false)
    private int dia;   // ✅ YA EXISTE
    
    // ... resto del código ...
}
```

**Pasos:**
1. Agregar campos `anio` y `mes` al modelo `Pedido`
2. Crear script de migración SQL para actualizar registros existentes
3. Actualizar `PedidoService` para procesar estos campos al cargar desde archivo

---

### **Opción 2: Usar fecha de simulación + día relativo**

**Ventaja:** No requiere cambiar BD
**Desventaja:** Menos flexible

```java
// En AlgoritmoGeneticoService o PlanificacionRequest
private LocalDate fechaInicio; // Ej: 2025-01-15

// Al procesar pedido:
LocalDate fechaPedido = fechaInicio.plusDays(pedido.getDia() - 1);
```

**Pasos:**
1. Agregar `fechaInicio` a `PlanificacionRequest`
2. Modificar 8 archivos para usar fecha base + offset
3. Interpretar `dia` como día relativo a la fecha de inicio

---

### **Opción 3: Wrapper/Adapter Pattern**

**Ventaja:** No modifica modelo ni algoritmo
**Desventaja:** Código adicional

```java
@Data
public class PedidoAlgoritmo {
    private final Pedido pedido;
    private final LocalDate fechaBase;
    
    public int getAnio() {
        return fechaBase.plusDays(pedido.getDia() - 1).getYear();
    }
    
    public int getMes() {
        return fechaBase.plusDays(pedido.getDia() - 1).getMonthValue();
    }
    
    public int getDia() {
        return fechaBase.plusDays(pedido.getDia() - 1).getDayOfMonth();
    }
    
    // Delegar otros métodos a pedido...
}
```

---

## 📋 Próximos Pasos

1. **Decidir qué solución usar** para el problema de `anio`/`mes`
2. **Implementar la solución elegida**
3. **Compilar y probar** que el algoritmo funciona
4. **Crear `SimulacionController`** para SSE
5. **Implementar `SimulacionOrchestrator`** para coordinar ticks

---

## 🎯 Estructura Lista para Futuras Simulaciones

Cuando necesites agregar `colapso` o `diario`:

```
planificador/
├── semanal/     # ✅ YA EXISTE
├── colapso/     # 🔜 FUTURO (copiar estructura de semanal/)
└── diario/      # 🔜 FUTURO (copiar estructura de semanal/)
```

Cada una tendrá su propio:
- `controller/` con endpoints `/api/planificacion/{tipo}`
- `service/` con lógica específica
- `model/` con variaciones del algoritmo
- `dto/` con DTOs propios
- `config/` con parámetros específicos
