package com.proyecto.backend.algoritmo;

import com.proyecto.backend.model.Aeropuerto;
import com.proyecto.backend.model.Pedido;
import com.proyecto.backend.model.PlanDeVuelo;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Algoritmo Genético para optimizar la asignación de pedidos a vuelos
 * 
 * NUEVA REPRESENTACIÓN CON ESCALAS:
 * 
 * - Gen: Clase RutaPedido que contiene una lista de VueloInstancia
 * - Cromosoma: Array de genes (RutaPedido[])
 * - Cada pedido puede usar 1, 2 o 3 vuelos (directo, 1 escala, 2 escalas)
 * 
 * Ejemplo:
 * Pedidos: [P0, P1, P2]
 * 
 * Cromosoma:
 *   genes[0] = RutaPedido([VueloInstancia(5, 278)])                                          // P0: Directo
 *   genes[1] = RutaPedido([VueloInstancia(2, 278), VueloInstancia(8, 278)])                  // P1: 1 escala
 *   genes[2] = RutaPedido([VueloInstancia(1, 280), VueloInstancia(4, 280), VueloInstancia(9, 280)])  // P2: 2 escalas
 */
@Slf4j
public class genetico {

    // ============================================
    // CONSTANTES DEL ALGORITMO GENÉTICO
    // ============================================
    
    private static final int TAMANIO_POBLACION = 100;      // Cantidad de individuos en la población
    private static final int GENERACIONES_MAX = 500;       // Número máximo de generaciones
    private static final double TASA_MUTACION = 0.15;      // 15% de probabilidad de mutación
    private static final double TASA_CRUCE = 0.8;          // 80% de probabilidad de cruce
    private static final int ELITISMO = 5;                 // Los 5 mejores pasan automáticamente
    private static final double PENALIZACION_SOBRECAPACIDAD = 1000.0;  // Penalización por exceder capacidad
    private static final double PENALIZACION_TIEMPO = 10.0;            // Penalización por tiempo de espera
    private static final double PENALIZACION_ESCALA = 5.0;             // Penalización por cada escala adicional
    private static final int TIEMPO_MINIMO_ESCALA = 120;               // 2 horas entre vuelos (escala)
    private static final int MAX_ESCALAS = 2;                          // Máximo 2 escalas (3 vuelos)
    
    // ============================================
    // PARÁMETROS DE TIEMPO Y RESTRICCIONES
    // ============================================
    
    /**
     * Tiempo mínimo que debe llegar un pedido antes de la salida del vuelo (en minutos)
     * Los pedidos deben estar 30 minutos antes del vuelo
     */
    private static final int TIEMPO_ANTICIPACION_VUELO = 30; // minutos
    
    /**
     * Tiempo de procesamiento/entrega para vuelos continentales (en días)
     * Vuelos dentro del mismo continente
     */
    private static final int TIEMPO_PROCESAMIENTO_CONTINENTAL = 2; // días
    
    /**
     * Tiempo de procesamiento/entrega para vuelos intercontinentales (en días)
     * Vuelos entre diferentes continentes
     */
    private static final int TIEMPO_PROCESAMIENTO_INTERCONTINENTAL = 5; // días
    
    /**
     * Minutos en un día (para conversiones)
     */
    private static final int MINUTOS_POR_DIA = 1440; // 24 * 60
    
    /**
     * Año base para cálculos
     */
    private int anioBase = 2025;
    
    // ============================================
    // DATOS DEL PROBLEMA
    // ============================================
    
    private List<Pedido> pedidos;
    private List<PlanDeVuelo> vuelos;
    private Map<String, Aeropuerto> aeropuertos; // Mapa código ICAO -> Aeropuerto
    private Random random;
    
    // ============================================
    // CONSTRUCTOR
    // ============================================
    
    /**
     * Constructor del algoritmo genético
     * 
     * @param pedidos Lista de pedidos a asignar
     * @param vuelos Lista de planes de vuelo disponibles
     * @param aeropuertos Mapa de aeropuertos (código ICAO -> Aeropuerto)
     */
    public genetico(List<Pedido> pedidos, List<PlanDeVuelo> vuelos, Map<String, Aeropuerto> aeropuertos) {
        this.pedidos = pedidos;
        this.vuelos = vuelos;
        this.aeropuertos = aeropuertos;
        this.random = new Random();
    }
    
    // ============================================
    // MÉTODOS AUXILIARES PARA FECHAS Y TIEMPOS
    // ============================================
    
    /**
     * Convierte una fecha (día, mes, año) a día del año (1-365/366)
     * 
     * @param dia Día del mes (1-31)
     * @param mes Mes (1-12)
     * @param anio Año
     * @return Día del año (1-365 o 1-366 si es bisiesto)
     */
    private int obtenerDiaDelAnio(int dia, int mes, int anio) {
        int[] diasPorMes = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        
        // Verificar si es año bisiesto
        boolean esBisiesto = (anio % 4 == 0 && anio % 100 != 0) || (anio % 400 == 0);
        if (esBisiesto) {
            diasPorMes[1] = 29;
        }
        
        int diaDelAnio = dia;
        for (int i = 0; i < mes - 1; i++) {
            diaDelAnio += diasPorMes[i];
        }
        
        return diaDelAnio;
    }
    
    /**
     * Verifica si un aeropuerto es continental (América) o intercontinental
     * 
     * @param codigoICAO Código ICAO del aeropuerto
     * @return true si es continental (primera letra del código es K, M, S, o T)
     */
    private boolean esContinental(String codigoICAO) {
        if (codigoICAO == null || codigoICAO.isEmpty()) {
            return false;
        }
        char primeraLetra = codigoICAO.charAt(0);
        // K = USA, M = México/Centroamérica, S = Sudamérica, T = Caribe
        return primeraLetra == 'K' || primeraLetra == 'M' || 
               primeraLetra == 'S' || primeraLetra == 'T';
    }
    
    /**
     * Calcula el tiempo de procesamiento requerido según el tipo de vuelo
     * 
     * @param origen Código ICAO del aeropuerto de origen
     * @param destino Código ICAO del aeropuerto de destino
     * @return Días de procesamiento requeridos
     */
    private int obtenerTiempoProcesamiento(String origen, String destino) {
        boolean origenContinental = esContinental(origen);
        boolean destinoContinental = esContinental(destino);
        
        // Si ambos son continentales, es vuelo continental
        if (origenContinental && destinoContinental) {
            return TIEMPO_PROCESAMIENTO_CONTINENTAL;
        } else {
            return TIEMPO_PROCESAMIENTO_INTERCONTINENTAL;
        }
    }
    
    // ============================================
    // CLASE INTERNA: CROMOSOMA (INDIVIDUO)
    // ============================================
    
    /**
     * Representa una solución (asignación de pedidos a vuelos)
     */
    @Getter
    @Setter
    public static class Cromosoma implements Comparable<Cromosoma> {
        private int[] genes;           // genes[i] = índice del vuelo asignado al pedido i
        private double fitness;        // Calidad de la solución (menor es mejor)
        private boolean esValido;      // Si respeta todas las restricciones
        
        public Cromosoma(int numeroPedidos) {
            this.genes = new int[numeroPedidos];
            this.fitness = Double.MAX_VALUE;
            this.esValido = true;
        }
        
        public Cromosoma(int[] genes) {
            this.genes = genes.clone();
            this.fitness = Double.MAX_VALUE;
            this.esValido = true;
        }
        
        /**
         * Crea una copia profunda del cromosoma
         */
        public Cromosoma clonar() {
            Cromosoma clon = new Cromosoma(this.genes);
            clon.fitness = this.fitness;
            clon.esValido = this.esValido;
            return clon;
        }
        
        @Override
        public int compareTo(Cromosoma otro) {
            return Double.compare(this.fitness, otro.fitness);
        }
        
        @Override
        public String toString() {
            return String.format("Cromosoma[fitness=%.2f, valido=%s, genes=%s]", 
                fitness, esValido, Arrays.toString(genes));
        }
    }
    
    // ============================================
    // CONSTRUCTOR
    // ============================================
    
    public genetico() {
        this.random = new Random();
    }
    
    /**
     * Inicializa el algoritmo genético con los datos del problema
     * 
     * @param pedidos Lista de pedidos a asignar
     * @param vuelos Lista de planes de vuelo disponibles
     * @param aeropuertosList Lista de aeropuertos (para calcular si son continentales o intercontinentales)
     */
    public void inicializar(List<Pedido> pedidos, List<PlanDeVuelo> vuelos, List<Aeropuerto> aeropuertosList) {
        this.pedidos = pedidos;
        this.vuelos = vuelos;
        
        // Crear mapa de aeropuertos para acceso rápido por código ICAO
        this.aeropuertos = new HashMap<>();
        for (Aeropuerto aeropuerto : aeropuertosList) {
            this.aeropuertos.put(aeropuerto.getCodigoICAO(), aeropuerto);
        }
        
        log.info("=== ALGORITMO GENÉTICO INICIALIZADO ===");
        log.info("Pedidos a asignar: {}", pedidos.size());
        log.info("Vuelos disponibles: {}", vuelos.size());
        log.info("Aeropuertos cargados: {}", aeropuertos.size());
        log.info("Población: {}", TAMANIO_POBLACION);
        log.info("Generaciones máximas: {}", GENERACIONES_MAX);
        log.info("Tiempo anticipación vuelo: {} minutos", TIEMPO_ANTICIPACION_VUELO);
        log.info("Tiempo procesamiento continental: {} días", TIEMPO_PROCESAMIENTO_CONTINENTAL);
        log.info("Tiempo procesamiento intercontinental: {} días", TIEMPO_PROCESAMIENTO_INTERCONTINENTAL);
        log.info("=======================================");
    }
    
    // ============================================
    // MÉTODOS AUXILIARES PARA CÁLCULOS DE TIEMPO
    // ============================================
    
    /**
     * Determina si un vuelo es continental o intercontinental
     * 
     * @param vuelo Plan de vuelo a evaluar
     * @return true si es continental (mismo continente), false si es intercontinental
     */
    private boolean esVueloContinental(PlanDeVuelo vuelo) {
        Aeropuerto origen = aeropuertos.get(vuelo.getAeropuertoOrigen());
        Aeropuerto destino = aeropuertos.get(vuelo.getAeropuertoDestino());
        
        if (origen == null || destino == null) {
            log.warn("Aeropuerto no encontrado: origen={}, destino={}", 
                vuelo.getAeropuertoOrigen(), vuelo.getAeropuertoDestino());
            return true; // Asumir continental por defecto
        }
        
        // Comparar continentes
        return origen.getContinente().equalsIgnoreCase(destino.getContinente());
    }
    
    /**
     * Calcula el tiempo de procesamiento de un vuelo según si es continental o intercontinental
     * 
     * @param vuelo Plan de vuelo
     * @return Tiempo de procesamiento en días
     */
    private int obtenerTiempoProcesamiento(PlanDeVuelo vuelo) {
        if (esVueloContinental(vuelo)) {
            return TIEMPO_PROCESAMIENTO_CONTINENTAL;
        } else {
            return TIEMPO_PROCESAMIENTO_INTERCONTINENTAL;
        }
    }
    
    /**
     * Convierte día, hora y minuto a minutos totales desde el inicio del mes
     * 
     * @param dia Día del mes (1-31)
     * @param hora Hora del día (0-23)
     * @param minuto Minuto de la hora (0-59)
     * @return Minutos totales desde el día 1, 00:00
     */
    private int convertirATiempoTotal(int dia, int hora, int minuto) {
        return (dia - 1) * MINUTOS_POR_DIA + hora * 60 + minuto;
    }
    
    /**
     * Calcula el tiempo de espera de un pedido para un vuelo específico
     * 
     * @param pedido Pedido a evaluar
     * @param vuelo Vuelo al que se asignaría el pedido
     * @return Tiempo de espera en minutos (0 si no es factible)
     */
    private double calcularTiempoEspera(Pedido pedido, PlanDeVuelo vuelo) {
        // Tiempo en que llega el pedido (en minutos totales)
        int tiempoPedido = convertirATiempoTotal(pedido.getDia(), pedido.getHora(), pedido.getMinuto());
        
        // Tiempo de salida del vuelo (asumiendo día 1 para todos los vuelos)
        // Los vuelos se repiten diariamente
        int horaSalidaVuelo = vuelo.getHoraSalida().getHour();
        int minutoSalidaVuelo = vuelo.getHoraSalida().getMinute();
        
        // Buscar el próximo vuelo disponible después del pedido
        int tiempoVuelo = 0;
        int diaActual = pedido.getDia();
        
        // Buscar el día del vuelo que cumpla con el tiempo de anticipación
        while (true) {
            tiempoVuelo = convertirATiempoTotal(diaActual, horaSalidaVuelo, minutoSalidaVuelo);
            
            // El pedido debe llegar al menos TIEMPO_ANTICIPACION_VUELO minutos antes
            if (tiempoVuelo - tiempoPedido >= TIEMPO_ANTICIPACION_VUELO) {
                break; // Encontramos un vuelo válido
            }
            
            diaActual++; // Probar con el vuelo del día siguiente
            
            // Prevenir bucle infinito (máximo 31 días)
            if (diaActual > 31) {
                return Double.MAX_VALUE; // No factible
            }
        }
        
        // Tiempo de espera = tiempo hasta el vuelo + tiempo de procesamiento
        int tiempoEsperaHastaVuelo = tiempoVuelo - tiempoPedido;
        int tiempoProcesamiento = obtenerTiempoProcesamiento(vuelo) * MINUTOS_POR_DIA;
        
        return tiempoEsperaHastaVuelo + tiempoProcesamiento;
    }
    
    // ============================================
    // MÉTODO PRINCIPAL
    // ============================================
    
    /**
     * Ejecuta el algoritmo genético
     * @return La mejor solución encontrada
     */
    public Cromosoma ejecutar() {
        if (pedidos == null || vuelos == null || pedidos.isEmpty() || vuelos.isEmpty()) {
            throw new IllegalStateException("Debe inicializar el algoritmo con pedidos y vuelos");
        }
        
        log.info("Iniciando ejecución del algoritmo genético...");
        long tiempoInicio = System.currentTimeMillis();
        
        // 1. Crear población inicial
        List<Cromosoma> poblacion = crearPoblacionInicial();
        
        // 2. Evaluar fitness de la población inicial
        evaluarPoblacion(poblacion);
        
        // 3. Encontrar el mejor de la población inicial
        Cromosoma mejorGlobal = Collections.min(poblacion);
        log.info("Generación 0 - Mejor fitness: {}", mejorGlobal.getFitness());
        
        // 4. Evolucionar por generaciones
        for (int generacion = 1; generacion <= GENERACIONES_MAX; generacion++) {
            
            // Selección, cruce y mutación
            List<Cromosoma> nuevaPoblacion = evolucionarPoblacion(poblacion);
            
            // Evaluar nueva población
            evaluarPoblacion(nuevaPoblacion);
            
            // Actualizar población
            poblacion = nuevaPoblacion;
            
            // Actualizar mejor solución global
            Cromosoma mejorActual = Collections.min(poblacion);
            if (mejorActual.getFitness() < mejorGlobal.getFitness()) {
                mejorGlobal = mejorActual.clonar();
                log.info("Generación {} - ¡NUEVO MEJOR! Fitness: {}", generacion, mejorGlobal.getFitness());
            }
            
            // Log cada 50 generaciones
            if (generacion % 50 == 0) {
                log.info("Generación {} - Mejor fitness actual: {}", generacion, mejorActual.getFitness());
            }
            
            // Criterio de parada: convergencia
            if (esConvergente(poblacion)) {
                log.info("Convergencia alcanzada en generación {}", generacion);
                break;
            }
        }
        
        long tiempoTotal = System.currentTimeMillis() - tiempoInicio;
        log.info("=== ALGORITMO GENÉTICO COMPLETADO ===");
        log.info("Tiempo total: {} ms", tiempoTotal);
        log.info("Mejor fitness: {}", mejorGlobal.getFitness());
        log.info("Solución válida: {}", mejorGlobal.isEsValido());
        log.info("=====================================");
        
        return mejorGlobal;
    }
    
    // ============================================
    // INICIALIZACIÓN DE LA POBLACIÓN
    // ============================================
    
    /**
     * Crea la población inicial de cromosomas aleatorios
     */
    private List<Cromosoma> crearPoblacionInicial() {
        List<Cromosoma> poblacion = new ArrayList<>();
        
        for (int i = 0; i < TAMANIO_POBLACION; i++) {
            Cromosoma individuo = crearCromosomaAleatorio();
            poblacion.add(individuo);
        }
        
        log.debug("Población inicial creada con {} individuos", poblacion.size());
        return poblacion;
    }
    
    /**
     * Crea un cromosoma con genes aleatorios
     */
    private Cromosoma crearCromosomaAleatorio() {
        Cromosoma cromosoma = new Cromosoma(pedidos.size());
        
        // Asignar cada pedido a un vuelo aleatorio
        for (int i = 0; i < pedidos.size(); i++) {
            cromosoma.genes[i] = random.nextInt(vuelos.size());
        }
        
        return cromosoma;
    }
    
    // ============================================
    // EVALUACIÓN DE FITNESS
    // ============================================
    
    /**
     * Evalúa el fitness de toda la población
     */
    private void evaluarPoblacion(List<Cromosoma> poblacion) {
        for (Cromosoma cromosoma : poblacion) {
            evaluarFitness(cromosoma);
        }
    }
    
    /**
     * Calcula el fitness de un cromosoma (menor es mejor)
     * 
     * Función objetivo:
     * fitness = costoTiempoEspera + costoSobrecapacidad + costoVuelosUsados
     */
    private void evaluarFitness(Cromosoma cromosoma) {
        double fitness = 0.0;
        boolean esValido = true;
        
        // Mapear vuelos a sus pedidos asignados
        Map<Integer, List<Integer>> vueloAPedidos = new HashMap<>();
        for (int i = 0; i < cromosoma.genes.length; i++) {
            int indiceVuelo = cromosoma.genes[i];
            vueloAPedidos.computeIfAbsent(indiceVuelo, k -> new ArrayList<>()).add(i);
        }
        
        // Evaluar cada vuelo
        for (Map.Entry<Integer, List<Integer>> entrada : vueloAPedidos.entrySet()) {
            int indiceVuelo = entrada.getKey();
            List<Integer> indicesPedidos = entrada.getValue();
            
            PlanDeVuelo vuelo = vuelos.get(indiceVuelo);
            
            // Calcular carga total del vuelo y tiempo de espera de cada pedido
            int cargaTotal = 0;
            for (int indicePedido : indicesPedidos) {
                Pedido pedido = pedidos.get(indicePedido);
                cargaTotal += pedido.getCantidadProductos();
                
                // Calcular tiempo de espera del pedido
                double tiempoEspera = calcularTiempoEspera(pedido, vuelo);
                
                // Penalizar tiempo de espera (convertir a días para la penalización)
                double tiempoEsperaDias = tiempoEspera / MINUTOS_POR_DIA;
                fitness += tiempoEsperaDias * PENALIZACION_TIEMPO;
            }
            
            // Verificar capacidad
            if (cargaTotal > vuelo.getCapacidadMaxima()) {
                esValido = false;
                int exceso = cargaTotal - vuelo.getCapacidadMaxima();
                fitness += exceso * PENALIZACION_SOBRECAPACIDAD;
            }
            
            // Penalizar uso de vuelos (queremos usar menos vuelos)
            fitness += 1.0;
            
            // Recompensar utilización de capacidad
            double utilizacion = (double) cargaTotal / vuelo.getCapacidadMaxima();
            fitness -= utilizacion * 0.5; // Recompensa por buena utilización
        }
        
        cromosoma.setFitness(fitness);
        cromosoma.setEsValido(esValido);
    }
    
    // ============================================
    // OPERADORES GENÉTICOS
    // ============================================
    
    /**
     * Evoluciona la población aplicando selección, cruce y mutación
     */
    private List<Cromosoma> evolucionarPoblacion(List<Cromosoma> poblacionActual) {
        List<Cromosoma> nuevaPoblacion = new ArrayList<>();
        
        // 1. ELITISMO: Los mejores pasan directamente
        Collections.sort(poblacionActual);
        for (int i = 0; i < ELITISMO && i < poblacionActual.size(); i++) {
            nuevaPoblacion.add(poblacionActual.get(i).clonar());
        }
        
        // 2. Completar población con descendientes
        while (nuevaPoblacion.size() < TAMANIO_POBLACION) {
            
            // Seleccionar padres
            Cromosoma padre1 = seleccionarPorTorneo(poblacionActual);
            Cromosoma padre2 = seleccionarPorTorneo(poblacionActual);
            
            // Cruce
            Cromosoma hijo;
            if (random.nextDouble() < TASA_CRUCE) {
                hijo = cruzar(padre1, padre2);
            } else {
                hijo = padre1.clonar();
            }
            
            // Mutación
            if (random.nextDouble() < TASA_MUTACION) {
                mutar(hijo);
            }
            
            nuevaPoblacion.add(hijo);
        }
        
        return nuevaPoblacion;
    }
    
    /**
     * Selección por torneo: elige el mejor de k individuos aleatorios
     */
    private Cromosoma seleccionarPorTorneo(List<Cromosoma> poblacion) {
        int tamanioTorneo = 5;
        Cromosoma mejor = null;
        
        for (int i = 0; i < tamanioTorneo; i++) {
            Cromosoma candidato = poblacion.get(random.nextInt(poblacion.size()));
            if (mejor == null || candidato.getFitness() < mejor.getFitness()) {
                mejor = candidato;
            }
        }
        
        return mejor;
    }
    
    /**
     * Operador de cruce: Un Punto
     * Corta los cromosomas en un punto aleatorio e intercambia las partes
     */
    private Cromosoma cruzar(Cromosoma padre1, Cromosoma padre2) {
        int puntoCorte = random.nextInt(padre1.genes.length);
        Cromosoma hijo = new Cromosoma(padre1.genes.length);
        
        // Copiar primera parte del padre1
        for (int i = 0; i < puntoCorte; i++) {
            hijo.genes[i] = padre1.genes[i];
        }
        
        // Copiar segunda parte del padre2
        for (int i = puntoCorte; i < hijo.genes.length; i++) {
            hijo.genes[i] = padre2.genes[i];
        }
        
        return hijo;
    }
    
    /**
     * Operador de mutación: cambia aleatoriamente el vuelo asignado a algunos pedidos
     */
    private void mutar(Cromosoma cromosoma) {
        for (int i = 0; i < cromosoma.genes.length; i++) {
            if (random.nextDouble() < 0.1) { // 10% de probabilidad por gen
                cromosoma.genes[i] = random.nextInt(vuelos.size());
            }
        }
    }
    
    // ============================================
    // CRITERIOS DE CONVERGENCIA
    // ============================================
    
    /**
     * Verifica si la población ha convergido (poca diversidad)
     */
    private boolean esConvergente(List<Cromosoma> poblacion) {
        // Calcular desviación estándar del fitness
        double mediaFitness = poblacion.stream()
            .mapToDouble(Cromosoma::getFitness)
            .average()
            .orElse(0.0);
        
        double varianza = poblacion.stream()
            .mapToDouble(c -> Math.pow(c.getFitness() - mediaFitness, 2))
            .average()
            .orElse(0.0);
        
        double desviacionEstandar = Math.sqrt(varianza);
        
        // Si la desviación es muy pequeña, la población ha convergido
        return desviacionEstandar < 0.01;
    }
    
    // ============================================
    // MÉTODOS DE UTILIDAD
    // ============================================
    
    /**
     * Convierte un cromosoma en un mapa legible de asignaciones
     */
    public Map<String, List<Pedido>> convertirASolucion(Cromosoma cromosoma) {
        Map<String, List<Pedido>> asignaciones = new HashMap<>();
        
        for (int i = 0; i < cromosoma.genes.length; i++) {
            int indiceVuelo = cromosoma.genes[i];
            PlanDeVuelo vuelo = vuelos.get(indiceVuelo);
            Pedido pedido = pedidos.get(i);
            
            String claveVuelo = vuelo.getAeropuertoOrigen() + "-" + vuelo.getAeropuertoDestino();
            asignaciones.computeIfAbsent(claveVuelo, k -> new ArrayList<>()).add(pedido);
        }
        
        return asignaciones;
    }
    
    /**
     * Imprime estadísticas de la solución
     */
    public void imprimirEstadisticas(Cromosoma solucion) {
        Map<String, List<Pedido>> asignaciones = convertirASolucion(solucion);
        
        log.info("=== ESTADÍSTICAS DE LA SOLUCIÓN ===");
        log.info("Vuelos utilizados: {}", asignaciones.size());
        log.info("Fitness total: {}", solucion.getFitness());
        log.info("Solución válida: {}", solucion.isEsValido());
        
        for (Map.Entry<String, List<Pedido>> entrada : asignaciones.entrySet()) {
            String ruta = entrada.getKey();
            List<Pedido> pedidosVuelo = entrada.getValue();
            int cargaTotal = pedidosVuelo.stream()
                .mapToInt(Pedido::getCantidadProductos)
                .sum();
            
            log.info("Ruta {}: {} pedidos, carga total: {}", ruta, pedidosVuelo.size(), cargaTotal);
        }
        
        log.info("===================================");
    }
    
    // ============================================
    // MÉTODO MAIN PARA PRUEBAS
    // ============================================
    
    /**
     * Método main para probar el algoritmo genético de forma standalone
     */
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("    PRUEBA DEL ALGORITMO GENÉTICO - MoraPack    ");
        System.out.println("=================================================\n");
        
        // 1. Crear datos de prueba: Aeropuertos
        Map<String, Aeropuerto> aeropuertos = new HashMap<>();
        
        // Aeropuertos de prueba (usando constructor completo)
        Aeropuerto bogota = new Aeropuerto("SKBO", "Bogotá", "Colombia", -5, 10000, 
            4.7011, -74.1469, "América del Sur");
        aeropuertos.put("SKBO", bogota);
        
        Aeropuerto miami = new Aeropuerto("KMIA", "Miami", "Estados Unidos", -5, 15000,
            25.7959, -80.2870, "América del Norte");
        aeropuertos.put("KMIA", miami);
        
        Aeropuerto mexico = new Aeropuerto("MMMX", "Ciudad de México", "México", -6, 12000,
            19.4361, -99.0719, "América del Norte");
        aeropuertos.put("MMMX", mexico);
        
        Aeropuerto madrid = new Aeropuerto("LEMD", "Madrid", "España", 1, 20000,
            40.4719, -3.5626, "Europa");
        aeropuertos.put("LEMD", madrid);
        
        System.out.println("✓ Aeropuertos creados: " + aeropuertos.size());
        
        // 2. Crear planes de vuelo de prueba
        List<PlanDeVuelo> vuelos = new ArrayList<>();
        
        // Vuelo 1: Bogotá -> Miami (Continental)
        PlanDeVuelo v1 = new PlanDeVuelo("SKBO", "KMIA", 
            java.time.LocalTime.of(8, 0), java.time.LocalTime.of(12, 0), 500);
        vuelos.add(v1);
        
        // Vuelo 2: Miami -> Madrid (Intercontinental)
        PlanDeVuelo v2 = new PlanDeVuelo("KMIA", "LEMD",
            java.time.LocalTime.of(18, 0), java.time.LocalTime.of(8, 0), 800);
        vuelos.add(v2);
        
        // Vuelo 3: Bogotá -> México (Continental)
        PlanDeVuelo v3 = new PlanDeVuelo("SKBO", "MMMX",
            java.time.LocalTime.of(10, 0), java.time.LocalTime.of(14, 0), 400);
        vuelos.add(v3);
        
        // Vuelo 4: México -> Miami (Continental)
        PlanDeVuelo v4 = new PlanDeVuelo("MMMX", "KMIA",
            java.time.LocalTime.of(16, 0), java.time.LocalTime.of(20, 0), 600);
        vuelos.add(v4);
        
        System.out.println("✓ Planes de vuelo creados: " + vuelos.size());
        
        // 3. Crear pedidos de prueba
        List<Pedido> pedidos = new ArrayList<>();
        
        // Pedido 1: Bogotá -> Miami (puede ir directo)
        Pedido p1 = new Pedido(1, 6, 0, "KMIA", 100, "0001234");
        pedidos.add(p1);
        
        // Pedido 2: Bogotá -> Madrid (necesita conexión)
        Pedido p2 = new Pedido(1, 6, 30, "LEMD", 150, "0002345");
        pedidos.add(p2);
        
        // Pedido 3: Bogotá -> México (directo)
        Pedido p3 = new Pedido(1, 7, 0, "MMMX", 80, "0003456");
        pedidos.add(p3);
        
        // Pedido 4: Bogotá -> Miami (directo)
        Pedido p4 = new Pedido(1, 7, 30, "KMIA", 120, "0004567");
        pedidos.add(p4);
        
        // Pedido 5: Bogotá -> Miami (sobrecarga para probar restricciones)
        Pedido p5 = new Pedido(1, 7, 45, "KMIA", 200, "0005678");
        pedidos.add(p5);
        
        System.out.println("✓ Pedidos creados: " + pedidos.size());
        System.out.println();
        
        // 4. Crear y ejecutar el algoritmo genético
        System.out.println("Iniciando Algoritmo Genético...\n");
        
        genetico algoritmo = new genetico();
        algoritmo.inicializar(pedidos, vuelos, new ArrayList<>(aeropuertos.values()));
        
        long tiempoInicio = System.currentTimeMillis();
        Cromosoma mejorSolucion = algoritmo.ejecutar();
        long tiempoTotal = System.currentTimeMillis() - tiempoInicio;
        
        // 5. Mostrar resultados
        System.out.println("\n=================================================");
        System.out.println("              RESULTADOS FINALES                 ");
        System.out.println("=================================================");
        System.out.println("Tiempo de ejecución: " + tiempoTotal + " ms");
        System.out.println("Fitness de la mejor solución: " + mejorSolucion.getFitness());
        System.out.println("Solución válida: " + (mejorSolucion.isEsValido() ? "SÍ ✓" : "NO ✗"));
        System.out.println();
        
        // 6. Mostrar asignaciones detalladas
        System.out.println("=== ASIGNACIONES PEDIDO -> VUELO ===");
        for (int i = 0; i < pedidos.size(); i++) {
            Pedido pedido = pedidos.get(i);
            int indiceVuelo = mejorSolucion.getGenes()[i];
            PlanDeVuelo vuelo = vuelos.get(indiceVuelo);
            
            System.out.printf("Pedido Cliente %s (%d productos): %s -> %s%n",
                pedido.getClienteId(),
                pedido.getCantidadProductos(),
                vuelo.getAeropuertoOrigen(),
                vuelo.getAeropuertoDestino()
            );
        }
        System.out.println();
        
        // 7. Mostrar estadísticas por vuelo
        algoritmo.imprimirEstadisticas(mejorSolucion);
        
        System.out.println("\n=================================================");
        System.out.println("           PRUEBA COMPLETADA CON ÉXITO          ");
        System.out.println("=================================================");
    }
}