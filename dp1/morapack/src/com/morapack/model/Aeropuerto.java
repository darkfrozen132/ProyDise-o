package com.morapack.model;

/**
 * Entidad que representa un aeropuerto
 * Preparada para ser convertida a JPA Entity cuando se integre Spring Boot
 */
public class Aeropuerto {
    
    private String id; // Código ICAO
    private String codigoICAO;
    private String ciudad;
    private String pais;
    private String codigoCorto;
    private int husoHorario; // GMT offset
    private int capacidadAlmacen;
    private String continente;
    private double latitud;
    private double longitud;
    private boolean esSede = false;
    
    // Constructores
    public Aeropuerto() {}
    
    public Aeropuerto(String codigoICAO, String ciudad, String pais, String continente, double latitud, double longitud) {
        this.id = codigoICAO;
        this.codigoICAO = codigoICAO;
        this.ciudad = ciudad;
        this.pais = pais;
        this.continente = continente;
        this.latitud = latitud;
        this.longitud = longitud;
        this.codigoCorto = codigoICAO.substring(0, Math.min(3, codigoICAO.length()));
        this.husoHorario = 0; // Valor por defecto
        this.capacidadAlmacen = 1000; // Valor por defecto
    }
    
    public Aeropuerto(String codigoICAO, String ciudad, String pais, String continente) {
        this(codigoICAO, ciudad, pais, continente, 0.0, 0.0);
    }
    
    public Aeropuerto(String codigoICAO, String ciudad, String pais, String codigoCorto, 
                     int husoHorario, int capacidadAlmacen, double latitud, double longitud) {
        this.id = codigoICAO;
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
    
    // Métodos de negocio
    private String determinarContinente() {
        if (codigoICAO == null || codigoICAO.isEmpty()) return "Desconocido";
        
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
     * Calcula la distancia en kilómetros a otro aeropuerto usando la fórmula de Haversine
     */
    public double calcularDistancia(Aeropuerto otro) {
        if (otro == null) return Double.MAX_VALUE;
        
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
    
    public boolean mismoContinente(Aeropuerto otro) {
        return otro != null && this.continente.equals(otro.continente);
    }
    
    // Getters y setters
    public String getId() { return id; }
    public void setId(String id) { 
        this.id = id; 
        this.codigoICAO = id; // Mantener sincronizado
    }
    
    public String getCodigo() { return codigoICAO; }
    public String getCodigoICAO() { return codigoICAO; }
    public void setCodigoICAO(String codigoICAO) { 
        this.codigoICAO = codigoICAO;
        this.id = codigoICAO; // Mantener sincronizado
    }
    
    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }
    
    public String getPais() { return pais; }
    public void setPais(String pais) { this.pais = pais; }
    
    public String getCodigoCorto() { return codigoCorto; }
    public void setCodigoCorto(String codigoCorto) { this.codigoCorto = codigoCorto; }
    
    public int getHusoHorario() { return husoHorario; }
    public void setHusoHorario(int husoHorario) { this.husoHorario = husoHorario; }
    
    public int getCapacidadAlmacen() { return capacidadAlmacen; }
    public void setCapacidadAlmacen(int capacidadAlmacen) { this.capacidadAlmacen = capacidadAlmacen; }
    
    public String getContinente() { return continente; }
    public void setContinente(String continente) { this.continente = continente; }
    
    public double getLatitud() { return latitud; }
    public void setLatitud(double latitud) { this.latitud = latitud; }
    
    public double getLongitud() { return longitud; }
    public void setLongitud(double longitud) { this.longitud = longitud; }
    
    public boolean esSede() { return esSede; }
    public void setSede(boolean esSede) { this.esSede = esSede; }
    
    @Override
    public String toString() {
        return String.format("Aeropuerto[%s] %s, %s (%s)", 
                           codigoICAO, ciudad, pais, continente);
    }
}
