package morapack.modelo;

/**
 * Representa un aeropuerto con sus características para el sistema MoraPack
 */
public class Aeropuerto {
    private String codigoICAO;
    private String ciudad;
    private String pais;
    private String codigoCorto;
    private int husoHorario; // GMT offset
    private int capacidadAlmacen;
    private String continente;
    private double latitud;
    private double longitud;
    
    public Aeropuerto(String codigoICAO, String ciudad, String pais, String codigoCorto, 
                     int husoHorario, int capacidadAlmacen, double latitud, double longitud) {
        this.codigoICAO = codigoICAO;
        this.ciudad = ciudad;
        this.pais = pais;
        this.codigoCorto = codigoCorto;
        this.husoHorario = husoHorario;
        this.capacidadAlmacen = capacidadAlmacen;
        this.latitud = latitud;
        this.longitud = longitud;
        this.continente = determinarContinente();
    }
    
    private String determinarContinente() {
        // Determinar continente basado en código ICAO
        char primera = codigoICAO.charAt(0);
        switch (primera) {
            case 'S': return "América";    // Sudamérica
            case 'K': case 'C': case 'P': return "América";  // Norteamérica  
            case 'E': case 'L': case 'U': return "Europa";   // Europa
            case 'O': case 'V': case 'Z': return "Asia";     // Asia
            default: return "Desconocido";
        }
    }
    
    /**
     * Verifica si este aeropuerto es una sede de MoraPack
     */
    public boolean esSede() {
        return "Lima".equals(ciudad) || "Bruselas".equals(ciudad) || "Baku".equals(ciudad);
    }
    
    /**
     * Calcula la distancia en kilómetros a otro aeropuerto usando la fórmula de Haversine
     */
    public double calcularDistancia(Aeropuerto otro) {
        final double R = 6371; // Radio de la Tierra en km
        
        double lat1Rad = Math.toRadians(this.latitud);
        double lat2Rad = Math.toRadians(otro.latitud);
        double deltaLat = Math.toRadians(otro.latitud - this.latitud);
        double deltaLon = Math.toRadians(otro.longitud - this.longitud);
        
        double a = Math.sin(deltaLat/2) * Math.sin(deltaLat/2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLon/2) * Math.sin(deltaLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        
        return R * c;
    }
    
    // Getters
    public String getCodigoICAO() { return codigoICAO; }
    public String getCiudad() { return ciudad; }
    public String getPais() { return pais; }
    public String getCodigoCorto() { return codigoCorto; }
    public int getHusoHorario() { return husoHorario; }
    public int getCapacidadAlmacen() { return capacidadAlmacen; }
    public String getContinente() { return continente; }
    public double getLatitud() { return latitud; }
    public double getLongitud() { return longitud; }
    
    public void setLatitud(double latitud) { this.latitud = latitud; }
    public void setLongitud(double longitud) { this.longitud = longitud; }
    
    @Override
    public String toString() {
        return String.format("%s (%s) - %s, %s [Capacidad: %d]", 
                           codigoICAO, ciudad, pais, continente, capacidadAlmacen);
    }
}
