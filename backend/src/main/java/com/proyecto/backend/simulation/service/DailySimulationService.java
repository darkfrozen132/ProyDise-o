package com.proyecto.backend.simulation.service;

import com.proyecto.backend.model.PedidoDiario;
import com.proyecto.backend.planificador.semanal.dto.response.PlanificacionResponseSimple;
import com.proyecto.backend.planificador.semanal.dto.response.VueloSimplificadoDTO;
import com.proyecto.backend.planificador.semanal.model.SubRuta;
import com.proyecto.backend.planificador.semanal.service.AlgoritmoGeneticoService;
import com.proyecto.backend.model.PedidoSemanal;
import com.proyecto.backend.repository.PedidoDiarioRepository;
import com.proyecto.backend.simulation.dto.DailyOperationResponse;
import com.proyecto.backend.simulation.dto.DailySimulationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Servicio para Operación DIARIA
 * 
 * Características:
 * - Lee de la tabla pedidos_diario
 * - Procesa TODOS los pedidos de una vez (sin WebSocket)
 * - Operación SÍNCRONA: llamas → procesa → devuelve resultado
 * - No requiere fecha de inicio (la calcula de los pedidos)
 * 
 * @author Sistema Package Planner
 * @version 2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailySimulationService {

    private final PedidoDiarioRepository pedidoDiarioRepository;
    private final AlgoritmoGeneticoService algoritmoGeneticoService;

    /**
     * Ejecuta la operación diaria de forma SÍNCRONA
     * Procesa todos los pedidos de pedidos_diario y devuelve el resultado
     * 
     * VALIDACIÓN: Solo procesa pedidos cuya fecha/hora sea >= hora actual del servidor
     * Los pedidos con fecha pasada son ignorados (no tiene sentido planificarlos)
     * 
     * @param request Parámetros del AG (opcional, tiene defaults)
     * @return DailyOperationResponse con las rutas generadas
     */
    public DailyOperationResponse ejecutarOperacionDiaria(DailySimulationRequest request) {
        log.info("🌅 [DAILY] Iniciando operación diaria...");
        long inicioTotal = System.currentTimeMillis();
        
        // Hora actual del servidor (referencia para validar pedidos)
        LocalDateTime horaActualServidor = LocalDateTime.now();
        log.info("🕐 [DAILY] Hora actual del servidor: {}", horaActualServidor);

        try {
            // 1. Cargar TODOS los pedidos de la tabla pedidos_diario
            List<PedidoDiario> todosPedidos = pedidoDiarioRepository.findAll();
            
            if (todosPedidos.isEmpty()) {
                log.warn("⚠️ [DAILY] No hay pedidos en la tabla pedidos_diario");
                return DailyOperationResponse.builder()
                        .success(false)
                        .mensaje("No hay pedidos en la tabla pedidos_diario")
                        .totalPedidos(0)
                        .build();
            }

            log.info("📦 [DAILY] Cargados {} pedidos totales de pedidos_diario", todosPedidos.size());

            // 2. MODO DESARROLLO: Procesar TODOS los pedidos sin filtrar por fecha
            // En producción, descomentar el filtro de fecha
            List<PedidoDiario> pedidosValidos = new ArrayList<>(todosPedidos);
            int pedidosDescartados = 0;
            
            // COMENTADO PARA DESARROLLO - Descomentar para producción:
            /*
            List<PedidoDiario> pedidosValidos = todosPedidos.stream()
                    .filter(p -> {
                        LocalDateTime fechaPedido = LocalDateTime.of(
                                p.getAnio(), p.getMes(), p.getDia(), 
                                p.getHora(), p.getMinuto());
                        return !fechaPedido.isBefore(horaActualServidor);
                    })
                    .toList();

            int pedidosDescartados = todosPedidos.size() - pedidosValidos.size();
            
            if (pedidosDescartados > 0) {
                log.warn("⏰ [DAILY] {} pedidos descartados por tener fecha pasada (anterior a {})", 
                        pedidosDescartados, horaActualServidor);
            }

            if (pedidosValidos.isEmpty()) {
                log.warn("⚠️ [DAILY] Todos los pedidos tienen fecha pasada, nada que procesar");
                return DailyOperationResponse.builder()
                        .success(false)
                        .mensaje(String.format(
                                "No hay pedidos válidos. %d pedidos descartados por tener fecha anterior a %s", 
                                pedidosDescartados, horaActualServidor))
                        .totalPedidos(todosPedidos.size())
                        .pedidosSinRuta(pedidosDescartados)
                        .build();
            }
            */

            log.info("✅ [DAILY] {} pedidos para procesar (modo desarrollo: sin filtro de fecha)", 
                    pedidosValidos.size());

            // 3. Calcular rango de fechas de los pedidos VÁLIDOS
            LocalDateTime fechaMin = calcularFechaMinima(pedidosValidos);
            LocalDateTime fechaMax = calcularFechaMaxima(pedidosValidos);
            
            log.info("📅 [DAILY] Rango de pedidos válidos: {} → {}", fechaMin, fechaMax);

            // 4. Ejecutar el AG de forma síncrona
            log.info("🧬 [DAILY] Ejecutando algoritmo genético...");
            
            AlgoritmoGeneticoService.ResultadoPlanificacionDiaria resultado = 
                algoritmoGeneticoService.planificarDiarioSincrono(
                    pedidosValidos,
                    fechaMin,
                    request.getTamanioPoblacion(),
                    request.getMaxGeneraciones(),
                    request.getLimiteGeneracionesSinMejora()
                );

            // 4. Construir respuesta
            List<DailyOperationResponse.RutaResumen> rutasResumen = new ArrayList<>();
            
            if (resultado.solucion != null && resultado.solucion.getRutas() != null) {
                for (Map.Entry<PedidoSemanal, List<SubRuta>> entry : resultado.solucion.getRutas().entrySet()) {
                    PedidoSemanal pedido = entry.getKey();
                    List<SubRuta> subrutas = entry.getValue();
                    
                    if (subrutas != null && !subrutas.isEmpty()) {
                        // Extraer aeropuertos de la ruta
                        List<String> aeropuertosRuta = new ArrayList<>();
                        for (SubRuta sr : subrutas) {
                            // Agregar hub de origen
                            if (aeropuertosRuta.isEmpty() && sr.getHubOrigen() != null) {
                                aeropuertosRuta.add(sr.getHubOrigen());
                            }
                            // Agregar destinos de cada vuelo
                            if (sr.getVuelos() != null) {
                                for (var vuelo : sr.getVuelos()) {
                                    if (vuelo.getDestino() != null) {
                                        aeropuertosRuta.add(vuelo.getDestino());
                                    }
                                }
                            }
                        }
                        
                        rutasResumen.add(DailyOperationResponse.RutaResumen.builder()
                                .pedidoId(pedido.getId())
                                .clienteId(pedido.getClienteId())
                                .destino(pedido.getAeropuertoDestinoId())
                                .cantidadProductos(pedido.getCantidadProductos())
                                .aeropuertosRuta(aeropuertosRuta)
                                .totalTramos(subrutas.stream().mapToInt(s -> s.getVuelos() != null ? s.getVuelos().size() : 0).sum())
                                .build());
                    }
                }
            }

            // 🆕 CONVERTIR A VUELOS DETALLADOS para visualización en mapa
            List<VueloSimplificadoDTO> vuelosDetallados = new ArrayList<>();
            if (resultado.solucion != null && resultado.worldTemporal != null) {
                try {
                    PlanificacionResponseSimple responseSimple = 
                        algoritmoGeneticoService.convertirAResponseSimple(resultado.solucion, resultado.worldTemporal);
                    vuelosDetallados = responseSimple.getVuelos();
                    log.info("✈️ [DAILY] Convertidos {} vuelos detallados para visualización", vuelosDetallados.size());
                } catch (Exception e) {
                    log.warn("⚠️ [DAILY] No se pudieron convertir vuelos detallados: {}", e.getMessage());
                }
            }

            long tiempoTotal = System.currentTimeMillis() - inicioTotal;

            log.info("✅ [DAILY] Operación completada en {}ms: {} válidos → {} rutas, {} vuelos (descartados: {})", 
                    tiempoTotal, pedidosValidos.size(), rutasResumen.size(), vuelosDetallados.size(), pedidosDescartados);

            return DailyOperationResponse.builder()
                    .success(true)
                    .mensaje(String.format(
                            "Operación completada: %d pedidos válidos procesados, %d rutas generadas, %d vuelos, %d descartados por fecha pasada", 
                            pedidosValidos.size(), rutasResumen.size(), vuelosDetallados.size(), pedidosDescartados))
                    .totalPedidos(todosPedidos.size())
                    .pedidosValidos(pedidosValidos.size())
                    .pedidosDescartados(pedidosDescartados)
                    .pedidosAsignados(resultado.pedidosAsignados)
                    .pedidosSinRuta(resultado.pedidosSinRuta)
                    .totalVuelos(vuelosDetallados.size())
                    .mejorFitness(resultado.mejorFitness)
                    .tiempoProcesamiento(tiempoTotal)
                    .horaServidorUsada(horaActualServidor)
                    .fechaInicioPedidos(fechaMin)
                    .fechaFinPedidos(fechaMax)
                    .rutas(rutasResumen)
                    .vuelos(vuelosDetallados)
                    .build();

        } catch (Exception e) {
            log.error("❌ [DAILY] Error en operación diaria: {}", e.getMessage(), e);
            return DailyOperationResponse.builder()
                    .success(false)
                    .mensaje("Error: " + e.getMessage())
                    .tiempoProcesamiento(System.currentTimeMillis() - inicioTotal)
                    .build();
        }
    }

    // ============ MÉTODOS AUXILIARES ============

    private LocalDateTime calcularFechaMinima(List<PedidoDiario> pedidos) {
        return pedidos.stream()
                .map(p -> LocalDateTime.of(p.getAnio(), p.getMes(), p.getDia(), p.getHora(), p.getMinuto()))
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
    }

    private LocalDateTime calcularFechaMaxima(List<PedidoDiario> pedidos) {
        return pedidos.stream()
                .map(p -> LocalDateTime.of(p.getAnio(), p.getMes(), p.getDia(), p.getHora(), p.getMinuto()))
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now().plusDays(1));
    }
}
