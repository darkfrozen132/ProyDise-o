package morapack.datos.modelos;

import java.util.Objects;
import java.util.Set;
import java.util.HashSet;

/**
 * Representa un aeropuerto en el sistema de distribución MoraPack.
 * Contiene información sobre ubicación, capacidad y características operativas.
 */
public class Aeropuerto {

    private final String codigoICAO;
    private final String ciudad;
    private final String pais;
    private final String codigoCorto;
    private final int husoHorario;
    private final int capacidadAlmacen;
    private final double latitud;
    private final double longitud;
    private final Continente continente;

    // Capacidad actual disponible (variable durante ejecución)
    private int capacidadDisponible;

    // Sedes principales con stock ilimitado
    private static final Set<String> SEDES_PRINCIPALES = Set.of("SPIM", "EBCI", "UBBB");

    /**
     * Constructor completo para un aeropuerto
     * @param codigoICAO Código ICAO de 4 letras
     * @param ciudad Nombre de la ciudad
     * @param pais Nombre del país
     * @param codigoCorto Código interno abreviado
     * @param husoHorario Huso horario GMT
     * @param capacidadAlmacen Capacidad máxima del almacén
     * @param latitud Coordenada de latitud
     * @param longitud Coordenada de longitud
     * @param continente Continente al que pertenece
     */
    public Aeropuerto(String codigoICAO, String ciudad, String pais, String codigoCorto,
                     int husoHorario, int capacidadAlmacen, double latitud, double longitud,
                     Continente continente) {

        // Validaciones
        if (codigoICAO == null || codigoICAO.trim().length() != 4) {
            throw new IllegalArgumentException("Código ICAO debe tener exactamente 4 caracteres");
        }
        if (ciudad == null || ciudad.trim().isEmpty()) {
            throw new IllegalArgumentException("Ciudad no puede estar vacía");
        }
        if (pais == null || pais.trim().isEmpty()) {
            throw new IllegalArgumentException("País no puede estar vacío");
        }
        if (capacidadAlmacen < 0) {
            throw new IllegalArgumentException("Capacidad del almacén no puede ser negativa");
        }
        if (continente == null) {
            throw new IllegalArgumentException("Continente no puede ser null");
        }

        this.codigoICAO = codigoICAO.trim().toUpperCase();
        this.ciudad = ciudad.trim();
        this.pais = pais.trim();
        this.codigoCorto = codigoCorto != null ? codigoCorto.trim().toLowerCase() : "";
        this.husoHorario = husoHorario;
        this.capacidadAlmacen = capacidadAlmacen;
        this.latitud = latitud;
        this.longitud = longitud;
        this.continente = continente;

        // Inicializar capacidad disponible igual a la máxima
        this.capacidadDisponible = capacidadAlmacen;
    }

    /**
     * Constructor desde línea CSV
     * @param lineaCSV Línea del archivo aeropuertos.csv
     * @return Nueva instancia de Aeropuerto
     * @throws IllegalArgumentException si el formato es incorrecto
     */
    public static Aeropuerto desdeCSV(String lineaCSV) {
        if (lineaCSV == null || lineaCSV.trim().isEmpty()) {
            throw new IllegalArgumentException("Línea CSV no puede estar vacía");
        }

        String[] campos = lineaCSV.split(",");
        if (campos.length != 9) {
            throw new IllegalArgumentException("Línea CSV debe tener 9 campos: " + lineaCSV);
        }

        try {
            String icao = campos[0].trim();
            String ciudad = campos[1].trim();
            String pais = campos[2].trim();
            String codigo = campos[3].trim();
            int huso = Integer.parseInt(campos[4].trim());
            int capacidad = Integer.parseInt(campos[5].trim());
            double lat = Double.parseDouble(campos[6].trim());
            double lon = Double.parseDouble(campos[7].trim());
            Continente cont = Continente.porCodigo(campos[8].trim());

            return new Aeropuerto(icao, ciudad, pais, codigo, huso, capacidad, lat, lon, cont);

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Error al parsear números en línea CSV: " + lineaCSV, e);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Error al crear aeropuerto desde CSV: " + lineaCSV, e);
        }
    }

    // Getters
    public String getCodigoICAO() {
        return codigoICAO;
    }

    public String getCiudad() {
        return ciudad;
    }

    public String getPais() {
        return pais;
    }

    public String getCodigoCorto() {
        return codigoCorto;
    }

    public int getHusoHorario() {
        return husoHorario;
    }

    public int getCapacidadAlmacen() {
        return capacidadAlmacen;
    }

    public double getLatitud() {
        return latitud;
    }

    public double getLongitud() {
        return longitud;
    }

    public Continente getContinente() {
        return continente;
    }

    public int getCapacidadDisponible() {
        return capacidadDisponible;
    }

    /**
     * Calcula el porcentaje de ocupación del almacén
     * @return Porcentaje de ocupación (0-100)
     */
    public double getPorcentajeOcupacion() {
        if (capacidadAlmacen == 0) {
            return 0.0;
        }
        return ((double) (capacidadAlmacen - capacidadDisponible) / capacidadAlmacen) * 100.0;
    }

    /**
     * Verifica si es una sede principal de MoraPack
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
     * Verifica si puede almacenar una cantidad específica de productos
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
     * Reserva capacidad en el almacén
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
     * Reinicia la capacidad disponible a la máxima
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
     * Verifica si está en el mismo continente que otro aeropuerto
     * @param otro Otro aeropuerto
     * @return true si están en el mismo continente
     */
    public boolean esMismoContinente(Aeropuerto otro) {
        if (otro == null) {
            return false;
        }
        return this.continente.esMismoContinente(otro.continente);
    }

    /**
     * Obtiene información completa del aeropuerto
     * @return String con información detallada
     */
    public String getInformacionCompleta() {
        return String.format("%s - %s, %s (%s) [%s] - Capacidad: %d/%d",
            codigoICAO, ciudad, pais, continente.getCodigo(),
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

    @Override
    public String toString() {
        return String.format("%s (%s, %s)", codigoICAO, ciudad, pais);
    }
}