package com.proyecto.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Entidad que representa un Pedido/Envío SEMANAL en el sistema MoraPack
 * Usado para planificaciones de una semana completa
 * 
 * Formato de archivo: dd-hh-mm-dest-###-IdClien
 * Ejemplo: 30-09-15-SEQM-145-0054321
 */
@Entity
@Table(name = "pedidos_semanal")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class PedidoSemanal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotNull(message = "El año es obligatorio")
    private int anio;

    @Column(nullable = false)
    @NotNull(message = "El mes es obligatorio")
    private int mes;

    @Column(nullable = false)
    @NotNull(message = "El dia es obligatorio")
    private int dia;

    @Column(nullable = false)
    @NotNull(message = "La hora es obligatoria")
    private int hora;

    @Column(nullable = false)
    @NotNull(message = "El minuto es obligatorio")
    private int minuto;

    @Column(name = "aeropuerto_destino_id", nullable = false, length = 4)
    @NotNull(message = "El aeropuerto destino es obligatorio")
    private String aeropuertoDestinoId;

    @Column(name = "cantidad_productos", nullable = false)
    @Positive(message = "La cantidad de productos debe ser positiva")
    private int cantidadProductos;

    @Column(name = "cliente_id", nullable = false, length = 7)
    @NotNull(message = "El ID del cliente es obligatorio")
    private String clienteId;

    /**
     * Constructor con todos los parametros
     */
    public PedidoSemanal(int anio, int mes, int dia, int hora, int minuto, String aeropuertoDestinoId, 
                         int cantidadProductos, String clienteId) {
        this.anio = anio;
        this.mes = mes;
        this.dia = dia;
        this.hora = hora;
        this.minuto = minuto;
        this.aeropuertoDestinoId = aeropuertoDestinoId;
        this.cantidadProductos = cantidadProductos;
        this.clienteId = clienteId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PedidoSemanal)) return false;
        PedidoSemanal pedido = (PedidoSemanal) o;
        return id != null && id.equals(pedido.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
