package com.morapack.model;

import java.time.LocalDateTime;

/**
 * Entidad que representa un pedido de cliente
 * Preparada para ser convertida a JPA Entity cuando se integre Spring Boot
 */
public class Pedido {
    
    private String id;
    private String clienteId;
    private String aeropuertoDestinoId;
    private int cantidadProductos;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaLimiteEntrega;
    private int prioridad; // 1=alta, 2=media, 3=baja
    private EstadoPedido estado;
    private String sedeAsignadaId;
    private Double costoEstimado;
    
    // Constructores
    public Pedido() {
        this.fechaCreacion = LocalDateTime.now();
        this.estado = EstadoPedido.PENDIENTE;
    }
    
    public Pedido(String id, String clienteId, String aeropuertoDestinoId, int cantidadProductos, int prioridad) {
        this();
        this.id = id;
        this.clienteId = clienteId;
        this.aeropuertoDestinoId = aeropuertoDestinoId;
        this.cantidadProductos = cantidadProductos;
        this.prioridad = prioridad;
        this.fechaLimiteEntrega = calcularFechaLimite();
    }
    
    // Métodos de negocio
    private LocalDateTime calcularFechaLimite() {
        // Reglas de negocio MoraPack: 3 días máximo por defecto
        return fechaCreacion.plusDays(3);
    }
    
    public void actualizarFechaLimite(boolean mismoContinente) {
        int diasLimite = mismoContinente ? 2 : 3;
        this.fechaLimiteEntrega = fechaCreacion.plusDays(diasLimite);
    }
    
    public double calcularCosto(boolean mismoContinente, double distancia) {
        double costoBase = cantidadProductos * 10.0; // $10 por producto
        double costoDistancia = distancia * 0.1; // $0.1 por km
        
        // Factor por continente
        double factorContinente = mismoContinente ? 1.0 : 1.5;
        
        // Factor por prioridad (alta=3x, media=2x, baja=1x)
        double factorPrioridad = 4.0 - prioridad;
        
        return (costoBase + costoDistancia) * factorContinente * factorPrioridad;
    }
    
    public double calcularUrgencia() {
        long horasRestantes = java.time.Duration.between(LocalDateTime.now(), fechaLimiteEntrega).toHours();
        return (4.0 - prioridad) / Math.max(horasRestantes, 1);
    }
    
    public boolean estaVencido() {
        return LocalDateTime.now().isAfter(fechaLimiteEntrega);
    }
    
    public boolean esFactible(boolean mismoContinente) {
        double tiempoEnvio = mismoContinente ? 0.5 : 1.0;
        LocalDateTime fechaEntregaEstimada = LocalDateTime.now().plusHours((long)(tiempoEnvio * 24));
        return fechaEntregaEstimada.isBefore(fechaLimiteEntrega) || 
               fechaEntregaEstimada.isEqual(fechaLimiteEntrega);
    }
    
    // Getters y setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }
    
    public String getAeropuertoDestinoId() { return aeropuertoDestinoId; }
    public void setAeropuertoDestinoId(String aeropuertoDestinoId) { this.aeropuertoDestinoId = aeropuertoDestinoId; }
    
    public int getCantidadProductos() { return cantidadProductos; }
    public void setCantidadProductos(int cantidadProductos) { this.cantidadProductos = cantidadProductos; }
    
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    
    public LocalDateTime getFechaLimiteEntrega() { return fechaLimiteEntrega; }
    public void setFechaLimiteEntrega(LocalDateTime fechaLimiteEntrega) { this.fechaLimiteEntrega = fechaLimiteEntrega; }
    
    public int getPrioridad() { return prioridad; }
    public void setPrioridad(int prioridad) { this.prioridad = prioridad; }
    
    public EstadoPedido getEstado() { return estado; }
    public void setEstado(EstadoPedido estado) { this.estado = estado; }
    
    public String getSedeAsignadaId() { return sedeAsignadaId; }
    public void setSedeAsignadaId(String sedeAsignadaId) { this.sedeAsignadaId = sedeAsignadaId; }
    
    public Double getCostoEstimado() { return costoEstimado; }
    public void setCostoEstimado(Double costoEstimado) { this.costoEstimado = costoEstimado; }
    
    @Override
    public String toString() {
        return String.format("Pedido[%s] Cliente:%s → %s (%d productos, P%d, %s)", 
                           id, clienteId, aeropuertoDestinoId, cantidadProductos, prioridad, estado);
    }
}
