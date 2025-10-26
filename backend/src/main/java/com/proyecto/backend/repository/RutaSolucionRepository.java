package com.proyecto.backend.repository;

import com.proyecto.backend.model.RutaSolucion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para gestionar las rutas solución en la base de datos.
 */
@Repository
public interface RutaSolucionRepository extends JpaRepository<RutaSolucion, Long> {
    
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
}
