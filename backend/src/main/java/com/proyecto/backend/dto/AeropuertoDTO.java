package com.proyecto.backend.dto;

import com.proyecto.backend.model.Aeropuerto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para transferir datos de aeropuertos al frontend
 * Formato específico requerido por el cliente React
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AeropuertoDTO {
    
    private String codigoICAO;
    private String ciudad;
    private String pais;
    private int husoHorario;
    private int capacidadAlmacen;
    private double latitud;
    private double longitud;
    private String continente;
    private int capacidadDisponible;

    /**
     * Convierte una entidad Aeropuerto a DTO
     */
    public static AeropuertoDTO fromEntity(Aeropuerto aeropuerto) {
        if (aeropuerto == null) {
            return null;
        }
        
        return new AeropuertoDTO(
            aeropuerto.getCodigoICAO(),
            aeropuerto.getCiudad(),
            aeropuerto.getPais(),
            aeropuerto.getHusoHorario(),
            aeropuerto.getCapacidadAlmacen(),
            aeropuerto.getLatitud(),
            aeropuerto.getLongitud(),
            aeropuerto.getContinente(),
            aeropuerto.getCapacidadDisponible()
        );
    }
}
