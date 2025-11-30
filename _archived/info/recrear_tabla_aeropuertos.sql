-- ============================================
-- Script para RECREAR la tabla aeropuertos
-- con el orden correcto de columnas
-- ============================================

-- Deshabilitar verificación de claves foráneas
SET FOREIGN_KEY_CHECKS = 0;

-- Eliminar tabla aeropuertos si existe
DROP TABLE IF EXISTS aeropuertos;

-- Reactivar verificación de claves foráneas
SET FOREIGN_KEY_CHECKS = 1;

-- Mostrar tablas restantes
SHOW TABLES;

SELECT '✅ Tabla aeropuertos eliminada. Inicia el backend para recrearla correctamente.' as Resultado;
