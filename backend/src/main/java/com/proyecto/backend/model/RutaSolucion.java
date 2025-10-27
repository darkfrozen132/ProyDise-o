package com.proyecto.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa una ruta solución en el sistema de planificación.
 * Almacena información sobre las rutas calculadas durante la simulación.
 */
@Entity
@Table(name = "rutas_solucion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class RutaSolucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "origin_code", nullable = false, length = 4)
    private String originCode;

    @Column(name = "destination_code", nullable = false, length = 4)
    private String destinationCode;

    @Column(name = "salida")
    private LocalDateTime salida;

    @Column(name = "llegada")
    private LocalDateTime llegada;

    @Column(name = "capacidad")
    private Integer capacidad;

    @Column(name = "altitud")
    private Integer altitud;

    @Column(name = "velocidad")
    private Integer speed;

    @Column(name = "region_origen", length = 50)
    private String regionOrigen;

    @Column(name = "region_destino", length = 50)
    private String regionDestino;

    @Column(name = "total_paquetes")
    private Integer totalPaquetes;

    // Coordenadas de origen
    @Column(name = "origen_latitud")
    private Double origenLatitud;

    @Column(name = "origen_longitud")
    private Double origenLongitud;

    // Coordenadas de destino
    @Column(name = "destino_latitud")
    private Double destinoLatitud;

    @Column(name = "destino_longitud")
    private Double destinoLongitud;

    // Coordenadas actuales del vuelo (se actualizan en tiempo real durante el vuelo)
    @Column(name = "current_latitud")
    private Double currentLatitud;

    @Column(name = "current_longitud")
    private Double currentLongitud;

    @Column(name = "progreso")
    private Double progreso; // Porcentaje de progreso (0-100)

    @Column(name = "en_vuelo")
    private Boolean enVuelo = false; // Indica si el vuelo está actualmente en progreso

    // Lista de pedidos transportados (relación uno a muchos)
    @OneToMany(mappedBy = "rutaSolucion", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<VueloPedido> vuelos = new ArrayList<>();

    /**
     * Agrega un pedido a esta ruta
     */
    public void agregarVuelo(VueloPedido vuelo) {
        vuelos.add(vuelo);
        vuelo.setRutaSolucion(this);
    }

    /**
     * Remueve un pedido de esta ruta
     */
    public void removerVuelo(VueloPedido vuelo) {
        vuelos.remove(vuelo);
        vuelo.setRutaSolucion(null);
    }
}
