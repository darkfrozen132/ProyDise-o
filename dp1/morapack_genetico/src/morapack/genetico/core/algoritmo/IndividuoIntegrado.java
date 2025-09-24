package morapack.genetico.core.algoritmo;

import morapack.modelo.Pedido;
import morapack.modelo.Aeropuerto;
import morapack.datos.CargadorDatosCSV;
import morapack.planificacion.PlanificadorAvanzadoEscalas;
import morapack.planificacion.PlanificadorAvanzadoEscalas;
import morapack.planificacion.RutaCompleta;
import java.util.*;

/**
 * Individuo integrado que combina asignación simple con planificación completa avanzada
 */
public class IndividuoIntegrado {
    
    private final List<Pedido> pedidos;
    private final PlanificadorAvanzadoEscalas planificador;
    private final PlanificadorAvanzadoEscalas planificadorAvanzado;
    
    // Cromosoma híbrido: tanto asignación simple como rutas completas
    private int[] asignacionSedes;           // Asignación simple (compatibilidad)
    private List<RutaCompleta> rutasCompletas; // Planificación completa (avanzado)
    
    private double fitness;
    private boolean fitnessCalculado;
    
    // 🎲 Sistema de semillas para reproducibilidad
    private final Random random;
    private final long seed;
    
    // 🌍 Mapa de aeropuertos para consulta de continentes
    private static Map<String, Aeropuerto> mapaAeropuertos = null;
    
    public IndividuoIntegrado(List<Pedido> pedidos, PlanificadorAvanzadoEscalas planificador) {
        this(pedidos, planificador, planificador, System.nanoTime());
    }
    
    public IndividuoIntegrado(List<Pedido> pedidos, PlanificadorAvanzadoEscalas planificador, long seed) {
        this(pedidos, planificador, planificador, seed);
    }
    
    /**
     * Constructor con planificador avanzado (mantenido por compatibilidad)
     */
    public IndividuoIntegrado(List<Pedido> pedidos, PlanificadorAvanzadoEscalas planificador, 
                             PlanificadorAvanzadoEscalas planificadorAvanzado) {
        this(pedidos, planificador, planificadorAvanzado, System.nanoTime());
    }
    
    /**
     * Constructor maestro con semilla personalizable 🎲
     */
    public IndividuoIntegrado(List<Pedido> pedidos, PlanificadorAvanzadoEscalas planificador, 
                             PlanificadorAvanzadoEscalas planificadorAvanzado, long seed) {
        this.pedidos = pedidos;
        this.planificador = planificador;
        this.planificadorAvanzado = planificadorAvanzado;
        this.seed = seed;
        this.random = new Random(seed);
        this.asignacionSedes = new int[pedidos.size()];
        this.rutasCompletas = new ArrayList<>(Collections.nCopies(pedidos.size(), null));
        this.fitness = 0.0;
        this.fitnessCalculado = false;
    }
    
    /**
     * Inicializa el individuo con planificación real avanzada y asignación geográficamente inteligente
     */
    /**
     * 🧬 INICIALIZACIÓN ALEATORIA REAL (Algoritmo Genético Auténtico)
     */
    public void inicializarConPlanificacion() {
        inicializarAleatorio(); // Cambio principal: usar inicialización aleatoria
    }
    
    /**
     * 🎲 Inicialización completamente aleatoria para empezar con fitness bajo
     */
    public void inicializarAleatorio() {
        String[] sedes = {"SPIM", "EBCI", "UBBB"};
        
        // ✅ REINICIAR capacidades UNA SOLA VEZ al inicio del individuo
        if (planificadorAvanzado != null) {
            planificadorAvanzado.reiniciarCapacidades();
        }
        
        for (int i = 0; i < pedidos.size(); i++) {
            Pedido pedido = pedidos.get(i);
            String destino = pedido.getAeropuertoDestinoId();
            
            // 🎲 ASIGNACIÓN COMPLETAMENTE ALEATORIA (no inteligente)  
            int sedeIndex = random.nextInt(sedes.length);
            asignacionSedes[i] = sedeIndex;
            
            // 🎲 PLANIFICACIÓN CONTROLADA - usar planificadorAvanzado que controla capacidad
            String origen = sedes[sedeIndex];
            
            try {
                // ✅ USAR PLANIFICADOR AVANZADO que controla capacidades compartidas Y tiempo del pedido
                RutaCompleta ruta = null;
                if (planificadorAvanzado != null) {
                    // Calcular tiempo mínimo basado en hora del pedido + ventana de recojo
                    int tiempoMinimo = PlanificadorAvanzadoEscalas.calcularTiempoMinimoPedido(pedido);
                    ruta = planificadorAvanzado.planificarRuta(origen, destino, pedido.getCantidadProductos(), tiempoMinimo);
                }
                // Fallback solo si el avanzado falla completamente
                if (ruta == null) {
                    ruta = planificador.planificarRuta(origen, destino, pedido.getCantidadProductos());
                }
                rutasCompletas.set(i, ruta);
            } catch (Exception e) {
                // Muchas rutas fallarán inicialmente (fitness bajo)
                rutasCompletas.set(i, null);
            }
        }
        
        fitnessCalculado = false;
    }
    
    /**
     * Asigna sede de manera inteligente basada en la región geográfica del destino
     * Ahora usa información geográfica real si está disponible
     */
    private int asignarSedeInteligente(String destino) {
        cargarAeropuertosSiEsNecesario();
        
        // ✅ MEJORADO: Usar continente del CSV si está disponible
        Aeropuerto aeropuertoDestino = mapaAeropuertos.get(destino);
        if (aeropuertoDestino != null && aeropuertoDestino.getContinente() != null) {
            String continente = aeropuertoDestino.getContinente();
            
            // Mapeo directo por continente desde CSV
            switch (continente) {
                case "SAM": // Sudamérica
                    if (random.nextDouble() < 0.90) return 0; // SPIM
                    break;
                case "EUR": // Europa  
                    if (random.nextDouble() < 0.85) return 1; // EBCI
                    break;
                case "ASI": // Asia
                    if (random.nextDouble() < 0.85) return 2; // UBBB
                    break;
                case "NAM": // Norteamérica (usar sede más cercana)
                    if (random.nextDouble() < 0.60) return 0; // SPIM (por cercanía geográfica)
                    else return 1; // EBCI
                case "AFR": // África (usar EBCI por cercanía)
                    if (random.nextDouble() < 0.80) return 1; // EBCI
                    break;
                case "OCE": // Oceanía (usar UBBB por región del Pacífico)
                    if (random.nextDouble() < 0.70) return 2; // UBBB
                    break;
            }
        }
        
        // FALLBACK: Mapeo geográfico basado en códigos ICAO (método anterior)
        // SPIM (Lima, Perú) - Sudamérica y Centroamérica
        if (destino.startsWith("S") || destino.startsWith("M") || destino.startsWith("T")) {
            if (random.nextDouble() < 0.9) return 0; // SPIM
        }
        
        // EBCI (Bruselas, Bélgica) - Europa y África
        if (destino.startsWith("E") || destino.startsWith("L") || 
            destino.startsWith("G") || destino.startsWith("F") || destino.startsWith("D")) {
            if (random.nextDouble() < 0.85) return 1; // EBCI
        }
        
        // UBBB (Baku, Azerbaiyán) - Asia, Medio Oriente, Oceanía
        if (destino.startsWith("O") || destino.startsWith("U") || destino.startsWith("Z") || 
            destino.startsWith("V") || destino.startsWith("R") || destino.startsWith("Y") ||
            destino.startsWith("P") || destino.startsWith("A") || destino.startsWith("N")) {
            if (random.nextDouble() < 0.85) return 2; // UBBB
        }
        
        // Para casos sin coincidencia clara, elegir la sede más probable
        // Distribuir proporcionalmente: SPIM 40%, EBCI 35%, UBBB 25%
        double probabilidad = random.nextDouble();
        if (probabilidad < 0.40) return 0; // SPIM
        else if (probabilidad < 0.75) return 1; // EBCI 
        else return 2; // UBBB
    }
    
    /**
     * Evalúa el fitness combinando ambos enfoques
     * ✅ CORREGIDO: Re-planifica todas las rutas para evitar duplicación de vuelos
     */
    public void evaluarFitness() {
        if (fitnessCalculado) return;
        
        // ✅ REINICIAR capacidades antes de re-planificar todas las rutas
        if (planificadorAvanzado != null) {
            planificadorAvanzado.reiniciarCapacidades();
        }
        
        // ✅ RE-PLANIFICAR todas las rutas con las sedes asignadas
        String[] sedes = {"SPIM", "EBCI", "UBBB"};
        for (int i = 0; i < pedidos.size(); i++) {
            Pedido pedido = pedidos.get(i);
            String origen = sedes[asignacionSedes[i]];
            String destino = pedido.getAeropuertoDestinoId();
            
            try {
                RutaCompleta ruta = null;
                if (planificadorAvanzado != null) {
                    // Calcular tiempo mínimo basado en hora del pedido + ventana de recojo
                    int tiempoMinimo = PlanificadorAvanzadoEscalas.calcularTiempoMinimoPedido(pedido);
                    ruta = planificadorAvanzado.planificarRuta(origen, destino, pedido.getCantidadProductos(), tiempoMinimo);
                }
                if (ruta == null) {
                    ruta = planificador.planificarRuta(origen, destino, pedido.getCantidadProductos());
                }
                rutasCompletas.set(i, ruta);
            } catch (Exception e) {
                rutasCompletas.set(i, null);
            }
        }
        
        // Calcular fitness basado en rutas reales planificadas
        fitness = calcularFitness();
        fitnessCalculado = true;
    }
    
    /**
     * Calcula el fitness basado en planificación real de rutas
     * ✅ NUEVO: Penaliza vuelos intercontinentales y prioriza vuelos directos
     */
    private double calcularFitness() {
        double fitness = 0.0;
        String[] sedes = {"SPIM", "EBCI", "UBBB"};
        
        for (int i = 0; i < rutasCompletas.size(); i++) {
            RutaCompleta ruta = rutasCompletas.get(i);
            if (ruta != null) {
                // Bonus base por ruta planificada
                double bonusBase = 1000.0;
                
                // ✅ NUEVO: Factor continental/intercontinental
                String origen = sedes[asignacionSedes[i]]; // Sede asignada
                String destino = pedidos.get(i).getAeropuertoDestinoId();
                double factorContinental = esVueloContinental(origen, destino) ? 1.0 : 0.5;
                
                // Aplicar factor continental al bonus base
                fitness += bonusBase * factorContinental;
                
                // Penalizar por tiempo de viaje (también con factor continental)
                double tiempoTotal = ruta.calcularTiempoTotal();
                fitness -= (tiempoTotal * 0.5 * factorContinental); // Menos penalización para intercontinentales
                
                // ✅ MEJORADO: Bonus por tipo de ruta (priorizar directos)
                String tipoRuta = ruta.getTipoRuta();
                double bonusTipoRuta = 0.0;
                if ("DIRECTO".equals(tipoRuta)) {
                    bonusTipoRuta = 400.0; // ✅ INCREMENTADO: Máxima prioridad para directos
                } else if ("UNA_CONEXION".equals(tipoRuta)) {
                    bonusTipoRuta = 150.0; // ✅ INCREMENTADO: Buena prioridad para una conexión
                } else if ("DOS_CONEXIONES".equals(tipoRuta)) {
                    bonusTipoRuta = 50.0;  // Mínima prioridad para dos conexiones
                }
                
                // Aplicar bonus con factor continental
                fitness += bonusTipoRuta * factorContinental;
                
            } else {
                // Penalización por pedido sin ruta
                fitness -= 500.0;
            }
        }
        
        return fitness;
    }
    
    /**
     * ✅ MEJORADO: Determina si un vuelo es continental usando datos del CSV
     */
    private boolean esVueloContinental(String origen, String destino) {
        cargarAeropuertosSiEsNecesario();
        
        Aeropuerto aeropuertoOrigen = mapaAeropuertos.get(origen);
        Aeropuerto aeropuertoDestino = mapaAeropuertos.get(destino);
        
        if (aeropuertoOrigen != null && aeropuertoDestino != null) {
            String continenteOrigen = aeropuertoOrigen.getContinente();
            String continenteDestino = aeropuertoDestino.getContinente();
            
            // Comparar continentes directamente desde CSV
            return continenteOrigen != null && continenteOrigen.equals(continenteDestino);
        }
        
        // Fallback al método anterior si no se encuentran en CSV
        return (esAeropuertoSudamericano(origen) && esAeropuertoSudamericano(destino)) ||
               (esAeropuertoEuropeo(origen) && esAeropuertoEuropeo(destino)) ||
               (esAeropuertoAsiatico(origen) && esAeropuertoAsiatico(destino)) ||
               (esAeropuertoNorteamericano(origen) && esAeropuertoNorteamericano(destino)) ||
               (esAeropuertoAfricano(origen) && esAeropuertoAfricano(destino)) ||
               (esAeropuertoOceania(origen) && esAeropuertoOceania(destino));
    }
    
    /**
     * ✅ NUEVO: Carga aeropuertos del CSV la primera vez que se necesiten
     */
    private static void cargarAeropuertosSiEsNecesario() {
        if (mapaAeropuertos == null) {
            mapaAeropuertos = new HashMap<>();
            try {
                List<Aeropuerto> aeropuertos = CargadorDatosCSV.cargarAeropuertos();
                for (Aeropuerto aeropuerto : aeropuertos) {
                    mapaAeropuertos.put(aeropuerto.getCodigoICAO(), aeropuerto);
                }
                // System.out.println("🌍 Aeropuertos cargados para detección continental: " + mapaAeropuertos.size());
            } catch (Exception e) {
                System.err.println("⚠️ Error cargando aeropuertos, usando fallback ICAO: " + e.getMessage());
                mapaAeropuertos = new HashMap<>(); // Mapa vacío para usar fallback
            }
        }
    }
    
    /**
     * ✅ NUEVO: Métodos auxiliares para determinar continentes por código ICAO
     */
    private boolean esAeropuertoSudamericano(String codigo) {
        return codigo.startsWith("S");
    }
    
    private boolean esAeropuertoEuropeo(String codigo) {
        return codigo.startsWith("E") || codigo.startsWith("L");
    }
    
    private boolean esAeropuertoAsiatico(String codigo) {
        return codigo.startsWith("O") || codigo.startsWith("U") || codigo.startsWith("V") || codigo.startsWith("Z");
    }
    
    private boolean esAeropuertoNorteamericano(String codigo) {
        return codigo.startsWith("K") || codigo.startsWith("C") || codigo.startsWith("M") || codigo.startsWith("T");
    }
    
    private boolean esAeropuertoAfricano(String codigo) {
        return codigo.startsWith("F") || codigo.startsWith("G") || codigo.startsWith("H");
    }
    
    private boolean esAeropuertoOceania(String codigo) {
        return codigo.startsWith("Y") || codigo.startsWith("A") || codigo.startsWith("N") || codigo.startsWith("P");
    }
    
    /**
     * Crea una copia del individuo (con nueva semilla aleatoria)
     */
    public IndividuoIntegrado copiar() {
        IndividuoIntegrado copia;
        if (planificadorAvanzado != null) {
            copia = new IndividuoIntegrado(pedidos, planificador, planificadorAvanzado);
        } else {
            copia = new IndividuoIntegrado(pedidos, planificador);
        }
        copia.asignacionSedes = Arrays.copyOf(this.asignacionSedes, this.asignacionSedes.length);
        copia.rutasCompletas = new ArrayList<>(this.rutasCompletas);
        copia.fitness = this.fitness;
        copia.fitnessCalculado = this.fitnessCalculado;
        return copia;
    }
    
    /**
     * Asigna una ruta específica
     */
    public void asignarRuta(int indice, RutaCompleta ruta) {
        if (indice >= 0 && indice < rutasCompletas.size()) {
            rutasCompletas.set(indice, ruta);
            fitnessCalculado = false;
        }
    }
    
    /**
     * Re-planifica una ruta específica
     */
    /**
     * 🔀 MUTACIÓN EVOLUTIVA - Intenta mejorar rutas problemáticas
     */
    public void replanificarRuta(int indice) {
        if (indice >= 0 && indice < pedidos.size()) {
            Pedido pedido = pedidos.get(indice);
            String[] sedes = {"SPIM", "EBCI", "UBBB"};
            
            RutaCompleta rutaActual = rutasCompletas.get(indice);
            
            // 🧬 MUTACIÓN INTELIGENTE: Si la ruta actual es mala o null, mejorarla
            if (rutaActual == null) {
                // 🎯 REPARACIÓN: Si no hay ruta, usar asignación inteligente
                int nuevaSedeIndex = asignarSedeInteligente(pedido.getAeropuertoDestinoId());
                asignacionSedes[indice] = nuevaSedeIndex;
                String origen = sedes[nuevaSedeIndex];
                
                try {
                    // Intentar con planificador avanzado primero
                    RutaCompleta nuevaRuta = null;
                    if (planificadorAvanzado != null) {
                        int tiempoMinimo = PlanificadorAvanzadoEscalas.calcularTiempoMinimoPedido(pedido);
                        nuevaRuta = planificadorAvanzado.planificarRuta(origen, pedido.getAeropuertoDestinoId(), pedido.getCantidadProductos(), tiempoMinimo);
                    }
                    if (nuevaRuta == null) {
                        nuevaRuta = planificador.planificarRuta(origen, pedido.getAeropuertoDestinoId(), pedido.getCantidadProductos());
                    }
                    rutasCompletas.set(indice, nuevaRuta);
                } catch (Exception e) {
                    rutasCompletas.set(indice, null);
                }
            } else {
                // 🔄 EXPLORACIÓN: Si ya hay ruta, explorar otras sedes para mejorar
                int sedeActual = asignacionSedes[indice];
                int nuevaSedeIndex;
                
                // 70% probabilidad de usar asignación inteligente, 30% aleatoria (exploración)
                if (random.nextDouble() < 0.7) {
                    nuevaSedeIndex = asignarSedeInteligente(pedido.getAeropuertoDestinoId());
                } else {
                    // Exploración aleatoria de otras sedes
                    do {
                        nuevaSedeIndex = random.nextInt(sedes.length);
                    } while (nuevaSedeIndex == sedeActual && sedes.length > 1);
                }
                
                asignacionSedes[indice] = nuevaSedeIndex;
                String origen = sedes[nuevaSedeIndex];
                
                try {
                    // ✅ USAR PLANIFICADOR AVANZADO que controla capacidades Y tiempo pedido (CORREGIDO)
                    RutaCompleta nuevaRuta = null;
                    if (planificadorAvanzado != null) {
                        int tiempoMinimo = PlanificadorAvanzadoEscalas.calcularTiempoMinimoPedido(pedido);
                        nuevaRuta = planificadorAvanzado.planificarRuta(origen, pedido.getAeropuertoDestinoId(), pedido.getCantidadProductos(), tiempoMinimo);
                    }
                    // Solo si el avanzado falla completamente
                    if (nuevaRuta == null) {
                        nuevaRuta = planificador.planificarRuta(origen, pedido.getAeropuertoDestinoId(), pedido.getCantidadProductos());
                    }
                    rutasCompletas.set(indice, nuevaRuta);
                } catch (Exception e) {
                    // Si la nueva sede falla, mantener la ruta anterior
                    asignacionSedes[indice] = sedeActual;
                }
            }
            
            fitnessCalculado = false;
        }
    }
    
    /**
     * Cuenta las rutas que fueron planificadas exitosamente
     */
    public int contarRutasPlanificadas() {
        return (int) rutasCompletas.stream().filter(Objects::nonNull).count();
    }
    
    /**
     * Obtiene resumen del individuo
     */
    public String obtenerResumen() {
        return String.format("Fitness: %.2f | Rutas: %d/%d",
                           getFitness(), contarRutasPlanificadas(), pedidos.size());
    }
    
    /**
     * Obtiene descripción detallada
     */
    public String obtenerDescripcionDetallada() {
        StringBuilder sb = new StringBuilder();
        sb.append("INDIVIDUO INTEGRADO - PLANIFICACIÓN HÍBRIDA\n");
        sb.append("==========================================\n\n");
        
        sb.append("📊 RESUMEN GENERAL:\n");
        sb.append("  • Fitness total: ").append(String.format("%.2f", fitness)).append("\n");
        sb.append("  • Rutas planificadas: ").append(contarRutasPlanificadas()).append("/").append(pedidos.size()).append("\n\n");
        
        sb.append("📋 DETALLE POR PEDIDO:\n");
        String[] sedes = {"SPIM", "EBCI", "UBBB"};
        
        for (int i = 0; i < pedidos.size(); i++) {
            Pedido pedido = pedidos.get(i);
            RutaCompleta ruta = rutasCompletas.get(i);
            String sedeOrigen = sedes[asignacionSedes[i]];
            
            sb.append(String.format("  [%02d] %s → %s | Sede: %s | ",
                    i + 1, sedeOrigen, pedido.getAeropuertoDestinoId(), sedeOrigen));
            
            if (ruta != null) {
                sb.append("Ruta: ").append(ruta.obtenerDescripcion());
            } else {
                sb.append("❌ Sin ruta planificada");
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }
    
    // Getters
    public double getFitness() { 
        if (!fitnessCalculado) evaluarFitness();
        return fitness; 
    }
    
    public RutaCompleta getRuta(int indice) { 
        return indice >= 0 && indice < rutasCompletas.size() ? rutasCompletas.get(indice) : null; 
    }
    
    public int[] getAsignacionSedes() { return Arrays.copyOf(asignacionSedes, asignacionSedes.length); }
    public List<RutaCompleta> getRutasCompletas() { return new ArrayList<>(rutasCompletas); }
    
    // 🎲 Métodos relacionados con semillas
    public long getSeed() { return seed; }
    
    /**
     * Reinicia el generador de números aleatorios con la semilla actual
     * (Útil para reproducir el mismo comportamiento)
     */
    public void reiniciarGeneradorAleatorio() {
        // Reiniciar el generador con la misma semilla
        random.setSeed(seed);
    }
    
    /**
     * Obtiene información de debug sobre la semilla
     */
    public String obtenerInfoSemilla() {
        return String.format("🎲 Semilla: %d | Hash: %08X", seed, Long.hashCode(seed));
    }
    
    /**
     * Crea una copia determinística con la misma semilla
     */
    public IndividuoIntegrado copiarConMismaSemilla() {
        IndividuoIntegrado copia;
        if (planificadorAvanzado != null) {
            copia = new IndividuoIntegrado(pedidos, planificador, planificadorAvanzado, seed);
        } else {
            copia = new IndividuoIntegrado(pedidos, planificador, seed);
        }
        copia.asignacionSedes = Arrays.copyOf(this.asignacionSedes, this.asignacionSedes.length);
        copia.rutasCompletas = new ArrayList<>(this.rutasCompletas);
        copia.fitness = this.fitness;
        copia.fitnessCalculado = this.fitnessCalculado;
        return copia;
    }
}
