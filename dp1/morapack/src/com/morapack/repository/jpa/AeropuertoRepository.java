package com.morapack.repository.jpa;

import com.morapack.model.Aeropuerto;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para la entidad Aeropuerto
 * Cuando se integre Spring Boot, extenderá JpaRepository<Aeropuerto, String>
 */
public interface AeropuertoRepository {
    
    // Métodos básicos de CRUD
    Aeropuerto save(Aeropuerto aeropuerto);
    Optional<Aeropuerto> findById(String id);
    List<Aeropuerto> findAll();
    void deleteById(String id);
    boolean existsById(String id);
    long count();
    
    // Consultas por código
    Optional<Aeropuerto> findByCodigoICAO(String codigoICAO);
    Optional<Aeropuerto> findByCodigoCorto(String codigoCorto);
    
    // Consultas geográficas
    List<Aeropuerto> findByContinente(String continente);
    List<Aeropuerto> findByPais(String pais);
    List<Aeropuerto> findByCiudad(String ciudad);
    
    // Consultas por capacidad
    List<Aeropuerto> findByCapacidadAlmacenGreaterThanEqual(int capacidadMinima);
    
    // Consultas para sedes
    List<Aeropuerto> findByEsSedeTrue(); // Solo aeropuertos que son sedes
    List<Aeropuerto> findByEsSedeFalse(); // Solo aeropuertos destino
    
    // Consultas combinadas
    List<Aeropuerto> findByContinenteAndEsSedeTrue(String continente);
    List<Aeropuerto> findByPaisAndEsSedeTrue(String pais);
    
    // Búsquedas de texto
    List<Aeropuerto> findByCiudadContainingIgnoreCase(String ciudadParcial);
    List<Aeropuerto> findByPaisContainingIgnoreCase(String paisParcial);
}
