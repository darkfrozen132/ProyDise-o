package morapack.planificacion;

import morapack.modelo.Vuelo;
import java.util.List;
import java.util.ArrayList;

/**
 * Clase que representa una ruta completa (puede tener múltiples vuelos)
 */
public class RutaCompleta {
    private List<Vuelo> vuelos;
    private String tipoRuta;
    private List<String> escalas;
    
    public RutaCompleta() {
        this.vuelos = new ArrayList<>();
        this.escalas = new ArrayList<>();
    }
    
    public void agregarVuelo(Vuelo vuelo) {
        vuelos.add(vuelo);
    }
    
    public boolean esViable() {
        return !vuelos.isEmpty();
    }
    
    public String obtenerDescripcion() {
        if (vuelos.isEmpty()) return "SIN RUTA";
        
        StringBuilder sb = new StringBuilder();
        
        switch (tipoRuta) {
            case "DIRECTO":
                Vuelo directo = vuelos.get(0);
                sb.append(String.format("DIRECTO: %s→%s (%s-%s)", 
                         directo.getOrigen(), directo.getDestino(),
                         directo.getHoraSalida(), directo.getHoraLlegada()));
                break;
                
            case "UNA_CONEXION":
                Vuelo v1 = vuelos.get(0);
                Vuelo v2 = vuelos.get(1);
                sb.append(String.format("CONEXIÓN: %s→%s→%s (%s-%s vía %s)", 
                         v1.getOrigen(), escalas.get(0), v2.getDestino(),
                         v1.getHoraSalida(), v2.getHoraLlegada(), escalas.get(0)));
                break;
                
            case "DOS_CONEXIONES":
                Vuelo vx1 = vuelos.get(0);
                Vuelo vx3 = vuelos.get(2);
                sb.append(String.format("2 CONEXIONES: %s→%s→%s→%s (%s-%s)", 
                         vx1.getOrigen(), escalas.get(0), escalas.get(1), vx3.getDestino(),
                         vx1.getHoraSalida(), vx3.getHoraLlegada()));
                break;
        }
        
        return sb.toString();
    }
    
    public int calcularTiempoTotal() {
        if (vuelos.isEmpty()) return Integer.MAX_VALUE;
        
        Vuelo primero = vuelos.get(0);
        Vuelo ultimo = vuelos.get(vuelos.size() - 1);
        
        return convertirHoraAMinutos(ultimo.getHoraLlegada()) - 
               convertirHoraAMinutos(primero.getHoraSalida());
    }
    
    private int convertirHoraAMinutos(String hora) {
        try {
            String[] partes = hora.split(":");
            int horas = Integer.parseInt(partes[0]);
            int minutos = partes.length > 1 ? Integer.parseInt(partes[1]) : 0;
            return horas * 60 + minutos;
        } catch (Exception e) {
            return 720;
        }
    }
    
    // ==================== NUEVOS MÉTODOS PARA COMPATIBILIDAD ====================
    private int cantidadPaquetes = 0; // Para compatibilidad con MainRapidoColonia
    
    public void setCantidadPaquetes(int cantidad) {
        this.cantidadPaquetes = cantidad;
    }
    
    public int getCantidadPaquetes() {
        return cantidadPaquetes;
    }
    
    // Getters y setters
    public List<Vuelo> getVuelos() { return vuelos; }
    public String getTipoRuta() { return tipoRuta; }
    public void setTipoRuta(String tipoRuta) { this.tipoRuta = tipoRuta; }
    public List<String> getEscalas() { return escalas; }
    public void setEscalas(List<String> escalas) { this.escalas = escalas; }
}
