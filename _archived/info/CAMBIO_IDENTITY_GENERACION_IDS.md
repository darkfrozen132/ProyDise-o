# 🔑 Cambio de Estrategia de Generación de IDs

## 📋 Resumen

Se cambió la estrategia de generación de IDs de **`SEQUENCE`** a **`IDENTITY`** para evitar que Hibernate cree tablas de secuencias automáticamente.

---

## ⚙️ Cambios Realizados

### 1. **Entidades Modificadas**

#### `Pedido.java`
**Antes:**
```java
@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pedido_seq")
@SequenceGenerator(
    name = "pedido_seq",
    sequenceName = "pedido_sequence",
    initialValue = 100000,
    allocationSize = 100
)
private Long id;
```

**Después:**
```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

#### `PlanDeVuelo.java`
**Antes:**
```java
@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "plan_vuelo_seq")
@SequenceGenerator(
    name = "plan_vuelo_seq",
    sequenceName = "plan_vuelo_sequence",
    initialValue = 100000,
    allocationSize = 100
)
private Long id;
```

**Después:**
```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

---

## 🎯 Impacto

### ✅ **Ventajas de IDENTITY**

1. **Simplicidad**: No crea tablas adicionales de secuencias (`pedido_seq`, `plan_vuelo_sequence`, etc.)
2. **Compatibilidad con MySQL**: `AUTO_INCREMENT` es el método nativo de MySQL
3. **Menos Tablas**: La base de datos solo tendrá las 5 tablas del ORM

### ⚠️ **Desventajas de IDENTITY** (conocidas pero aceptables)

1. **Performance en Batch Inserts**: IDENTITY es ligeramente más lento que SEQUENCE en inserciones masivas
2. **Sin Pre-asignación**: No se pueden obtener IDs antes de insertar el registro
3. **Sin Cache**: Cada inserción requiere una consulta a la BD para obtener el ID generado

---

## 📊 Tablas que se Crearán Ahora

Con `IDENTITY`, Hibernate creará **SOLO** estas 5 tablas:

1. ✅ `aeropuertos` (con `codigo_icao` como PK - String)
2. ✅ `planesdevuelo` (con `id` BIGINT AUTO_INCREMENT)
3. ✅ `pedidos` (con `id` BIGINT AUTO_INCREMENT)
4. ✅ `rutas_solucion` (con `id` BIGINT AUTO_INCREMENT)
5. ✅ `vuelo_pedidos` (con `id` BIGINT AUTO_INCREMENT)

### ❌ **Tablas que YA NO se Crearán**

- ❌ `pedido_seq`
- ❌ `pedido_sequence`
- ❌ `plan_vuelo_sequence`
- ❌ `cliente_seq`

---

## 🔧 Configuración Relacionada

### `DatabaseCleanupConfig` - DESHABILITADO

El componente de limpieza automática ha sido **deshabilitado** comentando `@Configuration`:

```java
//@Configuration  // ← DESHABILITADO
@RequiredArgsConstructor
@Slf4j
public class DatabaseCleanupConfig {
    // ...
}
```

**Motivo**: Las tablas auxiliares ya no existen, por lo que intentar limpiarlas generaría errores.

---

## 🚀 Próximos Pasos

1. **Reiniciar el Backend**: Las tablas se crearán automáticamente con `ddl-auto=update`
2. **Verificar Tablas Creadas**: Solo deberían existir las 5 tablas del ORM
3. **Cargar Datos Iniciales**:
   - `POST /api/aeropuertos/cargar`
   - `POST /api/planesdevuelo/cargar`
   - `POST /api/pedidos/cargar`

---

## 📝 Notas Importantes

- **AUTO_INCREMENT en MySQL**: Los IDs comenzarán desde 1 y se incrementarán automáticamente
- **Sin Conflictos**: Cada tabla tiene su propio contador AUTO_INCREMENT independiente
- **Migraciones Futuras**: Si necesitas cambiar el valor inicial del AUTO_INCREMENT, usa:
  ```sql
  ALTER TABLE planesdevuelo AUTO_INCREMENT = 100000;
  ```

---

## 🔄 Reversión (si es necesaria)

Si necesitas volver a `SEQUENCE` en el futuro:

1. Descomentar las anotaciones `@SequenceGenerator`
2. Cambiar `GenerationType.IDENTITY` → `GenerationType.SEQUENCE`
3. Hibernate creará automáticamente las tablas de secuencias

---

**Fecha del cambio**: 3 de noviembre de 2025  
**Motivo**: Simplificar la base de datos eliminando tablas auxiliares de secuencias
