# 🔄 Cambios en la Estrategia de Generación de IDs

## ✅ Cambios Realizados

### 1. **Estrategia de IDs cambiada de SEQUENCE a IDENTITY**

**Antes:**
```java
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "plan_vuelo_seq")
@SequenceGenerator(
    name = "plan_vuelo_seq",
    sequenceName = "plan_vuelo_sequence",
    initialValue = 100000,
    allocationSize = 100
)
```

**Ahora:**
```java
@GeneratedValue(strategy = GenerationType.IDENTITY)
```

### 2. **Entidades Modificadas**
- ✅ `PlanDeVuelo.java` - Ahora usa `IDENTITY`
- ✅ `Pedido.java` - Ahora usa `IDENTITY`

### 3. **DatabaseCleanupConfig Deshabilitado**
- ❌ `@Configuration` comentado → la limpieza automática está **DESACTIVADA**
- Ahora JPA solo creará las tablas del ORM sin limpiar nada

---

## 📊 Tablas que se Crearán Automáticamente

Solo se crearán **5 tablas** (las del ORM):

1. ✅ `aeropuertos`
2. ✅ `planesdevuelo` 
3. ✅ `pedidos`
4. ✅ `rutas_solucion`
5. ✅ `vuelo_pedidos`

---

## ❌ Tablas que YA NO se Crearán

Con `IDENTITY` en lugar de `SEQUENCE`, Hibernate **NO creará**:

- ❌ `pedido_seq`
- ❌ `pedido_sequence`
- ❌ `plan_vuelo_sequence`
- ❌ `cliente_seq`
- ❌ `counters` (era tabla manual)
- ❌ `clientes` (no tiene entidad)
- ❌ `pedidos_v2` (no tiene entidad)

---

## 🎯 Ventajas de IDENTITY

### ✅ Pros:
- No crea tablas adicionales de secuencias
- Más simple y limpio
- Compatible con MySQL AUTO_INCREMENT
- Menos tablas en la base de datos

### ⚠️ Contras:
- Ligeramente más lento en inserciones masivas (batch insert menos eficiente)
- No pre-asigna IDs en memoria

---

## 🚀 Próximos Pasos

1. **Iniciar el backend**: Las tablas se crearán automáticamente
   ```bash
   mvn spring-boot:run
   ```

2. **Cargar datos iniciales**:
   - POST `/api/aeropuertos/cargar`
   - POST `/api/planesdevuelo/cargar`
   - POST `/api/pedidos/cargar` (si existe)

3. **Verificar tablas creadas**:
   ```sql
   SHOW TABLES;
   ```

---

## 🔧 Si Necesitas Volver a Habilitar la Limpieza Automática

Edita `DatabaseCleanupConfig.java` y descomenta `@Configuration`:

```java
@Configuration  // ← Descomentar esta línea
@RequiredArgsConstructor
@Slf4j
public class DatabaseCleanupConfig {
    // ...
}
```

---

## 📝 Notas Importantes

- ⚠️ Con IDENTITY, los IDs empiezan desde 1 (no desde 100000)
- ✅ La tabla `aeropuertos` usa el `codigo_icao` como PK (no afectada)
- ✅ Solo las tablas `pedidos` y `planesdevuelo` usan IDs auto-generados
- 🔄 Las tablas `rutas_solucion` y `vuelo_pedidos` ya usaban IDENTITY

---

**Fecha**: 3 de noviembre de 2025  
**Estado**: ✅ Listo para ejecutar
