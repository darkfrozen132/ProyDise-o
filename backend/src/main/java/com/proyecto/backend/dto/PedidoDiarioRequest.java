package com.proyecto.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para crear un nuevo pedido diario
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoDiarioRequest {

    @NotBlank(message = "El ID del cliente es obligatorio")
    @Size(max = 8, message = "El ID del cliente no puede tener más de 8 caracteres")
    private String clienteId;

    @NotBlank(message = "El aeropuerto destino es obligatorio")
    @Size(min = 4, max = 4, message = "El código del aeropuerto debe tener 4 caracteres")
    private String aeropuertoDestinoId;

    @NotNull(message = "La cantidad de productos es obligatoria")
    @Positive(message = "La cantidad de productos debe ser positiva")
    private Integer cantidadProductos;

    @NotNull(message = "El día es obligatorio")
    private Integer dia;

    @NotNull(message = "El mes es obligatorio")
    private Integer mes;

    @NotNull(message = "El año es obligatorio")
    private Integer anio;

    @NotNull(message = "La hora es obligatoria")
    private Integer hora;

    @NotNull(message = "El minuto es obligatorio")
    private Integer minuto;
}
