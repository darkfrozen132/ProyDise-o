package com.proyecto.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;
import java.util.Set;

/**
 * Representa un aeropuerto en el sistema de distribucion.
 * Contiene informacion sobre ubicacion, capacidad y caracterosticas operativas.
 */
@Entity
@Table(name = "aeropuertos")
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
public class Aeropuerto {

    @Id
    @Column(name = "codigo_icao", length = 4, nullable = false)
    private String codigoICAO;

    @Column(name = "ciudad", nullable = false, length = 100)
    private String ciudad;

    @Column(name = "pais", nullable = false, length = 100)
    private String pais;

    @Column(name = "huso_horario", nullable = false)
    private int husoHorario;

    @Column(name = "capacidad_almacen", nullable = false)
    private int capacidadAlmacen;

    @Column(name = "latitud", nullable = false)
    private double latitud;

    @Column(name = "longitud", nullable = false)
    private double longitud;

    @Column(name = "continente", nullable = false, length = 50)
    private String continente;

    // Capacidad actual disponible (variable durante ejecucion)
    @Transient
    private int capacidadDisponible;

    // Sedes principales con stock ilimitado
    private static final Set<String> SEDES_PRINCIPALES = Set.of("SPIM", "EBCI", "UBBB");

    /**
     * Constructor completo para un aeropuerto
     * @param codigoICAO Codigo ICAO de 4 letras
     * @param ciudad Nombre de la ciudad
     * @param pais Nombre del pais
     * @param husoHorario Huso horario GMT
     * @param capacidadAlmacen Capacidad maxima del almacen
     * @param latitud Coordenada de latitud
     * @param longitud Coordenada de longitud
     * @param continente Continente al que pertenece
     */
    public Aeropuerto(String codigoICAO, String ciudad, String pais,
                     int husoHorario, int capacidadAlmacen, double latitud, double longitud,
                     String continente) {

        // Validaciones
        if (codigoICAO == null || codigoICAO.trim().length() != 4) {
            throw new IllegalArgumentException("Codigo ICAO debe tener exactamente 4 caracteres");
        }
        if (ciudad == null || ciudad.trim().isEmpty()) {
            throw new IllegalArgumentException("Ciudad no puede estar vacia");
        }
        if (pais == null || pais.trim().isEmpty()) {
            throw new IllegalArgumentException("Pa�s no puede estar vacio");
        }
        if (capacidadAlmacen < 0) {
            throw new IllegalArgumentException("Capacidad del almacen no puede ser negativa");
        }
        if (continente == null || continente.trim().isEmpty()) {
            throw new IllegalArgumentException("Continente no puede estar vacio");
        }

        this.codigoICAO = codigoICAO.trim().toUpperCase();
        this.ciudad = ciudad.trim();
        this.pais = pais.trim();
        this.husoHorario = husoHorario;
        this.capacidadAlmacen = capacidadAlmacen;
        this.latitud = latitud;
        this.longitud = longitud;
        this.continente = continente.trim();

        // Inicializar capacidad disponible igual a la maxima
        this.capacidadDisponible = capacidadAlmacen;
    }

    

    /**
     * Calcula el porcentaje de ocupacion del almacen
     * @return Porcentaje de ocupacion (0-100)
     */
    public double calcularPorcentajeOcupacion() {
        if (capacidadAlmacen == 0) {
            return 0.0;
        }
        return ((double) (capacidadAlmacen - capacidadDisponible) / capacidadAlmacen) * 100.0;
    }

    /**
     * Verifica si es una sede principal
     * @return true si es sede principal (Lima, Bruselas, Baku)
     */
    public boolean esSedePrincipal() {
        return SEDES_PRINCIPALES.contains(codigoICAO);
    }

    /**
     * Verifica si tiene stock ilimitado (sedes principales)
     * @return true si tiene stock ilimitado
     */
    public boolean tieneStockIlimitado() {
        return esSedePrincipal();
    }

    /**
     * Verifica si puede almacenar una cantidad especifica de productos
     * @param cantidad Cantidad de productos a almacenar
     * @return true si hay capacidad suficiente
     */
    public boolean puedeAlmacenar(int cantidad) {
        if (tieneStockIlimitado()) {
            return true; // Sedes principales tienen capacidad ilimitada
        }
        return capacidadDisponible >= cantidad;
    }

    /**
     * Reserva capacidad en el almacen
     * @param cantidad Cantidad a reservar
     * @return true si se pudo reservar
     */
    public boolean reservarCapacidad(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("Cantidad no puede ser negativa");
        }

        if (tieneStockIlimitado()) {
            return true; // Siempre se puede reservar en sedes principales
        }

        if (capacidadDisponible >= cantidad) {
            capacidadDisponible -= cantidad;
            return true;
        }

        return false;
    }

    /**
     * Libera capacidad previamente reservada
     * @param cantidad Cantidad a liberar
     */
    public void liberarCapacidad(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("Cantidad no puede ser negativa");
        }

        if (!tieneStockIlimitado()) {
            capacidadDisponible = Math.min(capacidadAlmacen, capacidadDisponible + cantidad);
        }
    }

    /**
     * Reinicia la capacidad disponible a la maxima
     */
    public void reiniciarCapacidad() {
        this.capacidadDisponible = capacidadAlmacen;
    }

    /**
     * Calcula la distancia euclidiana a otro aeropuerto
     * @param otro Otro aeropuerto
     * @return Distancia en grados (aproximada)
     */
    public double calcularDistancia(Aeropuerto otro) {
        if (otro == null) {
            throw new IllegalArgumentException("Aeropuerto no puede ser null");
        }

        double deltaLat = this.latitud - otro.latitud;
        double deltaLon = this.longitud - otro.longitud;

        return Math.sqrt(deltaLat * deltaLat + deltaLon * deltaLon);
    }

    /**
     * Verifica si esta en el mismo continente que otro aeropuerto
     * @param otro Otro aeropuerto
     * @return true si estan en el mismo continente
     */
    public boolean esMismoContinente(Aeropuerto otro) {
        if (otro == null) {
            return false;
        }
        return this.continente.equalsIgnoreCase(otro.continente);
    }

    /**
     * Obtiene informacion completa del aeropuerto
     * @return String con informacion detallada
     */
    public String obtenerInformacionCompleta() {
        return String.format("%s - %s, %s (%s) [%s] - Capacidad: %d/%d",
            codigoICAO, ciudad, pais, continente,
            esSedePrincipal() ? "SEDE" : "DESTINO",
            capacidadDisponible, capacidadAlmacen);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Aeropuerto that = (Aeropuerto) obj;
        return Objects.equals(codigoICAO, that.codigoICAO);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigoICAO);
    }
}
