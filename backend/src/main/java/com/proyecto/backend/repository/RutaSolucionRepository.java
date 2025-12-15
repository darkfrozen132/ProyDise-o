package com.proyecto.backend.repository;

import com.proyecto.backend.model.RutaSolucion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para gestionar las rutas solución en la base de datos.
 */
@Repository
public interface RutaSolucionRepository extends JpaRepository<RutaSolucion, Long> {
    
    /**
     * Busca una ruta por su ID único de vuelo
     * 
     * @param idVuelo ID del vuelo (ej: "UA123_D0_2025-01-15T00:00")
     * @return Optional con la ruta si existe
     */
    Optional<RutaSolucion> findByIdVuelo(String idVuelo);
    
    /**
     * Busca rutas por código de origen
     */
    List<RutaSolucion> findByOriginCode(String originCode);
    
    /**
     * Busca rutas por código de destino
     */
    List<RutaSolucion> findByDestinationCode(String destinationCode);
    
    /**
     * Busca rutas por origen y destino
     */
    List<RutaSolucion> findByOriginCodeAndDestinationCode(String originCode, String destinationCode);
    
    /**
     * Busca rutas que están actualmente en vuelo
     * 
     * @return Lista de rutas en progreso
     */
    List<RutaSolucion> findByEnVueloTrue();
    
    /**
     * Busca rutas que NO están en vuelo
     * 
     * @return Lista de rutas completadas o no iniciadas
     */
    List<RutaSolucion> findByEnVueloFalse();
}
