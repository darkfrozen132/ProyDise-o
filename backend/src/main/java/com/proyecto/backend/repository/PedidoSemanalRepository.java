package com.proyecto.backend.repository;

import com.proyecto.backend.model.PedidoSemanal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PedidoSemanalRepository extends JpaRepository<PedidoSemanal, Long> {

    // Buscar pedidos por aeropuerto destino
    List<PedidoSemanal> findByAeropuertoDestinoId(String aeropuertoDestinoId);

    // Buscar pedidos por cliente
    List<PedidoSemanal> findByClienteId(String clienteId);

    // Buscar pedidos por día
    List<PedidoSemanal> findByDia(int dia);

    // Contar pedidos por aeropuerto destino
    long countByAeropuertoDestinoId(String aeropuertoDestinoId);

    // Eliminar todos los pedidos con un solo DELETE nativo
    @Modifying
    @Query(value = "DELETE FROM pedidos_semanal", nativeQuery = true)
    void deleteAllNative();

}
