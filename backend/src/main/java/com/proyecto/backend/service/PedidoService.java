package com.proyecto.backend.service;

import com.proyecto.backend.model.Pedido;
import com.proyecto.backend.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PedidoService {

    private final PedidoRepository pedidoRepository;

    /**
     * Obtiene todos los pedidos
     */
    @Transactional(readOnly = true)
    public List<Pedido> obtenerTodos() {
        return pedidoRepository.findAll();
    }

    /**
     * Busca un pedido por ID
     */
    @Transactional(readOnly = true)
    public Pedido buscarPorId(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado: " + id));
    }

    /**
     * Busca pedidos por estado
     */
    @Transactional(readOnly = true)
    public List<Pedido> buscarPorEstado(String estado) {
        return pedidoRepository.findByEstado(estado);
    }

    /**
     * Busca pedidos por aeropuerto destino
     */
    @Transactional(readOnly = true)
    public List<Pedido> buscarPorAeropuertoDestino(String aeropuertoDestinoId) {
        return pedidoRepository.findByAeropuertoDestinoId(aeropuertoDestinoId);
    }

    /**
     * Busca pedidos por cliente
     */
    @Transactional(readOnly = true)
    public List<Pedido> buscarPorCliente(String clienteId) {
        return pedidoRepository.findByClienteId(clienteId);
    }

    /**
     * Busca pedidos por día
     */
    @Transactional(readOnly = true)
    public List<Pedido> buscarPorDia(int dia) {
        return pedidoRepository.findByDia(dia);
    }

    /**
     * Busca pedidos por estado y aeropuerto destino
     */
    @Transactional(readOnly = true)
    public List<Pedido> buscarPorEstadoYDestino(String estado, String aeropuertoDestinoId) {
        return pedidoRepository.findByEstadoAndAeropuertoDestinoId(estado, aeropuertoDestinoId);
    }

    /**
     * Obtiene estadísticas de pedidos
     */
    @Transactional(readOnly = true)
    public Map<String, Long> obtenerEstadisticas() {
        List<Pedido> pedidos = pedidoRepository.findAll();
        
        return Map.of(
            "total", (long) pedidos.size(),
            "pendientes", pedidoRepository.countByEstado("PENDIENTE"),
            "asignados", pedidoRepository.countByEstado("ASIGNADO"),
            "enRuta", pedidoRepository.countByEstado("EN_RUTA"),
            "entregados", pedidoRepository.countByEstado("ENTREGADO"),
            "cancelados", pedidoRepository.countByEstado("CANCELADO")
        );
    }

    /**
     * Crea un nuevo pedido
     */
    @Transactional
    public Pedido crear(Pedido pedido) {
        log.info("Creando pedido para cliente: {} con destino: {}", 
            pedido.getClienteId(), pedido.getAeropuertoDestinoId());
        return pedidoRepository.save(pedido);
    }

    /**
     * Actualiza un pedido existente
     */
    @Transactional
    public Pedido actualizar(Long id, Pedido pedido) {
        Pedido existente = buscarPorId(id);

        existente.setDia(pedido.getDia());
        existente.setHora(pedido.getHora());
        existente.setMinuto(pedido.getMinuto());
        existente.setAeropuertoDestinoId(pedido.getAeropuertoDestinoId());
        existente.setCantidadProductos(pedido.getCantidadProductos());
        existente.setClienteId(pedido.getClienteId());
        existente.setEstado(pedido.getEstado());

        log.info("Actualizando pedido ID: {}", id);
        return pedidoRepository.save(existente);
    }

    /**
     * Actualiza el estado de un pedido
     */
    @Transactional
    public Pedido actualizarEstado(Long id, String nuevoEstado) {
        Pedido pedido = buscarPorId(id);
        String estadoAnterior = pedido.getEstado();
        pedido.setEstado(nuevoEstado);
        
        log.info("Actualizando estado del pedido ID: {} de {} a {}", id, estadoAnterior, nuevoEstado);
        return pedidoRepository.save(pedido);
    }

    /**
     * Elimina un pedido
     */
    @Transactional
    public void eliminar(Long id) {
        Pedido pedido = buscarPorId(id);
        pedidoRepository.delete(pedido);
        log.info("Pedido eliminado: ID {}", id);
    }

    /**
     * Limpia todos los pedidos de la base de datos con DELETE nativo optimizado
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void limpiarPedidos() {
        long count = pedidoRepository.count();
        pedidoRepository.deleteAllNative();
        log.info("Se eliminaron {} pedidos de la base de datos (DELETE nativo)", count);
    }

    /**
     * Carga pedidos desde el archivo de texto
     * IMPORTANTE: Limpia la BD antes de cargar para evitar duplicados
     * Formato: dd-hh-mm-dest-###-IdClien
     * Ejemplo: 30-09-15-SEQM-145-0054321
     * @return Lista de pedidos cargados
     */
    @Transactional
    public List<Pedido> cargarDesdeArchivo() {
        // Limpiar la base de datos antes de cargar
        log.info("Limpiando pedidos existentes...");
        limpiarPedidos();
        
        List<Pedido> pedidosParaGuardar = new ArrayList<>();

        try {
            log.info("Iniciando lectura de archivo de pedidos...");
            ClassPathResource resource = new ClassPathResource("datos/Pedidos.txt");

            // PASO 1: Leer TODO el archivo primero
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), "UTF-8"))) {

                String linea;
                int lineaNumero = 0;

                while ((linea = reader.readLine()) != null) {
                    lineaNumero++;

                    // Saltar líneas vacías
                    if (linea.trim().isEmpty()) {
                        continue;
                    }

                    try {
                        Pedido pedido = parsearLineaPedido(linea);
                        if (pedido != null) {
                            pedidosParaGuardar.add(pedido);
                        }
                    } catch (Exception e) {
                        log.warn("Error parseando línea {}: {} - Error: {}", 
                            lineaNumero, linea, e.getMessage());
                    }
                }

                log.info("Lectura completada. {} pedidos parseados", pedidosParaGuardar.size());
            }

            // PASO 2: Guardar todos en lotes
            if (!pedidosParaGuardar.isEmpty()) {
                log.info("Guardando {} pedidos en la base de datos...", pedidosParaGuardar.size());
                
                int batchSize = 2000;
                for (int i = 0; i < pedidosParaGuardar.size(); i += batchSize) {
                    int end = Math.min(i + batchSize, pedidosParaGuardar.size());
                    List<Pedido> batch = pedidosParaGuardar.subList(i, end);
                    guardarBatch(batch);
                    log.info("Guardados {}/{} pedidos", end, pedidosParaGuardar.size());
                }

                log.info("✓ Total de pedidos guardados: {}", pedidosParaGuardar.size());
                return pedidosParaGuardar;
            } else {
                log.info("No hay pedidos para guardar");
                return new ArrayList<>();
            }

        } catch (IOException e) {
            log.error("Error leyendo archivo de pedidos: {}", e.getMessage());
            throw new RuntimeException("No se pudo cargar el archivo de pedidos", e);
        }
    }

    /**
     * Guarda un lote de pedidos en una transacción separada
     */
    @Transactional
    private void guardarBatch(List<Pedido> pedidos) {
        pedidoRepository.saveAll(pedidos);
    }

    /**
     * Parsea una línea del archivo y crea un objeto Pedido
     * Formato: dd-hh-mm-dest-###-IdClien
     * Ejemplo: 30-09-15-SEQM-145-0054321
     */
    private Pedido parsearLineaPedido(String linea) {
        if (linea == null || linea.trim().isEmpty()) {
            return null;
        }

        String[] partes = linea.trim().split("-");
        
        if (partes.length != 6) {
            log.warn("Formato de línea inválido (esperaba 6 partes): {}", linea);
            return null;
        }

        try {
            int dia = Integer.parseInt(partes[0].trim());
            int hora = Integer.parseInt(partes[1].trim());
            int minuto = Integer.parseInt(partes[2].trim());
            String aeropuertoDestino = partes[3].trim();
            int cantidadProductos = Integer.parseInt(partes[4].trim());
            String clienteId = partes[5].trim();

            return new Pedido(dia, hora, minuto, aeropuertoDestino, cantidadProductos, clienteId);

        } catch (NumberFormatException e) {
            log.warn("Error parseando números en línea: {} - Error: {}", linea, e.getMessage());
            return null;
        }
    }

}
