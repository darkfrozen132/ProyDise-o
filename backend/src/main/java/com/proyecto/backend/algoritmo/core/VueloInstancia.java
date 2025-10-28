package com.proyecto.backend.algoritmo.core;

import com.proyecto.backend.model.Aeropuerto;
import com.proyecto.backend.model.PlanDeVuelo;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;

/**
 * Instancia concreta de un vuelo en una fecha especifica
 * Representa la expansion de un PlanDeVuelo template en el horizonte temporal
 *
 * Maneja correctamente:
 * - Conversion de horarios locales a UTC
 * - Cruces de dia (vuelos que llegan al dia siguiente)
 * - Control de capacidad individual por instancia
 */
@Slf4j
@Getter
public class VueloInstancia {

    // Template del vuelo
    private final PlanDeVuelo template;

    // Dia relativo en la simulacion (0, 1, 2...)
    private final int diaRelativo;

    // Fecha base de la simulacion
    private final LocalDate fechaBase;

    // Fechas/horas UTC calculadas
    private final LocalDateTime salidaUTC;
    private final LocalDateTime llegadaUTC;

    // ID unico de esta instancia
    private final String id;

    // Capacidad y uso
    private final int capacidadMaxima;
    private int capacidadUsada;

    /**
     * Constructor: Crea una instancia del vuelo para un dia especifico
     *
     * @param template Plan de vuelo template
     * @param diaRelativo Dia relativo (0 = primer dia, 1 = segundo dia, etc.)
     * @param fechaBase Fecha base de la simulacion
     * @param aeropuertoOrigen Aeropuerto origen (para huso horario)
     * @param aeropuertoDestino Aeropuerto destino (para huso horario)
     */
    public VueloInstancia(PlanDeVuelo template, int diaRelativo, LocalDate fechaBase,
                         Aeropuerto aeropuertoOrigen, Aeropuerto aeropuertoDestino) {
        this.template = template;
        this.diaRelativo = diaRelativo;
        this.fechaBase = fechaBase;
        this.capacidadMaxima = template.getCapacidadMaxima();
        this.capacidadUsada = 0;

        // Calcular salida y llegada en UTC
        this.salidaUTC = calcularSalidaUTC(fechaBase, diaRelativo, template, aeropuertoOrigen);
        this.llegadaUTC = calcularLlegadaUTC(fechaBase, diaRelativo, template,
                                              aeropuertoOrigen, aeropuertoDestino);

        // Generar ID unico
        this.id = generarId();

        log.debug("VueloInstancia creada: {} - Salida UTC: {} - Llegada UTC: {} - Capacidad: {}",
                 id, salidaUTC, llegadaUTC, capacidadMaxima);
    }

    /**
     * Calcula la hora de salida en UTC
     *
     * Proceso:
     * 1. Toma la hora local de salida del template
     * 2. Suma el dia relativo para obtener la fecha correcta
     * 3. Aplica el offset del huso horario del aeropuerto origen
     *
     * @param fechaBase Fecha base de la simulacion
     * @param diaRelativo Dia relativo
     * @param template Template del vuelo
     * @param aeropuertoOrigen Aeropuerto origen
     * @return Fecha/hora de salida en UTC
     */
    private LocalDateTime calcularSalidaUTC(LocalDate fechaBase, int diaRelativo,
                                           PlanDeVuelo template, Aeropuerto aeropuertoOrigen) {
        // Fecha del vuelo = fechaBase + diaRelativo
        LocalDate fechaVuelo = fechaBase.plusDays(diaRelativo);

        // Hora local de salida (del template)
        LocalTime horaSalidaLocal = template.getHoraSalida();

        // Combinar fecha + hora local
        LocalDateTime salidaLocal = LocalDateTime.of(fechaVuelo, horaSalidaLocal);

        // Convertir a UTC: restar el offset del huso horario
        // Ejemplo: Lima GMT-5 → 00:58 local = 05:58 UTC (00:58 - (-5) = 05:58)
        int offsetOrigen = aeropuertoOrigen.getHusoHorario();
        LocalDateTime salidaUTC = salidaLocal.minusHours(offsetOrigen);

        return salidaUTC;
    }

    /**
     * Calcula la hora de llegada en UTC
     *
     * Proceso:
     * 1. Toma la hora local de llegada del template (mismo dia calendario que salida)
     * 2. Aplica el offset del huso horario del aeropuerto destino
     * 3. Si llegadaUTC < salidaUTC, significa que cruzo al dia siguiente
     *
     * Ejemplo problematico:
     * SPIM → EBCI: 00:58 → 22:02
     * - Salida: 00:58 Lima (GMT-5) = 05:58 UTC
     * - Llegada: 22:02 Bruselas (GMT+2) = 20:02 UTC
     * - Como 20:02 < 05:58, significa que llega al dia SIGUIENTE
     * - Resultado: 20:02 UTC del dia siguiente
     *
     * @param fechaBase Fecha base de la simulacion
     * @param diaRelativo Dia relativo
     * @param template Template del vuelo
     * @param aeropuertoOrigen Aeropuerto origen
     * @param aeropuertoDestino Aeropuerto destino
     * @return Fecha/hora de llegada en UTC
     */
    private LocalDateTime calcularLlegadaUTC(LocalDate fechaBase, int diaRelativo,
                                            PlanDeVuelo template,
                                            Aeropuerto aeropuertoOrigen,
                                            Aeropuerto aeropuertoDestino) {
        // Fecha del vuelo (misma que salida)
        LocalDate fechaVuelo = fechaBase.plusDays(diaRelativo);

        // Hora local de llegada (del template, mismo dia calendario)
        LocalTime horaLlegadaLocal = template.getHoraLlegada();

        // Combinar fecha + hora local
        LocalDateTime llegadaLocal = LocalDateTime.of(fechaVuelo, horaLlegadaLocal);

        // Convertir a UTC: restar el offset del huso horario
        // Ejemplo: Bruselas GMT+2 → 22:02 local = 20:02 UTC (22:02 - 2 = 20:02)
        int offsetDestino = aeropuertoDestino.getHusoHorario();
        LocalDateTime llegadaUTC = llegadaLocal.minusHours(offsetDestino);

        // Detectar cruce de dia: si llegadaUTC < salidaUTC, sumar 1 dia
        LocalDateTime salidaUTC = calcularSalidaUTC(fechaBase, diaRelativo, template, aeropuertoOrigen);
        if (llegadaUTC.isBefore(salidaUTC) || llegadaUTC.equals(salidaUTC)) {
            llegadaUTC = llegadaUTC.plusDays(1);
            log.debug("Vuelo {} cruza al dia siguiente: llegada ajustada a {}",
                     generarIdPreliminar(template, diaRelativo), llegadaUTC);
        }

        return llegadaUTC;
    }

    /**
     * Genera el ID unico de esta instancia
     * Formato: ORIGEN-DESTINO-YYYYMMDD-HHMM
     *
     * Ejemplo: SPIM-SEQM-20250117-0334
     *
     * @return ID unico
     */
    private String generarId() {
        LocalTime horaSalida = template.getHoraSalida();
        LocalDate fechaReal = fechaBase.plusDays(diaRelativo);

        return String.format("%s-%s-%04d%02d%02d-%02d%02d",
                template.getAeropuertoOrigen(),
                template.getAeropuertoDestino(),
                fechaReal.getYear(),
                fechaReal.getMonthValue(),
                fechaReal.getDayOfMonth(),
                horaSalida.getHour(),
                horaSalida.getMinute());
    }

    /**
     * Helper para logging (antes de tener this.salidaUTC)
     */
    private String generarIdPreliminar(PlanDeVuelo plan, int dia) {
        LocalTime horaSalida = plan.getHoraSalida();
        LocalDate fechaReal = fechaBase.plusDays(dia);

        return String.format("%s-%s-%04d%02d%02d-%02d%02d",
                plan.getAeropuertoOrigen(),
                plan.getAeropuertoDestino(),
                fechaReal.getYear(),
                fechaReal.getMonthValue(),
                fechaReal.getDayOfMonth(),
                horaSalida.getHour(),
                horaSalida.getMinute());
    }

    /**
     * Verifica si hay capacidad disponible
     *
     * @param cantidad Cantidad a verificar
     * @return true si hay capacidad suficiente
     */
    public boolean tieneCapacidad(int cantidad) {
        return (capacidadUsada + cantidad) <= capacidadMaxima;
    }

    /**
     * Asigna capacidad a este vuelo
     *
     * @param cantidad Cantidad a asignar
     * @return true si se pudo asignar
     */
    public boolean asignarCapacidad(int cantidad) {
        if (!tieneCapacidad(cantidad)) {
            return false;
        }

        capacidadUsada += cantidad;
        log.debug("Capacidad asignada en {}: {} / {} ({} restante)",
                 id, capacidadUsada, capacidadMaxima, getCapacidadRestante());
        return true;
    }

    /**
     * Libera capacidad previamente asignada
     *
     * @param cantidad Cantidad a liberar
     */
    public void liberarCapacidad(int cantidad) {
        capacidadUsada = Math.max(0, capacidadUsada - cantidad);
        log.debug("Capacidad liberada en {}: {} / {} ({} restante)",
                 id, capacidadUsada, capacidadMaxima, getCapacidadRestante());
    }

    /**
     * Obtiene la capacidad restante
     *
     * @return Capacidad restante
     */
    public int getCapacidadRestante() {
        return capacidadMaxima - capacidadUsada;
    }

    /**
     * Calcula el porcentaje de ocupacion
     *
     * @return Porcentaje de ocupacion (0-100)
     */
    public double getPorcentajeOcupacion() {
        if (capacidadMaxima == 0) {
            return 0.0;
        }
        return (capacidadUsada * 100.0) / capacidadMaxima;
    }

    /**
     * Calcula la duracion del vuelo en minutos
     *
     * @return Duracion en minutos
     */
    public long getDuracionMinutos() {
        return java.time.Duration.between(salidaUTC, llegadaUTC).toMinutes();
    }

    /**
     * Obtiene el codigo ICAO del origen
     *
     * @return Codigo del aeropuerto origen
     */
    public String getOrigen() {
        return template.getAeropuertoOrigen();
    }

    /**
     * Obtiene el codigo ICAO del destino
     *
     * @return Codigo del aeropuerto destino
     */
    public String getDestino() {
        return template.getAeropuertoDestino();
    }

    /**
     * Convierte esta instancia a VueloUso (para compatibilidad con codigo existente)
     *
     * @return VueloUso equivalente
     */
    public VueloUso toVueloUso() {
        VueloUso vueloUso = new VueloUso();
        vueloUso.setPlanVuelo(template);
        vueloUso.setIndiceDia(diaRelativo);

        // Convertir LocalDateTime a minutos UTC desde epoca base
        // Por ahora simplificar: usar minutos del dia
        int minutosSalida = salidaUTC.getHour() * 60 + salidaUTC.getMinute();
        int minutosLlegada = llegadaUTC.getHour() * 60 + llegadaUTC.getMinute();

        vueloUso.setSalidaUTC(minutosSalida);
        vueloUso.setLlegadaUTC(minutosLlegada);
        vueloUso.setCantidadAsignada(0); // Se asigna luego

        // IMPORTANTE: Pasar el ID completo con fecha
        vueloUso.setIdCompleto(this.id);

        return vueloUso;
    }

    @Override
    public String toString() {
        return String.format("VueloInstancia[%s, salida=%s UTC, llegada=%s UTC, capacidad=%d/%d (%.1f%%)]",
                id, salidaUTC, llegadaUTC, capacidadUsada, capacidadMaxima, getPorcentajeOcupacion());
    }
}
