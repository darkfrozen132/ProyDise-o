-- Script para actualizar pedidos existentes con anio y mes
-- Ejecutar este script ANTES de iniciar el backend con las nuevas columnas

-- Agregar columnas anio y mes si no existen (solo para MySQL)
ALTER TABLE pedidos
ADD COLUMN IF NOT EXISTS anio INT DEFAULT 2025 NOT NULL,
ADD COLUMN IF NOT EXISTS mes INT DEFAULT 1 NOT NULL;

-- Actualizar todos los pedidos existentes que tengan anio=0 o mes=0
UPDATE pedidos
SET anio = 2025, mes = 1
WHERE anio = 0 OR mes = 0;

-- Verificar la actualizacion
SELECT id, anio, mes, dia, hora, minuto, aeropuerto_destino_id, estado
FROM pedidos
ORDER BY id
LIMIT 10;
