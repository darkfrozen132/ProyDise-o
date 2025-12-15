package com.proyecto.backend.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Representa un pedido individual transportado en una ruta de vuelo.
 * Relaciona los pedidos (orders) con las rutas solución.
 * 
 * Corresponde a los elementos del array "orders" en el JSON del SSE:
 * {
 *   "orderId": "Ped123",
 *   "cantidad": 50
 * }
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

    /**
     * ID del pedido (ej: "Ped123")
     * Corresponde a "orderId" en el JSON del SSE
     */
    @Column(name = "order_id", nullable = false, length = 20)
    private String orderId;

    /**
     * Cantidad de productos/paquetes en este pedido
     * Corresponde a "cantidad" en el JSON del SSE
     */
    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    // Relación muchos a uno con RutaSolucion
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ruta_solucion_id", nullable = false)
    private RutaSolucion rutaSolucion;

    /**
     * Constructor de conveniencia
     */
    public VueloPedido(String orderId, Integer cantidad) {
        this.orderId = orderId;
        this.cantidad = cantidad;
    }
    
    /**
     * Getter legacy para quantity - usa getCantidad() en su lugar
     * @deprecated
     */
    @Deprecated
    public Integer getQuantity() {
        return cantidad;
    }
    
    /**
     * Setter legacy para quantity - usa setCantidad() en su lugar
     * @deprecated
     */
    @Deprecated
    public void setQuantity(Integer quantity) {
        this.cantidad = quantity;
    }
}
