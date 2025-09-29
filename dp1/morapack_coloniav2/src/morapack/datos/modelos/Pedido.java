package morapack.datos.modelos;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Representa un pedido de productos MPE en el sistema MoraPack.
 * Formato ID: dd-hh-mm-dest-###-IdClien
 */
public class Pedido {

    private final int dia;
    private final int hora;
    private final int minuto;
    private final String codigoDestino;
    private final int cantidadProductos;
    private final String idCliente;
    private final String idPedido;

    // Tiempo del pedido y plazo calculado
    private LocalDateTime tiempoPedido;
    private LocalDateTime tiempoLimiteEntrega;
    private int diasPlazo;

    // Estado del pedido
    private EstadoPedido estado;
    private String observaciones;

    // Patrón regex para validar formato: dd-hh-mm-dest-###-IdClien
    private static final Pattern PATRON_PEDIDO = Pattern.compile(
        "^(\\d{2})-(\\d{2})-(\\d{2})-([A-Z]{4})-(\\d{3})-(\\d{7})$"
    );

    /**
     * Estados posibles de un pedido
     */
    public enum EstadoPedido {
        PENDIENTE("Pendiente de planificación"),
        PLANIFICADO("Ruta planificada"),
        EN_TRANSITO("En tránsito"),
        ENTREGADO("Entregado al cliente"),
        RETRASADO("Retrasado - fuera de plazo"),
        CANCELADO("Cancelado");

        private final String descripcion;

        EstadoPedido(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    /**
     * Constructor privado - usar métodos factory
     */
    private Pedido(int dia, int hora, int minuto, String codigoDestino,
                  int cantidadProductos, String idCliente) {
        this.dia = dia;
        this.hora = hora;
        this.minuto = minuto;
        this.codigoDestino = codigoDestino.toUpperCase();
        this.cantidadProductos = cantidadProductos;
        this.idCliente = idCliente;
        this.idPedido = construirIdPedido();
        this.estado = EstadoPedido.PENDIENTE;
        this.observaciones = "";
    }

    /**
     * Crea un pedido desde una línea CSV
     * @param lineaCSV Línea con formato: dd-hh-mm-dest-###-IdClien
     * @param mes Mes del archivo (1-12)
     * @param anio Año del pedido
     * @return Nueva instancia de Pedido
     * @throws IllegalArgumentException si el formato es incorrecto
     */
    public static Pedido desdeCSV(String lineaCSV, int mes, int anio) {
        if (lineaCSV == null || lineaCSV.trim().isEmpty()) {
            throw new IllegalArgumentException("Línea CSV no puede estar vacía");
        }

        String idPedido = lineaCSV.trim();
        Matcher matcher = PATRON_PEDIDO.matcher(idPedido);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Formato de pedido inválido: " + idPedido);
        }

        try {
            int dia = Integer.parseInt(matcher.group(1));
            int hora = Integer.parseInt(matcher.group(2));
            int minuto = Integer.parseInt(matcher.group(3));
            String destino = matcher.group(4);
            int cantidad = Integer.parseInt(matcher.group(5));
            String cliente = matcher.group(6);

            // Validaciones
            validarComponentes(dia, hora, minuto, destino, cantidad, cliente, mes);

            Pedido pedido = new Pedido(dia, hora, minuto, destino, cantidad, cliente);
            pedido.establecerTiempoPedido(anio, mes);

            return pedido;

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Error al parsear números en: " + idPedido, e);
        }
    }

    /**
     * Crea un pedido con parámetros específicos
     * @param dia Día del mes (1-31)
     * @param hora Hora (0-23)
     * @param minuto Minuto (0-59)
     * @param codigoDestino Código ICAO destino
     * @param cantidadProductos Cantidad de productos (1-999)
     * @param idCliente ID del cliente (7 dígitos)
     * @param mes Mes (1-12)
     * @param anio Año
     * @return Nueva instancia de Pedido
     */
    public static Pedido crear(int dia, int hora, int minuto, String codigoDestino,
                              int cantidadProductos, String idCliente, int mes, int anio) {

        validarComponentes(dia, hora, minuto, codigoDestino, cantidadProductos, idCliente, mes);

        Pedido pedido = new Pedido(dia, hora, minuto, codigoDestino, cantidadProductos, idCliente);
        pedido.establecerTiempoPedido(anio, mes);

        return pedido;
    }

    /**
     * Valida los componentes del pedido
     */
    private static void validarComponentes(int dia, int hora, int minuto, String destino,
                                         int cantidad, String cliente, int mes) {
        if (dia < 1 || dia > 31) {
            throw new IllegalArgumentException("Día debe estar entre 1 y 31: " + dia);
        }
        if (hora < 0 || hora > 23) {
            throw new IllegalArgumentException("Hora debe estar entre 0 y 23: " + hora);
        }
        if (minuto < 0 || minuto > 59) {
            throw new IllegalArgumentException("Minuto debe estar entre 0 y 59: " + minuto);
        }
        if (destino == null || destino.length() != 4) {
            throw new IllegalArgumentException("Código destino debe tener 4 caracteres: " + destino);
        }
        if (cantidad < 1 || cantidad > 999) {
            throw new IllegalArgumentException("Cantidad debe estar entre 1 y 999: " + cantidad);
        }
        if (cliente == null || cliente.length() != 7 || !cliente.matches("\\d{7}")) {
            throw new IllegalArgumentException("ID cliente debe tener 7 dígitos: " + cliente);
        }
        if (mes < 1 || mes > 12) {
            throw new IllegalArgumentException("Mes debe estar entre 1 y 12: " + mes);
        }
    }

    /**
     * Construye el ID del pedido en formato estándar
     */
    private String construirIdPedido() {
        return String.format("%02d-%02d-%02d-%s-%03d-%s",
            dia, hora, minuto, codigoDestino, cantidadProductos, idCliente);
    }

    /**
     * Establece el tiempo del pedido en huso horario del destino
     * INTERPRETACIÓN: dd-hh-mm están en hora LOCAL del aeropuerto destino
     */
    private void establecerTiempoPedido(int anio, int mes) {
        // El tiempo del pedido YA ESTÁ en huso horario del destino
        // No necesita conversión adicional
        this.tiempoPedido = LocalDateTime.of(anio, mes, dia, hora, minuto);
    }

    /**
     * Calcula el plazo límite basado en los continentes de origen y destino
     * @param aeropuertoOrigen Aeropuerto de origen (sede principal)
     * @param aeropuertoDestino Aeropuerto de destino
     */
    public void calcularPlazoEntrega(Aeropuerto aeropuertoOrigen, Aeropuerto aeropuertoDestino) {
        if (aeropuertoOrigen == null || aeropuertoDestino == null) {
            throw new IllegalArgumentException("Aeropuertos no pueden ser null");
        }

        this.diasPlazo = Continente.calcularPlazoEntrega(
            aeropuertoOrigen.getContinente(),
            aeropuertoDestino.getContinente()
        );

        // El tiempo del pedido YA ESTÁ en huso horario del destino
        // Simplemente agregamos los días de plazo
        this.tiempoLimiteEntrega = tiempoPedido.plusDays(diasPlazo);
    }

    /**
     * Convierte el tiempo del pedido (en huso destino) a UTC para cálculos
     * @param aeropuertoDestino Aeropuerto destino con información de huso
     * @return Tiempo del pedido en UTC
     */
    public LocalDateTime getTiempoPedidoUTC(Aeropuerto aeropuertoDestino) {
        if (aeropuertoDestino == null) {
            throw new IllegalArgumentException("Aeropuerto destino no puede ser null");
        }

        // Convertir desde huso destino a UTC
        int offsetDestino = aeropuertoDestino.getHusoHorario();
        return tiempoPedido.minusHours(offsetDestino);
    }

    /**
     * Convierte el tiempo límite de entrega (en huso destino) a UTC
     * @param aeropuertoDestino Aeropuerto destino con información de huso
     * @return Tiempo límite en UTC
     */
    public LocalDateTime getTiempoLimiteEntregaUTC(Aeropuerto aeropuertoDestino) {
        if (aeropuertoDestino == null || tiempoLimiteEntrega == null) {
            return null;
        }

        // Convertir desde huso destino a UTC
        int offsetDestino = aeropuertoDestino.getHusoHorario();
        return tiempoLimiteEntrega.minusHours(offsetDestino);
    }

    /**
     * Verifica si el pedido puede ser procesado (destino no es sede principal)
     * @return true si el destino no es sede principal
     */
    public boolean esProcesable() {
        // Verificar que no sea destino a sedes principales
        return !codigoDestino.equals("SPIM") &&
               !codigoDestino.equals("EBCI") &&
               !codigoDestino.equals("UBBB");
    }

    /**
     * Verifica si el pedido está dentro del plazo (usando huso destino)
     * @param tiempoActual Tiempo actual en el mismo huso que el pedido (huso destino)
     * @return true si está dentro del plazo
     */
    public boolean estaDentroPlazo(LocalDateTime tiempoActual) {
        if (tiempoLimiteEntrega == null) {
            return true; // No se ha calculado plazo aún
        }
        return tiempoActual.isBefore(tiempoLimiteEntrega) || tiempoActual.isEqual(tiempoLimiteEntrega);
    }

    /**
     * Verifica si el pedido está dentro del plazo usando UTC para comparación precisa
     * @param tiempoActualUTC Tiempo actual en UTC
     * @param aeropuertoDestino Aeropuerto destino para conversión de husos
     * @return true si está dentro del plazo
     */
    public boolean estaDentroPlazoUTC(LocalDateTime tiempoActualUTC, Aeropuerto aeropuertoDestino) {
        LocalDateTime limiteUTC = getTiempoLimiteEntregaUTC(aeropuertoDestino);
        if (limiteUTC == null) {
            return true; // No se ha calculado plazo aún
        }
        return tiempoActualUTC.isBefore(limiteUTC) || tiempoActualUTC.isEqual(limiteUTC);
    }

    /**
     * Calcula las horas restantes hasta el vencimiento (usando huso destino)
     * @param tiempoActual Tiempo actual en el mismo huso que el pedido
     * @return Horas restantes (negativo si ya venció)
     */
    public long horasRestantes(LocalDateTime tiempoActual) {
        if (tiempoLimiteEntrega == null) {
            return Long.MAX_VALUE;
        }
        return java.time.Duration.between(tiempoActual, tiempoLimiteEntrega).toHours();
    }

    /**
     * Calcula las horas restantes hasta el vencimiento usando UTC
     * @param tiempoActualUTC Tiempo actual en UTC
     * @param aeropuertoDestino Aeropuerto destino para conversión de husos
     * @return Horas restantes (negativo si ya venció)
     */
    public long horasRestantesUTC(LocalDateTime tiempoActualUTC, Aeropuerto aeropuertoDestino) {
        LocalDateTime limiteUTC = getTiempoLimiteEntregaUTC(aeropuertoDestino);
        if (limiteUTC == null) {
            return Long.MAX_VALUE;
        }
        return java.time.Duration.between(tiempoActualUTC, limiteUTC).toHours();
    }

    // Getters
    public int getDia() { return dia; }
    public int getHora() { return hora; }
    public int getMinuto() { return minuto; }
    public String getCodigoDestino() { return codigoDestino; }
    public int getCantidadProductos() { return cantidadProductos; }
    public String getIdCliente() { return idCliente; }
    public String getIdPedido() { return idPedido; }
    public LocalDateTime getTiempoPedido() { return tiempoPedido; }
    public LocalDateTime getTiempoLimiteEntrega() { return tiempoLimiteEntrega; }
    public int getDiasPlazo() { return diasPlazo; }
    public EstadoPedido getEstado() { return estado; }
    public String getObservaciones() { return observaciones; }

    // Setters para gestión de estado
    public void setEstado(EstadoPedido estado) {
        this.estado = estado;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones != null ? observaciones : "";
    }

    /**
     * Obtiene el tiempo del pedido como LocalTime
     * @return Hora del pedido
     */
    public LocalTime getHoraPedido() {
        return LocalTime.of(hora, minuto);
    }

    /**
     * Información resumida del pedido
     * @return String con información clave
     */
    public String getResumen() {
        return String.format("Pedido %s: %d productos → %s (Cliente: %s) [%s]",
            idPedido, cantidadProductos, codigoDestino, idCliente, estado.getDescripcion());
    }

    /**
     * Información completa del pedido
     * @return String con toda la información
     */
    public String getInformacionCompleta() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== PEDIDO ===\n");
        sb.append("ID: ").append(idPedido).append("\n");
        sb.append("Cliente: ").append(idCliente).append("\n");
        sb.append("Destino: ").append(codigoDestino).append("\n");
        sb.append("Cantidad: ").append(cantidadProductos).append(" productos\n");
        sb.append("Tiempo pedido: ").append(tiempoPedido).append("\n");

        if (tiempoLimiteEntrega != null) {
            sb.append("Plazo límite: ").append(tiempoLimiteEntrega).append(" (").append(diasPlazo).append(" días)\n");
        }

        sb.append("Estado: ").append(estado.getDescripcion()).append("\n");

        if (!observaciones.isEmpty()) {
            sb.append("Observaciones: ").append(observaciones).append("\n");
        }

        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Pedido pedido = (Pedido) obj;
        return Objects.equals(idPedido, pedido.idPedido);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idPedido);
    }

    @Override
    public String toString() {
        return getResumen();
    }
}