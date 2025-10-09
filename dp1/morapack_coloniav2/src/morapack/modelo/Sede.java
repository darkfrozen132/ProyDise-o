package morapack.modelo;

import java.util.List;
import java.util.ArrayList;

/**
 * Representa una sede de MoraPack que puede procesar pedidos
 */
public class Sede {
    private String id;
    private String nombre;
    private String aeropuertoId; // ID del aeropuerto donde está ubicada la sede
    private double capacidadMaxima;
    private String tipo; // PRINCIPAL, DEPOSITO
    private String estado; // ACTIVA, INACTIVA, MANTENIMIENTO
    private List<String> pedidosAsignadosIds; // IDs de pedidos asignados a esta sede
    
    public Sede() {
        this.pedidosAsignadosIds = new ArrayList<>();
        this.estado = "ACTIVA";
        this.tipo = "DEPOSITO";
    }
    
    public Sede(String id, String nombre, String aeropuertoId, double capacidadMaxima, String tipo) {
        this();
        this.id = id;
        this.nombre = nombre;
        this.aeropuertoId = aeropuertoId;
        this.capacidadMaxima = capacidadMaxima;
        this.tipo = tipo;
    }
    
    public Sede(String id, String nombre, String aeropuertoId, int capacidadOperativa) {
        this(id, nombre, aeropuertoId, capacidadOperativa, "DEPOSITO");
    }
    
    public Sede(String id, String nombre, String aeropuertoId) {
        this(id, nombre, aeropuertoId, 100, "DEPOSITO");
    }
    
    /**
     * Verifica si la sede puede atender un pedido adicional
     */
    public boolean puedeAtender() {
        double cargaActual = pedidosAsignadosIds.size();
        return cargaActual < capacidadMaxima && "ACTIVA".equals(estado);
    }
    
    public boolean puedeAtender(int cantidadPedidos) {
        return "ACTIVA".equals(estado) && cantidadPedidos <= capacidadMaxima;
    }
    
    /**
     * Asigna un pedido a esta sede
     */
    public boolean asignarPedido(String pedidoId) {
        if (puedeAtender() && !pedidosAsignadosIds.contains(pedidoId)) {
            pedidosAsignadosIds.add(pedidoId);
            return true;
        }
        return false;
    }
    
    /**
     * Libera un pedido de esta sede
     */
    public boolean liberarPedido(String pedidoId) {
        return pedidosAsignadosIds.remove(pedidoId);
    }
    
    /**
     * Calcula el porcentaje de carga actual
     */
    public double calcularPorcentajeCarga() {
        return (pedidosAsignadosIds.size() / capacidadMaxima) * 100.0;
    }
    
    /**
     * Verifica si la sede está disponible para operaciones
     */
    public boolean estaActiva() {
        return "ACTIVA".equals(estado);
    }
    
    /**
     * Calcula la distancia aproximada a otro punto geográfico
     */
    public double calcularDistancia(double lat2, double lon2) {
        // Por ahora retorna una distancia por defecto
        // En una implementación completa, obtendría las coordenadas del aeropuerto
        return 1000.0; 
    }
    
    // Getters y setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    
    public String getAeropuertoId() { return aeropuertoId; }
    public void setAeropuertoId(String aeropuertoId) { this.aeropuertoId = aeropuertoId; }
    
    public double getCapacidadMaxima() { return capacidadMaxima; }
    public void setCapacidadMaxima(double capacidadMaxima) { this.capacidadMaxima = capacidadMaxima; }
    
    // Compatibilidad con código existente
    public int getCapacidadOperativa() { return (int) capacidadMaxima; }
    public void setCapacidadOperativa(int capacidadOperativa) { this.capacidadMaxima = capacidadOperativa; }
    
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    
    public List<String> getPedidosAsignadosIds() { return new ArrayList<>(pedidosAsignadosIds); }
    public void setPedidosAsignadosIds(List<String> pedidosAsignadosIds) { 
        this.pedidosAsignadosIds = pedidosAsignadosIds != null ? new ArrayList<>(pedidosAsignadosIds) : new ArrayList<>(); 
    }
    
    public int getCantidadPedidosAsignados() { return pedidosAsignadosIds.size(); }
    
    @Override
    public String toString() {
        return String.format("Sede[%s] %s en %s (Cap: %.0f, %s, %s)", 
                           id, nombre, aeropuertoId, capacidadMaxima, tipo, estado);
    }
}
