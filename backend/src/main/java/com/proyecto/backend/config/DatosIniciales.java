package com.proyecto.backend.config;

import com.proyecto.backend.model.RutaSolucion;
import com.proyecto.backend.model.VueloPedido;
import com.proyecto.backend.repository.RutaSolucionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * Configuración para inicializar datos hardcodeados en la BD.
 * Crea un vuelo de demostración de Lima a Egipto.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DatosIniciales {

    @Bean
    CommandLineRunner initRutaSolucion(RutaSolucionRepository rutaSolucionRepository) {
        return args -> {
            // Verificar si ya existen datos
            if (rutaSolucionRepository.count() > 0) {
                log.info("✅ Ya existen rutas en la BD, omitiendo inicialización");
                return;
            }

            log.info("🚀 Inicializando datos hardcodeados: Vuelo Lima -> Berlín");

            // Crear ruta solución: Lima (SPIM) -> Berlín (EDDI)
            RutaSolucion rutaLimaBerlin = new RutaSolucion();
            rutaLimaBerlin.setOriginCode("SPIM");
            rutaLimaBerlin.setDestinationCode("EDDI");
            
            // ⭐ Horarios de vuelo sincronizados con la simulación
            // Salida: 01:00 AM, Llegada: 10:00 AM (9 horas = 540 minutos)
            // Con TIME_SCALE=60 minutos/segundo → Durará 540/60 = 9 segundos reales
            LocalDateTime salida = LocalDateTime.of(2025, 1, 1, 1, 0);  // 1:00 AM
            LocalDateTime llegada = LocalDateTime.of(2025, 1, 1, 10, 0); // 10:00 AM
            
            rutaLimaBerlin.setSalida(salida);
            rutaLimaBerlin.setLlegada(llegada);
            
            // Datos del vuelo
            rutaLimaBerlin.setCapacidad(500);
            rutaLimaBerlin.setAltitud(35000);
            rutaLimaBerlin.setSpeed(900);
            rutaLimaBerlin.setRegionOrigen("America");
            rutaLimaBerlin.setRegionDestino("Europa");
            rutaLimaBerlin.setTotalPaquetes(350);
            
            // Coordenadas de Lima (Origen) - Jorge Chávez
            rutaLimaBerlin.setOrigenLatitud(-12.0219);
            rutaLimaBerlin.setOrigenLongitud(-77.1143);
            
            // Coordenadas de Berlín (Destino) - Tempelhof Airport (EDDI)
            rutaLimaBerlin.setDestinoLatitud(52.4731);
            rutaLimaBerlin.setDestinoLongitud(13.4040);
            
            // Agregar algunos pedidos de ejemplo
            VueloPedido pedido1 = new VueloPedido("PED-LIMA-001", 150);
            VueloPedido pedido2 = new VueloPedido("PED-LIMA-002", 100);
            VueloPedido pedido3 = new VueloPedido("PED-LIMA-003", 100);
            
            rutaLimaBerlin.agregarVuelo(pedido1);
            rutaLimaBerlin.agregarVuelo(pedido2);
            rutaLimaBerlin.agregarVuelo(pedido3);
            
            // Guardar en BD
            rutaSolucionRepository.save(rutaLimaBerlin);
            
            log.info("✅ Ruta Lima -> Berlín creada exitosamente");
            log.info("⏰ Horarios: Salida={}, Llegada={}, Duración={} minutos ({} horas)", 
                salida, llegada, 
                java.time.Duration.between(salida, llegada).toMinutes(),
                java.time.Duration.between(salida, llegada).toMinutes() / 60.0);
            log.info("📍 Origen: Lima ({}, {})", 
                rutaLimaBerlin.getOrigenLatitud(), 
                rutaLimaBerlin.getOrigenLongitud());
            log.info("📍 Destino: Berlín ({}, {})", 
                rutaLimaBerlin.getDestinoLatitud(), 
                rutaLimaBerlin.getDestinoLongitud());
            log.info("📦 Total paquetes: {}", rutaLimaBerlin.getTotalPaquetes());
        };
    }
}
