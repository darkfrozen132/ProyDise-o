package morapack.modelo;

/**
 * Representa un aeropuerto con sus características para el sistema MoraPack
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
    private boolean esSede = false;
    
    // Constructores
    public Aeropuerto() {}
    
    public Aeropuerto(String codigoICAO, String ciudad, String pais, String continente) {
        this.id = codigoICAO;
        this.codigoICAO = codigoICAO;
        this.ciudad = ciudad;
        this.pais = pais;
        this.continente = continente;
        this.codigoCorto = codigoICAO.substring(0, Math.min(3, codigoICAO.length()));
        this.husoHorario = 0; // Valor por defecto
        this.capacidadAlmacen = 1000; // Valor por defecto
    }
    
    public Aeropuerto(String codigoICAO, String ciudad, String pais, String codigoCorto, 
                     int husoHorario, int capacidadAlmacen) {
        this.id = codigoICAO;
        this.codigoICAO = codigoICAO;
        this.ciudad = ciudad;
        this.pais = pais;
        this.codigoCorto = codigoCorto;
        this.husoHorario = husoHorario;
        this.capacidadAlmacen = capacidadAlmacen;
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
     * Calcula una medida de distancia basada en la diferencia de huso horario
     * Mayor diferencia de GMT = mayor "distancia" temporal
     */
    public double calcularDistancia(Aeropuerto otro) {
        if (otro == null) return Double.MAX_VALUE;
        
        // Diferencia absoluta entre husos horarios
        int diferenciaGMT = Math.abs(this.husoHorario - otro.husoHorario);
        
        // Convertir a una medida de distancia (cada hora de diferencia = ~1000km aprox)
        return diferenciaGMT * 1000.0;
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
    
    public boolean esSede() { return esSede; }
    public void setSede(boolean esSede) { this.esSede = esSede; }
    
    @Override
    public String toString() {
        return String.format("Aeropuerto[%s] %s, %s (%s)", 
                           codigoICAO, ciudad, pais, continente);
    }
}
