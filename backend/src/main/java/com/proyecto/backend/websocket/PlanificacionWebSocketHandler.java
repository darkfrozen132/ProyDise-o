package com.proyecto.backend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.backend.planificador.semanal.service.AlgoritmoGeneticoService;
import com.proyecto.backend.websocket.dto.PlanificacionWSRequest;
import com.proyecto.backend.websocket.dto.PlanificacionWSResponse;
import com.proyecto.backend.websocket.dto.ProgresoAGDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Handler para WebSocket de planificación en tiempo real
 * 
 * Maneja comunicación bidireccional con el cliente para:
 * - Iniciar planificación con parámetros
 * - Enviar progreso del algoritmo genético en tiempo real
 * - Pausar/reanudar/cancelar ejecución
 * - Enviar resultado final
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlanificacionWebSocketHandler extends TextWebSocketHandler {
    
    private final AlgoritmoGeneticoService algoritmoService;
    private final com.proyecto.backend.service.PedidoService pedidoService;
    private final ObjectMapper objectMapper;
    
    // Sesiones activas: {sessionId -> WebSocketSession}
    private final Map<String, WebSocketSession> sesiones = new ConcurrentHashMap<>();
    
    // Tareas en ejecución: {sessionId -> CompletableFuture}
    private final Map<String, CompletableFuture<Void>> tareasEnEjecucion = new ConcurrentHashMap<>();
    
    // Estado de planificación continua por sesión
    private final Map<String, EstadoPlanificacion> estadosPlanificacion = new ConcurrentHashMap<>();
    
    /**
     * Clase para mantener el estado de planificación continua
     */
    private static class EstadoPlanificacion {
        LocalDateTime tiempoActualSimulacion;  // Tiempo actual en la simulación (fecha + hora)
        int factorK;                           // Factor de tiempo (K=14)
        int contadorEjecuciones;               // Número de replanificaciones
        int saltoAlgoritmoMinutos = 5;         // Sa = 5 minutos (por defecto)
        
        EstadoPlanificacion(LocalDate fechaInicial, int factorK) {
            // Iniciar en la fecha a las 00:00 (medianoche, hora 0)
            this.tiempoActualSimulacion = LocalDateTime.of(fechaInicial, java.time.LocalTime.MIDNIGHT);
            this.factorK = factorK;
            this.contadorEjecuciones = 0;
        }
        
        /**
         * Avanza la simulación según el Salto de Consumo (Sc)
         * 
         * Sc = K × Sa
         * Ejemplo: K=14, Sa=5min → Sc=70 minutos por iteración
         */
        void avanzarTiempo(long duracionRealMs) {
            // Calcular Sc = K × Sa
            int saltoConsumoMinutos = factorK * saltoAlgoritmoMinutos;
            
            // Avanzar el tiempo simulado en Sc minutos (NO en K minutos)
            tiempoActualSimulacion = tiempoActualSimulacion.plusMinutes(saltoConsumoMinutos);
            contadorEjecuciones++;
            
            log.debug("⏱️ Tiempo avanzado: +{} minutos (Sc = K×Sa = {}×{})", 
                     saltoConsumoMinutos, factorK, saltoAlgoritmoMinutos);
        }
        
        LocalDate getFechaActual() {
            return tiempoActualSimulacion.toLocalDate();
        }
    }
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sesiones.put(session.getId(), session);
        log.debug("🔌 WebSocket conectado: {}", session.getId());
        
        enviarMensaje(session, PlanificacionWSResponse.builder()
                .tipo("conectado")
                .mensaje("Conexión WebSocket establecida")
                .datos(Map.of("sessionId", session.getId()))
                .build());
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            PlanificacionWSRequest request = objectMapper.readValue(
                    message.getPayload(), PlanificacionWSRequest.class);
            
            log.debug("📩 Mensaje recibido de {}: accion={}", session.getId(), request.getAccion());
            
            switch (request.getAccion()) {
                case "iniciar" -> iniciarPlanificacion(session, request);
                case "pausar" -> pausarPlanificacion(session);
                case "reanudar" -> reanudarPlanificacion(session);
                case "cancelar" -> cancelarPlanificacion(session);
                default -> enviarError(session, "Acción desconocida: " + request.getAccion());
            }
            
        } catch (Exception e) {
            log.error("Error procesando mensaje", e);
            enviarError(session, "Error procesando mensaje: " + e.getMessage());
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("🔌 WebSocket desconectado: {} - status: {}", session.getId(), status);
        
        // Cancelar tarea si existe
        CompletableFuture<Void> tarea = tareasEnEjecucion.remove(session.getId());
        if (tarea != null && !tarea.isDone()) {
            tarea.cancel(true);
            algoritmoService.cancelar(session.getId());
        }
        
        // Resetear todos los pedidos a PENDIENTE
        try {
            int pedidosReseteados = pedidoService.resetearTodosAPendiente();
            log.info("✅ {} pedidos reseteados a PENDIENTE al cerrar WebSocket", pedidosReseteados);
        } catch (Exception e) {
            log.error("❌ Error al resetear pedidos a PENDIENTE", e);
        }
        
        // Limpiar estado
        estadosPlanificacion.remove(session.getId());
        sesiones.remove(session.getId());
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("❌ Error en WebSocket {}", session.getId(), exception);
    }
    
    // ============ ACCIONES ============
    
    /**
     * Inicia la planificación en un thread asíncrono
     * VERSIÓN SIMPLIFICADA: Ejecuta UNA SOLA VEZ y devuelve todos los vuelos
     */
    private void iniciarPlanificacion(WebSocketSession session, PlanificacionWSRequest request) {
        // Validar parámetros
        if (request.getFecha() == null || request.getFactorK() == null) {
            enviarError(session, "Faltan parámetros: fecha y factorK son obligatorios");
            return;
        }
        
        // Verificar si ya hay una tarea en ejecución
        if (tareasEnEjecucion.containsKey(session.getId())) {
            log.warn("⚠️ Ya hay una planificación en curso para sesión: {}", session.getId());
            enviarError(session, "Ya hay una planificación en curso");
            return;
        }
        
        log.info("🚀 INICIANDO PLANIFICACIÓN SIMPLIFICADA");
        log.info("📅 Fecha: {}, Factor K: {}", request.getFecha(), request.getFactorK());
        
        // Validar que la fecha sea 2025 o posterior
        LocalDate fechaSolicitada = request.getFecha();
        final LocalDate fechaValidada;
        if (fechaSolicitada.getYear() < 2025) {
            log.warn("⚠️ Fecha anterior a 2025 detectada: {}. Ajustando a 2025-01-01", fechaSolicitada);
            fechaValidada = LocalDate.of(2025, 1, 1);
        } else {
            fechaValidada = fechaSolicitada;
        }
        
        // Obtener o crear estado de planificación
        EstadoPlanificacion estado = estadosPlanificacion.computeIfAbsent(
            session.getId(),
            k -> new EstadoPlanificacion(fechaValidada, request.getFactorK())
        );
        
        // Usar el tiempo actual de la simulación (avanza automáticamente)
        final LocalDateTime tiempoActualSimulacion = estado.tiempoActualSimulacion;
        
        log.info("🚀 Iteración #{}: tiempo={}, K={}", 
                estado.contadorEjecuciones + 1, tiempoActualSimulacion, request.getFactorK());
        
        // Callback de progreso - Solo guardamos la última solución
        final ProgresoAGDTO[] ultimoProgreso = {null};
        final long inicioEjecucion = System.currentTimeMillis();
        
        Consumer<ProgresoAGDTO> callbackProgreso = (progreso) -> {
            // Solo guardamos, no enviamos durante el proceso
            ultimoProgreso[0] = progreso;
        };
        
        // Ejecutar en thread asíncrono
        CompletableFuture<Void> tarea = CompletableFuture.runAsync(() -> {
            try {
                algoritmoService.planificarConProgresoWS(
                        session.getId(),
                        tiempoActualSimulacion,  // Pasar tiempo completo (fecha + hora)
                        request.getFactorK(),
                        request.getTamanioPoblacion(),
                        request.getMaxGeneraciones(),
                        request.getLimiteGeneracionesSinMejora(),
                        callbackProgreso
                );
                
                // Calcular duración real
                long duracionReal = System.currentTimeMillis() - inicioEjecucion;
                
                // Guardar tiempo ANTES de avanzar (para mostrar en respuesta)
                LocalDateTime tiempoEstaEjecucion = estado.tiempoActualSimulacion;
                
                // Avanzar el tiempo de simulación para la PRÓXIMA ejecución
                estado.avanzarTiempo(duracionReal);
                
                log.info("✅ Iteración #{} completada en {}ms. Próximo: {}", 
                         estado.contadorEjecuciones, duracionReal, estado.tiempoActualSimulacion);
                
                // Calcular fechas de inicio y fin de la solución
                String fechaInicioVuelos = null;
                String fechaFinalVuelos = null;
                
                if (ultimoProgreso[0] != null && ultimoProgreso[0].getSolucion() != null) {
                    Object solucion = ultimoProgreso[0].getSolucion();
                    // Extraer fechas de la solución si es un Map
                    if (solucion instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> solucionMap = (java.util.Map<String, Object>) solucion;
                        Object vuelosObj = solucionMap.get("vuelos");
                        
                        if (vuelosObj instanceof java.util.List) {
                            @SuppressWarnings("unchecked")
                            java.util.List<Object> vuelos = (java.util.List<Object>) vuelosObj;
                            
                            // Buscar primera y última fecha en todos los vuelos
                            for (Object vueloObj : vuelos) {
                                if (vueloObj instanceof java.util.Map) {
                                    @SuppressWarnings("unchecked")
                                    java.util.Map<String, Object> vuelo = (java.util.Map<String, Object>) vueloObj;
                                    String fechaInicio = (String) vuelo.get("fechaInicial");
                                    String fechaFinal = (String) vuelo.get("fechaFinal");
                                    
                                    if (fechaInicio != null) {
                                        if (fechaInicioVuelos == null || fechaInicio.compareTo(fechaInicioVuelos) < 0) {
                                            fechaInicioVuelos = fechaInicio;
                                        }
                                    }
                                    
                                    if (fechaFinal != null) {
                                        if (fechaFinalVuelos == null || fechaFinal.compareTo(fechaFinalVuelos) > 0) {
                                            fechaFinalVuelos = fechaFinal;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Enviar SOLO la solución final con información de tiempo
                if (ultimoProgreso[0] != null && ultimoProgreso[0].getSolucion() != null) {
                    // Calcular avance según Sc = K × Sa (70 minutos)
                    int saltoConsumoMinutos = estado.factorK * estado.saltoAlgoritmoMinutos;
                    long minutosAvance = saltoConsumoMinutos;
                    
                    // Contar pedidos en la solución
                    int numPedidosPlanificados = 0;
                    Object solucionObj = ultimoProgreso[0].getSolucion();
                    if (solucionObj instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> solMap = (java.util.Map<String, Object>) solucionObj;
                        Object vuelosObj = solMap.get("vuelos");
                        if (vuelosObj instanceof java.util.List) {
                            @SuppressWarnings("unchecked")
                            java.util.List<Object> vuelos = (java.util.List<Object>) vuelosObj;
                            for (Object vueloObj : vuelos) {
                                if (vueloObj instanceof java.util.Map) {
                                    @SuppressWarnings("unchecked")
                                    java.util.Map<String, Object> vuelo = (java.util.Map<String, Object>) vueloObj;
                                    Object pedidosObj = vuelo.get("pedidos");
                                    if (pedidosObj instanceof java.util.List) {
                                        numPedidosPlanificados += ((java.util.List<?>) pedidosObj).size();
                                    }
                                }
                            }
                        }
                    }
                    
                    log.debug("📦 Pedidos planificados en esta iteración: {}", numPedidosPlanificados);
                    
                    java.util.Map<String, Object> datosRespuesta = new java.util.HashMap<>();
                    datosRespuesta.put("ejecucionNumero", estado.contadorEjecuciones);
                    datosRespuesta.put("tiempoSimulacionActual", tiempoEstaEjecucion.toString());
                    datosRespuesta.put("proximoTiempo", estado.tiempoActualSimulacion.toString());
                    datosRespuesta.put("duracionRealMs", duracionReal);
                    datosRespuesta.put("avanceSimuladoMin", minutosAvance);
                    datosRespuesta.put("pedidosPlanificados", numPedidosPlanificados);
                    
                    // Agregar fechas de vuelos si se encontraron
                    if (fechaInicioVuelos != null) {
                        datosRespuesta.put("fechaInicioVuelos", fechaInicioVuelos);
                    }
                    if (fechaFinalVuelos != null) {
                        datosRespuesta.put("fechaFinalVuelos", fechaFinalVuelos);
                    }
                    
                    enviarMensaje(session, PlanificacionWSResponse.builder()
                            .tipo("completado")
                            .mensaje(String.format("Planificación #%d completada", estado.contadorEjecuciones))
                            .solucion(ultimoProgreso[0].getSolucion())
                            .datos(datosRespuesta)
                            .build());
                } else {
                    enviarMensaje(session, PlanificacionWSResponse.builder()
                            .tipo("completado")
                            .mensaje(String.format("Planificación #%d completada", estado.contadorEjecuciones))
                            .datos(Map.of(
                                "ejecucionNumero", estado.contadorEjecuciones,
                                "tiempoSimulacionActual", tiempoEstaEjecucion.toString(),
                                "proximoTiempo", estado.tiempoActualSimulacion.toString()
                            ))
                            .build());
                }
                
            } catch (Exception e) {
                log.error("Error en planificación", e);
                enviarError(session, "Error en planificación: " + e.getMessage());
            } finally {
                tareasEnEjecucion.remove(session.getId());
            }
        });
        
        tareasEnEjecucion.put(session.getId(), tarea);
    }
    
    /**
     * Pausa la planificación en curso
     */
    private void pausarPlanificacion(WebSocketSession session) {
        algoritmoService.pausar(session.getId());
        
        enviarMensaje(session, PlanificacionWSResponse.builder()
                .tipo("pausado")
                .mensaje("Planificación pausada")
                .build());
    }
    
    /**
     * Reanuda la planificación pausada
     */
    private void reanudarPlanificacion(WebSocketSession session) {
        algoritmoService.reanudar(session.getId());
        
        enviarMensaje(session, PlanificacionWSResponse.builder()
                .tipo("reanudado")
                .mensaje("Planificación reanudada")
                .build());
    }
    
    /**
     * Cancela la planificación en curso
     */
    private void cancelarPlanificacion(WebSocketSession session) {
        CompletableFuture<Void> tarea = tareasEnEjecucion.remove(session.getId());
        if (tarea != null && !tarea.isDone()) {
            tarea.cancel(true);
        }
        
        algoritmoService.cancelar(session.getId());
        
        enviarMensaje(session, PlanificacionWSResponse.builder()
                .tipo("cancelado")
                .mensaje("Planificación cancelada")
                .build());
    }
    
    // ============ UTILIDADES ============
    
    /**
     * Envía un error al cliente
     */
    private void enviarError(WebSocketSession session, String mensajeError) {
        PlanificacionWSResponse response = PlanificacionWSResponse.builder()
                .tipo("error")
                .mensaje(mensajeError)
                .build();
        
        enviarMensaje(session, response);
    }
    
    /**
     * Envía un mensaje JSON al cliente
     */
    private void enviarMensaje(WebSocketSession session, PlanificacionWSResponse response) {
        if (!session.isOpen()) {
            log.warn("⚠️ Sesión cerrada, no se puede enviar mensaje: {}", session.getId());
            return;
        }
        
        try {
            String json = objectMapper.writeValueAsString(response);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.error("❌ Error enviando mensaje WebSocket", e);
        }
    }
}
