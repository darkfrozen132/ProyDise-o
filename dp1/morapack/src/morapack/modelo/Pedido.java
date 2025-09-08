package morapack.modelo;

import java.time.LocalDateTime;

/**
 * Representa un pedido de cliente que debe ser enviado desde una sede MoraPack
 */
public class Pedido {
    private String id;
    private String clienteId;
    private Aeropuerto destino;
    private int cantidadProductos;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaLimiteEntrega;
    private int prioridad; // 1=alta, 2=media, 3=baja
    
    public Pedido(String id, String clienteId, Aeropuerto destino, int cantidadProductos, int prioridad) {
        this.id = id;
        this.clienteId = clienteId;
        this.destino = destino;
        this.cantidadProductos = cantidadProductos;
        this.prioridad = prioridad;
        this.fechaCreacion = LocalDateTime.now();
        this.fechaLimiteEntrega = calcularFechaLimite();
    }
    
    private LocalDateTime calcularFechaLimite() {
        // Reglas de negocio MoraPack:
        // - Mismo continente: 2 días máximo
        // - Diferente continente: 3 días máximo
        
        // Por ahora asumimos 3 días máximo por defecto
        return fechaCreacion.plusDays(3);
    }
    
    /**
     * Actualiza la fecha límite basada en la sede asignada
     */
    public void actualizarFechaLimite(Aeropuerto sede) {
        boolean mismoContinente = sede.getContinente().equals(destino.getContinente());
        int diasLimite = mismoContinente ? 2 : 3;
        this.fechaLimiteEntrega = fechaCreacion.plusDays(diasLimite);
    }
    
    /**
     * Calcula el costo de envío desde una sede específica
     */
    public double calcularCosto(Aeropuerto sede, double distancia) {
        boolean mismoContinente = sede.getContinente().equals(destino.getContinente());
        
        // Factores de costo según especificaciones MoraPack:
        double costoBase = cantidadProductos * 10.0; // $10 por producto base
        double costoDistancia = distancia * 0.1; // $0.1 por km
        double factorContinente = mismoContinente ? 1.0 : 1.5; // 50% más caro intercontinental
        double factorPrioridad = 4.0 - prioridad; // Prioridad alta cuesta más
        
        return (costoBase + costoDistancia) * factorContinente * factorPrioridad;
    }
    
    /**
     * Verifica si el pedido puede ser enviado desde una sede cumpliendo plazos
     */
    public boolean esFactible(Aeropuerto sede) {
        boolean mismoContinente = sede.getContinente().equals(destino.getContinente());
        
        // Tiempo de envío según reglas MoraPack:
        // - Mismo continente: 0.5 días (12 horas)
        // - Diferente continente: 1 día (24 horas)
        double tiempoEnvio = mismoContinente ? 0.5 : 1.0;
        
        LocalDateTime fechaEntregaEstimada = LocalDateTime.now().plusHours((long)(tiempoEnvio * 24));
        return fechaEntregaEstimada.isBefore(fechaLimiteEntrega) || 
               fechaEntregaEstimada.isEqual(fechaLimiteEntrega);
    }
    
    // Getters y setters
    public String getId() { return id; }
    public String getClienteId() { return clienteId; }
    public Aeropuerto getDestino() { return destino; }
    public int getCantidadProductos() { return cantidadProductos; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getFechaLimiteEntrega() { return fechaLimiteEntrega; }
    public int getPrioridad() { return prioridad; }
    
    @Override
    public String toString() {
        return String.format("Pedido[%s] Cliente:%s → %s (%d productos, P%d)", 
                           id, clienteId, destino.getCiudad(), cantidadProductos, prioridad);
    }
}
