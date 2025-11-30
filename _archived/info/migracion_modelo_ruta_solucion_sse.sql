-- =====================================================
-- MIGRACIÓN: Actualizar modelo RutaSolucion para SSE
-- =====================================================
-- Fecha: 2025-11-01
-- Propósito: Adaptar el modelo para coincidir con los datos del SSE
-- =====================================================

USE proyecto_diseno;

-- 1. Agregar columna id_vuelo (único identificador del vuelo)
ALTER TABLE rutas_solucion 
ADD COLUMN id_vuelo VARCHAR(100) UNIQUE 
COMMENT 'ID completo del vuelo (ej: UA123_D0_2025-01-15T00:00)';

-- 2. Actualizar nombres de columnas de región para coincidir con JSON
-- (Renombrar regionOrigen/regionDestino a regionOrigin/regionDestination)
ALTER TABLE rutas_solucion 
CHANGE COLUMN region_origen region_origin VARCHAR(50) 
COMMENT 'Región de origen (ej: AMERICA, EUROPA)';

ALTER TABLE rutas_solucion 
CHANGE COLUMN region_destino region_destination VARCHAR(50) 
COMMENT 'Región de destino (ej: AMERICA, EUROPA)';

-- 3. Renombrar altitud a altitude para coincidir con JSON
ALTER TABLE rutas_solucion 
CHANGE COLUMN altitud altitude INT 
COMMENT 'Altitud del vuelo en pies (ej: 35000)';

-- 4. Eliminar columna velocidad (speed) - no es necesaria
ALTER TABLE rutas_solucion 
DROP COLUMN IF EXISTS velocidad;

-- 5. Eliminar columna total_paquetes - se calcula desde orders
ALTER TABLE rutas_solucion 
DROP COLUMN IF EXISTS total_paquetes;

-- 6. Renombrar coordenadas de origen para coincidir con JSON (originLat/originLng)
ALTER TABLE rutas_solucion 
CHANGE COLUMN origen_latitud origin_lat DOUBLE 
COMMENT 'Latitud del aeropuerto de origen';

ALTER TABLE rutas_solucion 
CHANGE COLUMN origen_longitud origin_lng DOUBLE 
COMMENT 'Longitud del aeropuerto de origen';

-- 7. Renombrar coordenadas de destino para coincidir con JSON (destinationLat/destinationLng)
ALTER TABLE rutas_solucion 
CHANGE COLUMN destino_latitud destination_lat DOUBLE 
COMMENT 'Latitud del aeropuerto de destino';

ALTER TABLE rutas_solucion 
CHANGE COLUMN destino_longitud destination_lng DOUBLE 
COMMENT 'Longitud del aeropuerto de destino';

-- 8. Renombrar coordenadas actuales (currentLat/currentLng)
ALTER TABLE rutas_solucion 
CHANGE COLUMN current_latitud current_lat DOUBLE 
COMMENT 'Latitud actual del vuelo (tracking en tiempo real)';

ALTER TABLE rutas_solucion 
CHANGE COLUMN current_longitud current_lng DOUBLE 
COMMENT 'Longitud actual del vuelo (tracking en tiempo real)';

-- 9. Actualizar tabla vuelo_pedidos para usar "cantidad" en lugar de "quantity"
ALTER TABLE vuelo_pedidos 
CHANGE COLUMN quantity cantidad INT NOT NULL 
COMMENT 'Cantidad de productos/paquetes en este pedido';

-- 10. Agregar índices para mejorar rendimiento
CREATE INDEX idx_rutas_id_vuelo ON rutas_solucion(id_vuelo);
CREATE INDEX idx_rutas_en_vuelo ON rutas_solucion(en_vuelo);
CREATE INDEX idx_rutas_salida ON rutas_solucion(salida);
CREATE INDEX idx_vuelo_pedidos_order_id ON vuelo_pedidos(order_id);

-- =====================================================
-- VERIFICACIÓN
-- =====================================================

-- Verificar estructura de rutas_solucion
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'proyecto_diseno'
  AND TABLE_NAME = 'rutas_solucion'
ORDER BY ORDINAL_POSITION;

-- Verificar estructura de vuelo_pedidos
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'proyecto_diseno'
  AND TABLE_NAME = 'vuelo_pedidos'
ORDER BY ORDINAL_POSITION;

-- =====================================================
-- NOTAS
-- =====================================================
-- 
-- Mapeo JSON del SSE -> Base de Datos:
-- 
-- JSON SSE:
-- {
--   "id": "UA123_D0_2025-01-15T00:00",           → id_vuelo
--   "originCode": "KLAX",                        → origin_code
--   "destinationCode": "KJFK",                   → destination_code
--   "salida": "2025-01-15T00:30:00",            → salida
--   "llegada": "2025-01-15T08:45:00",           → llegada
--   "capacidad": 1000,                          → capacidad
--   "altitude": 35000,                          → altitude
--   "regionOrigin": "AMERICA",                  → region_origin
--   "regionDestination": "AMERICA",             → region_destination
--   "orders": [                                 → Tabla vuelo_pedidos
--     {
--       "orderId": "Ped123",                    → order_id
--       "cantidad": 50                          → cantidad
--     }
--   ],
--   "ruta": {
--     "origin": {
--       "lat": 33.9425,                         → origin_lat
--       "lng": -118.408                         → origin_lng
--     },
--     "destination": {
--       "lat": 40.6398,                         → destination_lat
--       "lng": -73.7789                         → destination_lng
--     }
--   }
-- }
-- 
-- Coordenadas actuales (tracking en tiempo real):
-- - current_lat: Latitud actual del vuelo (interpolada)
-- - current_lng: Longitud actual del vuelo (interpolada)
-- - progreso: Porcentaje de progreso (0.0 a 100.0)
-- - en_vuelo: true si el vuelo está activo
-- 
-- =====================================================
