package morapack.modelo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Representa una instancia específica de un vuelo en una fecha determinada.
 * Cada plan de vuelo (plantilla) se puede instanciar múltiples veces para diferentes días,
 * manteniendo capacidad y asignaciones independientes para cada instancia.
 */
public class VueloInstancia {
    
    // Identificadores
    private String idInstancia; // Formato: {plantillaId}-{YYYY-MM-DD}
    private String idPlantilla; // ID del plan de vuelo base
    private LocalDate fechaVuelo; // Fecha específica de esta instancia
    
    // Información heredada de la plantilla
    private String origen; // Código ICAO aeropuerto origen
    private String destino; // Código ICAO aeropuerto destino
    private LocalTime horaSalidaLocal; // Hora salida en zona local del origen
    private LocalTime horaLlegadaLocal; // Hora llegada en zona local del destino
    private int capacidadMaxima; // Capacidad máxima del avión
    
    // Tiempos en UTC (calculados con zonas horarias)
    private ZonedDateTime horaSalidaUTC;
    private ZonedDateTime horaLlegadaUTC;
    
    // Control de capacidad por fecha
    private int capacidadUsada; // Productos ya asignados
    private int capacidadDisponible; // Capacidad restante
    private Map<String, Integer> productosPorPedido; // Seguimiento por pedido
    
    // Estado de la instancia
    private String estado; // PROGRAMADO, EN_CURSO, COMPLETADO, CANCELADO
    private String aerolinea; // Aerolínea operadora
    private String numeroVuelo; // Número de vuelo
    
    // Metadatos
    private boolean esIntercontinental;
    private double distanciaKm;
    private int duracionMinutos;
    
    /**
     * Constructor principal
     */
    public VueloInstancia(Vuelo plantilla, LocalDate fecha, ZoneId zonaOrigen, ZoneId zonaDestino) {
        
        // Generar identificadores
        this.idPlantilla = generarIdPlantilla(plantilla);
        this.fechaVuelo = fecha;
        this.idInstancia = String.format("%s-%s", idPlantilla, fecha.toString());
        
        // Copiar información de la plantilla
        this.origen = plantilla.getOrigen();
        this.destino = plantilla.getDestino();
        this.capacidadMaxima = plantilla.getCapacidad();
        this.capacidadUsada = 0;
        this.capacidadDisponible = this.capacidadMaxima;
        
        // Parsear horarios locales
        this.horaSalidaLocal = LocalTime.parse(plantilla.getHoraSalida());
        this.horaLlegadaLocal = LocalTime.parse(plantilla.getHoraLlegada());
        
        // Calcular horarios UTC
        calcularHorariosUTC(fecha, zonaOrigen, zonaDestino);
        
        // Inicializar estado
        this.estado = "PROGRAMADO";
        this.productosPorPedido = new HashMap<>();
        
        // Extraer información adicional del ID de plantilla si está disponible
        extraerInformacionVuelo();
        
        // Calcular duración
        this.duracionMinutos = (int) java.time.Duration.between(
            horaSalidaUTC.toLocalDateTime(), 
            horaLlegadaUTC.toLocalDateTime()
        ).toMinutes();
    }
    
    /**
     * Genera un ID único para la plantilla de vuelo
     */
    private String generarIdPlantilla(Vuelo plantilla) {
        // Formato: ORIGEN-DESTINO-HSALIDA
        return String.format("%s-%s-%s", 
            plantilla.getOrigen(), 
            plantilla.getDestino(), 
            plantilla.getHoraSalida().replace(":", "")
        );
    }
    
    /**
     * Calcula los horarios UTC considerando zonas horarias y posibles cambios de día
     */
    private void calcularHorariosUTC(LocalDate fecha, ZoneId zonaOrigen, ZoneId zonaDestino) {
        
        // Hora de salida UTC
        LocalDateTime salidaLocal = LocalDateTime.of(fecha, horaSalidaLocal);
        this.horaSalidaUTC = ZonedDateTime.of(salidaLocal, zonaOrigen).withZoneSameInstant(ZoneId.of("UTC"));
        
        // Hora de llegada UTC (considerar cambio de día)
        LocalDateTime llegadaLocal = LocalDateTime.of(fecha, horaLlegadaLocal);
        
        // Si la hora de llegada es menor que la de salida, el vuelo llega al día siguiente
        if (horaLlegadaLocal.isBefore(horaSalidaLocal)) {
            llegadaLocal = llegadaLocal.plusDays(1);
        }
        
        this.horaLlegadaUTC = ZonedDateTime.of(llegadaLocal, zonaDestino).withZoneSameInstant(ZoneId.of("UTC"));
    }
    
    /**
     * Extrae información adicional del vuelo si está disponible en el ID
     */
    private void extraerInformacionVuelo() {
        // Por defecto, usar códigos genéricos
        this.aerolinea = "MORA"; // MoraPack Air
        this.numeroVuelo = String.format("MR%03d", Math.abs(idPlantilla.hashCode() % 1000));
    }
    
    /**
     * Intenta asignar productos al vuelo
     */
    public boolean asignarProductos(String pedidoId, int cantidadProductos) {
        
        if (cantidadProductos <= 0) return false;
        if (capacidadDisponible < cantidadProductos) return false;
        
        // Asignar productos
        this.capacidadUsada += cantidadProductos;
        this.capacidadDisponible -= cantidadProductos;
        
        // Registrar por pedido
        productosPorPedido.put(pedidoId, 
            productosPorPedido.getOrDefault(pedidoId, 0) + cantidadProductos);
        
        return true;
    }
    
    /**
     * Libera productos del vuelo
     */
    public boolean liberarProductos(String pedidoId, int cantidadProductos) {
        
        Integer asignados = productosPorPedido.get(pedidoId);
        if (asignados == null || asignados < cantidadProductos) return false;
        
        // Liberar productos
        this.capacidadUsada -= cantidadProductos;
        this.capacidadDisponible += cantidadProductos;
        
        // Actualizar registro por pedido
        int nuevaCantidad = asignados - cantidadProductos;
        if (nuevaCantidad > 0) {
            productosPorPedido.put(pedidoId, nuevaCantidad);
        } else {
            productosPorPedido.remove(pedidoId);
        }
        
        return true;
    }
    
    /**
     * Verifica si el vuelo puede partir en el momento especificado
     */
    public boolean puedePartirEn(ZonedDateTime momento) {
        return momento.isBefore(horaSalidaUTC) || momento.isEqual(horaSalidaUTC);
    }
    
    /**
     * Verifica si el vuelo llegará antes del momento especificado
     */
    public boolean llegaraAntesDe(ZonedDateTime momento) {
        return horaLlegadaUTC.isBefore(momento) || horaLlegadaUTC.isEqual(momento);
    }
    
    /**
     * Calcula el porcentaje de ocupación
     */
    public double getPorcentajeOcupacion() {
        return capacidadMaxima > 0 ? (double) capacidadUsada / capacidadMaxima * 100.0 : 0.0;
    }
    
    /**
     * Verifica si el vuelo está lleno
     */
    public boolean estaLleno() {
        return capacidadDisponible <= 0;
    }
    
    /**
     * Verifica si el vuelo está vacío
     */
    public boolean estaVacio() {
        return capacidadUsada == 0;
    }
    
    /**
     * Obtiene el número de pedidos diferentes en el vuelo
     */
    public int getNumeroPedidosDistintos() {
        return productosPorPedido.size();
    }
    
    /**
     * Verifica si es un vuelo de consolidación
     */
    public boolean esVueloConsolidado() {
        return getNumeroPedidosDistintos() > 1;
    }
    
    /**
     * Marca el vuelo como en curso
     */
    public void marcarEnCurso() {
        if ("PROGRAMADO".equals(this.estado)) {
            this.estado = "EN_CURSO";
        }
    }
    
    /**
     * Marca el vuelo como completado
     */
    public void marcarCompletado() {
        if ("EN_CURSO".equals(this.estado)) {
            this.estado = "COMPLETADO";
        }
    }
    
    /**
     * Marca el vuelo como cancelado
     */
    public void marcarCancelado() {
        this.estado = "CANCELADO";
        // Liberar toda la capacidad
        this.capacidadUsada = 0;
        this.capacidadDisponible = this.capacidadMaxima;
        this.productosPorPedido.clear();
    }
    
    // Getters y Setters
    public String getIdInstancia() { return idInstancia; }
    public String getIdPlantilla() { return idPlantilla; }
    public LocalDate getFechaVuelo() { return fechaVuelo; }
    
    public String getOrigen() { return origen; }
    public String getDestino() { return destino; }
    public LocalTime getHoraSalidaLocal() { return horaSalidaLocal; }
    public LocalTime getHoraLlegadaLocal() { return horaLlegadaLocal; }
    
    public ZonedDateTime getHoraSalidaUTC() { return horaSalidaUTC; }
    public ZonedDateTime getHoraLlegadaUTC() { return horaLlegadaUTC; }
    
    public int getCapacidadMaxima() { return capacidadMaxima; }
    public int getCapacidadUsada() { return capacidadUsada; }
    public int getCapacidadDisponible() { return capacidadDisponible; }
    
    public String getEstado() { return estado; }
    public String getAerolinea() { return aerolinea; }
    public String getNumeroVuelo() { return numeroVuelo; }
    
    public boolean isEsIntercontinental() { return esIntercontinental; }
    public void setEsIntercontinental(boolean esIntercontinental) { this.esIntercontinental = esIntercontinental; }
    
    public double getDistanciaKm() { return distanciaKm; }
    public void setDistanciaKm(double distanciaKm) { this.distanciaKm = distanciaKm; }
    
    public int getDuracionMinutos() { return duracionMinutos; }
    
    public Map<String, Integer> getProductosPorPedido() { return new HashMap<>(productosPorPedido); }
    
    @Override
    public String toString() {
        return String.format("VueloInstancia[%s] %s→%s %s (%s) - %d/%d productos - %s", 
            numeroVuelo, origen, destino, fechaVuelo, 
            horaSalidaLocal, capacidadUsada, capacidadMaxima, estado);
    }
    
    /**
     * Información detallada para debugging y reportes
     */
    public String toStringDetallado() {
        return String.format(
            "VueloInstancia[%s]\n" +
            "  ID: %s\n" +
            "  Plantilla: %s\n" +
            "  Fecha: %s\n" +
            "  Ruta: %s → %s\n" +
            "  Salida local: %s (UTC: %s)\n" +
            "  Llegada local: %s (UTC: %s)\n" +
            "  Duración: %d minutos\n" +
            "  Capacidad: %d/%d (%.1f%% ocupado)\n" +
            "  Pedidos: %d diferentes\n" +
            "  Estado: %s\n" +
            "  Consolidado: %s",
            numeroVuelo, idInstancia, idPlantilla, fechaVuelo,
            origen, destino,
            horaSalidaLocal, horaSalidaUTC.toLocalTime(),
            horaLlegadaLocal, horaLlegadaUTC.toLocalTime(),
            duracionMinutos,
            capacidadUsada, capacidadMaxima, getPorcentajeOcupacion(),
            getNumeroPedidosDistintos(),
            estado,
            esVueloConsolidado() ? "SÍ" : "NO"
        );
    }
}
