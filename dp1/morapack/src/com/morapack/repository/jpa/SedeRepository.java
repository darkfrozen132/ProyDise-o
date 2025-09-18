package com.morapack.repository.jpa;

import com.morapack.model.Sede;
import com.morapack.model.TipoSede;
import com.morapack.model.EstadoSede;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para la entidad Sede
 * Cuando se integre Spring Boot, extenderá JpaRepository<Sede, String>
 */
public interface SedeRepository {
    
    // Métodos básicos de CRUD
    Sede save(Sede sede);
    Optional<Sede> findById(String id);
    List<Sede> findAll();
    void deleteById(String id);
    boolean existsById(String id);
    long count();
    
    // Consultas por aeropuerto
    List<Sede> findByAeropuertoId(String aeropuertoId);
    Optional<Sede> findByAeropuertoIdAndTipo(String aeropuertoId, TipoSede tipo);
    
    // Consultas por tipo
    List<Sede> findByTipo(TipoSede tipo);
    List<Sede> findByTipoAndEstado(TipoSede tipo, EstadoSede estado);
    
    // Consultas por estado
    List<Sede> findByEstado(EstadoSede estado);
    List<Sede> findByEstadoIn(List<EstadoSede> estados);
    
    // Consultas por capacidad
    List<Sede> findByCapacidadMaximaGreaterThanEqual(double capacidadMinima);
    List<Sede> findByCapacidadMaximaBetween(double capacidadMin, double capacidadMax);
    
    // Consultas para optimización
    List<Sede> findSedesActivas(); // Solo sedes activas
    List<Sede> findSedesDisponibles(); // Sedes activas con capacidad disponible
    List<Sede> findSedesConCapacidad(int pedidosAAsignar); // Sedes que pueden manejar X pedidos
    
    // Búsquedas de texto
    List<Sede> findByNombreContainingIgnoreCase(String nombreParcial);
    
    // Consultas combinadas
    List<Sede> findByEstadoAndCapacidadMaximaGreaterThan(EstadoSede estado, double capacidad);
    List<Sede> findByTipoAndCapacidadMaximaGreaterThanEqual(TipoSede tipo, double capacidad);
    
    // Estadísticas
    long countByEstado(EstadoSede estado);
    long countByTipo(TipoSede tipo);
    
    // Consultas específicas del negocio
    List<Sede> findSedesPrincipales(); // Solo sedes principales activas
    List<Sede> findDepositos(); // Solo depósitos activos
}
