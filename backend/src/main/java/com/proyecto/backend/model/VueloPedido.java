package com.proyecto.backend.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Representa un pedido individual transportado en una ruta de vuelo.
 * Relaciona los pedidos (orders) con las rutas solución.
 */
@Entity
@Table(name = "vuelo_pedidos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "rutaSolucion")
public class VueloPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, length = 20)
    private String orderId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    // Relación muchos a uno con RutaSolucion
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ruta_solucion_id", nullable = false)
    private RutaSolucion rutaSolucion;

    /**
     * Constructor de conveniencia
     */
    public VueloPedido(String orderId, Integer quantity) {
        this.orderId = orderId;
        this.quantity = quantity;
    }
}
