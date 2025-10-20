# Solución al Problema de Timeout de Conexiones

## Problema
```
Connection is not available, request timed out after 30152ms
No operations allowed after connection closed
```

## Causas
1. **Transacción muy larga**: Una sola transacción guardando miles de registros
2. **Pool de conexiones agotado**: No hay conexiones disponibles
3. **Timeout de MySQL**: Las conexiones se cierran por inactividad
4. **Latencia a AWS RDS**: Red lenta entre tu servidor y RDS

## Soluciones Aplicadas

### 1. ✅ Pool de Conexiones HikariCP Optimizado

```properties
# Más conexiones disponibles
spring.datasource.hikari.maximum-pool-size=20

# Conexiones viven más tiempo
spring.datasource.hikari.max-lifetime=600000  # 10 minutos

# Test de conexión antes de usar
spring.datasource.hikari.connection-test-query=SELECT 1

# Transacciones pueden durar más
spring.transaction.default-timeout=300  # 5 minutos
```

### 2. ✅ Batch Inserts Reales en MySQL

**CRÍTICO**: `rewriteBatchedStatements=true`

```properties
spring.datasource.url=...&rewriteBatchedStatements=true
```

Esto convierte:
```sql
-- ANTES (lento):
INSERT INTO tabla VALUES (1, 'a');
INSERT INTO tabla VALUES (2, 'b');
INSERT INTO tabla VALUES (3, 'c');

-- AHORA (rápido):
INSERT INTO tabla VALUES (1, 'a'), (2, 'b'), (3, 'c');
```

### 3. ✅ Transacciones Pequeñas

**Antes** (una transacción gigante):
```java
@Transactional
public void cargar() {
    // Guarda 10,000 registros en UNA transacción
    // Timeout después de 30 segundos
}
```

**Ahora** (múltiples transacciones):
```java
public void cargar() {
    // Lee todos los datos primero
    
    // Guarda en lotes de 500
    for (batch : batches) {
        guardarBatch(batch);  // Transacción separada
    }
}

@Transactional
private void guardarBatch(List<T> batch) {
    repository.saveAll(batch);
}
```

### 4. ✅ SEQUENCE en lugar de IDENTITY

Permite que Hibernate haga batch inserts reales:
- Pre-asigna 50 IDs en memoria
- No necesita consultar la BD por cada INSERT

## Verificación

Después de estos cambios, deberías ver:

```
Lectura completada. 1234 planes de vuelo parseados
Guardando 1234 planes de vuelo en la base de datos...
Progreso: 500/1234 planes guardados
Progreso: 1000/1234 planes guardados
✓ Total de planes de vuelo guardados: 1234
```

## Performance Esperada

| Métrica | Antes | Ahora |
|---------|-------|-------|
| Tiempo de carga (1000 registros) | 30+ seg (timeout) | 3-5 seg |
| Conexiones usadas | 1 (bloqueada) | 1 por batch |
| Transacciones | 1 muy larga | Múltiples cortas |
| Batch inserts | ❌ No | ✅ Sí |

## Troubleshooting

### Si sigue habiendo timeout:

1. **Reduce el tamaño del batch**:
   ```java
   int batchSize = 200; // En lugar de 500
   ```

2. **Aumenta el timeout**:
   ```properties
   spring.datasource.hikari.connection-timeout=60000
   spring.transaction.default-timeout=600
   ```

3. **Verifica la latencia a AWS**:
   ```bash
   ping basedatosdp1.cgyrteyzi7pp.us-east-1.rds.amazonaws.com
   ```

4. **Consulta los parámetros de MySQL RDS**:
   - `wait_timeout`: debe ser >= 600 segundos
   - `max_connections`: debe ser >= 100

## Monitoreo

Para ver el estado del pool de conexiones, activa logs:
```properties
logging.level.com.zaxxer.hikari=DEBUG
```
