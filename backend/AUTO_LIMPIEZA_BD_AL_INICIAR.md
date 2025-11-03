# 🧹 Auto-Limpieza de Base de Datos al Iniciar

## Descripción

El backend ahora está configurado para **limpiar automáticamente la base de datos** cada vez que se inicia la aplicación.

## ⚠️ ADVERTENCIA

Esta funcionalidad **ELIMINA TODOS LOS DATOS** al iniciar el backend. 

**Solo debe usarse en:**
- ✅ Entorno de desarrollo
- ✅ Pruebas locales
- ✅ Testing

**NO usar en:**
- ❌ Producción
- ❌ Staging
- ❌ Cualquier entorno con datos importantes

## Implementación

La limpieza automática se realiza mediante el componente:
```
src/main/java/com/proyecto/backend/config/DatabaseCleanupConfig.java
```

### ¿Qué hace?

1. **Deshabilita las claves foráneas** temporalmente
2. **Limpia todas las tablas** usando `TRUNCATE TABLE`:
   - `vuelo_pedidos`
   - `rutas_solucion`
   - `pedidos`
   - `planesdevuelo`
   - `aeropuertos`
   - `clientes`
   - `pedidos_v2`

3. **Reactiva las claves foráneas**

4. **Reinicia los contadores AUTO_INCREMENT** a 1:
   - `aeropuertos`
   - `planesdevuelo`
   - `pedidos`
   - `rutas_solucion`
   - `vuelo_pedidos`

5. **Reinicia las secuencias** en la tabla `counters`:
   - `pedido_seq`
   - `plan_vuelo_seq`
   - `cliente_seq`

6. **Verifica y muestra** el estado de todas las tablas

## Logs al Iniciar

Cuando el backend inicie, verás logs como:

```
🧹 ========================================
🧹 LIMPIANDO BASE DE DATOS AL INICIAR
🧹 ========================================
Limpiando tabla: vuelo_pedidos
Limpiando tabla: rutas_solucion
Limpiando tabla: pedidos
Limpiando tabla: planesdevuelo
Limpiando tabla: aeropuertos
Limpiando tabla: clientes
Limpiando tabla: pedidos_v2
Reiniciando secuencias AUTO_INCREMENT
Reiniciando sequences en tabla counters
📊 Estado de la base de datos:
   - Aeropuertos: 0
   - Planes de Vuelo: 0
   - Pedidos: 0
   - Rutas Solución: 0
   - Vuelo-Pedido: 0
✅ Base de datos limpiada exitosamente
🧹 ========================================
```

## Cómo Deshabilitar

Si necesitas **DESHABILITAR** la limpieza automática, tienes 3 opciones:

### Opción 1: Comentar el Bean (Recomendado)
Edita `DatabaseCleanupConfig.java` y comenta el método:

```java
// @Bean
// @Transactional
// public CommandLineRunner cleanDatabase() {
//     ...
// }
```

### Opción 2: Eliminar el archivo
Elimina completamente el archivo:
```bash
rm src/main/java/com/proyecto/backend/config/DatabaseCleanupConfig.java
```

### Opción 3: Configuración por perfil
Agrega `@Profile("dev")` para que solo se ejecute en desarrollo:

```java
@Configuration
@RequiredArgsConstructor
@Slf4j
@Profile("dev")  // Solo en perfil 'dev'
public class DatabaseCleanupConfig {
    ...
}
```

Y ejecuta con:
```bash
# Con limpieza (perfil dev)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Sin limpieza (perfil prod)
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

## Flujo de Trabajo Típico

1. **Iniciar backend** → Se limpia la BD automáticamente
2. **Cargar datos** (aeropuertos, planes de vuelo, pedidos)
3. **Trabajar/probar** con datos limpios
4. **Reiniciar backend** → Se limpia nuevamente y vuelve al paso 2

## Ventajas

✅ **Siempre empiezas con datos limpios**
✅ **No hay datos corruptos de ejecuciones anteriores**
✅ **Pruebas más consistentes y reproducibles**
✅ **No necesitas ejecutar scripts SQL manualmente**
✅ **Los IDs siempre empiezan desde 1**

## Desventajas

⚠️ **Pierdes todos los datos al reiniciar**
⚠️ **Debes recargar datos cada vez**
⚠️ **NO apto para producción**

## Alternativas

Si quieres limpiar manualmente sin reiniciar el backend, sigue usando el script SQL:

```bash
mysql -h basedatosdp1.cgyrteyzi7pp.us-east-1.rds.amazonaws.com \
      -u admin -p'123456789dp1!' dp1 < limpiar_base_datos.sql
```

## Configuración Relacionada

- **Archivo**: `src/main/java/com/proyecto/backend/config/DatabaseCleanupConfig.java`
- **Script SQL**: `limpiar_base_datos.sql`
- **Tipo**: Bean de configuración Spring (`@Configuration`)
- **Ejecución**: `CommandLineRunner` (se ejecuta al inicio)
- **Transaccional**: Sí (`@Transactional`)

---

**Última actualización**: 3 de noviembre de 2025
