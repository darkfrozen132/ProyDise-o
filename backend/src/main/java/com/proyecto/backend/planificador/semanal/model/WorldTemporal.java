package com.proyecto.backend.planificador.semanal.model;

import com.proyecto.backend.model.Aeropuerto;
import com.proyecto.backend.model.PlanDeVuelo;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Extension temporal del World para una ejecucion especifica del algoritmo
 *
 * Responsabilidades:
 * - Expande los templates de PlanDeVuelo en instancias concretas para el horizonte temporal
 * - Mantiene el estado mutable de capacidades durante la ejecucion
 * - Proporciona busquedas rapidas de vuelos disponibles por dia
 *
 * Ciclo de vida:
 * 1. Se crea al inicio de cada ejecucion de planificacion
 * 2. Expande vuelos para el horizonte (ej: 7 dias)
 * 3. Se usa durante la generacion de rutas
 * 4. Se descarta al terminar (garbage collected)
 */
@Slf4j
@Getter
public class WorldTemporal {

    // World base (singleton, inmutable)
    private final World worldBase;

    // Parametros temporales
    private final LocalDate fechaInicio;
    private final LocalDate fechaFin;
    private final int numeroDias;
    
    // Hora de inicio de la simulacion (para filtrar vuelos anteriores)
    private final LocalDateTime horaInicioSimulacion;

    // Vuelos expandidos: Map<id, VueloInstancia>
    // Ejemplo key: "SPIM-SEQM-20250117-0334"
    private final Map<String, VueloInstancia> vuelosExpandidos;

    // Indice: vuelos por dia y origen
    // Map<diaRelativo, Map<codigoOrigen, List<VueloInstancia>>>
    private final Map<Integer, Map<String, List<VueloInstancia>>> vuelosPorDiaYOrigen;

    /**
     * Constructor: Crea el WorldTemporal y expande los vuelos
     *
     * @param worldBase World base con templates
     * @param fechaInicio Fecha de inicio de la simulacion
     * @param numeroDias Numero de dias del horizonte temporal
     */
    public WorldTemporal(World worldBase, LocalDate fechaInicio, int numeroDias) {
        this(worldBase, fechaInicio.atStartOfDay(), numeroDias);
    }
    
    /**
     * Constructor con hora de inicio especifica
     *
     * @param worldBase World base con templates
     * @param horaInicio Fecha y hora de inicio de la simulacion (UTC)
     * @param numeroDias Numero de dias del horizonte temporal
     */
    public WorldTemporal(World worldBase, LocalDateTime horaInicio, int numeroDias) {
        this.worldBase = worldBase;
        this.fechaInicio = horaInicio.toLocalDate();
        this.horaInicioSimulacion = horaInicio;
        this.numeroDias = numeroDias;
        this.fechaFin = fechaInicio.plusDays(numeroDias - 1);

        log.info("Creando WorldTemporal para horizonte: {} a {} ({} dias), hora inicio: {}",
                fechaInicio, fechaFin, numeroDias, horaInicio);

        // Expandir vuelos
        long inicio = System.currentTimeMillis();
        this.vuelosExpandidos = expandirVuelos();
        this.vuelosPorDiaYOrigen = construirIndices();

        long duracion = System.currentTimeMillis() - inicio;
        log.info("WorldTemporal creado: {} vuelos expandidos desde {} templates en {} ms",
                vuelosExpandidos.size(), worldBase.getPlanesVuelo().size(), duracion);
    }

    /**
     * Expande todos los templates de vuelos para el horizonte temporal
     * Solo incluye vuelos que salgan despues de la hora de inicio de la simulacion
     *
     * @return Map de vuelos expandidos
     */
    private Map<String, VueloInstancia> expandirVuelos() {
        Map<String, VueloInstancia> vuelos = new HashMap<>();
        int vuelosFiltrados = 0;

        // Para cada template
        for (PlanDeVuelo template : worldBase.getPlanesVuelo()) {
            Aeropuerto origen = worldBase.getAeropuerto(template.getAeropuertoOrigen());
            Aeropuerto destino = worldBase.getAeropuerto(template.getAeropuertoDestino());

            if (origen == null || destino == null) {
                log.warn("Aeropuerto no encontrado para vuelo {} -> {}",
                        template.getAeropuertoOrigen(), template.getAeropuertoDestino());
                continue;
            }

            // Expandir para cada dia del horizonte
            for (int dia = 0; dia < numeroDias; dia++) {
                VueloInstancia instancia = new VueloInstancia(
                        template, dia, fechaInicio, origen, destino
                );
                
                // Filtrar vuelos que salgan antes de la hora de inicio de la simulacion
                if (instancia.getSalidaUTC().isBefore(horaInicioSimulacion)) {
                    vuelosFiltrados++;
                    continue;
                }
                
                vuelos.put(instancia.getId(), instancia);
            }
        }
        
        if (vuelosFiltrados > 0) {
            log.info("Se filtraron {} vuelos que salian antes de {}", 
                    vuelosFiltrados, horaInicioSimulacion);
        }

        return vuelos;
    }

    /**
     * Construye indices para busquedas rapidas
     *
     * @return Indice de vuelos por dia y origen
     */
    private Map<Integer, Map<String, List<VueloInstancia>>> construirIndices() {
        Map<Integer, Map<String, List<VueloInstancia>>> indice = new HashMap<>();

        for (VueloInstancia vuelo : vuelosExpandidos.values()) {
            int dia = vuelo.getDiaRelativo();
            String origen = vuelo.getOrigen();

            indice.computeIfAbsent(dia, k -> new HashMap<>())
                  .computeIfAbsent(origen, k -> new ArrayList<>())
                  .add(vuelo);
        }

        // Ordenar por hora de salida
        for (Map<String, List<VueloInstancia>> vuelosPorOrigen : indice.values()) {
            for (List<VueloInstancia> vuelos : vuelosPorOrigen.values()) {
                vuelos.sort(Comparator.comparing(VueloInstancia::getSalidaUTC));
            }
        }

        return indice;
    }

    /**
     * Obtiene un vuelo especifico por ID
     *
     * @param vueloId ID del vuelo
     * @return VueloInstancia o null si no existe
     */
    public VueloInstancia getVuelo(String vueloId) {
        return vuelosExpandidos.get(vueloId);
    }

    /**
     * Obtiene todos los vuelos disponibles desde un origen en un dia especifico
     *
     * @param codigoOrigen Codigo ICAO del origen
     * @param diaRelativo Dia relativo (0, 1, 2...)
     * @return Lista de vuelos (vacia si no hay)
     */
    public List<VueloInstancia> getVuelosDesde(String codigoOrigen, int diaRelativo) {
        if (diaRelativo < 0 || diaRelativo >= numeroDias) {
            return Collections.emptyList();
        }

        Map<String, List<VueloInstancia>> vuelosPorOrigen = vuelosPorDiaYOrigen.get(diaRelativo);
        if (vuelosPorOrigen == null) {
            return Collections.emptyList();
        }

        return vuelosPorOrigen.getOrDefault(codigoOrigen, Collections.emptyList());
    }

    /**
     * Busca vuelos directos desde origen a destino en un dia especifico
     *
     * @param origen Codigo ICAO origen
     * @param destino Codigo ICAO destino
     * @param diaRelativo Dia relativo
     * @return Lista de vuelos directos
     */
    public List<VueloInstancia> buscarVuelosDirectos(String origen, String destino, int diaRelativo) {
        return getVuelosDesde(origen, diaRelativo).stream()
                .filter(v -> v.getDestino().equals(destino))
                .collect(Collectors.toList());
    }

    /**
     * Verifica si un vuelo tiene capacidad disponible
     *
     * @param vueloId ID del vuelo
     * @param cantidad Cantidad a verificar
     * @return true si hay capacidad suficiente
     */
    public boolean tieneCapacidad(String vueloId, int cantidad) {
        VueloInstancia vuelo = vuelosExpandidos.get(vueloId);
        if (vuelo == null) {
            return false;
        }
        return vuelo.tieneCapacidad(cantidad);
    }

    /**
     * Asigna capacidad a un vuelo
     *
     * @param vueloId ID del vuelo
     * @param cantidad Cantidad a asignar
     * @return true si se pudo asignar
     */
    public boolean asignarCapacidad(String vueloId, int cantidad) {
        VueloInstancia vuelo = vuelosExpandidos.get(vueloId);
        if (vuelo == null) {
            log.warn("Intento de asignar capacidad a vuelo inexistente: {}", vueloId);
            return false;
        }
        return vuelo.asignarCapacidad(cantidad);
    }

    /**
     * Libera capacidad de un vuelo
     *
     * @param vueloId ID del vuelo
     * @param cantidad Cantidad a liberar
     */
    public void liberarCapacidad(String vueloId, int cantidad) {
        VueloInstancia vuelo = vuelosExpandidos.get(vueloId);
        if (vuelo != null) {
            vuelo.liberarCapacidad(cantidad);
        }
    }

    /**
     * Obtiene la capacidad restante de un vuelo
     *
     * @param vueloId ID del vuelo
     * @return Capacidad restante (0 si no existe)
     */
    public int getCapacidadRestante(String vueloId) {
        VueloInstancia vuelo = vuelosExpandidos.get(vueloId);
        return (vuelo != null) ? vuelo.getCapacidadRestante() : 0;
    }

    /**
     * Calcula el dia relativo de un pedido
     *
     * @param anio Año del pedido
     * @param mes Mes del pedido
     * @param dia Dia del pedido
     * @return Dia relativo (0, 1, 2...) o -1 si esta fuera del horizonte
     */
    public int calcularDiaRelativo(int anio, int mes, int dia) {
        LocalDate fechaPedido = LocalDate.of(anio, mes, dia);
        return calcularDiaRelativo(fechaPedido);
    }

    /**
     * Calcula el dia relativo de una fecha
     *
     * @param fecha Fecha a calcular
     * @return Dia relativo (0, 1, 2...) o -1 si esta fuera del horizonte
     */
    public int calcularDiaRelativo(LocalDate fecha) {
        if (fecha.isBefore(fechaInicio) || fecha.isAfter(fechaFin)) {
            return -1;
        }
        return (int) java.time.temporal.ChronoUnit.DAYS.between(fechaInicio, fecha);
    }

    /**
     * Verifica si una fecha esta dentro del horizonte temporal
     *
     * @param fecha Fecha a verificar
     * @return true si esta dentro del horizonte
     */
    public boolean estaDentroDelHorizonte(LocalDate fecha) {
        return !fecha.isBefore(fechaInicio) && !fecha.isAfter(fechaFin);
    }

    /**
     * Resetea las capacidades de todos los vuelos expandidos
     * Usado por el algoritmo genetico para evaluar diferentes soluciones
     */
    public void resetearCapacidades() {
        for (VueloInstancia vuelo : vuelosExpandidos.values()) {
            vuelo.resetearCapacidad();
        }
        log.trace("Capacidades de {} vuelos reseteadas", vuelosExpandidos.size());
    }

    /**
     * Obtiene estadisticas del WorldTemporal
     *
     * @return String con estadisticas
     */
    public String getEstadisticas() {
        int vuelosUsados = (int) vuelosExpandidos.values().stream()
                .filter(v -> v.getCapacidadUsada() > 0)
                .count();

        int capacidadTotalUsada = vuelosExpandidos.values().stream()
                .mapToInt(VueloInstancia::getCapacidadUsada)
                .sum();

        int capacidadTotalMaxima = vuelosExpandidos.values().stream()
                .mapToInt(VueloInstancia::getCapacidadMaxima)
                .sum();

        double porcentajeUsoGlobal = (capacidadTotalMaxima > 0) ?
                (capacidadTotalUsada * 100.0 / capacidadTotalMaxima) : 0.0;

        return String.format(
            "WorldTemporal[%s a %s (%d dias), %d vuelos expandidos, %d vuelos usados, " +
            "capacidad: %d/%d (%.1f%%), aeropuertos: %d, hubs: %d]",
            fechaInicio, fechaFin, numeroDias,
            vuelosExpandidos.size(), vuelosUsados,
            capacidadTotalUsada, capacidadTotalMaxima, porcentajeUsoGlobal,
            worldBase.getAeropuertos().size(), worldBase.getHubs().size()
        );
    }

    /**
     * Obtiene el aeropuerto del world base
     *
     * @param codigoICAO Codigo del aeropuerto
     * @return Aeropuerto o null si no existe
     */
    public Aeropuerto getAeropuerto(String codigoICAO) {
        return worldBase.getAeropuerto(codigoICAO);
    }

    /**
     * Obtiene los hubs del world base
     *
     * @return Lista de codigos de hubs
     */
    public List<String> getHubs() {
        return worldBase.getHubs();
    }

    /**
     * Verifica si dos aeropuertos estan en el mismo continente
     *
     * @param codigo1 Primer aeropuerto
     * @param codigo2 Segundo aeropuerto
     * @return true si estan en el mismo continente
     */
    public boolean mismoContinente(String codigo1, String codigo2) {
        return worldBase.mismoContinente(codigo1, codigo2);
    }

    /**
     * Calcula la distancia entre dos aeropuertos
     *
     * @param codigo1 Primer aeropuerto
     * @param codigo2 Segundo aeropuerto
     * @return Distancia en km
     */
    public double calcularDistanciaKm(String codigo1, String codigo2) {
        return worldBase.calcularDistanciaKm(codigo1, codigo2);
    }

    /**
     * Crea una copia independiente del WorldTemporal para evaluación paralela.
     * Los vuelos expandidos se copian con sus capacidades reseteadas.
     * 
     * @return Nueva instancia de WorldTemporal con vuelos independientes
     */
    public WorldTemporal copiarParaEvaluacion() {
        // Crear nuevo mapa de vuelos con capacidades reseteadas
        Map<String, VueloInstancia> vuelosCopiados = new HashMap<>();
        for (Map.Entry<String, VueloInstancia> entry : vuelosExpandidos.entrySet()) {
            vuelosCopiados.put(entry.getKey(), entry.getValue().copiar());
        }
        
        // Reconstruir índice por día y origen
        Map<Integer, Map<String, List<VueloInstancia>>> indiceCopiado = new HashMap<>();
        for (Map.Entry<Integer, Map<String, List<VueloInstancia>>> diaEntry : vuelosPorDiaYOrigen.entrySet()) {
            int dia = diaEntry.getKey();
            Map<String, List<VueloInstancia>> origenMap = new HashMap<>();
            
            for (Map.Entry<String, List<VueloInstancia>> origenEntry : diaEntry.getValue().entrySet()) {
                String origen = origenEntry.getKey();
                List<VueloInstancia> vuelosOrigen = new ArrayList<>();
                
                for (VueloInstancia vuelo : origenEntry.getValue()) {
                    // Usar el vuelo copiado del mapa principal
                    vuelosOrigen.add(vuelosCopiados.get(vuelo.getId()));
                }
                origenMap.put(origen, vuelosOrigen);
            }
            indiceCopiado.put(dia, origenMap);
        }
        
        return new WorldTemporal(this, vuelosCopiados, indiceCopiado);
    }

    /**
     * Constructor privado para copia
     */
    private WorldTemporal(WorldTemporal original, 
                          Map<String, VueloInstancia> vuelosCopiados,
                          Map<Integer, Map<String, List<VueloInstancia>>> indiceCopiado) {
        this.worldBase = original.worldBase;
        this.fechaInicio = original.fechaInicio;
        this.fechaFin = original.fechaFin;
        this.numeroDias = original.numeroDias;
        this.horaInicioSimulacion = original.horaInicioSimulacion;
        this.vuelosExpandidos = vuelosCopiados;
        this.vuelosPorDiaYOrigen = indiceCopiado;
    }
}
