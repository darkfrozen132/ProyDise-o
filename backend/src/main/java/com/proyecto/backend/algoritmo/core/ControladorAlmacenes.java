package com.proyecto.backend.algoritmo.core;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador de capacidad de almacenes usando Difference Arrays
 *
 * Rastrea la ocupacion de almacenes en aeropuertos a lo largo del tiempo
 * usando granularidad de slots (bloques de 60 minutos)
 *
 * Hubs (SPIM, EBCI, UBBB) tienen capacidad ilimitada y no se rastrean
 *
 * Complejidad:
 * - agregarIntervalo(): O(1)
 * - puedeCaber(): O(slots) cuando sucio, sino O(slots a verificar)
 */
@Slf4j
public class ControladorAlmacenes {

    // Granularidad del slot en minutos
    private static final int MINUTOS_POR_SLOT = 60;

    // Numero total de slots (ejemplo: 7 dias × 24 horas = 168 slots)
    private final int numeroSlots;

    // Fecha base del horizonte temporal
    private final LocalDateTime fechaBase;

    // Almacenes: Map<codigoAeropuerto, Almacen>
    private final Map<String, Almacen> almacenes;

    /**
     * Clase interna que representa un almacen individual
     */
    private static class Almacen {
        // Capacidad maxima del almacen (0 = ilimitado para hubs)
        final int capacidadMaxima;

        // Difference array: delta[slot] = cambio en ese slot
        final int[] delta;

        // Prefix sum: pref[slot] = ocupacion acumulada hasta ese slot
        final int[] pref;

        // Flag: true si delta cambio y pref necesita reconstruirse
        boolean sucio;

        Almacen(int numeroSlots, int capacidadMaxima) {
            this.capacidadMaxima = capacidadMaxima;
            this.delta = new int[numeroSlots + 1]; // +1 para evitar out of bounds
            this.pref = new int[numeroSlots + 1];
            this.sucio = false;
        }
    }

    /**
     * Constructor
     *
     * @param numeroDias Numero de dias del horizonte temporal
     * @param fechaBase Fecha base del horizonte
     */
    public ControladorAlmacenes(int numeroDias, LocalDateTime fechaBase) {
        this.numeroSlots = numeroDias * 24; // dias × 24 horas
        this.fechaBase = fechaBase;
        this.almacenes = new HashMap<>();
        log.info("ControladorAlmacenes creado: {} slots ({} dias × 24 horas), fecha base: {}",
                numeroSlots, numeroDias, fechaBase);
    }

    /**
     * Registra un aeropuerto con su capacidad
     *
     * @param codigoAeropuerto Codigo ICAO del aeropuerto
     * @param capacidadMaxima Capacidad maxima del almacen (0 = ilimitado para hubs)
     */
    public void registrarAeropuerto(String codigoAeropuerto, int capacidadMaxima) {
        if (!almacenes.containsKey(codigoAeropuerto)) {
            almacenes.put(codigoAeropuerto, new Almacen(numeroSlots, capacidadMaxima));
            String tipo = capacidadMaxima == 0 ? "HUB (ilimitado)" : String.valueOf(capacidadMaxima);
            log.debug("Aeropuerto registrado: {} (capacidad: {})", codigoAeropuerto, tipo);
        }
    }

    /**
     * Convierte LocalDateTime a slot relativo
     *
     * @param fechaHora Fecha y hora UTC
     * @return Numero de slot
     */
    private int fechaHoraASlot(LocalDateTime fechaHora) {
        // Calcular minutos desde fechaBase
        long minutosDesdeBase = java.time.Duration.between(fechaBase, fechaHora).toMinutes();
        return (int) (minutosDesdeBase / MINUTOS_POR_SLOT);
    }

    /**
     * Agrega una reserva de almacen en un intervalo de tiempo
     *
     * @param codigoAeropuerto Codigo del aeropuerto
     * @param inicio Fecha/hora UTC de inicio de la reserva
     * @param fin Fecha/hora UTC de fin de la reserva (exclusivo)
     * @param cantidad Cantidad de productos
     */
    public void agregarIntervalo(String codigoAeropuerto, LocalDateTime inicio,
                                 LocalDateTime fin, int cantidad) {
        Almacen almacen = almacenes.get(codigoAeropuerto);
        if (almacen == null) {
            log.warn("Aeropuerto {} no registrado, ignorando reserva", codigoAeropuerto);
            return;
        }

        // Si capacidad ilimitada (hub), no rastrear ocupacion
        if (almacen.capacidadMaxima == 0) {
            log.trace("Hub {} tiene capacidad ilimitada, no se rastrea", codigoAeropuerto);
            return;
        }

        int slotInicio = fechaHoraASlot(inicio);
        int slotFin = fechaHoraASlot(fin);

        // Asegurar que no exceda los limites
        slotInicio = Math.max(0, Math.min(slotInicio, numeroSlots));
        slotFin = Math.max(0, Math.min(slotFin, numeroSlots));

        if (slotInicio >= slotFin) {
            log.warn("Intervalo invalido: slotInicio={} >= slotFin={} ({} -> {})",
                    slotInicio, slotFin, inicio, fin);
            return;
        }

        // Difference array: marcar inicio y fin
        almacen.delta[slotInicio] += cantidad;
        almacen.delta[slotFin] -= cantidad;
        almacen.sucio = true;

        log.debug("Reserva en {}: slots [{}, {}) = {} productos ({} -> {})",
                codigoAeropuerto, slotInicio, slotFin, cantidad, inicio, fin);
    }

    /**
     * Verifica si hay capacidad disponible en un intervalo
     *
     * @param codigoAeropuerto Codigo del aeropuerto
     * @param inicio Fecha/hora UTC de inicio
     * @param fin Fecha/hora UTC de fin (exclusivo)
     * @param cantidad Cantidad a verificar
     * @return true si hay capacidad suficiente en TODOS los slots del intervalo
     */
    public boolean puedeCaber(String codigoAeropuerto, LocalDateTime inicio,
                              LocalDateTime fin, int cantidad) {
        Almacen almacen = almacenes.get(codigoAeropuerto);
        if (almacen == null) {
            log.warn("Aeropuerto {} no registrado, asumiendo capacidad disponible", codigoAeropuerto);
            return true;
        }

        // Si capacidad ilimitada (hub), siempre cabe
        if (almacen.capacidadMaxima == 0) {
            return true;
        }

        int slotInicio = fechaHoraASlot(inicio);
        int slotFin = fechaHoraASlot(fin);

        // Asegurar limites
        slotInicio = Math.max(0, Math.min(slotInicio, numeroSlots));
        slotFin = Math.max(0, Math.min(slotFin, numeroSlots));

        if (slotInicio >= slotFin) {
            return true; // Intervalo vacio
        }

        // Si esta sucio, reconstruir prefix sum
        if (almacen.sucio) {
            reconstruirPrefixSum(almacen);
        }

        // Verificar que en TODOS los slots del intervalo haya capacidad
        for (int slot = slotInicio; slot < slotFin; slot++) {
            int ocupacionActual = almacen.pref[slot];
            if (ocupacionActual + cantidad > almacen.capacidadMaxima) {
                log.debug("No cabe en {}: slot {} ocupacion={} + {} > capacidad={}",
                        codigoAeropuerto, slot, ocupacionActual, cantidad, almacen.capacidadMaxima);
                return false;
            }
        }

        return true;
    }

    /**
     * Calcula el maximo ajuste de cantidad que cabe en un intervalo
     *
     * Usa binary search para encontrar la cantidad maxima que cabe
     *
     * @param codigoAeropuerto Codigo del aeropuerto
     * @param inicio Fecha/hora UTC de inicio
     * @param fin Fecha/hora UTC de fin
     * @param cantidadSolicitada Cantidad solicitada
     * @return Cantidad maxima que realmente cabe
     */
    public int calcularMaximoAjuste(String codigoAeropuerto, LocalDateTime inicio,
                                    LocalDateTime fin, int cantidadSolicitada) {
        Almacen almacen = almacenes.get(codigoAeropuerto);
        if (almacen == null || almacen.capacidadMaxima == 0) {
            return cantidadSolicitada; // Sin limite
        }

        // Si cabe todo, retornar la cantidad solicitada
        if (puedeCaber(codigoAeropuerto, inicio, fin, cantidadSolicitada)) {
            return cantidadSolicitada;
        }

        // Binary search para encontrar la cantidad maxima que cabe
        int izquierda = 0;
        int derecha = cantidadSolicitada;
        int mejor = 0;

        while (izquierda <= derecha) {
            int medio = (izquierda + derecha) / 2;

            if (puedeCaber(codigoAeropuerto, inicio, fin, medio)) {
                mejor = medio;
                izquierda = medio + 1; // Intentar con mas
            } else {
                derecha = medio - 1; // Intentar con menos
            }
        }

        log.debug("Ajuste de capacidad en {}: solicitado={}, cabe={}",
                codigoAeropuerto, cantidadSolicitada, mejor);

        return mejor;
    }

    /**
     * Reconstruye el prefix sum (suma acumulada) desde el difference array
     *
     * @param almacen Almacen a reconstruir
     */
    private void reconstruirPrefixSum(Almacen almacen) {
        almacen.pref[0] = almacen.delta[0];
        for (int i = 1; i < almacen.pref.length; i++) {
            almacen.pref[i] = almacen.pref[i - 1] + almacen.delta[i];
        }
        almacen.sucio = false;
    }

    /**
     * Obtiene la ocupacion actual de un aeropuerto en un slot especifico
     *
     * @param codigoAeropuerto Codigo del aeropuerto
     * @param slot Numero de slot
     * @return Ocupacion en ese slot (0 si no existe)
     */
    public int obtenerOcupacion(String codigoAeropuerto, int slot) {
        Almacen almacen = almacenes.get(codigoAeropuerto);
        if (almacen == null) {
            return 0;
        }

        if (slot < 0 || slot >= numeroSlots) {
            return 0;
        }

        if (almacen.sucio) {
            reconstruirPrefixSum(almacen);
        }

        return almacen.pref[slot];
    }

    /**
     * Obtiene la capacidad maxima de un aeropuerto
     *
     * @param codigoAeropuerto Codigo del aeropuerto
     * @return Capacidad maxima (0 = ilimitada)
     */
    public int obtenerCapacidadMaxima(String codigoAeropuerto) {
        Almacen almacen = almacenes.get(codigoAeropuerto);
        return (almacen != null) ? almacen.capacidadMaxima : 0;
    }

    /**
     * Obtiene estadisticas del controlador
     *
     * @return String con estadisticas
     */
    public String obtenerEstadisticas() {
        int almacenesRegistrados = almacenes.size();
        int almacenesConCapacidad = (int) almacenes.values().stream()
                .filter(a -> a.capacidadMaxima > 0)
                .count();

        return String.format("ControladorAlmacenes[%d aeropuertos, %d con capacidad limitada, %d slots (%d dias)]",
                almacenesRegistrados, almacenesConCapacidad, numeroSlots, numeroSlots / 24);
    }
}
