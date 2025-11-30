-- ============================================
-- Script para ELIMINAR TODAS LAS TABLAS
-- ⚠️ PRECAUCIÓN: Esto elimina PERMANENTEMENTE todas las tablas
-- ⚠️ NO SE PUEDEN RECUPERAR los datos después de ejecutar esto
-- ============================================

-- Deshabilitar verificación de claves foráneas
SET FOREIGN_KEY_CHECKS = 0;

-- ==================== ELIMINAR TABLAS DE DATOS ====================

-- Tablas principales con entidades JPA
DROP TABLE IF EXISTS vuelo_pedidos;
DROP TABLE IF EXISTS rutas_solucion;
DROP TABLE IF EXISTS pedidos;
DROP TABLE IF EXISTS planesdevuelo;
DROP TABLE IF EXISTS aeropuertos;

-- ==================== NOTA SOBRE TABLAS DE SECUENCIAS ====================
-- Las tablas *_seq y *_sequence las crea automáticamente Hibernate
-- NO es necesario eliminarlas manualmente, Hibernate las recrea según necesite

-- Reactivar verificación de claves foráneas
SET FOREIGN_KEY_CHECKS = 1;

-- ==================== VERIFICACIÓN ====================

-- Mostrar todas las tablas restantes (debería estar vacío)
SHOW TABLES;

SELECT '✅ Todas las tablas han sido eliminadas' as Resultado;
SELECT '⚠️ ADVERTENCIA: Los datos NO se pueden recuperar' as Advertencia;
