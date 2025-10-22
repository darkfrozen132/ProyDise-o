package com.proyecto.backend.algoritmo.service;

import com.proyecto.backend.algoritmo.core.*;
import com.proyecto.backend.algoritmo.dto.request.PlanificacionRequest;
import com.proyecto.backend.algoritmo.dto.response.*;
import com.proyecto.backend.model.Aeropuerto;
import com.proyecto.backend.model.Pedido;
import com.proyecto.backend.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Servicio del algoritmo genetico para planificacion de rutas
 * Version inicial simplificada - base para iteraciones futuras
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AlgoritmoGeneticoService {

    private final WorldCacheService worldCacheService;
    private final PedidoRepository pedidoRepository;

    // Constantes de negocio
    private static final int PLAZO_MISMO_CONTINENTE_DIAS = 2;
    private static final int PLAZO_DIFERENTE_CONTINENTE_DIAS = 3;
    private static final int VENTANA_RECOJO_HORAS = 2;

    /**
     * Ejecuta la planificacion de rutas para una fecha dada
     *
     * @param request Request con parametros de planificacion
     * @return Response con la planificacion completa
     */
    @Transactional(readOnly = true)
    public PlanificacionResponse planificar(PlanificacionRequest request) {
        long inicio = System.currentTimeMillis();

        log.info("Iniciando planificacion para fecha {} con K={}", request.getFecha(), request.getFactorK());

        // Obtener el World
        World world = worldCacheService.getWorld();

        // Cargar pedidos en el rango de tiempo
        List<Pedido> pedidos = cargarPedidosEnRango(request);
        log.info("Cargados {} pedidos para procesar", pedidos.size());

        if (pedidos.isEmpty()) {
            log.warn("No hay pedidos para procesar en el rango especificado");
            return crearResponseVacio(request, inicio);
        }

        // TODO: Implementar algoritmo genetico completo
        // Por ahora, retornar una solucion vacia para probar el flujo
        Solution solucion = crearSolucionVacia(pedidos);

        // Convertir solucion a response DTO (incluye los pedidos para verificacion)
        PlanificacionResponse response = convertirAResponse(solucion, world, request, pedidos, inicio);

        long tiempoTotal = System.currentTimeMillis() - inicio;
        log.info("Planificacion completada en {} ms", tiempoTotal);

        return response;
    }

    /**
     * Carga los pedidos en el rango de tiempo especificado
     *
     * @param request Request con parametros
     * @return Lista de pedidos a procesar
     */
    private List<Pedido> cargarPedidosEnRango(PlanificacionRequest request) {
        LocalDate fecha = request.getFecha();
        int rangoMinutos = request.calcularRangoConsumoMinutos();

        int anio = fecha.getYear();
        int mes = fecha.getMonthValue();
        int dia = fecha.getDayOfMonth();

        log.info("Cargando pedidos para fecha: {}/{}/{} (rango: {} minutos)",
                anio, mes, dia, rangoMinutos);

        // Filtrar pedidos por anio, mes, dia y estado PENDIENTE
        // TODO: Implementar filtrado por rango de minutos con el factor K
        List<Pedido> pedidos = pedidoRepository.findAll().stream()
                .filter(p -> p.getAnio() == anio)
                .filter(p -> p.getMes() == mes)
                .filter(p -> p.getDia() == dia)
                .filter(p -> "PENDIENTE".equals(p.getEstado()))
                .toList();

        log.info("Encontrados {} pedidos que cumplen los criterios", pedidos.size());

        return pedidos;
    }

    /**
     * Crea una solucion vacia (placeholder)
     *
     * @param pedidos Pedidos a procesar
     * @return Solucion vacia
     */
    private Solution crearSolucionVacia(List<Pedido> pedidos) {
        Solution solucion = new Solution();

        // Marcar todos como no entregados por ahora
        solucion.setPedidosNoEntregados(pedidos.size());
        solucion.setObjetivo(0.0);

        return solucion;
    }

    /**
     * Crea un response vacio cuando no hay pedidos
     *
     * @param request Request original
     * @param inicio Tiempo de inicio
     * @return Response vacio
     */
    private PlanificacionResponse crearResponseVacio(PlanificacionRequest request, long inicio) {
        PlanificacionResponse response = new PlanificacionResponse();

        PlanificacionResponse.MetadataPlanificacion metadata = new PlanificacionResponse.MetadataPlanificacion();
        metadata.setFechaInicio(LocalDateTime.of(request.getFecha(), LocalTime.MIDNIGHT));
        metadata.setFechaFin(metadata.getFechaInicio());
        metadata.setFactorK(request.getFactorK());
        metadata.setSaltoConsumoMinutos(request.calcularRangoConsumoMinutos());
        metadata.setSaltoAlgoritmoMinutos(request.getParametrosGenetico().getSaltoAlgoritmoMinutos());
        metadata.setPedidosProcesados(0);
        metadata.setTiempoEjecucionMs(System.currentTimeMillis() - inicio);

        response.setMetadata(metadata);
        response.setAeropuertos(new ArrayList<>());
        response.setVuelos(new ArrayList<>());
        response.setRutas(new ArrayList<>());

        return response;
    }

    /**
     * Convierte una Solution a PlanificacionResponse
     *
     * @param solucion Solucion del algoritmo
     * @param world World con datos
     * @param request Request original
     * @param pedidos Lista de pedidos procesados
     * @param inicio Tiempo de inicio
     * @return Response DTO
     */
    private PlanificacionResponse convertirAResponse(
            Solution solucion, World world, PlanificacionRequest request,
            List<Pedido> pedidos, long inicio) {

        PlanificacionResponse response = new PlanificacionResponse();

        // Crear metadata
        PlanificacionResponse.MetadataPlanificacion metadata = new PlanificacionResponse.MetadataPlanificacion();
        metadata.setFechaInicio(LocalDateTime.of(request.getFecha(), LocalTime.MIDNIGHT));
        metadata.setFechaFin(metadata.getFechaInicio().plusMinutes(request.calcularRangoConsumoMinutos()));
        metadata.setFactorK(request.getFactorK());
        metadata.setSaltoConsumoMinutos(request.calcularRangoConsumoMinutos());
        metadata.setSaltoAlgoritmoMinutos(request.getParametrosGenetico().getSaltoAlgoritmoMinutos());
        metadata.setPedidosProcesados(solucion.getTotalPedidos());
        metadata.setPedidosATiempo(solucion.getPedidosATiempo());
        metadata.setPedidosTarde(solucion.getPedidosTarde());
        metadata.setPedidosNoEntregados(solucion.getPedidosNoEntregados());
        metadata.setObjetivo(solucion.getObjetivo());
        metadata.setTiempoEjecucionMs(System.currentTimeMillis() - inicio);
        metadata.setGeneracionesEjecutadas(0); // TODO: actualizar cuando implementemos el GA

        response.setMetadata(metadata);

        // Convertir pedidos a resumen para verificacion
        response.setPedidosProcesados(convertirPedidosAResumen(pedidos));

        // Convertir aeropuertos
        response.setAeropuertos(convertirAeropuertos(world));

        // Convertir vuelos (vacio por ahora)
        response.setVuelos(new ArrayList<>());

        // Convertir rutas (vacio por ahora)
        response.setRutas(new ArrayList<>());

        return response;
    }

    /**
     * Convierte pedidos a DTOs de resumen
     *
     * @param pedidos Lista de pedidos
     * @return Lista de DTOs
     */
    private List<PlanificacionResponse.PedidoResumenDTO> convertirPedidosAResumen(List<Pedido> pedidos) {
        List<PlanificacionResponse.PedidoResumenDTO> resumen = new ArrayList<>();

        for (Pedido pedido : pedidos) {
            PlanificacionResponse.PedidoResumenDTO dto = new PlanificacionResponse.PedidoResumenDTO();
            dto.setId(pedido.getId());

            // Formatear fecha: yyyy-MM-dd HH:mm
            String fecha = String.format("%04d-%02d-%02d %02d:%02d",
                    pedido.getAnio(), pedido.getMes(), pedido.getDia(),
                    pedido.getHora(), pedido.getMinuto());
            dto.setFecha(fecha);

            dto.setDestino(pedido.getAeropuertoDestinoId());
            dto.setCantidad(pedido.getCantidadProductos());
            dto.setClienteId(pedido.getClienteId());
            dto.setEstado(pedido.getEstado());

            resumen.add(dto);
        }

        return resumen;
    }

    /**
     * Convierte aeropuertos a DTOs
     *
     * @param world World con datos
     * @return Lista de DTOs de aeropuertos
     */
    private List<AeropuertoEstadoDTO> convertirAeropuertos(World world) {
        List<AeropuertoEstadoDTO> aeropuertos = new ArrayList<>();

        for (Aeropuerto aeropuerto : world.getAeropuertos().values()) {
            AeropuertoEstadoDTO dto = new AeropuertoEstadoDTO();
            dto.setCode(aeropuerto.getCodigoICAO());
            dto.setLat(aeropuerto.getLatitud());
            dto.setLng(aeropuerto.getLongitud());
            dto.setName(aeropuerto.getCiudad() + " - " + aeropuerto.getPais());
            dto.setRegion(aeropuerto.getContinente());
            dto.setCountry(aeropuerto.getPais());
            dto.setSede(aeropuerto.esSedePrincipal());
            dto.setCapacity(aeropuerto.tieneStockIlimitado() ? "ILIMITADO" : String.valueOf(aeropuerto.getCapacidadAlmacen()));
            dto.setPackages(0); // TODO: calcular ocupacion real

            aeropuertos.add(dto);
        }

        return aeropuertos;
    }
}
