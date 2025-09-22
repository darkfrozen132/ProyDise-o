package morapack.planificacion;

import morapack.modelo.Aeropuerto;

/**
 * Representa una ruta optimizada individual
 */
public class RutaOptimizada {
    private String pedidoId;
    private Aeropuerto origen;
    private Aeropuerto destino;
    private int cantidadProductos;
    private int prioridad;
    private int horaEstimada;
    private double distanciaGMT;
    
    public RutaOptimizada() {}
    
    // Getters y setters
    public String getPedidoId() { return pedidoId; }
    public void setPedidoId(String pedidoId) { this.pedidoId = pedidoId; }
    
    public Aeropuerto getOrigen() { return origen; }
    public void setOrigen(Aeropuerto origen) { this.origen = origen; }
    
    public Aeropuerto getDestino() { return destino; }
    public void setDestino(Aeropuerto destino) { this.destino = destino; }
    
    public int getCantidadProductos() { return cantidadProductos; }
    public void setCantidadProductos(int cantidadProductos) { this.cantidadProductos = cantidadProductos; }
    
    public int getPrioridad() { return prioridad; }
    public void setPrioridad(int prioridad) { this.prioridad = prioridad; }
    
    public int getHoraEstimada() { return horaEstimada; }
    public void setHoraEstimada(int horaEstimada) { this.horaEstimada = horaEstimada; }
    
    public double getDistanciaGMT() { return distanciaGMT; }
    public void setDistanciaGMT(double distanciaGMT) { this.distanciaGMT = distanciaGMT; }
    
    @Override
    public String toString() {
        return String.format("Ruta[%s: %s→%s, %d prod, %02d:00, %.0f u]", 
                           pedidoId, 
                           origen != null ? origen.getCodigoICAO() : "NULL",
                           destino != null ? destino.getCodigoICAO() : "NULL",
                           cantidadProductos, horaEstimada, distanciaGMT);
    }
}
