package com.proyecto.backend.repository;

import com.proyecto.backend.model.PlanDeVuelo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanDeVueloRepository extends JpaRepository<PlanDeVuelo, Long> {

    // Buscar planes de vuelo por aeropuerto de origen
    List<PlanDeVuelo> findByAeropuertoOrigen(String aeropuertoOrigen);

    // Buscar planes de vuelo por aeropuerto de destino
    List<PlanDeVuelo> findByAeropuertoDestino(String aeropuertoDestino);

    // Buscar planes de vuelo entre dos aeropuertos especificos
    List<PlanDeVuelo> findByAeropuertoOrigenAndAeropuertoDestino(String aeropuertoOrigen, String aeropuertoDestino);

    // Verificar si existe un plan de vuelo entre dos aeropuertos
    boolean existsByAeropuertoOrigenAndAeropuertoDestino(String aeropuertoOrigen, String aeropuertoDestino);

}
