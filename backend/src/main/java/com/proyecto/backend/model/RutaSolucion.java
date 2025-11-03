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

    /**
     * ID completo del vuelo (ej: "UA123_D0_2025-01-15T00:00")
     * Corresponde al campo "id" en el JSON del SSE
     */
    @Column(name = "id_vuelo", nullable = false, length = 100, unique = true)
    private String idVuelo;

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
    private Integer altitude;

    @Column(name = "region_origen", length = 50)
    private String regionOrigin;

    @Column(name = "region_destino", length = 50)
    private String regionDestination;

    // ==================== COORDENADAS RUTA ====================
    
    /**
     * Coordenadas de origen (lat/lng)
     * Corresponde a "ruta.origin" en el JSON del SSE
     */
    @Column(name = "origen_latitud")
    private Double originLat;

    @Column(name = "origen_longitud")
    private Double originLng;

    /**
     * Coordenadas de destino (lat/lng)
     * Corresponde a "ruta.destination" en el JSON del SSE
     */
    @Column(name = "destino_latitud")
    private Double destinationLat;

    @Column(name = "destino_longitud")
    private Double destinationLng;

    // ==================== COORDENADAS ACTUALES (TRACKING EN TIEMPO REAL) ====================
    
    /**
     * Coordenadas actuales del vuelo (se actualizan en tiempo real durante el vuelo)
     * Estas coordenadas se interpolan entre origin y destination según el progreso
     */
    @Column(name = "current_latitud")
    private Double currentLat;

    @Column(name = "current_longitud")
    private Double currentLng;

    /**
     * Porcentaje de progreso del vuelo (0.0 a 100.0)
     * Se calcula según el tiempo transcurrido entre salida y llegada
     */
    @Column(name = "progreso")
    private Double progreso;

    /**
     * Indica si el vuelo está actualmente en progreso
     */
    @Column(name = "en_vuelo")
    private Boolean enVuelo = false;

    // ==================== RELACIÓN CON PEDIDOS (ORDERS) ====================
    
    /**
     * Lista de pedidos transportados en este vuelo
     * Corresponde al array "orders" en el JSON del SSE
     * Cada VueloPedido tiene orderId y cantidad
     */
    @OneToMany(mappedBy = "rutaSolucion", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<VueloPedido> orders = new ArrayList<>();

    /**
     * Agrega un pedido a este vuelo (order)
     */
    public void agregarOrder(VueloPedido order) {
        orders.add(order);
        order.setRutaSolucion(this);
    }

    /**
     * Remueve un pedido de este vuelo (order)
     */
    public void removerOrder(VueloPedido order) {
        orders.remove(order);
        order.setRutaSolucion(null);
    }
    
    /**
     * Método legacy - usa agregarOrder en su lugar
     * @deprecated
     */
    @Deprecated
    public void agregarVuelo(VueloPedido vuelo) {
        agregarOrder(vuelo);
    }

    /**
     * Método legacy - usa removerOrder en su lugar
     * @deprecated
     */
    @Deprecated
    public void removerVuelo(VueloPedido vuelo) {
        removerOrder(vuelo);
    }
}
