package morapack.datos.modelos;

import java.time.LocalTime;
import java.time.Duration;
import java.util.Objects;

/**
 * Representa un vuelo en el sistema de distribución MoraPack.
 * Contiene información sobre origen, destino, horarios y capacidad.
 */
public class Vuelo {

    private final String aeropuertoOrigen;
    private final String aeropuertoDestino;
    private final LocalTime horaSalida;
    private final LocalTime horaLlegada;
    private final int capacidadMaxima;

    // Estado dinámico del vuelo
    private int capacidadDisponible;
    private EstadoVuelo estado;

    // ID único del vuelo
    private final String idVuelo;

    /**
     * Estados posibles de un vuelo
     */
    public enum EstadoVuelo {
        PROGRAMADO("Programado"),
        DISPONIBLE("Disponible para reservas"),
        COMPLETO("Capacidad completa"),
        EN_VUELO("En vuelo"),
        COMPLETADO("Vuelo completado"),
        CANCELADO("Cancelado"),
        RETRASADO("Retrasado");

        private final String descripcion;

        EstadoVuelo(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    /**
     * Constructor completo para un vuelo
     * @param aeropuertoOrigen Código ICAO del aeropuerto origen
     * @param aeropuertoDestino Código ICAO del aeropuerto destino
     * @param horaSalida Hora de salida
     * @param horaLlegada Hora de llegada
     * @param capacidadMaxima Capacidad máxima en productos
     */
    public Vuelo(String aeropuertoOrigen, String aeropuertoDestino,
                LocalTime horaSalida, LocalTime horaLlegada, int capacidadMaxima) {

        // Validaciones
        if (aeropuertoOrigen == null || aeropuertoOrigen.trim().length() != 4) {
            throw new IllegalArgumentException("Código origen debe tener 4 caracteres");
        }
        if (aeropuertoDestino == null || aeropuertoDestino.trim().length() != 4) {
            throw new IllegalArgumentException("Código destino debe tener 4 caracteres");
        }
        if (aeropuertoOrigen.equalsIgnoreCase(aeropuertoDestino)) {
            throw new IllegalArgumentException("Aeropuerto origen y destino no pueden ser iguales");
        }
        if (horaSalida == null) {
            throw new IllegalArgumentException("Hora de salida no puede ser null");
        }
        if (horaLlegada == null) {
            throw new IllegalArgumentException("Hora de llegada no puede ser null");
        }
        if (capacidadMaxima <= 0) {
            throw new IllegalArgumentException("Capacidad debe ser mayor a 0");
        }

        this.aeropuertoOrigen = aeropuertoOrigen.trim().toUpperCase();
        this.aeropuertoDestino = aeropuertoDestino.trim().toUpperCase();
        this.horaSalida = horaSalida;
        this.horaLlegada = horaLlegada;
        this.capacidadMaxima = capacidadMaxima;

        // Inicializar estado
        this.capacidadDisponible = capacidadMaxima;
        this.estado = EstadoVuelo.PROGRAMADO;

        // Generar ID único
        this.idVuelo = generarIdVuelo();
    }

    /**
     * Constructor desde línea CSV
     * @param lineaCSV Línea del archivo planes_de_vuelo.csv
     * @return Nueva instancia de Vuelo
     * @throws IllegalArgumentException si el formato es incorrecto
     */
    public static Vuelo desdeCSV(String lineaCSV) {
        if (lineaCSV == null || lineaCSV.trim().isEmpty()) {
            throw new IllegalArgumentException("Línea CSV no puede estar vacía");
        }

        String[] campos = lineaCSV.split(",");
        if (campos.length != 5) {
            throw new IllegalArgumentException("Línea CSV debe tener 5 campos: " + lineaCSV);
        }

        try {
            String origen = campos[0].trim();
            String destino = campos[1].trim();
            LocalTime salida = LocalTime.parse(campos[2].trim());
            LocalTime llegada = LocalTime.parse(campos[3].trim());
            int capacidad = Integer.parseInt(campos[4].trim());

            return new Vuelo(origen, destino, salida, llegada, capacidad);

        } catch (Exception e) {
            throw new IllegalArgumentException("Error al crear vuelo desde CSV: " + lineaCSV, e);
        }
    }

    /**
     * Genera un ID único para el vuelo
     * @return ID en formato ORIGEN-DESTINO-HHMMSS
     */
    private String generarIdVuelo() {
        String horaFormateada = horaSalida.toString().replace(":", "");
        return String.format("%s-%s-%s", aeropuertoOrigen, aeropuertoDestino, horaFormateada);
    }

    /**
     * Calcula la duración del vuelo considerando horarios locales
     * NOTA: Este método es simplificado para horarios en el mismo huso.
     * Para cálculos precisos con diferentes husos, usar calcularDuracionReal()
     * @return Duración del vuelo
     */
    public Duration calcularDuracion() {
        if (horaLlegada.isBefore(horaSalida)) {
            // Vuelo que llega al día siguiente
            return Duration.between(horaSalida, horaLlegada.plusHours(24));
        } else {
            return Duration.between(horaSalida, horaLlegada);
        }
    }

    /**
     * Verifica si el vuelo cruza medianoche
     * @return true si llega al día siguiente
     */
    public boolean cruzaMedianoche() {
        return horaLlegada.isBefore(horaSalida);
    }

    /**
     * Calcula la duración real del vuelo considerando husos horarios
     * HoraSalida = hora local del aeropuerto origen
     * HoraLlegada = hora local del aeropuerto destino
     * @param aeropuertoOrigen Aeropuerto de origen con huso horario
     * @param aeropuertoDestino Aeropuerto de destino con huso horario
     * @return Duración real del vuelo
     */
    public Duration calcularDuracionReal(Aeropuerto aeropuertoOrigen, Aeropuerto aeropuertoDestino) {
        if (aeropuertoOrigen == null || aeropuertoDestino == null) {
            throw new IllegalArgumentException("Aeropuertos no pueden ser null");
        }

        // Convertir hora de salida a UTC
        // HoraSalida está en huso horario del origen
        int offsetOrigen = aeropuertoOrigen.getHusoHorario();
        LocalTime salidaUTC = horaSalida.minusHours(offsetOrigen);

        // Convertir hora de llegada a UTC
        // HoraLlegada está en huso horario del destino
        int offsetDestino = aeropuertoDestino.getHusoHorario();
        LocalTime llegadaUTC = horaLlegada.minusHours(offsetDestino);

        // Si llegadaUTC < salidaUTC, significa que llega al día siguiente
        if (llegadaUTC.isBefore(salidaUTC)) {
            llegadaUTC = llegadaUTC.plusHours(24);
        }

        return Duration.between(salidaUTC, llegadaUTC);
    }

    /**
     * Verifica si el vuelo cruza medianoche considerando husos horarios reales
     * @param aeropuertoOrigen Aeropuerto de origen
     * @param aeropuertoDestino Aeropuerto de destino
     * @return true si el vuelo llega al día siguiente en UTC
     */
    public boolean cruzaMedianocheReal(Aeropuerto aeropuertoOrigen, Aeropuerto aeropuertoDestino) {
        if (aeropuertoOrigen == null || aeropuertoDestino == null) {
            return false;
        }

        int offsetOrigen = aeropuertoOrigen.getHusoHorario();
        int offsetDestino = aeropuertoDestino.getHusoHorario();

        LocalTime salidaUTC = horaSalida.minusHours(offsetOrigen);
        LocalTime llegadaUTC = horaLlegada.minusHours(offsetDestino);

        return llegadaUTC.isBefore(salidaUTC);
    }

    /**
     * Verifica si puede transportar una cantidad específica de productos
     * @param cantidad Cantidad de productos a transportar
     * @return true si hay capacidad suficiente
     */
    public boolean puedeTransportar(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("Cantidad no puede ser negativa");
        }
        return capacidadDisponible >= cantidad && estado == EstadoVuelo.PROGRAMADO;
    }

    /**
     * Reserva capacidad en el vuelo
     * @param cantidad Cantidad a reservar
     * @return true si se pudo reservar
     */
    public boolean reservarCapacidad(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("Cantidad no puede ser negativa");
        }

        if (!puedeTransportar(cantidad)) {
            return false;
        }

        capacidadDisponible -= cantidad;

        // Actualizar estado si queda lleno
        if (capacidadDisponible == 0) {
            estado = EstadoVuelo.COMPLETO;
        } else if (estado == EstadoVuelo.PROGRAMADO) {
            estado = EstadoVuelo.DISPONIBLE;
        }

        return true;
    }

    /**
     * Libera capacidad previamente reservada
     * @param cantidad Cantidad a liberar
     */
    public void liberarCapacidad(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("Cantidad no puede ser negativa");
        }

        capacidadDisponible = Math.min(capacidadMaxima, capacidadDisponible + cantidad);

        // Actualizar estado
        if (capacidadDisponible > 0 && estado == EstadoVuelo.COMPLETO) {
            estado = EstadoVuelo.DISPONIBLE;
        }
    }

    /**
     * Reinicia la capacidad disponible a la máxima
     */
    public void reiniciarCapacidad() {
        this.capacidadDisponible = capacidadMaxima;
        if (estado == EstadoVuelo.COMPLETO) {
            estado = EstadoVuelo.PROGRAMADO;
        }
    }

    /**
     * Marca el vuelo como cancelado
     */
    public void cancelar() {
        this.estado = EstadoVuelo.CANCELADO;
        this.capacidadDisponible = 0;
    }

    /**
     * Verifica si el vuelo está disponible para reservas
     * @return true si está disponible
     */
    public boolean estaDisponible() {
        return estado == EstadoVuelo.PROGRAMADO || estado == EstadoVuelo.DISPONIBLE;
    }

    /**
     * Verifica si es un vuelo directo entre dos aeropuertos específicos
     * @param origen Código ICAO origen
     * @param destino Código ICAO destino
     * @return true si es vuelo directo entre esos aeropuertos
     */
    public boolean esVueloDirecto(String origen, String destino) {
        if (origen == null || destino == null) {
            return false;
        }
        return this.aeropuertoOrigen.equalsIgnoreCase(origen) &&
               this.aeropuertoDestino.equalsIgnoreCase(destino);
    }

    /**
     * Calcula el porcentaje de ocupación
     * @return Porcentaje de ocupación (0-100)
     */
    public double getPorcentajeOcupacion() {
        if (capacidadMaxima == 0) {
            return 0.0;
        }
        return ((double) (capacidadMaxima - capacidadDisponible) / capacidadMaxima) * 100.0;
    }

    /**
     * Verifica si dos vuelos pueden conectarse (llegada antes de salida con tiempo mínimo)
     * NOTA: Este método usa horarios locales simplificados.
     * Para cálculos precisos, usar puedeConectarConReal()
     * @param siguienteVuelo Vuelo que conecta
     * @param tiempoMinimoConexion Tiempo mínimo de conexión en minutos
     * @return true si pueden conectarse
     */
    public boolean puedeConectarCon(Vuelo siguienteVuelo, int tiempoMinimoConexion) {
        if (siguienteVuelo == null) {
            return false;
        }

        // El destino de este vuelo debe ser el origen del siguiente
        if (!this.aeropuertoDestino.equals(siguienteVuelo.aeropuertoOrigen)) {
            return false;
        }

        // Calcular tiempo entre llegada y salida del siguiente vuelo
        LocalTime llegadaEsteVuelo = this.horaLlegada;
        LocalTime salidaSiguienteVuelo = siguienteVuelo.horaSalida;

        // Manejar cruces de medianoche
        if (this.cruzaMedianoche()) {
            llegadaEsteVuelo = llegadaEsteVuelo.plusHours(24);
        }

        Duration tiempoEspera;
        if (salidaSiguienteVuelo.isBefore(llegadaEsteVuelo)) {
            // El siguiente vuelo sale al día siguiente
            tiempoEspera = Duration.between(llegadaEsteVuelo, salidaSiguienteVuelo.plusHours(24));
        } else {
            tiempoEspera = Duration.between(llegadaEsteVuelo, salidaSiguienteVuelo);
        }

        return tiempoEspera.toMinutes() >= tiempoMinimoConexion;
    }

    /**
     * Verifica si dos vuelos pueden conectarse considerando husos horarios reales
     * @param siguienteVuelo Vuelo que conecta
     * @param aeropuertoConexion Aeropuerto de conexión (debe tener huso horario)
     * @param tiempoMinimoConexion Tiempo mínimo de conexión en minutos
     * @return true si pueden conectarse
     */
    public boolean puedeConectarConReal(Vuelo siguienteVuelo, Aeropuerto aeropuertoConexion, int tiempoMinimoConexion) {
        if (siguienteVuelo == null || aeropuertoConexion == null) {
            return false;
        }

        // El destino de este vuelo debe ser el origen del siguiente
        if (!this.aeropuertoDestino.equals(siguienteVuelo.aeropuertoOrigen)) {
            return false;
        }

        // Verificar que el aeropuerto de conexión sea el correcto
        if (!this.aeropuertoDestino.equals(aeropuertoConexion.getCodigoICAO())) {
            return false;
        }

        // Convertir llegada de este vuelo a hora local del aeropuerto de conexión
        // horaLlegada ya está en huso del destino (aeropuerto de conexión)
        LocalTime llegadaLocal = this.horaLlegada;

        // Convertir salida del siguiente vuelo a hora local del aeropuerto de conexión
        // horaSalida ya está en huso del origen (aeropuerto de conexión)
        LocalTime salidaLocal = siguienteVuelo.horaSalida;

        // Calcular tiempo de espera en el aeropuerto de conexión
        Duration tiempoEspera;
        if (salidaLocal.isBefore(llegadaLocal)) {
            // El siguiente vuelo sale al día siguiente
            tiempoEspera = Duration.between(llegadaLocal, salidaLocal.plusHours(24));
        } else {
            tiempoEspera = Duration.between(llegadaLocal, salidaLocal);
        }

        return tiempoEspera.toMinutes() >= tiempoMinimoConexion;
    }

    /**
     * Obtiene información resumida del vuelo
     * @return String con información clave
     */
    public String getResumen() {
        return String.format("Vuelo %s: %s → %s (%s-%s) Cap: %d/%d [%s]",
            idVuelo, aeropuertoOrigen, aeropuertoDestino,
            horaSalida, horaLlegada,
            capacidadMaxima - capacidadDisponible, capacidadMaxima,
            estado.getDescripcion());
    }

    /**
     * Obtiene información completa del vuelo
     * @return String con toda la información
     */
    public String getInformacionCompleta() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== VUELO ===\n");
        sb.append("ID: ").append(idVuelo).append("\n");
        sb.append("Ruta: ").append(aeropuertoOrigen).append(" → ").append(aeropuertoDestino).append("\n");
        sb.append("Salida: ").append(horaSalida).append("\n");
        sb.append("Llegada: ").append(horaLlegada);

        if (cruzaMedianoche()) {
            sb.append(" (+1 día)");
        }
        sb.append("\n");

        sb.append("Duración: ").append(formatearDuracion(calcularDuracion())).append("\n");
        sb.append("Capacidad: ").append(capacidadMaxima - capacidadDisponible)
          .append("/").append(capacidadMaxima).append(" productos\n");
        sb.append(String.format("Ocupación: %.1f%%\n", getPorcentajeOcupacion()));
        sb.append("Estado: ").append(estado.getDescripcion()).append("\n");

        return sb.toString();
    }

    /**
     * Obtiene información completa del vuelo incluyendo análisis de husos horarios
     * @param aeropuertoOrigen Aeropuerto de origen con información de huso
     * @param aeropuertoDestino Aeropuerto de destino con información de huso
     * @return String con análisis detallado incluyendo horarios UTC
     */
    public String getInformacionCompletaConHusos(Aeropuerto aeropuertoOrigen, Aeropuerto aeropuertoDestino) {
        StringBuilder sb = new StringBuilder();
        sb.append(getInformacionCompleta());

        if (aeropuertoOrigen != null && aeropuertoDestino != null) {
            sb.append("\n--- ANÁLISIS DE HUSOS HORARIOS ---\n");

            // Información de husos
            sb.append(String.format("Huso origen (%s): GMT%+d\n",
                aeropuertoOrigen.getCodigoICAO(), aeropuertoOrigen.getHusoHorario()));
            sb.append(String.format("Huso destino (%s): GMT%+d\n",
                aeropuertoDestino.getCodigoICAO(), aeropuertoDestino.getHusoHorario()));

            // Conversión a UTC
            int offsetOrigen = aeropuertoOrigen.getHusoHorario();
            int offsetDestino = aeropuertoDestino.getHusoHorario();

            LocalTime salidaUTC = horaSalida.minusHours(offsetOrigen);
            LocalTime llegadaUTC = horaLlegada.minusHours(offsetDestino);

            sb.append(String.format("Salida UTC: %s\n", salidaUTC));
            sb.append(String.format("Llegada UTC: %s", llegadaUTC));

            if (cruzaMedianocheReal(aeropuertoOrigen, aeropuertoDestino)) {
                sb.append(" (+1 día)");
            }
            sb.append("\n");

            // Duración real
            Duration duracionReal = calcularDuracionReal(aeropuertoOrigen, aeropuertoDestino);
            sb.append(String.format("Duración real: %s\n", formatearDuracion(duracionReal)));
        }

        return sb.toString();
    }

    /**
     * Formatea una duración como texto legible
     */
    private String formatearDuracion(Duration duracion) {
        long horas = duracion.toHours();
        long minutos = duracion.toMinutes() % 60;
        return String.format("%dh %02dm", horas, minutos);
    }

    // Getters
    public String getAeropuertoOrigen() { return aeropuertoOrigen; }
    public String getAeropuertoDestino() { return aeropuertoDestino; }
    public LocalTime getHoraSalida() { return horaSalida; }
    public LocalTime getHoraLlegada() { return horaLlegada; }
    public int getCapacidadMaxima() { return capacidadMaxima; }
    public int getCapacidadDisponible() { return capacidadDisponible; }
    public EstadoVuelo getEstado() { return estado; }
    public String getIdVuelo() { return idVuelo; }

    // Setter para estado (para gestión externa)
    public void setEstado(EstadoVuelo estado) {
        this.estado = estado;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Vuelo vuelo = (Vuelo) obj;
        return Objects.equals(idVuelo, vuelo.idVuelo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idVuelo);
    }

    @Override
    public String toString() {
        return getResumen();
    }
}