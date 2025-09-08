package morapack.modelo;

import java.time.LocalTime;

/**
 * Representa un vuelo entre dos aeropuertos en el sistema MoraPack
 */
public class Vuelo {
    private String id;
    private Aeropuerto origen;
    private Aeropuerto destino;
    private LocalTime horaSalida;
    private LocalTime horaLlegada;
    private int capacidad;
    private int duracionMinutos;
    
    // Para construcción desde CSV
    private String codigoOrigen;
    private String codigoDestino;
    
    public Vuelo(Aeropuerto origen, Aeropuerto destino, LocalTime horaSalida, 
                LocalTime horaLlegada, int capacidad) {
        this.origen = origen;
        this.destino = destino;
        this.horaSalida = horaSalida;
        this.horaLlegada = horaLlegada;
        this.capacidad = capacidad;
        this.id = generarId();
        this.duracionMinutos = calcularDuracion();
    }
    
    // Constructor simplificado para CSV
    public Vuelo(String codigoOrigen, String codigoDestino, String horaSalidaStr, 
                String horaLlegadaStr, int capacidad) {
        this.codigoOrigen = codigoOrigen;
        this.codigoDestino = codigoDestino;
        this.horaSalida = LocalTime.parse(horaSalidaStr);
        this.horaLlegada = LocalTime.parse(horaLlegadaStr);
        this.capacidad = capacidad;
        this.id = String.format("%s-%s-%s", codigoOrigen, codigoDestino, horaSalidaStr.replace(":", ""));
        this.duracionMinutos = calcularDuracionSimple();
        // Los aeropuertos se asignarán después cuando estén disponibles
    }
    
    private String generarId() {
        return String.format("%s-%s-%s", 
                           origen.getCodigoICAO(), 
                           destino.getCodigoICAO(),
                           horaSalida.toString().replace(":", ""));
    }
    
    private int calcularDuracion() {
        // Ajustar por husos horarios
        int minutosOrigen = horaSalida.getHour() * 60 + horaSalida.getMinute();
        int minutosDestino = horaLlegada.getHour() * 60 + horaLlegada.getMinute();
        
        // Ajuste por huso horario
        int diferenciaHuso = destino.getHusoHorario() - origen.getHusoHorario();
        minutosDestino -= (diferenciaHuso * 60);
        
        int duracion = minutosDestino - minutosOrigen;
        if (duracion <= 0) {
            duracion += 24 * 60; // Vuelo cruza medianoche
        }
        
        return duracion;
    }
    
    private int calcularDuracionSimple() {
        // Cálculo simple sin considerar husos horarios (para constructor CSV)
        int minutosOrigen = horaSalida.getHour() * 60 + horaSalida.getMinute();
        int minutosDestino = horaLlegada.getHour() * 60 + horaLlegada.getMinute();
        
        int duracion = minutosDestino - minutosOrigen;
        if (duracion <= 0) {
            duracion += 24 * 60; // Vuelo cruza medianoche
        }
        
        return duracion;
    }
    
    public boolean esMismoContinente() {
        if (origen != null && destino != null) {
            return origen.getContinente().equals(destino.getContinente());
        }
        return false;
    }
    
    public int getTiempoEsperaHoras() {
        // Tiempo de manejo en aeropuerto según MoraPack
        return esMismoContinente() ? 2 : 4;
    }
    
    // Getters
    public String getId() { return id; }
    public Aeropuerto getOrigen() { return origen; }
    public Aeropuerto getDestino() { return destino; }
    public LocalTime getHoraSalida() { return horaSalida; }
    public LocalTime getHoraLlegada() { return horaLlegada; }
    public int getCapacidad() { return capacidad; }
    public int getDuracionMinutos() { return duracionMinutos; }
    
    // Para construcción desde CSV
    public String getCodigoOrigen() { return codigoOrigen; }
    public String getCodigoDestino() { return codigoDestino; }
    
    public void setOrigen(Aeropuerto origen) { 
        this.origen = origen; 
        if (origen != null && destino != null) {
            this.duracionMinutos = calcularDuracion();
        }
    }
    
    public void setDestino(Aeropuerto destino) { 
        this.destino = destino; 
        if (origen != null && destino != null) {
            this.duracionMinutos = calcularDuracion();
        }
    }
    
    @Override
    public String toString() {
        if (origen != null && destino != null) {
            return String.format("Vuelo %s→%s [%s-%s] Cap:%d (%dmin)", 
                               origen.getCiudad(), destino.getCiudad(),
                               horaSalida, horaLlegada, capacidad, duracionMinutos);
        } else {
            return String.format("Vuelo %s→%s [%s-%s] Cap:%d (%dmin)", 
                               codigoOrigen, codigoDestino,
                               horaSalida, horaLlegada, capacidad, duracionMinutos);
        }
    }
}
