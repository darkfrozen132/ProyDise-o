package com.proyecto.backend.repository;

import com.proyecto.backend.model.Aeropuerto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AeropuertoRepository extends JpaRepository<Aeropuerto, String> {

    // Buscar por codigo ICAO
    Optional<Aeropuerto> findByCodigoICAO(String codigoICAO);

    // Buscar por ciudad
    List<Aeropuerto> findByCiudadContainingIgnoreCase(String ciudad);

    // Buscar por pais
    List<Aeropuerto> findByPaisContainingIgnoreCase(String pais);

    // Buscar por continente
    List<Aeropuerto> findByContinenteIgnoreCase(String continente);

    // Verificar si existe por codigo ICAO
    boolean existsByCodigoICAO(String codigoICAO);

    // Obtener todos los códigos ICAO existentes (para verificación masiva)
    @Query("SELECT a.codigoICAO FROM Aeropuerto a")
    List<String> findAllCodigosICAO();

    // Eliminar todos los aeropuertos con un solo DELETE nativo
    @Modifying
    @Query(value = "DELETE FROM aeropuertos", nativeQuery = true)
    void deleteAllNative();

}

