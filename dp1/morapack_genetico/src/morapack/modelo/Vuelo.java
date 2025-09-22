package morapack.modelo;

/**
 * Representa un vuelo en el sistema MoraPack
 */
public class Vuelo {
    private String origen;
    private String destino; 
    private String horaSalida;
    private String horaLlegada;
    private int capacidad;
    
    public Vuelo() {}
    
    public Vuelo(String origen, String destino, String horaSalida, String horaLlegada, int capacidad) {
        this.origen = origen;
        this.destino = destino;
        this.horaSalida = horaSalida;
        this.horaLlegada = horaLlegada;
        this.capacidad = capacidad;
    }
    
    // Getters y setters
    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }
    
    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }
    
    public String getHoraSalida() { return horaSalida; }
    public void setHoraSalida(String horaSalida) { this.horaSalida = horaSalida; }
    
    public String getHoraLlegada() { return horaLlegada; }
    public void setHoraLlegada(String horaLlegada) { this.horaLlegada = horaLlegada; }
    
    public int getCapacidad() { return capacidad; }
    public void setCapacidad(int capacidad) { this.capacidad = capacidad; }
    
    @Override
    public String toString() {
        return String.format("Vuelo[%s→%s] %s-%s (Cap: %d)", 
                           origen, destino, horaSalida, horaLlegada, capacidad);
    }
}
