-- Script para agregar 30 pedidos de prueba con horarios entre 0:00 y 5:00
-- Estos pedidos tendrán fechas límite en las primeras 5 horas del día 1 de enero 2025

-- Pedidos distribuidos en las primeras 5 horas
INSERT INTO pedidos (aeropuerto_destino_id, anio, mes, dia, hora, minuto, cantidad_productos, cliente_id, estado) VALUES
-- Hora 0 (00:00 - 00:59) - 6 pedidos
('SGAS', 2025, 1, 1, 0, 15, 150, '9000001', 'PENDIENTE'),
('EHAM', 2025, 1, 1, 0, 25, 200, '9000002', 'PENDIENTE'),
('OPKC', 2025, 1, 1, 0, 35, 180, '9000003', 'PENDIENTE'),
('SUAA', 2025, 1, 1, 0, 45, 220, '9000004', 'PENDIENTE'),
('OMDB', 2025, 1, 1, 0, 50, 190, '9000005', 'PENDIENTE'),
('SKBO', 2025, 1, 1, 0, 55, 210, '9000006', 'PENDIENTE'),

-- Hora 1 (01:00 - 01:59) - 6 pedidos
('OERK', 2025, 1, 1, 1, 10, 175, '9000007', 'PENDIENTE'),
('LOWW', 2025, 1, 1, 1, 20, 165, '9000008', 'PENDIENTE'),
('SUAA', 2025, 1, 1, 1, 30, 195, '9000009', 'PENDIENTE'),
('EKCH', 2025, 1, 1, 1, 40, 185, '9000010', 'PENDIENTE'),
('SABE', 2025, 1, 1, 1, 50, 205, '9000011', 'PENDIENTE'),
('VIDP', 2025, 1, 1, 1, 55, 155, '9000012', 'PENDIENTE'),

-- Hora 2 (02:00 - 02:59) - 6 pedidos
('LKPR', 2025, 1, 1, 2, 5, 170, '9000013', 'PENDIENTE'),
('OJAI', 2025, 1, 1, 2, 15, 215, '9000014', 'PENDIENTE'),
('LATI', 2025, 1, 1, 2, 25, 160, '9000015', 'PENDIENTE'),
('SGAS', 2025, 1, 1, 2, 35, 225, '9000016', 'PENDIENTE'),
('EHAM', 2025, 1, 1, 2, 45, 145, '9000017', 'PENDIENTE'),
('OPKC', 2025, 1, 1, 2, 55, 230, '9000018', 'PENDIENTE'),

-- Hora 3 (03:00 - 03:59) - 6 pedidos
('SUAA', 2025, 1, 1, 3, 10, 140, '9000019', 'PENDIENTE'),
('OMDB', 2025, 1, 1, 3, 20, 235, '9000020', 'PENDIENTE'),
('SKBO', 2025, 1, 1, 3, 30, 135, '9000021', 'PENDIENTE'),
('OERK', 2025, 1, 1, 3, 40, 240, '9000022', 'PENDIENTE'),
('LOWW', 2025, 1, 1, 3, 50, 130, '9000023', 'PENDIENTE'),
('SUAA', 2025, 1, 1, 3, 55, 245, '9000024', 'PENDIENTE'),

-- Hora 4 (04:00 - 04:59) - 6 pedidos
('EKCH', 2025, 1, 1, 4, 5, 125, '9000025', 'PENDIENTE'),
('SABE', 2025, 1, 1, 4, 15, 250, '9000026', 'PENDIENTE'),
('VIDP', 2025, 1, 1, 4, 25, 120, '9000027', 'PENDIENTE'),
('LKPR', 2025, 1, 1, 4, 35, 255, '9000028', 'PENDIENTE'),
('OJAI', 2025, 1, 1, 4, 45, 115, '9000029', 'PENDIENTE'),
('LATI', 2025, 1, 1, 4, 55, 260, '9000030', 'PENDIENTE');

-- Verificar cuántos pedidos se agregaron
SELECT COUNT(*) as pedidos_agregados FROM pedidos WHERE cliente_id LIKE '900%';

-- Ver resumen por hora
SELECT 
    hora,
    COUNT(*) as cantidad_pedidos,
    MIN(minuto) as primer_minuto,
    MAX(minuto) as ultimo_minuto
FROM pedidos 
WHERE cliente_id LIKE '900%'
GROUP BY hora
ORDER BY hora;
