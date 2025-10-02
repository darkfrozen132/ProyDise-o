package morapack.modelo;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Representa un micro-pedido individual (unidad 1) dentro de un pedido mayor.
 * Cada pedido original se divide en múltiples productos para permitir
 * distribución granular y cálculo preciso de tiempos de entrega.
 * 
 * El pedido completo se considera entregado cuando llega el último producto.
 */
public class Producto {
    
    // Identificadores
    private String id; // Formato: {pedidoId}-PROD-{secuencial}
    private String pedidoOrigenId; // ID del pedido original
    private int numeroSecuencial; // Número dentro del pedido (1, 2, 3...)
    private int totalProductosEnPedido; // Total de productos en el pedido original
    
    // Ubicación y destino
    private String aeropuertoOrigenId; // Código ICAO del aeropuerto origen
    private String aeropuertoDestinoId; // Código ICAO del aeropuerto destino
    private String sedeAsignadaId; // Sede MoraPack asignada
    
    // Tiempos (todos en UTC para consistencia)
    private ZonedDateTime horaCreacionUTC; // Cuándo se creó el producto (UTC)
    private ZonedDateTime horaMinimaPartidaUTC; // Cuándo puede partir el producto (UTC)
    private ZonedDateTime horaMaximaLlegadaUTC; // Límite de llegada (UTC)
    private ZonedDateTime horaPartidaRealUTC; // Cuándo partió realmente (UTC)
    private ZonedDateTime horaLlegadaRealUTC; // Cuándo llegó realmente (UTC)
    
    // Información del vuelo asignado
    private String vueloAsignadoId; // ID del vuelo asignado
    private LocalDateTime fechaVuelo; // Fecha específica del vuelo
    private String tipoRuta; // DIRECTO, ESCALA, DIVIDIDO, INTERCONTINENTAL
    
    // Estado del producto
    private String estado; // PENDIENTE, ASIGNADO, EN_VUELO, ENTREGADO, FALLIDO
    
    // Metadatos para seguimiento
    private String rutaCompleta; // Descripción de la ruta asignada
    private int numeroEscalas; // Cantidad de escalas en la ruta
    private String observaciones; // Notas adicionales
    
    /**
     * Constructor para crear un producto a partir de un pedido
     */
    public Producto(Pedido pedidoOriginal, int numeroSecuencial, int totalProductos, 
                   ZoneId zonaOrigenAeropuerto, ZoneId zonaDestinoAeropuerto) {
        
        this.pedidoOrigenId = pedidoOriginal.getId();
        this.numeroSecuencial = numeroSecuencial;
        this.totalProductosEnPedido = totalProductos;
        this.aeropuertoOrigenId = pedidoOriginal.getAeropuertoOrigenId();
        this.aeropuertoDestinoId = pedidoOriginal.getAeropuertoDestinoId();
        this.estado = "PENDIENTE";
        this.tipoRuta = "PENDIENTE_ASIGNACION";
        this.numeroEscalas = 0;
        
        // Generar ID único del producto
        this.id = String.format("%s-PROD-%04d", pedidoOrigenId, numeroSecuencial);
        
        // Convertir tiempos del pedido original a UTC
        convertirTiemposAUTC(pedidoOriginal, zonaOrigenAeropuerto, zonaDestinoAeropuerto);
    }
    
    /**
     * Convierte los tiempos del pedido original a UTC considerando zonas horarias
     */
    private void convertirTiemposAUTC(Pedido pedido, ZoneId zonaOrigen, ZoneId zonaDestino) {
        
        // Hora de creación del producto (usando zona del origen)
        LocalDateTime fechaCreacion = pedido.getFechaCreacion();
        this.horaCreacionUTC = ZonedDateTime.of(fechaCreacion, zonaOrigen).withZoneSameInstant(ZoneId.of("UTC"));
        
        // Hora mínima de partida: hora del pedido + tiempo preparación (zona origen)
        LocalDateTime fechaPedido = LocalDateTime.of(
            fechaCreacion.getYear(), 
            fechaCreacion.getMonth(),
            pedido.getDia(), 
            pedido.getHora(), 
            pedido.getMinuto()
        );
        
        // Agregar 30 minutos de preparación
        LocalDateTime fechaMinimaPartida = fechaPedido.plusMinutes(30);
        this.horaMinimaPartidaUTC = ZonedDateTime.of(fechaMinimaPartida, zonaOrigen).withZoneSameInstant(ZoneId.of("UTC"));
        
        // Hora máxima de llegada (zona destino)
        LocalDateTime fechaLimite = pedido.getFechaLimiteEntrega();
        this.horaMaximaLlegadaUTC = ZonedDateTime.of(fechaLimite, zonaDestino).withZoneSameInstant(ZoneId.of("UTC"));
    }
    
    /**
     * Asigna un vuelo específico al producto
     */
    public void asignarVuelo(String vueloId, LocalDateTime fechaVuelo, String tipoRuta, 
                           ZonedDateTime horaSalidaUTC, ZonedDateTime horaLlegadaUTC) {
        this.vueloAsignadoId = vueloId;
        this.fechaVuelo = fechaVuelo;
        this.tipoRuta = tipoRuta;
        this.horaPartidaRealUTC = horaSalidaUTC;
        this.horaLlegadaRealUTC = horaLlegadaUTC;
        this.estado = "ASIGNADO";
        
        // Generar descripción de ruta
        actualizarDescripcionRuta();
    }
    
    /**
     * Marca el producto como en vuelo
     */
    public void marcarEnVuelo() {
        if ("ASIGNADO".equals(this.estado)) {
            this.estado = "EN_VUELO";
        }
    }
    
    /**
     * Marca el producto como entregado
     */
    public void marcarEntregado() {
        if ("EN_VUELO".equals(this.estado)) {
            this.estado = "ENTREGADO";
        }
    }
    
    /**
     * Marca el producto como fallido
     */
    public void marcarFallido(String razon) {
        this.estado = "FALLIDO";
        this.observaciones = "FALLO: " + razon;
    }
    
    /**
     * Verifica si el producto puede partir en el tiempo especificado
     */
    public boolean puedePartirEn(ZonedDateTime momentoSalida) {
        return momentoSalida.isAfter(this.horaMinimaPartidaUTC) || 
               momentoSalida.isEqual(this.horaMinimaPartidaUTC);
    }
    
    /**
     * Verifica si el producto llegará a tiempo
     */
    public boolean llegaraATiempo(ZonedDateTime momentoLlegada) {
        return momentoLlegada.isBefore(this.horaMaximaLlegadaUTC) || 
               momentoLlegada.isEqual(this.horaMaximaLlegadaUTC);
    }
    
    /**
     * Calcula si el producto está retrasado
     */
    public boolean estaRetrasado() {
        if (horaLlegadaRealUTC == null) return false;
        return horaLlegadaRealUTC.isAfter(horaMaximaLlegadaUTC);
    }
    
    /**
     * Obtiene el tiempo de retraso en minutos
     */
    public long getMinutosRetraso() {
        if (!estaRetrasado()) return 0;
        return java.time.Duration.between(horaMaximaLlegadaUTC, horaLlegadaRealUTC).toMinutes();
    }
    
    /**
     * Verifica si es el último producto del pedido
     */
    public boolean esUltimoProducto() {
        return numeroSecuencial == totalProductosEnPedido;
    }
    
    /**
     * Actualiza la descripción de ruta basada en el tipo
     */
    private void actualizarDescripcionRuta() {
        switch (tipoRuta) {
            case "DIRECTO":
                rutaCompleta = String.format("%s → %s (DIRECTO)", aeropuertoOrigenId, aeropuertoDestinoId);
                numeroEscalas = 0;
                break;
            case "ESCALA":
                rutaCompleta = String.format("%s → ... → %s (CON ESCALAS)", aeropuertoOrigenId, aeropuertoDestinoId);
                break;
            case "DIVIDIDO":
                rutaCompleta = String.format("%s → %s (PEDIDO DIVIDIDO %d/%d)", 
                    aeropuertoOrigenId, aeropuertoDestinoId, numeroSecuencial, totalProductosEnPedido);
                break;
            case "INTERCONTINENTAL":
                rutaCompleta = String.format("%s → %s (INTERCONTINENTAL)", aeropuertoOrigenId, aeropuertoDestinoId);
                break;
            default:
                rutaCompleta = String.format("%s → %s (%s)", aeropuertoOrigenId, aeropuertoDestinoId, tipoRuta);
        }
    }
    
    // Getters y Setters
    public String getId() { return id; }
    public String getPedidoOrigenId() { return pedidoOrigenId; }
    public int getNumeroSecuencial() { return numeroSecuencial; }
    public int getTotalProductosEnPedido() { return totalProductosEnPedido; }
    
    public String getAeropuertoOrigenId() { return aeropuertoOrigenId; }
    public String getAeropuertoDestinoId() { return aeropuertoDestinoId; }
    public String getSedeAsignadaId() { return sedeAsignadaId; }
    public void setSedeAsignadaId(String sedeAsignadaId) { this.sedeAsignadaId = sedeAsignadaId; }
    
    public ZonedDateTime getHoraCreacionUTC() { return horaCreacionUTC; }
    public ZonedDateTime getHoraMinimaPartidaUTC() { return horaMinimaPartidaUTC; }
    public ZonedDateTime getHoraMaximaLlegadaUTC() { return horaMaximaLlegadaUTC; }
    public ZonedDateTime getHoraPartidaRealUTC() { return horaPartidaRealUTC; }
    public ZonedDateTime getHoraLlegadaRealUTC() { return horaLlegadaRealUTC; }
    
    public String getVueloAsignadoId() { return vueloAsignadoId; }
    public LocalDateTime getFechaVuelo() { return fechaVuelo; }
    public String getTipoRuta() { return tipoRuta; }
    public void setTipoRuta(String tipoRuta) { this.tipoRuta = tipoRuta; }
    
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    
    public String getRutaCompleta() { return rutaCompleta; }
    public int getNumeroEscalas() { return numeroEscalas; }
    public void setNumeroEscalas(int numeroEscalas) { this.numeroEscalas = numeroEscalas; }
    
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    
    @Override
    public String toString() {
        return String.format("Producto[%s] %s → %s (%s) - %s", 
            id, aeropuertoOrigenId, aeropuertoDestinoId, tipoRuta, estado);
    }
    
    /**
     * Información detallada para debugging
     */
    public String toStringDetallado() {
        return String.format(
            "Producto[%s]\n" +
            "  Pedido origen: %s (%d/%d)\n" +
            "  Ruta: %s\n" +
            "  Estado: %s\n" +
            "  Vuelo: %s (Fecha: %s)\n" +
            "  Partida mín UTC: %s\n" +
            "  Llegada máx UTC: %s\n" +
            "  Partida real UTC: %s\n" +
            "  Llegada real UTC: %s\n" +
            "  Retraso: %s minutos",
            id, pedidoOrigenId, numeroSecuencial, totalProductosEnPedido,
            rutaCompleta, estado, vueloAsignadoId, fechaVuelo,
            horaMinimaPartidaUTC, horaMaximaLlegadaUTC,
            horaPartidaRealUTC, horaLlegadaRealUTC,
            estaRetrasado() ? getMinutosRetraso() : "0"
        );
    }
}
