package com.proyecto.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalTime;

/**
 * Entidad que representa un plan de vuelo entre dos aeropuertos
 */
@Entity
@Table(name = "planesdevuelo")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class PlanDeVuelo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    //dia mes y año
    @Column(name = "aeropuerto_origen", nullable = false, length = 4)
    @NotNull(message = "El aeropuerto de origen es obligatorio")
    private String aeropuertoOrigen;

    @Column(name = "aeropuerto_destino", nullable = false, length = 4)
    @NotNull(message = "El aeropuerto de destino es obligatorio")
    private String aeropuertoDestino;

    @Column(name = "hora_salida", nullable = false)
    @NotNull(message = "La hora de salida es obligatoria")
    private LocalTime horaSalida;

    @Column(name = "hora_llegada", nullable = false)
    @NotNull(message = "La hora de llegada es obligatoria")
    private LocalTime horaLlegada;

    @Column(name = "capacidad_maxima", nullable = false)
    @Positive(message = "La capacidad maxima debe ser positiva")
    private int capacidadMaxima;

    /**
     * Constructor con todos los parametros
     */
    public PlanDeVuelo(String aeropuertoOrigen, String aeropuertoDestino,
                       LocalTime horaSalida, LocalTime horaLlegada, int capacidadMaxima) {
        this.aeropuertoOrigen = aeropuertoOrigen;
        this.aeropuertoDestino = aeropuertoDestino;
        this.horaSalida = horaSalida;
        this.horaLlegada = horaLlegada;
        this.capacidadMaxima = capacidadMaxima;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlanDeVuelo)) return false;
        PlanDeVuelo that = (PlanDeVuelo) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
