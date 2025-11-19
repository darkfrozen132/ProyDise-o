package com.proyecto.backend.config;

import com.proyecto.backend.repository.PedidoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

/**
 * Configuración para inicializar datos al iniciar la aplicación
 */
@Configuration
@Slf4j
public class DataInitializer {

    /**
     * Resetea todos los pedidos a estado PENDIENTE al iniciar la aplicación
     * Esto permite hacer pruebas frescas sin modificar manualmente la BD
     */
    @Bean
    CommandLineRunner initDatabase(PedidoRepository pedidoRepository) {
        return args -> {
            resetearEstadosPedidos(pedidoRepository);
        };
    }

    @Transactional
    void resetearEstadosPedidos(PedidoRepository pedidoRepository) {
        try {
            log.info("🔄 Reseteando estados de pedidos a PENDIENTE...");
            
            int pedidosActualizados = pedidoRepository.findAll().stream()
                .filter(p -> "ASIGNADO".equals(p.getEstado()))
                .peek(p -> p.setEstado("PENDIENTE"))
                .map(pedidoRepository::save)
                .toList()
                .size();
            
            log.info("✅ Reseteados {} pedidos a PENDIENTE", pedidosActualizados);
            
            long totalPendientes = pedidoRepository.findAll().stream()
                .filter(p -> "PENDIENTE".equals(p.getEstado()))
                .count();
            
            log.info("📊 Total de pedidos PENDIENTE: {}", totalPendientes);
            
        } catch (Exception e) {
            log.error("❌ Error al resetear estados de pedidos", e);
        }
    }
}
