package com.proyecto.backend.repository;

import com.proyecto.backend.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // Buscar pedidos por estado
    List<Pedido> findByEstado(String estado);

    // Buscar pedidos por aeropuerto destino
    List<Pedido> findByAeropuertoDestinoId(String aeropuertoDestinoId);

    // Buscar pedidos por cliente
    List<Pedido> findByClienteId(String clienteId);

    // Buscar pedidos por día
    List<Pedido> findByDia(int dia);

    // Buscar pedidos por estado y aeropuerto destino
    List<Pedido> findByEstadoAndAeropuertoDestinoId(String estado, String aeropuertoDestinoId);

    // Contar pedidos por estado
    long countByEstado(String estado);

    // Contar pedidos por aeropuerto destino
    long countByAeropuertoDestinoId(String aeropuertoDestinoId);

    // Eliminar todos los pedidos con un solo DELETE nativo
    @Modifying
    @Query(value = "DELETE FROM pedidos", nativeQuery = true)
    void deleteAllNative();

}
