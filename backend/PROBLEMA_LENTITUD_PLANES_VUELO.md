# Problema: Lentitud en la Carga de Planes de Vuelo

## 🔴 Problema Identificado

La carga de 2,866 planes de vuelo se queda **colgada/congelada** durante varios minutos y eventualmente falla con timeout de conexión MySQL.

### Evidencia
```
2025-11-03T19:13:57.588  INFO  PlanDeVueloService : Guardando 2866 planes de vuelo...
[... se queda CONGELADO aquí ...]
[3+ minutos después]
ERROR: Communications link failure
```

## 🔍 Causa Raíz

**Hibernate con `GenerationType.IDENTITY` NO puede hacer batch inserts verdaderos** porque necesita el ID generado inmediatamente después de cada INSERT individual.

### Flujo actual (LENTO):
1. Leer archivo → 2866 planes parseados ✅ (rápido, ~13ms)
2. Guardar en batch de 2000 → ❌ **AQUÍ SE CUELGA**
   - Hibernate ejecuta **2,866 INSERTs INDIVIDUALES**
   - Cada INSERT espera respuesta del servidor MySQL en AWS
   - Latencia de red AWS: ~100-200ms por INSERT
   - **Tiempo total: 2866 × 150ms ≈ 7 minutos**

## ✅ Soluciones Aplicadas

### Solución 1: Reducir Tamaño de Batch (IMPLEMENTADA)

**Cambios realizados:**
- `PlanDeVueloService.java`: batch_size 2000 → **50**
- `application.properties`: jdbc.batch_size 100 → **50**
- `application.properties`: transaction timeout 300s → **600s** (10 min)

**Resultado esperado:**
- 58 batches de 50 registros
- Commits más frecuentes = menor riesgo de timeout
- Tiempo estimado: **4-6 minutos** (sigue siendo lento)

### Solución 2: Usar SEQUENCE en lugar de IDENTITY (PENDIENTE, MEJOR)

**Ventaja:** Permite batch inserts verdaderos con `rewriteBatchedStatements=true`

**Cambio requerido** en `PlanDeVuelo.java`:
```java
// ANTES (actual):
@GeneratedValue(strategy = GenerationType.IDENTITY)

// DESPUÉS (recomendado):
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "plan_vuelo_seq")
@SequenceGenerator(name = "plan_vuelo_seq", sequenceName = "plan_vuelo_seq", allocationSize = 50)
```

**Resultado esperado:**
- Hibernate genera IDs en lotes de 50
- Un solo batch INSERT puede insertar 50 registros
- **Tiempo estimado: 10-20 segundos** para 2866 registros

## 📊 Comparación de Rendimiento

| Estrategia | INSERTs | Tiempo | Estado |
|------------|---------|--------|--------|
| IDENTITY (batch 2000) | 2866 individuales | 7+ min | ❌ Timeout |
| IDENTITY (batch 50) | 2866 individuales | 4-6 min | ⏳ Lento |
| SEQUENCE (batch 50) | 58 batch inserts | **10-20s** | ✅ **ÓPTIMO** |

## 🎯 Recomendación

**Migrar a SEQUENCE strategy:**
1. Aplicar cambios en `PlanDeVuelo.java`, `Pedido.java`
2. MySQL creará tablas `plan_vuelo_seq`, `pedido_seq`
3. Ganar 20x-30x en velocidad de inserción

## 📝 Notas Técnicas

- `rewriteBatchedStatements=true` ya está configurado en la URL de MySQL
- HikariCP configurado con 20 conexiones máximas
- SEQUENCE es la estrategia recomendada para bases de datos relacionales
- IDENTITY solo funciona bien en SQLite/PostgreSQL con RETURNING clause
