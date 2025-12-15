package com.proyecto.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Configuración para limpiar automáticamente la base de datos al iniciar la aplicación
 * 
 * ⚠️ ADVERTENCIA: Este componente ELIMINA TODOS LOS DATOS al iniciar
 * Solo usar en desarrollo/testing
 * 
 * ⚠️ DESHABILITADO: Comentar @Configuration para desactivar la limpieza automática
 */
//@Configuration  // ← DESHABILITADO: JPA creará solo las tablas del ORM
@RequiredArgsConstructor
@Slf4j
public class DatabaseCleanupConfig {

    private final JdbcTemplate jdbcTemplate;

    @Bean
    @Transactional
    public CommandLineRunner cleanDatabase() {
        return args -> {
            log.warn(" ========================================");
            log.warn(" LIMPIANDO BASE DE DATOS AL INICIAR");
            log.warn(" ========================================");
            
            try {
                // Deshabilitar verificación de claves foráneas
                jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
                
                // Limpiar todas las tablas
                log.info("Limpiando tabla: vuelo_pedidos");
                jdbcTemplate.execute("TRUNCATE TABLE vuelo_pedidos");
                
                log.info("Limpiando tabla: rutas_solucion");
                jdbcTemplate.execute("TRUNCATE TABLE rutas_solucion");
                
                log.info("Limpiando tabla: pedidos");
                jdbcTemplate.execute("TRUNCATE TABLE pedidos");
                
                log.info("Limpiando tabla: planesdevuelo");
                jdbcTemplate.execute("TRUNCATE TABLE planesdevuelo");
                
                log.info("Limpiando tabla: aeropuertos");
                jdbcTemplate.execute("TRUNCATE TABLE aeropuertos");
                
                log.info("Limpiando tabla: clientes");
                jdbcTemplate.execute("TRUNCATE TABLE clientes");
                
                log.info("Limpiando tabla: pedidos_v2");
                jdbcTemplate.execute("TRUNCATE TABLE pedidos_v2");
                
                // Reactivar verificación de claves foráneas
                jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
                
                // Reiniciar secuencias AUTO_INCREMENT
                log.info("Reiniciando secuencias AUTO_INCREMENT");
                jdbcTemplate.execute("ALTER TABLE aeropuertos AUTO_INCREMENT = 1");
                jdbcTemplate.execute("ALTER TABLE planesdevuelo AUTO_INCREMENT = 1");
                jdbcTemplate.execute("ALTER TABLE pedidos AUTO_INCREMENT = 1");
                jdbcTemplate.execute("ALTER TABLE rutas_solucion AUTO_INCREMENT = 1");
                jdbcTemplate.execute("ALTER TABLE vuelo_pedidos AUTO_INCREMENT = 1");
                
                // Reiniciar sequences específicas
                log.info("Reiniciando sequences en tabla counters");
                jdbcTemplate.execute("UPDATE counters SET val = 1 WHERE name = 'pedido_seq'");
                jdbcTemplate.execute("UPDATE counters SET val = 1 WHERE name = 'plan_vuelo_seq'");
                jdbcTemplate.execute("UPDATE counters SET val = 1 WHERE name = 'cliente_seq'");
                
                // Verificar que todo está limpio
                Long aeropuertos = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM aeropuertos", Long.class);
                Long planesVuelo = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM planesdevuelo", Long.class);
                Long pedidos = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pedidos", Long.class);
                Long rutas = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM rutas_solucion", Long.class);
                Long vueloPedidos = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM vuelo_pedidos", Long.class);
                
                log.info("Estado de la base de datos:");
                log.info("   - Aeropuertos: {}", aeropuertos);
                log.info("   - Planes de Vuelo: {}", planesVuelo);
                log.info("   - Pedidos: {}", pedidos);
                log.info("   - Rutas Solución: {}", rutas);
                log.info("   - Vuelo-Pedido: {}", vueloPedidos);
                
                log.warn("Base de datos limpiada exitosamente");
                log.warn("========================================");
                
            } catch (Exception e) {
                log.error("Error al limpiar la base de datos: {}", e.getMessage(), e);
                throw e;
            }
        };
    }
}
