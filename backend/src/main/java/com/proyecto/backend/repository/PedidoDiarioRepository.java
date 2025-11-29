package com.proyecto.backend.repository;

import com.proyecto.backend.model.PedidoDiario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PedidoDiarioRepository extends JpaRepository<PedidoDiario, Long> {

    // NOTA: El campo 'estado' ya no existe en la entidad.
    // El estado se maneja en RAM mediante SessionStateManager.

    // Buscar pedidos por aeropuerto destino
    List<PedidoDiario> findByAeropuertoDestinoId(String aeropuertoDestinoId);

    // Buscar pedidos por cliente
    List<PedidoDiario> findByClienteId(String clienteId);

    // Buscar pedidos por día
    List<PedidoDiario> findByDia(int dia);

    // Contar pedidos por aeropuerto destino
    long countByAeropuertoDestinoId(String aeropuertoDestinoId);

    // Eliminar todos los pedidos con un solo DELETE nativo
    @Modifying
    @Query(value = "DELETE FROM pedidos_diario", nativeQuery = true)
    void deleteAllNative();

}
