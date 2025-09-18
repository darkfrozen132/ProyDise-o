package com.morapack.model;

import java.util.List;
import java.util.ArrayList;

/**
 * Entidad que representa una sede de MoraPack
 * Preparada para ser convertida a JPA Entity cuando se integre Spring Boot
 */
public class Sede {
    
    private String id;
    private String nombre;
    private String aeropuertoId; // ID del aeropuerto donde está ubicada la sede
    private double capacidadMaxima;
    private TipoSede tipo;
    private EstadoSede estado;
    private List<String> pedidosAsignadosIds; // IDs de pedidos asignados a esta sede
    
    // Constructores
    public Sede() {
        this.pedidosAsignadosIds = new ArrayList<>();
        this.estado = EstadoSede.ACTIVA;
    }
    
    public Sede(String id, String nombre, String aeropuertoId, double capacidadMaxima, TipoSede tipo) {
        this();
        this.id = id;
        this.nombre = nombre;
        this.aeropuertoId = aeropuertoId;
        this.capacidadMaxima = capacidadMaxima;
        this.tipo = tipo;
    }
    
    public Sede(String id, String nombre, String aeropuertoId, int capacidadOperativa) {
        this(id, nombre, aeropuertoId, capacidadOperativa, TipoSede.DEPOSITO);
    }
    
    public Sede(String id, String nombre, String aeropuertoId) {
        this(id, nombre, aeropuertoId, 100, TipoSede.DEPOSITO);
    }
    
    // Métodos de negocio
    /**
     * Verifica si la sede puede atender un pedido adicional
     */
    public boolean puedeAtender() {
        double cargaActual = pedidosAsignadosIds.size();
        return cargaActual < capacidadMaxima && estado == EstadoSede.ACTIVA;
    }
    
    public boolean puedeAtender(int cantidadPedidos) {
        return estado == EstadoSede.ACTIVA && cantidadPedidos <= capacidadMaxima;
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
        return estado == EstadoSede.ACTIVA;
    }
    
    /**
     * Calcula la distancia aproximada a otro punto geográfico
     * Nota: Requeriría las coordenadas del aeropuerto asociado
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
    
    public TipoSede getTipo() { return tipo; }
    public void setTipo(TipoSede tipo) { this.tipo = tipo; }
    
    public EstadoSede getEstado() { return estado; }
    public void setEstado(EstadoSede estado) { this.estado = estado; }
    
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
