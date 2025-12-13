package com.proyecto.backend.service;

import com.proyecto.backend.dto.PedidoDiarioRequest;
import com.proyecto.backend.model.PedidoDiario;
import com.proyecto.backend.repository.PedidoDiarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Servicio para gestionar pedidos diarios
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PedidoDiarioService {

    private final PedidoDiarioRepository pedidoDiarioRepository;

    /**
     * Crea un nuevo pedido diario
     * @param request DTO con los datos del pedido
     * @return El pedido creado con su ID asignado
     */
    @Transactional
    public PedidoDiario crearPedido(PedidoDiarioRequest request) {
        PedidoDiario pedido = new PedidoDiario(
            request.getAnio(),
            request.getMes(),
            request.getDia(),
            request.getHora(),
            request.getMinuto(),
            request.getAeropuertoDestinoId(),
            request.getCantidadProductos(),
            request.getClienteId()
        );

        PedidoDiario savedPedido = pedidoDiarioRepository.save(pedido);
        log.info("Pedido diario creado con ID: {}", savedPedido.getId());
        return savedPedido;
    }

    /**
     * Obtiene todos los pedidos diarios
     */
    public List<PedidoDiario> obtenerTodos() {
        return pedidoDiarioRepository.findAll();
    }

    /**
     * Obtiene un pedido por su ID
     */
    public Optional<PedidoDiario> obtenerPorId(Long id) {
        return pedidoDiarioRepository.findById(id);
    }

    /**
     * Busca pedidos por aeropuerto destino
     */
    public List<PedidoDiario> buscarPorAeropuertoDestino(String aeropuertoId) {
        return pedidoDiarioRepository.findByAeropuertoDestinoId(aeropuertoId);
    }

    /**
     * Busca pedidos por cliente
     */
    public List<PedidoDiario> buscarPorCliente(String clienteId) {
        return pedidoDiarioRepository.findByClienteId(clienteId);
    }

    /**
     * Busca pedidos por día
     */
    public List<PedidoDiario> buscarPorDia(int dia) {
        return pedidoDiarioRepository.findByDia(dia);
    }

    /**
     * Elimina un pedido por su ID
     */
    @Transactional
    public boolean eliminarPedido(Long id) {
        if (pedidoDiarioRepository.existsById(id)) {
            pedidoDiarioRepository.deleteById(id);
            log.info("Pedido diario eliminado con ID: {}", id);
            return true;
        }
        return false;
    }

    /**
     * Elimina todos los pedidos diarios
     */
    @Transactional
    public void eliminarTodos() {
        pedidoDiarioRepository.deleteAllNative();
        log.info("Todos los pedidos diarios han sido eliminados");
    }

    /**
     * Cuenta el total de pedidos diarios
     */
    public long contarPedidos() {
        return pedidoDiarioRepository.count();
    }
}
