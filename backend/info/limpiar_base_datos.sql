-- ============================================
-- Script para LIMPIAR todas las tablas
-- Ejecutar con precaución: ELIMINA TODOS LOS DATOS
-- ============================================

-- Deshabilitar verificación de claves foráneas temporalmente
SET FOREIGN_KEY_CHECKS = 0;

-- Limpiar tabla de relación VueloPedido (muchos a muchos)
TRUNCATE TABLE vuelo_pedidos;

-- Limpiar tabla RutaSolucion (vuelos/rutas)
TRUNCATE TABLE rutas_solucion;

-- Limpiar tabla Pedido
TRUNCATE TABLE pedidos;

-- Limpiar tabla PlanDeVuelo
TRUNCATE TABLE planesdevuelo;

-- Limpiar tabla Aeropuerto
TRUNCATE TABLE aeropuertos;

-- Limpiar tablas auxiliares (clientes si existen)
TRUNCATE TABLE clientes;

-- Limpiar tablas de versiones antiguas
TRUNCATE TABLE pedidos_v2;

-- Reactivar verificación de claves foráneas
SET FOREIGN_KEY_CHECKS = 1;

-- Reiniciar las secuencias AUTO_INCREMENT
ALTER TABLE aeropuertos AUTO_INCREMENT = 1;
ALTER TABLE planesdevuelo AUTO_INCREMENT = 1;
ALTER TABLE pedidos AUTO_INCREMENT = 1;
ALTER TABLE rutas_solucion AUTO_INCREMENT = 1;
ALTER TABLE vuelo_pedidos AUTO_INCREMENT = 1;

-- Reiniciar sequences específicas (tabla counters)
UPDATE counters SET val = 1 WHERE name = 'pedido_seq';
UPDATE counters SET val = 1 WHERE name = 'plan_vuelo_seq';
UPDATE counters SET val = 1 WHERE name = 'cliente_seq';

-- Verificar que las tablas están vacías
SELECT 'Aeropuertos' as Tabla, COUNT(*) as Total FROM aeropuertos
UNION ALL
SELECT 'Planes de Vuelo', COUNT(*) FROM planesdevuelo
UNION ALL
SELECT 'Pedidos', COUNT(*) FROM pedidos
UNION ALL
SELECT 'Rutas Solución', COUNT(*) FROM rutas_solucion
UNION ALL
SELECT 'Vuelo-Pedido', COUNT(*) FROM vuelo_pedidos
UNION ALL
SELECT 'Clientes', COUNT(*) FROM clientes;

SELECT '✅ Base de datos limpiada exitosamente' as Resultado;
