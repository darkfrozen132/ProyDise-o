-- =====================================================
-- MIGRACIÓN: Agregar campos anio y mes a tabla pedidos
-- =====================================================
-- Fecha: 2025-10-30
-- Propósito: Permitir que el algoritmo genético use fechas completas
-- =====================================================

USE dp1;

-- Paso 1: Agregar columnas anio y mes (permiten NULL temporalmente)
ALTER TABLE pedidos 
ADD COLUMN anio INT NULL AFTER id,
ADD COLUMN mes INT NULL AFTER anio;

-- Paso 2: Actualizar registros existentes con valores por defecto
-- Asumimos que todos los pedidos son de Enero 2025
UPDATE pedidos 
SET anio = 2025, 
    mes = 1 
WHERE anio IS NULL;

-- Paso 3: Hacer las columnas NOT NULL
ALTER TABLE pedidos 
MODIFY COLUMN anio INT NOT NULL,
MODIFY COLUMN mes INT NOT NULL;

-- Verificar la estructura
DESCRIBE pedidos;

-- Verificar datos
SELECT id, anio, mes, dia, hora, minuto, aeropuerto_destino_id, cantidad_productos
FROM pedidos
LIMIT 10;

-- Estadísticas
SELECT 
    COUNT(*) as total_pedidos,
    COUNT(DISTINCT anio) as años_diferentes,
    COUNT(DISTINCT mes) as meses_diferentes,
    MIN(CONCAT(anio, '-', LPAD(mes, 2, '0'), '-', LPAD(dia, 2, '0'))) as fecha_minima,
    MAX(CONCAT(anio, '-', LPAD(mes, 2, '0'), '-', LPAD(dia, 2, '0'))) as fecha_maxima
FROM pedidos;

-- =====================================================
-- ROLLBACK (en caso de necesitar revertir)
-- =====================================================
-- ALTER TABLE pedidos DROP COLUMN anio;
-- ALTER TABLE pedidos DROP COLUMN mes;
-- =====================================================
