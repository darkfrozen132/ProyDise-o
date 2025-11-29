package com.proyecto.backend.service;

import com.proyecto.backend.model.PedidoSemanal;
import com.proyecto.backend.repository.PedidoSemanalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.sql.PreparedStatement;

@Service
@RequiredArgsConstructor
@Slf4j
public class PedidoService {

    private final PedidoSemanalRepository pedidoSemanalRepository;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Obtiene todos los pedidos semanales
     */
    @Transactional(readOnly = true)
    public List<PedidoSemanal> obtenerTodos() {
        return pedidoSemanalRepository.findAll();
    }

    /**
     * Busca un pedido por ID
     */
    @Transactional(readOnly = true)
    public PedidoSemanal buscarPorId(Long id) {
        return pedidoSemanalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado: " + id));
    }

    /**
     * Busca pedidos por aeropuerto destino
     */
    @Transactional(readOnly = true)
    public List<PedidoSemanal> buscarPorAeropuertoDestino(String aeropuertoDestinoId) {
        return pedidoSemanalRepository.findByAeropuertoDestinoId(aeropuertoDestinoId);
    }

    /**
     * Busca pedidos por cliente
     */
    @Transactional(readOnly = true)
    public List<PedidoSemanal> buscarPorCliente(String clienteId) {
        return pedidoSemanalRepository.findByClienteId(clienteId);
    }

    /**
     * Busca pedidos por día
     */
    @Transactional(readOnly = true)
    public List<PedidoSemanal> buscarPorDia(int dia) {
        return pedidoSemanalRepository.findByDia(dia);
    }

    /**
     * Obtiene estadísticas de pedidos
     */
    @Transactional(readOnly = true)
    public Map<String, Long> obtenerEstadisticas() {
        List<PedidoSemanal> pedidos = pedidoSemanalRepository.findAll();
        
        return Map.of(
            "total", (long) pedidos.size()
        );
    }

    /**
     * Crea un nuevo pedido
     */
    @Transactional
    public PedidoSemanal crear(PedidoSemanal pedido) {
        log.info("Creando pedido para cliente: {} con destino: {}", 
            pedido.getClienteId(), pedido.getAeropuertoDestinoId());
        return pedidoSemanalRepository.save(pedido);
    }

    /**
     * Actualiza un pedido existente
     */
    @Transactional
    public PedidoSemanal actualizar(Long id, PedidoSemanal pedido) {
        PedidoSemanal existente = buscarPorId(id);

        existente.setAnio(pedido.getAnio());
        existente.setMes(pedido.getMes());
        existente.setDia(pedido.getDia());
        existente.setHora(pedido.getHora());
        existente.setMinuto(pedido.getMinuto());
        existente.setAeropuertoDestinoId(pedido.getAeropuertoDestinoId());
        existente.setCantidadProductos(pedido.getCantidadProductos());
        existente.setClienteId(pedido.getClienteId());

        log.info("Actualizando pedido ID: {}", id);
        return pedidoSemanalRepository.save(existente);
    }

    /**
     * Elimina un pedido
     */
    @Transactional
    public void eliminar(Long id) {
        PedidoSemanal pedido = buscarPorId(id);
        pedidoSemanalRepository.delete(pedido);
        log.info("Pedido eliminado: ID {}", id);
    }

    /**
     * Limpia todos los pedidos de la base de datos con DELETE nativo optimizado
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void limpiarPedidos() {
        long count = pedidoSemanalRepository.count();
        pedidoSemanalRepository.deleteAllNative();
        log.info("Se eliminaron {} pedidos semanales de la base de datos (DELETE nativo)", count);
    }

    /**
     * Carga pedidos desde el archivo Pedidos.txt a la tabla pedidos_semanal
     * OPTIMIZADO: Lectura + batch insert grande
     * IMPORTANTE: Limpia la BD antes de cargar para evitar duplicados
     * Formato: id_pedido-aaaammdd-hh-mm-dest-###-IdClien
     * Ejemplo: 000000001-20250102-00-54-LOWW-002-0000068
     * @return Lista de pedidos cargados
     */
    @Transactional
    public List<PedidoSemanal> cargarDesdeArchivo() {
        // Limpiar la base de datos antes de cargar
        log.info("Limpiando pedidos semanales existentes...");
        limpiarPedidos();

        try {
            log.info("📦 Cargando pedidos desde Pedidos.txt a tabla pedidos_semanal...");
            
            ClassPathResource resource = new ClassPathResource("datos/Pedidos.txt");
            List<PedidoSemanal> pedidos = new ArrayList<>();
            
            long inicioLectura = System.currentTimeMillis();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream()))) {
                
                String linea;
                int lineaNumero = 0;
                
                while ((linea = reader.readLine()) != null) {
                    lineaNumero++;
                    
                    if (linea.trim().isEmpty()) {
                        continue;
                    }
                    
                    try {
                        // Formato: ID-YYYYMMDD-HH-MM-AEROPUERTO-CANTIDAD-CLIENTE
                        // Ejemplo: 000000001-20250102-01-02-EDDI-002-0029360
                        String[] partes = linea.split("-");
                        
                        if (partes.length != 7) {
                            log.warn("Línea {} tiene formato incorrecto (esperado 7 partes): {}", lineaNumero, linea);
                            continue;
                        }
                        
                        // Parsear fecha YYYYMMDD
                        String fecha = partes[1];
                        int anio = Integer.parseInt(fecha.substring(0, 4));
                        int mes = Integer.parseInt(fecha.substring(4, 6));
                        int dia = Integer.parseInt(fecha.substring(6, 8));
                        
                        PedidoSemanal pedido = new PedidoSemanal();
                        pedido.setAnio(anio);
                        pedido.setMes(mes);
                        pedido.setDia(dia);
                        pedido.setHora(Integer.parseInt(partes[2]));
                        pedido.setMinuto(Integer.parseInt(partes[3]));
                        pedido.setAeropuertoDestinoId(partes[4]);
                        pedido.setCantidadProductos(Integer.parseInt(partes[5]));
                        pedido.setClienteId(partes[6]);
                        
                        pedidos.add(pedido);
                        
                    } catch (Exception e) {
                        log.warn("Error parseando línea {}: {} - {}", lineaNumero, linea, e.getMessage());
                    }
                }
            }
            
            long finLectura = System.currentTimeMillis();
            log.info("✓ Lectura completada en {} ms: {} pedidos", (finLectura - inicioLectura), pedidos.size());
            
            // Guardar con JDBC batch insert
            if (!pedidos.isEmpty()) {
                log.info("💾 Guardando {} pedidos semanales en la base de datos...", pedidos.size());
                
                long inicioGuardado = System.currentTimeMillis();
                guardarConJdbcBatch(pedidos);
                long finGuardado = System.currentTimeMillis();
                
                long tiempoTotal = finGuardado - inicioLectura;
                log.info("✅ COMPLETADO: {} pedidos semanales cargados en {} ms total", pedidos.size(), tiempoTotal);
                log.info("   📊 Lectura: {} ms | Inserción BD: {} ms", 
                        (finLectura - inicioLectura), (finGuardado - inicioGuardado));
            }
            
            return pedidos;
            
        } catch (IOException e) {
            log.error("Error leyendo archivo Pedidos.txt: {}", e.getMessage());
            throw new RuntimeException("No se pudo cargar el archivo Pedidos.txt", e);
        }
    }

    /**
     * Guarda un lote de pedidos utilizando JDBC batch OPTIMIZADO con progress tracking
     * La BD genera IDs automáticamente (auto_increment) - NO se incluye ID en el INSERT
     * Muestra progreso cada 50,000 pedidos insertados
     */
    @Transactional
    private void guardarConJdbcBatch(List<PedidoSemanal> pedidos) {
        String sql = "INSERT INTO pedidos_semanal (anio, mes, dia, hora, minuto, aeropuerto_destino_id, cantidad_productos, cliente_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        int batchSize = 1000;
        int totalPedidos = pedidos.size();
        int procesados = 0;
        int progressInterval = 50_000; // Mostrar progreso cada 50k pedidos
        long inicioBatch = System.currentTimeMillis();
        
        // Dividir en chunks y procesar con progress tracking
        for (int i = 0; i < totalPedidos; i += batchSize) {
            int end = Math.min(i + batchSize, totalPedidos);
            List<PedidoSemanal> chunk = pedidos.subList(i, end);
            
            // Ejecutar batch insert
            jdbcTemplate.batchUpdate(sql, chunk, batchSize, (PreparedStatement ps, PedidoSemanal pedido) -> {
                ps.setInt(1, pedido.getAnio());
                ps.setInt(2, pedido.getMes());
                ps.setInt(3, pedido.getDia());
                ps.setInt(4, pedido.getHora());
                ps.setInt(5, pedido.getMinuto());
                ps.setString(6, pedido.getAeropuertoDestinoId());
                ps.setInt(7, pedido.getCantidadProductos());
                ps.setString(8, pedido.getClienteId());
            });
            
            procesados = end;
            
            // Mostrar progreso cada X pedidos
            if (procesados % progressInterval == 0 || procesados == totalPedidos) {
                long tiempoTranscurrido = System.currentTimeMillis() - inicioBatch;
                double porcentaje = (procesados * 100.0) / totalPedidos;
                long velocidad = (procesados * 1000L) / Math.max(1, tiempoTranscurrido);
                long tiempoRestanteMs = ((totalPedidos - procesados) * 1000L) / Math.max(1, velocidad);
                
                log.info("   💾 Insertados: {}/{} pedidos ({:.1f}%) - {} pedidos/seg - ETA: {} seg", 
                    procesados, totalPedidos, porcentaje, velocidad, (tiempoRestanteMs / 1000));
            }
        }
    }

    /**
     * Parsea una línea del archivo y crea un objeto PedidoSemanal
     * El ID del archivo se IGNORA porque algunos se repiten - la BD genera su propio ID
     * Formato: id_pedido-aaaammdd-hh-mm-dest-###-IdClien
     * Ejemplo: 000000001-20250102-00-54-LOWW-002-0000068
     */
    private PedidoSemanal parsearLineaPedido(String linea) {
        if (linea == null || linea.trim().isEmpty()) {
            return null;
        }

        String[] partes = linea.trim().split("-");
        
        if (partes.length != 7) {
            log.warn("Formato de línea inválido (esperaba 7 partes): {}", linea);
            return null;
        }

        try {
            // Formato: id_pedido-aaaammdd-hh-mm-dest-###-IdClien
            String fechaStr = partes[1].trim();  // aaaammdd
            
            // Extraer año, mes, día de la fecha
            int anio = Integer.parseInt(fechaStr.substring(0, 4));   // aaaa
            int mes = Integer.parseInt(fechaStr.substring(4, 6));    // mm
            int dia = Integer.parseInt(fechaStr.substring(6, 8));    // dd
            
            int hora = Integer.parseInt(partes[2].trim());
            int minuto = Integer.parseInt(partes[3].trim());
            String aeropuertoDestino = partes[4].trim();
            int cantidadProductos = Integer.parseInt(partes[5].trim());
            String clienteId = partes[6].trim();

            // Crear pedido SIN ID - la BD generará uno automáticamente
            return new PedidoSemanal(anio, mes, dia, hora, minuto, aeropuertoDestino, cantidadProductos, clienteId);

        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            log.warn("Error parseando línea: {} - Error: {}", linea, e.getMessage());
            return null;
        }
    }

}
