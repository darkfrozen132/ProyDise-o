package com.proyecto.backend.algoritmo.core;

import com.proyecto.backend.model.PlanDeVuelo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Uso concreto de un plan de vuelo en un dia especifico
 * Representa una instancia real de un vuelo con fecha y carga asignada
 */
@Data
@NoArgsConstructor
//@AllArgsConstructor
public class VueloUso {

    // Plan de vuelo template
    private PlanDeVuelo planVuelo;

    // Indice del dia (0 = primer dia de la simulacion)
    private int indiceDia;

    // Hora de salida en minutos UTC
    private int salidaUTC;

    // Hora de llegada en minutos UTC
    private int llegadaUTC;

    // Cantidad de productos asignados a este vuelo
    private int cantidadAsignada;

    /**
     * Constructor simplificado
     *
     * @param planVuelo Plan de vuelo
     * @param dia Dia del vuelo
     * @param salida Salida UTC en minutos
     * @param llegada Llegada UTC en minutos
     * @param cantidad Cantidad de productos
     */
    public VueloUso(PlanDeVuelo planVuelo, int dia, int salida, int llegada, int cantidad) {
        this.planVuelo = planVuelo;
        this.indiceDia = dia;
        this.salidaUTC = salida;
        this.llegadaUTC = llegada;
        this.cantidadAsignada = cantidad;
    }

    /**
     * Obtiene el codigo ICAO del origen
     *
     * @return Codigo del aeropuerto origen
     */
    public String getOrigen() {
        return planVuelo.getAeropuertoOrigen();
    }

    /**
     * Obtiene el codigo ICAO del destino
     *
     * @return Codigo del aeropuerto destino
     */
    public String getDestino() {
        return planVuelo.getAeropuertoDestino();
    }

    /**
     * Obtiene la capacidad maxima del vuelo
     *
     * @return Capacidad maxima
     */
    public int getCapacidadMaxima() {
        return planVuelo.getCapacidadMaxima();
    }

    /**
     * Genera un identificador unico para este vuelo
     *
     * @return ID en formato ORIGEN-DESTINO-D{dia}-HHMM
     */
    public String generarId() {
        int minutos = salidaUTC % 1440;
        if (minutos < 0) minutos += 1440;
        int horas = minutos / 60;
        int mins = minutos % 60;
        return String.format("%s-%s-D%d-%02d%02d",
                getOrigen(), getDestino(), indiceDia, horas, mins);
    }

    /**
     * Calcula la duracion del vuelo en minutos
     *
     * @return Duracion en minutos
     */
    public int getDuracionMinutos() {
        return llegadaUTC - salidaUTC;
    }

    /**
     * Calcula el porcentaje de ocupacion
     *
     * @return Porcentaje de ocupacion (0-100)
     */
    public double getPorcentajeOcupacion() {
        if (planVuelo.getCapacidadMaxima() == 0) {
            return 0.0;
        }
        return (cantidadAsignada * 100.0) / planVuelo.getCapacidadMaxima();
    }
}
