package com.proyecto.backend.planificador.semanal.service;

import com.proyecto.backend.planificador.semanal.model.World;
import com.proyecto.backend.model.Aeropuerto;
import com.proyecto.backend.model.PlanDeVuelo;
import com.proyecto.backend.repository.AeropuertoRepository;
import com.proyecto.backend.repository.PlanDeVueloRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio que mantiene el World en memoria como cache
 * Carga los datos de aeropuertos y planes de vuelo al iniciar la aplicacion
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WorldCacheService {

    private final AeropuertoRepository aeropuertoRepository;
    private final PlanDeVueloRepository planDeVueloRepository;

    // World en memoria (cache)
    private volatile World world;

    // Ultima actualizacion del cache
    private volatile LocalDateTime ultimaActualizacion;

    /**
     * Inicializa el World al arrancar la aplicacion
     * Solo carga si hay datos en la base de datos
     */
    @PostConstruct
    public void inicializar() {
        log.info("Inicializando WorldCacheService...");
        
        // Verificar si hay datos antes de cargar
        long totalAeropuertos = aeropuertoRepository.count();
        long totalPlanes = planDeVueloRepository.count();
        
        if (totalAeropuertos == 0 || totalPlanes == 0) {
            log.warn("⚠️ Base de datos vacía (Aeropuertos: {}, Planes: {})", totalAeropuertos, totalPlanes);
            log.warn("⚠️ World NO cargado. Debes cargar datos primero:");
            log.warn("   1. POST /api/aeropuertos/cargar");
            log.warn("   2. POST /api/planesdevuelo/cargar");
            log.warn("   3. POST /api/world/recargar (opcional, se carga automáticamente)");
            
            // Crear World vacío con listas vacías
            this.world = new World(List.of(), List.of());
            this.ultimaActualizacion = LocalDateTime.now();
        } else {
            log.info("Datos encontrados. Cargando World...");
            cargarWorld();
        }
    }

    /**
     * Carga o recarga el World desde la base de datos
     */
    @Transactional(readOnly = true)
    public synchronized void cargarWorld() {
        try {
            log.info("Cargando aeropuertos desde la base de datos...");
            List<Aeropuerto> aeropuertos = aeropuertoRepository.findAll();
            log.info("Cargados {} aeropuertos", aeropuertos.size());

            log.info("Cargando planes de vuelo desde la base de datos...");
            List<PlanDeVuelo> planesVuelo = planDeVueloRepository.findAll();
            log.info("Cargados {} planes de vuelo", planesVuelo.size());

            // Crear nuevo World
            World nuevoWorld = new World(aeropuertos, planesVuelo);

            // Actualizar referencias atomicamente
            this.world = nuevoWorld;
            this.ultimaActualizacion = LocalDateTime.now();

            log.info("World creado exitosamente: {}", nuevoWorld.getEstadisticas());
            log.info("Hubs disponibles: {}", nuevoWorld.getHubs());

        } catch (Exception e) {
            log.error("Error al cargar World desde la base de datos", e);
            throw new RuntimeException("No se pudo inicializar el World", e);
        }
    }

    /**
     * Obtiene el World actual
     *
     * @return World en memoria
     * @throws IllegalStateException si el World no ha sido inicializado
     */
    public World getWorld() {
        if (world == null) {
            throw new IllegalStateException("World no ha sido inicializado");
        }
        return world;
    }

    /**
     * Verifica si el World esta inicializado
     *
     * @return true si el World esta listo
     */
    public boolean estaInicializado() {
        return world != null;
    }

    /**
     * Obtiene la fecha y hora de la ultima actualizacion del cache
     *
     * @return Fecha y hora de ultima actualizacion
     */
    public LocalDateTime getUltimaActualizacion() {
        return ultimaActualizacion;
    }

    /**
     * Recarga el World desde la base de datos
     * Util cuando se agregan nuevos aeropuertos o planes de vuelo
     */
    public void refrescar() {
        log.info("Refrescando World...");
        cargarWorld();
    }

    /**
     * Obtiene estadisticas del World actual
     *
     * @return String con estadisticas
     */
    public String getEstadisticas() {
        if (!estaInicializado()) {
            return "World no inicializado";
        }
        return String.format("%s | Ultima actualizacion: %s",
                world.getEstadisticas(),
                ultimaActualizacion);
    }
}
