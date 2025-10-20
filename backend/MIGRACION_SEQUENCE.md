# Migración de IDENTITY a SEQUENCE

## Problema
Al cambiar de `GenerationType.IDENTITY` a `GenerationType.SEQUENCE`, aparece el error:
```
Duplicate entry '50' for key 'planesdevuelo.PRIMARY'
```

## Causa
La secuencia empieza en 1, pero ya existen registros con IDs 1, 2, 3... en la tabla.

## Solución

### Opción 1: Mantener datos existentes (RECOMENDADO)

1. **Ejecuta el script SQL** en tu base de datos MySQL:
   ```bash
   mysql -h basedatosdp1.cgyrteyzi7pp.us-east-1.rds.amazonaws.com -u admin -p dp1 < inicializar_secuencias.sql
   ```

2. **Reinicia la aplicación**
   - Los nuevos registros tendrán IDs desde 100000 en adelante
   - Los registros viejos (IDs 1-999) seguirán existiendo sin problemas

### Opción 2: Limpiar todo y empezar de cero

1. **Edita `application.properties`** y cambia:
   ```properties
   spring.jpa.hibernate.ddl-auto=create
   ```

2. **Reinicia la aplicación** (esto BORRARÁ todos los datos)

3. **Vuelve a cambiar a**:
   ```properties
   spring.jpa.hibernate.ddl-auto=update
   ```

## ¿Por qué SEQUENCE es más rápido?

| Aspecto | IDENTITY | SEQUENCE |
|---------|----------|----------|
| IDs generados | 1 por INSERT | 50 de una vez |
| Consultas a BD | 1000 para 1000 registros | ~20 para 1000 registros |
| Batch inserts | ❌ NO soportado | ✅ SÍ soportado |
| Velocidad | 🐌 20-30 seg | 🚀 2-3 seg |

## Configuración actual

- **initialValue**: 100000 (evita conflictos con datos existentes)
- **allocationSize**: 50 (pre-asigna 50 IDs en memoria)
- **batch_size**: 50 (agrupa 50 INSERTs en uno)

## Verificar que funciona

Después de ejecutar el script, verifica:
```sql
-- Ver el estado de las secuencias
SELECT NEXT VALUE FOR plan_vuelo_sequence;
SELECT NEXT VALUE FOR pedido_sequence;

-- Ver los IDs actuales en las tablas
SELECT MAX(id) FROM planesdevuelo;
SELECT MAX(id) FROM pedidos;
```

Los nuevos registros deberían tener IDs >= 100000.
