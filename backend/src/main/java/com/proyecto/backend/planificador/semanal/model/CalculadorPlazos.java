package com.proyecto.backend.planificador.semanal.model;

import com.proyecto.backend.model.Aeropuerto;
import com.proyecto.backend.model.Pedido;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Calcula plazos de entrega y clasifica pedidos segun lleguen a tiempo o tarde
 *
 * Reglas de negocio:
 * - Mismo continente: 2 dias de plazo
 * - Diferente continente: 3 dias de plazo
 * - Ventana de recojo: 2 horas desde llegada del ultimo vuelo
 */
@Slf4j
public class CalculadorPlazos {

    // Constantes de plazos de entrega (en dias)
    private static final int PLAZO_MISMO_CONTINENTE = 2;
    private static final int PLAZO_DIFERENTE_CONTINENTE = 3;

    // Ventana de recojo en horas
    private static final int VENTANA_RECOJO_HORAS = 2;

    private final WorldTemporal worldTemporal;

    public CalculadorPlazos(WorldTemporal worldTemporal) {
        this.worldTemporal = worldTemporal;
    }

    /**
     * Convierte minutos UTC del dia e indice de dia a LocalDateTime
     *
     * @param indiceDia Dia relativo (0 = primer dia)
     * @param minutosUTC Minutos desde medianoche UTC (0-1439)
     * @return LocalDateTime correspondiente
     */
    private LocalDateTime minutosALocalDateTime(int indiceDia, int minutosUTC) {
        LocalDate fecha = worldTemporal.getFechaInicio().plusDays(indiceDia);
        int horas = minutosUTC / 60;
        int minutos = minutosUTC % 60;
        return LocalDateTime.of(fecha, LocalTime.of(horas, minutos));
    }

    /**
     * Calcula el estado de entrega de un pedido
     *
     * @param pedido Pedido a evaluar
     * @param subrutas Lista de subrutas asignadas (puede ser vacia si no se entrego)
     * @return Estado de entrega
     */
    public EstadoEntrega calcularEstadoEntrega(Pedido pedido, List<SubRuta> subrutas) {
        // Si no hay rutas, no se entrego
        if (subrutas == null || subrutas.isEmpty()) {
            return EstadoEntrega.NO_ENTREGADO;
        }

        // Obtener aeropuerto destino
        Aeropuerto destino = worldTemporal.getAeropuerto(pedido.getAeropuertoDestinoId());
        if (destino == null) {
            log.warn("Aeropuerto destino {} no encontrado para pedido {}",
                    pedido.getAeropuertoDestinoId(), pedido.getId());
            return EstadoEntrega.NO_ENTREGADO;
        }

        // Obtener hub de origen (primera subruta)
        String hubOrigenId = subrutas.get(0).getHubOrigen();
        Aeropuerto hubOrigen = worldTemporal.getAeropuerto(hubOrigenId);
        if (hubOrigen == null) {
            log.warn("Hub origen {} no encontrado para pedido {}", hubOrigenId, pedido.getId());
            return EstadoEntrega.NO_ENTREGADO;
        }

        // Calcular fecha limite de entrega
        LocalDate fechaPedido = LocalDate.of(pedido.getAnio(), pedido.getMes(), pedido.getDia());
        LocalDateTime fechaLimite = calcularFechaLimite(fechaPedido, hubOrigen, destino);

        // Calcular fecha real de entrega (ultimo vuelo + ventana de recojo)
        LocalDateTime fechaEntregaReal = calcularFechaEntregaReal(subrutas);

        if (fechaEntregaReal == null) {
            log.warn("No se pudo calcular fecha de entrega real para pedido {}", pedido.getId());
            return EstadoEntrega.NO_ENTREGADO;
        }

        // Comparar fechas
        if (fechaEntregaReal.isAfter(fechaLimite)) {
            log.debug("Pedido {} TARDE: entrega {} > limite {}",
                    pedido.getId(), fechaEntregaReal, fechaLimite);
            return EstadoEntrega.ENTREGADO_TARDE;
        } else {
            log.debug("Pedido {} A TIEMPO: entrega {} <= limite {}",
                    pedido.getId(), fechaEntregaReal, fechaLimite);
            return EstadoEntrega.ENTREGADO_A_TIEMPO;
        }
    }

    /**
     * Calcula la fecha limite de entrega segun continentes
     *
     * @param fechaPedido Fecha del pedido
     * @param origen Aeropuerto de origen (hub)
     * @param destino Aeropuerto de destino
     * @return Fecha y hora limite de entrega
     */
    public LocalDateTime calcularFechaLimite(LocalDate fechaPedido, Aeropuerto origen, Aeropuerto destino) {
        int diasPlazo;

        if (origen.esMismoContinente(destino)) {
            diasPlazo = PLAZO_MISMO_CONTINENTE;
            log.trace("Mismo continente ({} - {}): {} dias",
                    origen.getContinente(), destino.getContinente(), diasPlazo);
        } else {
            diasPlazo = PLAZO_DIFERENTE_CONTINENTE;
            log.trace("Diferente continente ({} - {}): {} dias",
                    origen.getContinente(), destino.getContinente(), diasPlazo);
        }

        // La fecha limite es al final del dia (23:59:59)
        return fechaPedido.plusDays(diasPlazo).atTime(23, 59, 59);
    }

    /**
     * Calcula la fecha real de entrega desde las subrutas
     * (llegada del ultimo vuelo + ventana de recojo)
     *
     * @param subrutas Lista de subrutas del pedido
     * @return Fecha y hora real de entrega (fin de ventana de recojo)
     */
    private LocalDateTime calcularFechaEntregaReal(List<SubRuta> subrutas) {
        if (subrutas == null || subrutas.isEmpty()) {
            return null;
        }

        // Obtener ultimo vuelo de la ultima subruta
        SubRuta ultimaSubruta = subrutas.get(subrutas.size() - 1);
        List<VueloUso> vuelos = ultimaSubruta.getVuelos();

        if (vuelos == null || vuelos.isEmpty()) {
            return null;
        }

        VueloUso ultimoVuelo = vuelos.get(vuelos.size() - 1);

        // Convertir minutos UTC a LocalDateTime
        LocalDateTime llegadaUltimoVuelo = minutosALocalDateTime(
                ultimoVuelo.getIndiceDia(),
                ultimoVuelo.getLlegadaUTC()
        );

        // Fecha de entrega = llegada del ultimo vuelo + ventana de recojo
        return llegadaUltimoVuelo.plusHours(VENTANA_RECOJO_HORAS);
    }

    /**
     * Calcula la fecha limite para un pedido especifico
     *
     * @param pedido Pedido a evaluar
     * @return Fecha limite de entrega
     */
    public LocalDateTime calcularFechaLimitePedido(Pedido pedido) {
        // Obtener aeropuerto destino
        Aeropuerto destino = worldTemporal.getAeropuerto(pedido.getAeropuertoDestinoId());
        if (destino == null) {
            return null;
        }

        // Buscar hub mas cercano al destino como referencia
        Aeropuerto hubReferencia = null;
        for (String hubId : worldTemporal.getHubs()) {
            hubReferencia = worldTemporal.getAeropuerto(hubId);
            break; // Usar primer hub como referencia (podria mejorarse)
        }

        if (hubReferencia == null) {
            return null;
        }

        LocalDate fechaPedido = LocalDate.of(pedido.getAnio(), pedido.getMes(), pedido.getDia());
        return calcularFechaLimite(fechaPedido, hubReferencia, destino);
    }
}
