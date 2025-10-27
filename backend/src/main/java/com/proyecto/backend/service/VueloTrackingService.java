package com.proyecto.backend.service;

import com.proyecto.backend.model.RutaSolucion;
import com.proyecto.backend.repository.RutaSolucionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Servicio para gestionar el streaming de coordenadas de vuelos en tiempo real.
 * Sincronizado con SimulacionOrchestrator mediante TIME_SCALE.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VueloTrackingService {

    private final RutaSolucionRepository rutaSolucionRepository;
    private final TransactionTemplate transactionTemplate;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    
    private volatile boolean streaming = false;
    private Thread streamingThread;
    
    // ⭐ PARÁMETROS DE SINCRONIZACIÓN (deben coincidir con SimulacionOrchestrator)
    private static final double TIME_SCALE = 60.0; // minutos simulados por segundo real
    private static final long INTERVALO_TICK_MS = 1000; // cada 1 segundo

    /**
     * Registra un nuevo cliente SSE
     */
    public SseEmitter registrarCliente() {
        SseEmitter emitter = new SseEmitter(0L); // Sin timeout
        
        emitter.onCompletion(() -> {
            log.info("🔌 Cliente SSE desconectado");
            emitters.remove(emitter);
        });
        
        emitter.onTimeout(() -> {
            log.warn("⏱️ Timeout de cliente SSE");
            emitters.remove(emitter);
        });
        
        emitter.onError((ex) -> {
            log.error("❌ Error en cliente SSE: {}", ex.getMessage());
            emitters.remove(emitter);
        });
        
        emitters.add(emitter);
        log.info("✅ Cliente SSE registrado. Total clientes: {}", emitters.size());
        
        return emitter;
    }

    /**
     * Inicia el streaming de coordenadas
     */
    public void iniciarStreaming() {
        if (streaming) {
            log.warn("⚠️ El streaming ya está activo");
            return;
        }
        
        streaming = true;
        streamingThread = new Thread(() -> {
            try {
                streamearCoordenadas();
            } catch (Exception e) {
                log.error("❌ Error en thread de streaming", e);
            }
        });
        streamingThread.start();
        log.info("🚀 Streaming de coordenadas iniciado");
    }

    /**
     * Detiene el streaming de coordenadas
     */
    public void detenerStreaming() {
        streaming = false;
        if (streamingThread != null) {
            streamingThread.interrupt();
        }
        log.info("🛑 Streaming de coordenadas detenido");
    }

    /**
     * Simula el movimiento del vuelo sincronizado con el tiempo simulado
     */
    private void streamearCoordenadas() {
        try {
            // Obtener la ruta Lima -> Berlín de la BD
            List<RutaSolucion> rutas = rutaSolucionRepository.findByOriginCodeAndDestinationCode("SPIM", "EDDI");
            
            if (rutas.isEmpty()) {
                log.error("❌ No se encontró la ruta Lima -> Berlín en la BD");
                return;
            }
            
            RutaSolucion ruta = rutas.get(0);
            Long rutaId = ruta.getId();
            
            log.info("🛫 Ruta encontrada: ID={}, Origen={}, Destino={}", 
                rutaId, ruta.getOriginCode(), ruta.getDestinationCode());
            
            // ⭐ CALCULAR DURACIÓN DEL VUELO EN MINUTOS
            LocalDateTime horaSalida = ruta.getSalida();
            LocalDateTime horaLlegada = ruta.getLlegada();
            
            if (horaSalida == null || horaLlegada == null) {
                log.error("❌ La ruta no tiene horarios de salida/llegada definidos");
                return;
            }
            
            long duracionVueloMinutos = Duration.between(horaSalida, horaLlegada).toMinutes();
            
            log.info("⏰ Vuelo: Salida={}, Llegada={}, Duración={} minutos ({} horas)", 
                horaSalida, horaLlegada, duracionVueloMinutos, duracionVueloMinutos / 60.0);
            
            // ⭐ CALCULAR CUÁNTOS TICKS (SEGUNDOS REALES) DURARÁ EL VUELO
            // Fórmula: duracionVueloSegundosReales = duracionVueloMinutos / TIME_SCALE
            // Ejemplo: Si el vuelo dura 540 minutos (9 horas) y TIME_SCALE = 60
            //          entonces durará 540 / 60 = 9 segundos reales
            double duracionVueloSegundosReales = duracionVueloMinutos / TIME_SCALE;
            int totalPasos = (int) Math.ceil(duracionVueloSegundosReales);
            
            log.info("🎬 El vuelo durará {} segundos reales ({} pasos)", 
                String.format("%.1f", duracionVueloSegundosReales), totalPasos);
            log.info("📐 TIME_SCALE={} minutos/segundo → Cada segundo real = {} minutos simulados = {} horas simuladas", 
                TIME_SCALE, TIME_SCALE, TIME_SCALE / 60.0);
            
            // Marcar vuelo como en progreso
            actualizarRutaEnBD(rutaId, null, null, null, true);
            
            double latInicio = ruta.getOrigenLatitud();
            double lonInicio = ruta.getOrigenLongitud();
            double latFin = ruta.getDestinoLatitud();
            double lonFin = ruta.getDestinoLongitud();
            
            log.info("📍 Iniciando vuelo desde ({}, {}) hasta ({}, {})", 
                latInicio, lonInicio, latFin, lonFin);
            
            // ⭐ INTERPOLAR EN FUNCIÓN DE LA DURACIÓN REAL DEL VUELO
            for (int i = 0; i <= totalPasos && streaming; i++) {
                // Progreso: de 0.0 (salida) a 1.0 (llegada)
                double progreso = totalPasos > 0 ? (double) i / totalPasos : 1.0;
                double latActual = latInicio + (latFin - latInicio) * progreso;
                double lonActual = lonInicio + (lonFin - lonInicio) * progreso;
                
                // Actualizar coordenadas en la BD
                actualizarRutaEnBD(rutaId, latActual, lonActual, progreso * 100, true);
                
                // Calcular hora simulada actual del vuelo
                LocalDateTime horaSimuladaVuelo = horaSalida.plusMinutes((long)(duracionVueloMinutos * progreso));
                
                log.debug("📍 Paso {}/{}: lat={}, lng={}, progreso={}%, horaSimulada={}", 
                    i, totalPasos, 
                    String.format("%.4f", latActual), 
                    String.format("%.4f", lonActual), 
                    String.format("%.1f", progreso * 100),
                    horaSimuladaVuelo);
                
                // Crear evento con las coordenadas actuales
                Map<String, Object> evento = new java.util.HashMap<>();
                evento.put("id", rutaId);
                evento.put("originCode", ruta.getOriginCode());
                evento.put("destinationCode", ruta.getDestinationCode());
                evento.put("currentLatitude", latActual);
                evento.put("currentLongitude", lonActual);
                evento.put("progress", progreso * 100);
                evento.put("speed", ruta.getSpeed());
                evento.put("altitude", ruta.getAltitud());
                evento.put("totalPackages", ruta.getTotalPaquetes());
                evento.put("horaSimuladaVuelo", horaSimuladaVuelo.toString());
                evento.put("horaSalida", horaSalida.toString());
                evento.put("horaLlegada", horaLlegada.toString());
                
                // Enviar a todos los clientes conectados
                enviarEventoATodos(evento);
                
                // ⭐ Esperar INTERVALO_TICK_MS (debe coincidir con el tick de simulación)
                Thread.sleep(INTERVALO_TICK_MS);
            }
            
            // ⭐ Marcar vuelo como completado
            actualizarRutaEnBD(rutaId, latFin, lonFin, 100.0, false);
            
            log.info("✅ Vuelo completado - Llegada a {} en hora simulada {}", 
                ruta.getDestinationCode(), horaLlegada);
            
            // ⭐ Enviar evento de vuelo completado
            Map<String, Object> eventoFinal = new java.util.HashMap<>();
            eventoFinal.put("id", ruta.getId());
            eventoFinal.put("originCode", ruta.getOriginCode());
            eventoFinal.put("destinationCode", ruta.getDestinationCode());
            eventoFinal.put("currentLatitude", latFin);
            eventoFinal.put("currentLongitude", lonFin);
            eventoFinal.put("progress", 100.0);
            eventoFinal.put("speed", ruta.getSpeed());
            eventoFinal.put("altitude", ruta.getAltitud());
            eventoFinal.put("totalPackages", ruta.getTotalPaquetes());
            eventoFinal.put("completed", true);  // ⭐ Indicador de completado
            eventoFinal.put("message", "Vuelo completado - Llegada a " + ruta.getDestinationCode());
            
            enviarEventoATodos(eventoFinal);
            
            // ⭐ Esperar 2 segundos para que el cliente reciba el evento final
            Thread.sleep(2000);
            
            // ⭐ Cerrar todas las conexiones SSE
            cerrarTodasLasConexiones();
            
            log.info("🔌 Todas las conexiones SSE cerradas - Vuelo finalizado");
            
        } catch (InterruptedException e) {
            log.info("🛑 Streaming interrumpido");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("❌ Error en streaming: {}", e.getMessage(), e);
        } finally {
            streaming = false;
        }
    }

    /**
     * Envía un evento a todos los clientes conectados
     */
    private void enviarEventoATodos(Map<String, Object> evento) {
        List<SseEmitter> emittersAEliminar = new CopyOnWriteArrayList<>();
        
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                    .name("vuelo-update")
                    .data(evento));
            } catch (IOException e) {
                log.error("❌ Error al enviar evento a cliente: {}", e.getMessage());
                emittersAEliminar.add(emitter);
            }
        }
        
        // Eliminar emitters con error
        emitters.removeAll(emittersAEliminar);
    }
    
    /**
     * ⭐ Método transaccional para actualizar una ruta en la BD
     * Se ejecuta en una transacción separada para asegurar persistencia inmediata
     */
    public void actualizarRutaEnBD(Long rutaId, Double lat, Double lng, Double progreso, Boolean enVuelo) {
        transactionTemplate.execute(status -> {
            RutaSolucion ruta = rutaSolucionRepository.findById(rutaId)
                .orElseThrow(() -> new RuntimeException("Ruta no encontrada: " + rutaId));
            
            if (lat != null) {
                ruta.setCurrentLatitud(lat);
            }
            if (lng != null) {
                ruta.setCurrentLongitud(lng);
            }
            if (progreso != null) {
                ruta.setProgreso(progreso);
            }
            if (enVuelo != null) {
                ruta.setEnVuelo(enVuelo);
            }
            
            rutaSolucionRepository.save(ruta);
            entityManager.flush(); // Forzar persistencia inmediata
            
            log.debug("💾 BD actualizada - Ruta ID={}: lat={}, lng={}, progreso={}%, enVuelo={}", 
                rutaId, lat, lng, progreso, enVuelo);
            
            return null;
        });
    }
    
    /**
     * ⭐ Cierra todas las conexiones SSE activas
     */
    private void cerrarTodasLasConexiones() {
        log.info("🔌 Cerrando {} conexiones SSE...", emitters.size());
        
        for (SseEmitter emitter : emitters) {
            try {
                emitter.complete();  // ⭐ Cierra la conexión limpiamente
            } catch (Exception e) {
                log.warn("⚠️ Error al cerrar emitter: {}", e.getMessage());
            }
        }
        
        emitters.clear();
        log.info("✅ Todas las conexiones SSE cerradas");
    }

    /**
     * Obtiene el estado actual del streaming
     */
    public boolean isStreaming() {
        return streaming;
    }

    /**
     * Obtiene el número de clientes conectados
     */
    public int getClientesConectados() {
        return emitters.size();
    }

    /**
     * Obtiene la posición actual de un vuelo desde la BD
     */
    public Optional<Map<String, Object>> obtenerPosicionActual(Long rutaId) {
        return rutaSolucionRepository.findById(rutaId)
            .map(ruta -> {
                Map<String, Object> data = new java.util.HashMap<>();
                data.put("id", ruta.getId());
                data.put("originCode", ruta.getOriginCode());
                data.put("destinationCode", ruta.getDestinationCode());
                data.put("currentLatitude", ruta.getCurrentLatitud() != null ? ruta.getCurrentLatitud() : ruta.getOrigenLatitud());
                data.put("currentLongitude", ruta.getCurrentLongitud() != null ? ruta.getCurrentLongitud() : ruta.getOrigenLongitud());
                data.put("progress", ruta.getProgreso() != null ? ruta.getProgreso() : 0.0);
                data.put("enVuelo", ruta.getEnVuelo() != null ? ruta.getEnVuelo() : false);
                data.put("speed", ruta.getSpeed());
                data.put("altitude", ruta.getAltitud());
                data.put("totalPackages", ruta.getTotalPaquetes());
                return data;
            });
    }

    /**
     * Obtiene todos los vuelos actualmente en progreso
     */
    public List<Map<String, Object>> obtenerVuelosActivos() {
        return rutaSolucionRepository.findAll().stream()
            .filter(ruta -> Boolean.TRUE.equals(ruta.getEnVuelo()))
            .map(ruta -> {
                Map<String, Object> data = new java.util.HashMap<>();
                data.put("id", ruta.getId());
                data.put("originCode", ruta.getOriginCode());
                data.put("destinationCode", ruta.getDestinationCode());
                data.put("currentLatitude", ruta.getCurrentLatitud());
                data.put("currentLongitude", ruta.getCurrentLongitud());
                data.put("progress", ruta.getProgreso());
                data.put("speed", ruta.getSpeed());
                data.put("altitude", ruta.getAltitud());
                data.put("totalPackages", ruta.getTotalPaquetes());
                return data;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * ⭐ Obtiene TODAS las rutas de solución (activas e inactivas)
     * Para mostrar en el simulador
     */
    public List<Map<String, Object>> obtenerTodasLasRutas() {
        List<RutaSolucion> todasLasRutas = rutaSolucionRepository.findAll();
        
        log.debug("📊 Obteniendo {} rutas de la BD", todasLasRutas.size());
        
        return todasLasRutas.stream()
            .map(ruta -> {
                Map<String, Object> data = new java.util.HashMap<>();
                data.put("id", ruta.getId());
                data.put("originCode", ruta.getOriginCode());
                data.put("destinationCode", ruta.getDestinationCode());
                
                // Coordenadas de origen
                data.put("origenLatitud", ruta.getOrigenLatitud());
                data.put("origenLongitud", ruta.getOrigenLongitud());
                
                // Coordenadas de destino
                data.put("destinoLatitud", ruta.getDestinoLatitud());
                data.put("destinoLongitud", ruta.getDestinoLongitud());
                
                // Coordenadas actuales (si está en vuelo)
                Double currentLat = ruta.getCurrentLatitud() != null ? ruta.getCurrentLatitud() : ruta.getOrigenLatitud();
                Double currentLng = ruta.getCurrentLongitud() != null ? ruta.getCurrentLongitud() : ruta.getOrigenLongitud();
                
                data.put("currentLatitude", currentLat);
                data.put("currentLongitude", currentLng);
                
                // Log para debug
                if (Boolean.TRUE.equals(ruta.getEnVuelo())) {
                    log.debug("✈️ Ruta {} en vuelo: current({}, {}), progress={}%", 
                        ruta.getId(), currentLat, currentLng, ruta.getProgreso());
                }
                
                // Estado del vuelo
                data.put("enVuelo", ruta.getEnVuelo() != null ? ruta.getEnVuelo() : false);
                data.put("progress", ruta.getProgreso() != null ? ruta.getProgreso() : 0.0);
                
                // Información adicional
                data.put("speed", ruta.getSpeed());
                data.put("altitude", ruta.getAltitud());
                data.put("totalPackages", ruta.getTotalPaquetes());
                data.put("regionOrigen", ruta.getRegionOrigen());
                data.put("regionDestino", ruta.getRegionDestino());
                
                return data;
            })
            .collect(Collectors.toList());
    }
}
