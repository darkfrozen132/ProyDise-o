package morapack.datos.modelos;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Objects;

/**
 * Representa una instancia específica de un vuelo en una fecha particular.
 *
 * Los planes de vuelo se repiten diariamente. Esta clase encapsula:
 * - La plantilla del vuelo (origen, destino, horarios, capacidad base)
 * - La fecha específica en que opera
 * - La capacidad disponible para esa fecha en particular
 *
 * Ejemplo:
 * - Plantilla: SKBO->SEQM salida 03:34, capacidad 300
 * - Instancia día 1: capacidad restante 300
 * - Instancia día 2: capacidad restante 280 (si se reservaron 20)
 * - Instancia día 3: capacidad restante 300 (vuelo nuevo)
 */
public class VueloInstancia {

    private final Vuelo plantilla;
    private final LocalDate fecha;
    private int capacidadDisponible;
    private final String idInstancia;

    /**
     * Constructor de instancia de vuelo
     * @param plantilla Vuelo plantilla del plan de vuelo
     * @param fecha Fecha específica en que opera esta instancia
     */
    public VueloInstancia(Vuelo plantilla, LocalDate fecha) {
        if (plantilla == null) {
            throw new IllegalArgumentException("Plantilla de vuelo no puede ser null");
        }
        if (fecha == null) {
            throw new IllegalArgumentException("Fecha no puede ser null");
        }

        this.plantilla = plantilla;
        this.fecha = fecha;
        this.capacidadDisponible = plantilla.getCapacidadMaxima();
        this.idInstancia = generarIdInstancia();
    }

    /**
     * Constructor con capacidad inicial específica
     * @param plantilla Vuelo plantilla
     * @param fecha Fecha específica
     * @param capacidadInicial Capacidad disponible inicial
     */
    public VueloInstancia(Vuelo plantilla, LocalDate fecha, int capacidadInicial) {
        this(plantilla, fecha);
        this.capacidadDisponible = capacidadInicial;
    }

    /**
     * Genera un ID único para esta instancia de vuelo
     * Formato: ORIGEN-DESTINO-HHMMSS-YYYYMMDD
     */
    private String generarIdInstancia() {
        return String.format("%s-%04d%02d%02d",
            plantilla.getIdVuelo(),
            fecha.getYear(),
            fecha.getMonthValue(),
            fecha.getDayOfMonth()
        );
    }

    /**
     * Obtiene el horario de salida completo (fecha + hora)
     * @return LocalDateTime con fecha y hora de salida
     */
    public LocalDateTime getHorarioSalidaCompleto() {
        return LocalDateTime.of(
            fecha,
            plantilla.getHoraSalida()
        );
    }

    /**
     * Obtiene el horario de llegada completo (fecha + hora)
     * Considera cruces de medianoche automáticamente
     * @return LocalDateTime con fecha y hora de llegada
     */
    public LocalDateTime getHorarioLlegadaCompleto() {
        LocalDateTime salida = getHorarioSalidaCompleto();

        // Si el vuelo cruza medianoche, la llegada es al día siguiente
        if (plantilla.cruzaMedianoche()) {
            return LocalDateTime.of(
                fecha.plusDays(1),
                plantilla.getHoraLlegada()
            );
        } else {
            return LocalDateTime.of(
                fecha,
                plantilla.getHoraLlegada()
            );
        }
    }

    /**
     * Obtiene el horario de llegada completo considerando husos horarios reales
     * @param aeropuertoOrigen Aeropuerto origen con información de huso
     * @param aeropuertoDestino Aeropuerto destino con información de huso
     * @return LocalDateTime con fecha y hora de llegada ajustada
     */
    public LocalDateTime getHorarioLlegadaCompletoReal(Aeropuerto aeropuertoOrigen, Aeropuerto aeropuertoDestino) {
        LocalDateTime salida = getHorarioSalidaCompleto();

        // Calcular duración real considerando husos horarios
        Duration duracionReal = plantilla.calcularDuracionReal(aeropuertoOrigen, aeropuertoDestino);

        return salida.plus(duracionReal);
    }

    /**
     * Verifica si puede transportar una cantidad específica
     * @param cantidad Cantidad de productos
     * @return true si hay capacidad suficiente
     */
    public boolean puedeTransportar(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("Cantidad no puede ser negativa");
        }
        return capacidadDisponible >= cantidad;
    }

    /**
     * Reserva capacidad en esta instancia
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

        capacidadDisponible = Math.min(
            plantilla.getCapacidadMaxima(),
            capacidadDisponible + cantidad
        );
    }

    /**
     * Reinicia la capacidad a la máxima
     */
    public void reiniciarCapacidad() {
        this.capacidadDisponible = plantilla.getCapacidadMaxima();
    }

    /**
     * Calcula el porcentaje de ocupación
     * @return Porcentaje de ocupación (0-100)
     */
    public double getPorcentajeOcupacion() {
        if (plantilla.getCapacidadMaxima() == 0) {
            return 0.0;
        }
        return ((double) (plantilla.getCapacidadMaxima() - capacidadDisponible)
                / plantilla.getCapacidadMaxima()) * 100.0;
    }

    /**
     * Verifica si esta instancia puede conectar con otra
     * @param siguienteInstancia Siguiente vuelo
     * @param tiempoMinimoConexion Tiempo mínimo en minutos
     * @return true si pueden conectarse
     */
    public boolean puedeConectarCon(VueloInstancia siguienteInstancia, int tiempoMinimoConexion) {
        if (siguienteInstancia == null) {
            return false;
        }

        // El destino de este vuelo debe ser el origen del siguiente
        if (!this.getAeropuertoDestino().equals(siguienteInstancia.getAeropuertoOrigen())) {
            return false;
        }

        // Calcular tiempo entre llegada y salida
        LocalDateTime llegada = getHorarioLlegadaCompleto();
        LocalDateTime salidaSiguiente = siguienteInstancia.getHorarioSalidaCompleto();

        Duration tiempoEspera = Duration.between(llegada, salidaSiguiente);

        // Verificar tiempo mínimo y que no sea negativo
        return tiempoEspera.toMinutes() >= tiempoMinimoConexion;
    }

    /**
     * Verifica si esta instancia puede conectar con otra considerando husos horarios
     * @param siguienteInstancia Siguiente vuelo
     * @param aeropuertoConexion Aeropuerto de conexión
     * @param tiempoMinimoConexion Tiempo mínimo en minutos
     * @return true si pueden conectarse
     */
    public boolean puedeConectarConReal(VueloInstancia siguienteInstancia,
                                        Aeropuerto aeropuertoConexion,
                                        int tiempoMinimoConexion) {
        if (siguienteInstancia == null || aeropuertoConexion == null) {
            return false;
        }

        // Verificar conectividad física
        if (!this.getAeropuertoDestino().equals(siguienteInstancia.getAeropuertoOrigen())) {
            return false;
        }

        if (!this.getAeropuertoDestino().equals(aeropuertoConexion.getCodigoICAO())) {
            return false;
        }

        // La llegada ya está en hora local del aeropuerto de conexión
        LocalDateTime llegadaLocal = getHorarioLlegadaCompleto();

        // La salida también está en hora local del aeropuerto de conexión
        LocalDateTime salidaLocal = siguienteInstancia.getHorarioSalidaCompleto();

        Duration tiempoEspera = Duration.between(llegadaLocal, salidaLocal);

        return tiempoEspera.toMinutes() >= tiempoMinimoConexion;
    }

    /**
     * Verifica si es un vuelo directo entre dos aeropuertos
     * @param origen Código ICAO origen
     * @param destino Código ICAO destino
     * @return true si es vuelo directo
     */
    public boolean esVueloDirecto(String origen, String destino) {
        return plantilla.esVueloDirecto(origen, destino);
    }

    /**
     * Obtiene información resumida de la instancia
     * @return String con información clave
     */
    public String getResumen() {
        return String.format("VueloInstancia[%s el %s: %s → %s (%s-%s) Cap: %d/%d]",
            plantilla.getIdVuelo(),
            fecha,
            getAeropuertoOrigen(),
            getAeropuertoDestino(),
            plantilla.getHoraSalida(),
            plantilla.getHoraLlegada(),
            plantilla.getCapacidadMaxima() - capacidadDisponible,
            plantilla.getCapacidadMaxima()
        );
    }

    /**
     * Obtiene información completa de la instancia
     * @return String con información detallada
     */
    public String getInformacionCompleta() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== INSTANCIA DE VUELO ===\n");
        sb.append("ID Instancia: ").append(idInstancia).append("\n");
        sb.append("Fecha: ").append(fecha).append("\n");
        sb.append("Ruta: ").append(getAeropuertoOrigen()).append(" → ").append(getAeropuertoDestino()).append("\n");
        sb.append("Salida: ").append(getHorarioSalidaCompleto()).append("\n");
        sb.append("Llegada: ").append(getHorarioLlegadaCompleto()).append("\n");
        sb.append("Duración: ").append(formatearDuracion(plantilla.calcularDuracion())).append("\n");
        sb.append("Capacidad: ").append(plantilla.getCapacidadMaxima() - capacidadDisponible)
          .append("/").append(plantilla.getCapacidadMaxima()).append(" productos\n");
        sb.append(String.format("Ocupación: %.1f%%\n", getPorcentajeOcupacion()));
        sb.append("Plantilla: ").append(plantilla.getIdVuelo()).append("\n");

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

    // Getters principales
    public Vuelo getPlantilla() { return plantilla; }
    public LocalDate getFecha() { return fecha; }
    public int getCapacidadDisponible() { return capacidadDisponible; }
    public String getIdInstancia() { return idInstancia; }

    // Getters delegados a la plantilla
    public String getAeropuertoOrigen() { return plantilla.getAeropuertoOrigen(); }
    public String getAeropuertoDestino() { return plantilla.getAeropuertoDestino(); }
    public int getCapacidadMaxima() { return plantilla.getCapacidadMaxima(); }

    // Setter para capacidad (para gestión externa)
    public void setCapacidadDisponible(int capacidad) {
        if (capacidad < 0 || capacidad > plantilla.getCapacidadMaxima()) {
            throw new IllegalArgumentException("Capacidad fuera de rango válido");
        }
        this.capacidadDisponible = capacidad;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        VueloInstancia that = (VueloInstancia) obj;
        return Objects.equals(idInstancia, that.idInstancia);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idInstancia);
    }

    @Override
    public String toString() {
        return getResumen();
    }
}
