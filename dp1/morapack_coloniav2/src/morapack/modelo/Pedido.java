package morapack.modelo;

import java.time.LocalDateTime;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Representa un pedido de cliente que debe ser enviado desde una sede MoraPack
 * Formato ID: dd-hh-mm-dest-###-IdClien
 * Donde:
 * - dd: días (01-31)
 * - hh: horas (01-23)
 * - mm: minutos (01-59)
 * - dest: código aeropuerto destino (ej: SVMI, SBBR)
 * - ###: cantidad (001-999)
 * - IdClien: ID cliente 7 dígitos (0000001-9999999)
 */
public class Pedido {
    private String id; // Formato: dd-hh-mm-dest-###-IdClien
    private String clienteId; // 7 dígitos con ceros a la izquierda
    private String aeropuertoOrigenId; // Código del aeropuerto origen (ej: SPIM, EBCI, UBBB)
    private String aeropuertoDestinoId; // Código del aeropuerto destino (ej: SVMI, SBBR)
    private int cantidadProductos; // 001-999
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaLimiteEntrega;
    private int prioridad; // 1=alta, 2=media, 3=baja
    private String estado; // PENDIENTE, ASIGNADO, EN_RUTA, ENTREGADO, CANCELADO
    private String sedeAsignadaId; // ID de la sede asignada
    
    // Componentes del ID extraídos
    private int dia;
    private int hora;
    private int minuto;
    
    // Patrón para validar el formato del ID
    private static final Pattern ID_PATTERN = Pattern.compile(
        "^(\\d{2})-(\\d{2})-(\\d{2})-([A-Z]{4})-(\\d{3})-(\\d{7})$"
    );
    
    public Pedido() {
        this.fechaCreacion = LocalDateTime.now();
        this.estado = "PENDIENTE";
    }
    
    public Pedido(String clienteId, String aeropuertoDestinoId, int cantidadProductos, int prioridad) {
        this();
        this.clienteId = formatearClienteId(clienteId);
        this.aeropuertoDestinoId = aeropuertoDestinoId;
        this.cantidadProductos = cantidadProductos;
        this.prioridad = prioridad;
        this.fechaLimiteEntrega = calcularFechaLimite();
        
        // Generar ID basado en fecha actual
        generarId();
    }
    
    public Pedido(String id) {
        this();
        if (validarFormatoId(id)) {
            this.id = id;
            extraerComponentesId();
        } else {
            throw new IllegalArgumentException("Formato de ID inválido: " + id);
        }
    }
    
    /**
     * Genera el ID del pedido en formato: dd-hh-mm-dest-###-IdClien
     */
    private void generarId() {
        LocalDateTime ahora = LocalDateTime.now();
        this.dia = ahora.getDayOfMonth();
        this.hora = ahora.getHour();
        this.minuto = ahora.getMinute();
        
        String dd = String.format("%02d", dia);
        String hh = String.format("%02d", hora);
        String mm = String.format("%02d", minuto);
        String dest = aeropuertoDestinoId;
        String cantidad = String.format("%03d", cantidadProductos);
        String idClien = clienteId;
        
        this.id = String.format("%s-%s-%s-%s-%s-%s", dd, hh, mm, dest, cantidad, idClien);
    }
    
    /**
     * Valida que el ID tenga el formato correcto
     */
    private boolean validarFormatoId(String id) {
        if (id == null) return false;
        Matcher matcher = ID_PATTERN.matcher(id);
        return matcher.matches();
    }
    
    /**
     * Extrae los componentes del ID y los asigna a los campos correspondientes
     */
    private void extraerComponentesId() {
        Matcher matcher = ID_PATTERN.matcher(id);
        if (matcher.matches()) {
            this.dia = Integer.parseInt(matcher.group(1));
            this.hora = Integer.parseInt(matcher.group(2));
            this.minuto = Integer.parseInt(matcher.group(3));
            this.aeropuertoDestinoId = matcher.group(4);
            this.cantidadProductos = Integer.parseInt(matcher.group(5));
            this.clienteId = matcher.group(6);
        }
    }
    
    /**
     * Formatea el ID del cliente a 7 dígitos con ceros a la izquierda
     */
    private String formatearClienteId(String clienteId) {
        if (clienteId == null) return "0000000";
        
        // Remover caracteres no numéricos
        String numerico = clienteId.replaceAll("[^0-9]", "");
        
        // Convertir a número y formatear
        try {
            int numero = Integer.parseInt(numerico);
            return String.format("%07d", numero);
        } catch (NumberFormatException e) {
            return "0000000";
        }
    }
    
    /**
     * Crea un ID de pedido personalizado
     */
    public static String crearId(int dia, int hora, int minuto, String aeropuerto, 
                                int cantidad, String clienteId) {
        String dd = String.format("%02d", Math.max(1, Math.min(31, dia)));
        String hh = String.format("%02d", Math.max(1, Math.min(23, hora)));
        String mm = String.format("%02d", Math.max(1, Math.min(59, minuto)));
        String dest = aeropuerto != null ? aeropuerto : "XXXX";
        String cant = String.format("%03d", Math.max(1, Math.min(999, cantidad)));
        String client = clienteId != null ? String.format("%07d", 
                       Integer.parseInt(clienteId.replaceAll("[^0-9]", ""))) : "0000000";
        
        return String.format("%s-%s-%s-%s-%s-%s", dd, hh, mm, dest, cant, client);
    }
    
    // Getters adicionales para componentes del ID
    public int getDia() { return dia; }
    public int getHora() { return hora; }
    public int getMinuto() { return minuto; }
    
    /**
     * Obtiene la fecha y hora del pedido basada en los componentes del ID
     */
    public LocalDateTime getFechaPedido() {
        return LocalDateTime.of(fechaCreacion.getYear(), fechaCreacion.getMonth(), 
                               dia, hora, minuto);
    }
    
    /**
     * Valida si el formato del ID es correcto
     */
    public static boolean esFormatoValido(String id) {
        if (id == null) return false;
        return ID_PATTERN.matcher(id).matches();
    }
    
    /**
     * Extrae el código del aeropuerto destino del ID
     */
    public static String extraerAeropuertoDestino(String id) {
        Matcher matcher = ID_PATTERN.matcher(id);
        return matcher.matches() ? matcher.group(4) : null;
    }
    
    /**
     * Extrae la cantidad de productos del ID
     */
    public static int extraerCantidad(String id) {
        Matcher matcher = ID_PATTERN.matcher(id);
        return matcher.matches() ? Integer.parseInt(matcher.group(5)) : 0;
    }
    
    /**
     * Extrae el ID del cliente del ID del pedido
     */
    public static String extraerClienteId(String id) {
        Matcher matcher = ID_PATTERN.matcher(id);
        return matcher.matches() ? matcher.group(6) : null;
    }
    
    private LocalDateTime calcularFechaLimite() {
        // Reglas de negocio MoraPack:
        // - Mismo continente: 2 días máximo
        // - Diferente continente: 3 días máximo
        return fechaCreacion.plusDays(3); // Por defecto 3 días
    }
    
    /**
     * Actualiza la fecha límite basada en la sede asignada
     */
    public void actualizarFechaLimite(boolean mismoContinente) {
        int diasLimite = mismoContinente ? 2 : 3;
        this.fechaLimiteEntrega = fechaCreacion.plusDays(diasLimite);
    }
    
    /**
     * Calcula el costo de envío desde una sede específica
     */
    public double calcularCosto(boolean mismoContinente, double distancia) {
        double costoBase = cantidadProductos * 10.0; // $10 por producto
        double costoDistancia = distancia * 0.1; // $0.1 por km
        
        // Factor por continente
        double factorContinente = mismoContinente ? 1.0 : 1.5;
        
        // Factor por prioridad (alta=3x, media=2x, baja=1x)
        double factorPrioridad = 4.0 - prioridad;
        
        return (costoBase + costoDistancia) * factorContinente * factorPrioridad;
    }
    
    /**
     * Calcula la urgencia del pedido (mayor valor = más urgente)
     */
    public double calcularUrgencia() {
        long horasRestantes = java.time.Duration.between(LocalDateTime.now(), fechaLimiteEntrega).toHours();
        return (4.0 - prioridad) / Math.max(horasRestantes, 1);
    }
    
    public boolean estaVencido() {
        return LocalDateTime.now().isAfter(fechaLimiteEntrega);
    }
    
    /**
     * Verifica si el pedido puede ser enviado desde una sede cumpliendo plazos
     */
    public boolean esFactible(boolean mismoContinente) {
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
    public void setId(String id) { this.id = id; }
    
    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }
    
    public String getAeropuertoDestinoId() { return aeropuertoDestinoId; }
    public void setAeropuertoDestinoId(String aeropuertoDestinoId) { this.aeropuertoDestinoId = aeropuertoDestinoId; }
    
    public String getAeropuertoOrigenId() { return aeropuertoOrigenId; }
    public void setAeropuertoOrigenId(String aeropuertoOrigenId) { this.aeropuertoOrigenId = aeropuertoOrigenId; }
    
    public int getCantidadProductos() { return cantidadProductos; }
    public void setCantidadProductos(int cantidadProductos) { this.cantidadProductos = cantidadProductos; }
    
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    
    public LocalDateTime getFechaLimiteEntrega() { return fechaLimiteEntrega; }
    public void setFechaLimiteEntrega(LocalDateTime fechaLimiteEntrega) { this.fechaLimiteEntrega = fechaLimiteEntrega; }
    
    public int getPrioridad() { return prioridad; }
    public void setPrioridad(int prioridad) { this.prioridad = prioridad; }
    
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    
    public String getSedeAsignadaId() { return sedeAsignadaId; }
    public void setSedeAsignadaId(String sedeAsignadaId) { this.sedeAsignadaId = sedeAsignadaId; }
    
    @Override
    public String toString() {
        return String.format("Pedido[%s] %02d:%02d del día %02d → %s (%03d productos, P%d, %s)", 
                           id, hora, minuto, dia, aeropuertoDestinoId, cantidadProductos, prioridad, estado);
    }
}
