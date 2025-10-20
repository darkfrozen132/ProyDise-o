-- Script para inicializar las secuencias correctamente en MySQL
-- Ejecutar ANTES de usar las nuevas secuencias con SEQUENCE

-- OPCIÓN 1: Si quieres mantener los datos existentes
-- Eliminar las secuencias si existen (MySQL 8.0+)
DROP SEQUENCE IF EXISTS plan_vuelo_sequence;
DROP SEQUENCE IF EXISTS pedido_sequence;

-- Crear las secuencias empezando desde 100000
CREATE SEQUENCE plan_vuelo_sequence START WITH 100000 INCREMENT BY 50;
CREATE SEQUENCE pedido_sequence START WITH 100000 INCREMENT BY 50;

-- Verificar las secuencias creadas
SELECT 'Secuencias creadas correctamente';
SELECT NEXT VALUE FOR plan_vuelo_sequence AS primer_id_plan_vuelo;
SELECT NEXT VALUE FOR pedido_sequence AS primer_id_pedido;

-- IMPORTANTE: Los nuevos registros tendrán IDs desde 100000 en adelante
-- Los registros viejos (con IDs bajos) seguirán existiendo sin problema

-- -----------------------------------------------------------------------------
-- OPCIÓN 2 (ALTERNATIVA): Si quieres limpiar todo y empezar de cero
-- Descomentar las siguientes líneas si prefieres borrar todos los datos:
-- -----------------------------------------------------------------------------
-- TRUNCATE TABLE planesdevuelo;
-- TRUNCATE TABLE pedidos;
-- TRUNCATE TABLE aeropuertos;
-- 
-- DROP SEQUENCE IF EXISTS plan_vuelo_sequence;
-- DROP SEQUENCE IF EXISTS pedido_sequence;
-- 
-- CREATE SEQUENCE plan_vuelo_sequence START WITH 1 INCREMENT BY 50;
-- CREATE SEQUENCE pedido_sequence START WITH 1 INCREMENT BY 50;

