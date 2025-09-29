package morapack.datos.modelos;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Representa un cliente en el sistema MoraPack.
 * Mantiene información del cliente y su historial de pedidos.
 */
public class Cliente {

    private final String idCliente;
    private String nombre;
    private String contacto;
    private TipoCliente tipo;
    private EstadoCliente estado;

    // Historial y estadísticas
    private List<String> historialPedidos;
    private LocalDateTime fechaRegistro;
    private LocalDateTime ultimoPedido;
    private int totalPedidos;
    private int totalProductos;

    /**
     * Tipos de cliente según su volumen de pedidos
     */
    public enum TipoCliente {
        NUEVO("Cliente nuevo"),
        REGULAR("Cliente regular"),
        FRECUENTE("Cliente frecuente"),
        PREMIUM("Cliente premium"),
        CORPORATIVO("Cliente corporativo");

        private final String descripcion;

        TipoCliente(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    /**
     * Estados posibles de un cliente
     */
    public enum EstadoCliente {
        ACTIVO("Activo"),
        INACTIVO("Inactivo"),
        SUSPENDIDO("Suspendido"),
        VIP("VIP");

        private final String descripcion;

        EstadoCliente(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    /**
     * Constructor para crear un cliente
     * @param idCliente ID único del cliente (7 dígitos)
     */
    public Cliente(String idCliente) {
        validarIdCliente(idCliente);

        this.idCliente = idCliente;
        this.nombre = "Cliente " + idCliente; // Nombre por defecto
        this.contacto = "";
        this.tipo = TipoCliente.NUEVO;
        this.estado = EstadoCliente.ACTIVO;

        this.historialPedidos = new ArrayList<>();
        this.fechaRegistro = LocalDateTime.now();
        this.ultimoPedido = null;
        this.totalPedidos = 0;
        this.totalProductos = 0;
    }

    /**
     * Constructor completo
     * @param idCliente ID único del cliente
     * @param nombre Nombre del cliente
     * @param contacto Información de contacto
     */
    public Cliente(String idCliente, String nombre, String contacto) {
        this(idCliente);
        this.nombre = nombre != null && !nombre.trim().isEmpty() ? nombre.trim() : "Cliente " + idCliente;
        this.contacto = contacto != null ? contacto.trim() : "";
    }

    /**
     * Valida el formato del ID de cliente
     * @param idCliente ID a validar
     * @throws IllegalArgumentException si el ID no es válido
     */
    private void validarIdCliente(String idCliente) {
        if (idCliente == null) {
            throw new IllegalArgumentException("ID de cliente no puede ser null");
        }

        String id = idCliente.trim();
        if (id.length() != 7) {
            throw new IllegalArgumentException("ID de cliente debe tener exactamente 7 dígitos: " + idCliente);
        }

        if (!id.matches("\\d{7}")) {
            throw new IllegalArgumentException("ID de cliente debe ser numérico: " + idCliente);
        }
    }

    /**
     * Registra un nuevo pedido para el cliente
     * @param pedido Pedido a registrar
     */
    public void registrarPedido(Pedido pedido) {
        if (pedido == null) {
            throw new IllegalArgumentException("Pedido no puede ser null");
        }

        // Verificar que el pedido pertenece a este cliente
        if (!pedido.getIdCliente().equals(this.idCliente)) {
            throw new IllegalArgumentException("El pedido no pertenece a este cliente");
        }

        // Agregar al historial
        historialPedidos.add(pedido.getIdPedido());

        // Actualizar estadísticas
        totalPedidos++;
        totalProductos += pedido.getCantidadProductos();
        ultimoPedido = pedido.getTiempoPedido();

        // Actualizar tipo de cliente basado en historial
        actualizarTipoCliente();
    }

    /**
     * Actualiza el tipo de cliente basado en su historial
     */
    private void actualizarTipoCliente() {
        if (totalPedidos >= 50 || totalProductos >= 5000) {
            tipo = TipoCliente.CORPORATIVO;
        } else if (totalPedidos >= 20 || totalProductos >= 2000) {
            tipo = TipoCliente.PREMIUM;
        } else if (totalPedidos >= 10 || totalProductos >= 500) {
            tipo = TipoCliente.FRECUENTE;
        } else if (totalPedidos >= 3) {
            tipo = TipoCliente.REGULAR;
        }
        // Si tiene menos de 3 pedidos, sigue siendo NUEVO
    }

    /**
     * Calcula el promedio de productos por pedido
     * @return Promedio de productos por pedido
     */
    public double getPromedioProductosPorPedido() {
        if (totalPedidos == 0) {
            return 0.0;
        }
        return (double) totalProductos / totalPedidos;
    }

    /**
     * Verifica si el cliente es considerado VIP
     * @return true si es VIP
     */
    public boolean esVIP() {
        return estado == EstadoCliente.VIP || tipo == TipoCliente.PREMIUM || tipo == TipoCliente.CORPORATIVO;
    }

    /**
     * Verifica si el cliente está activo
     * @return true si está activo
     */
    public boolean estaActivo() {
        return estado == EstadoCliente.ACTIVO || estado == EstadoCliente.VIP;
    }

    /**
     * Calcula días desde el último pedido
     * @return Días desde último pedido, -1 si no tiene pedidos
     */
    public long diasDesdeUltimoPedido() {
        if (ultimoPedido == null) {
            return -1;
        }
        return java.time.Duration.between(ultimoPedido, LocalDateTime.now()).toDays();
    }

    /**
     * Verifica si el cliente necesita seguimiento (inactivo por mucho tiempo)
     * @param diasLimite Días límite para considerar inactivo
     * @return true si necesita seguimiento
     */
    public boolean necesitaSeguimiento(int diasLimite) {
        long diasInactivo = diasDesdeUltimoPedido();
        return diasInactivo > diasLimite && diasInactivo != -1;
    }

    /**
     * Obtiene la prioridad del cliente para planificación
     * @return Prioridad (1 = más alta, 5 = más baja)
     */
    public int getPrioridad() {
        switch (tipo) {
            case CORPORATIVO:
                return 1;
            case PREMIUM:
                return 2;
            case FRECUENTE:
                return 3;
            case REGULAR:
                return 4;
            case NUEVO:
            default:
                return 5;
        }
    }

    /**
     * Suspende el cliente
     * @param motivo Motivo de la suspensión
     */
    public void suspender(String motivo) {
        this.estado = EstadoCliente.SUSPENDIDO;
        // En una implementación real, se podría registrar el motivo
    }

    /**
     * Reactiva el cliente
     */
    public void reactivar() {
        if (estado == EstadoCliente.SUSPENDIDO) {
            this.estado = EstadoCliente.ACTIVO;
        }
    }

    /**
     * Promociona a cliente VIP
     */
    public void promoverAVIP() {
        this.estado = EstadoCliente.VIP;
    }

    /**
     * Obtiene un resumen del cliente
     * @return String con información resumida
     */
    public String getResumen() {
        return String.format("Cliente %s (%s): %d pedidos, %d productos [%s - %s]",
            idCliente, nombre, totalPedidos, totalProductos,
            tipo.getDescripcion(), estado.getDescripcion());
    }

    /**
     * Obtiene información completa del cliente
     * @return String con toda la información
     */
    public String getInformacionCompleta() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== CLIENTE ===\n");
        sb.append("ID: ").append(idCliente).append("\n");
        sb.append("Nombre: ").append(nombre).append("\n");
        sb.append("Contacto: ").append(contacto.isEmpty() ? "No especificado" : contacto).append("\n");
        sb.append("Tipo: ").append(tipo.getDescripcion()).append("\n");
        sb.append("Estado: ").append(estado.getDescripcion()).append("\n");
        sb.append("Fecha registro: ").append(fechaRegistro).append("\n");

        sb.append("\n--- ESTADÍSTICAS ---\n");
        sb.append("Total pedidos: ").append(totalPedidos).append("\n");
        sb.append("Total productos: ").append(totalProductos).append("\n");
        sb.append(String.format("Promedio productos/pedido: %.2f\n", getPromedioProductosPorPedido()));
        sb.append("Prioridad: ").append(getPrioridad()).append("/5\n");

        if (ultimoPedido != null) {
            sb.append("Último pedido: ").append(ultimoPedido).append("\n");
            sb.append("Días desde último pedido: ").append(diasDesdeUltimoPedido()).append("\n");
        } else {
            sb.append("Sin pedidos registrados\n");
        }

        sb.append("\n--- HISTORIAL ---\n");
        if (historialPedidos.isEmpty()) {
            sb.append("Sin historial de pedidos\n");
        } else {
            sb.append("Últimos pedidos: ");
            int limite = Math.min(5, historialPedidos.size());
            for (int i = historialPedidos.size() - limite; i < historialPedidos.size(); i++) {
                sb.append(historialPedidos.get(i));
                if (i < historialPedidos.size() - 1) sb.append(", ");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    // Getters y Setters
    public String getIdCliente() { return idCliente; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) {
        this.nombre = nombre != null && !nombre.trim().isEmpty() ? nombre.trim() : "Cliente " + idCliente;
    }

    public String getContacto() { return contacto; }
    public void setContacto(String contacto) {
        this.contacto = contacto != null ? contacto.trim() : "";
    }

    public TipoCliente getTipo() { return tipo; }
    public void setTipo(TipoCliente tipo) {
        this.tipo = tipo != null ? tipo : TipoCliente.NUEVO;
    }

    public EstadoCliente getEstado() { return estado; }
    public void setEstado(EstadoCliente estado) {
        this.estado = estado != null ? estado : EstadoCliente.ACTIVO;
    }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public LocalDateTime getUltimoPedido() { return ultimoPedido; }
    public int getTotalPedidos() { return totalPedidos; }
    public int getTotalProductos() { return totalProductos; }

    public List<String> getHistorialPedidos() {
        return new ArrayList<>(historialPedidos);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Cliente cliente = (Cliente) obj;
        return Objects.equals(idCliente, cliente.idCliente);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idCliente);
    }

    @Override
    public String toString() {
        return getResumen();
    }
}