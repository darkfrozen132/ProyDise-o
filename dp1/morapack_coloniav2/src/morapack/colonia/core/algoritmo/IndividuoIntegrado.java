package morapack.colonia.core.algoritmo;

import morapack.modelo.Pedido;
import morapack.modelo.Aeropuerto;
import morapack.datos.CargadorDatosCSV;
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
    
    // Mapa estático de aeropuertos para detección continental (copiado del genético)
    private static Map<String, Aeropuerto> mapaAeropuertos;
    
    public IndividuoIntegrado(List<Pedido> pedidos, PlanificadorAvanzadoEscalas planificador) {
        this.pedidos = pedidos;
        this.planificador = planificador;
        this.planificadorAvanzado = planificador; // Usar el mismo planificador para ambos
        this.asignacionSedes = new int[pedidos.size()];
        this.rutasCompletas = new ArrayList<>(Collections.nCopies(pedidos.size(), null));
        this.fitness = 0.0;
        this.fitnessCalculado = false;
    }
    
    /**
     * Constructor con planificador avanzado (mantenido por compatibilidad)
     */
    public IndividuoIntegrado(List<Pedido> pedidos, PlanificadorAvanzadoEscalas planificador, 
                             PlanificadorAvanzadoEscalas planificadorAvanzado) {
        this.pedidos = pedidos;
        this.planificador = planificador;
        this.planificadorAvanzado = planificadorAvanzado;
        this.asignacionSedes = new int[pedidos.size()];
        this.rutasCompletas = new ArrayList<>(Collections.nCopies(pedidos.size(), null));
        this.fitness = 0.0;
        this.fitnessCalculado = false;
    }
    
    /**
     * Inicializa el individuo con planificación real avanzada y asignación geográficamente inteligente
     */
    public void inicializarConPlanificacion() {
        Random random = new Random();
        String[] sedes = {"SPIM", "EBCI", "UBBB"}; // Usar las 3 sedes: Lima, Bruselas, Baku
        
        // Reiniciar capacidades del planificador avanzado
        if (planificadorAvanzado != null) {
            planificadorAvanzado.reiniciarCapacidades();
        }
        
        for (int i = 0; i < pedidos.size(); i++) {
            Pedido pedido = pedidos.get(i);
            String destino = pedido.getAeropuertoDestinoId();
            
            // 1. Asignación inteligente por región geográfica  
            int sedeIndex = asignarSedeInteligente(destino, random);
            asignacionSedes[i] = sedeIndex;
            
            // 2. Planificación completa usando el planificador avanzado
            String origen = sedes[sedeIndex];
            
            try {
                RutaCompleta ruta = null;
                
                // Intentar primero con planificador avanzado (múltiples escalas)
                if (planificadorAvanzado != null) {
                    int cantidad = pedido.getCantidadProductos();
                    ruta = planificadorAvanzado.planificarRuta(origen, destino, cantidad);
                    

                }
                
                // Si no funciona, usar planificador simple
                if (ruta == null) {
                    ruta = planificador.planificarRuta(origen, destino, pedido.getCantidadProductos());
                    

                }
                
                rutasCompletas.set(i, ruta);
            } catch (Exception e) {
                // Si no se puede planificar, mantener null
                rutasCompletas.set(i, null);

            }
        }
        
        fitnessCalculado = false;
    }
    
    /**
     * Asigna sede de manera inteligente basada en la región geográfica del destino
     * Ahora usa información geográfica real si está disponible
     */
    private int asignarSedeInteligente(String destino, Random random) {
        // Información de sedes con sus coordenadas aproximadas
        // SPIM (Lima): -12.0219, -77.1144 (Sudamérica)
        // EBCI (Bruselas): 50.9014, 4.4844 (Europa) 
        // UBBB (Baku): 40.4675, 50.0467 (Asia/Eurasia)
        
        // Mapeo geográfico mejorado basado en códigos ICAO
        
        // SPIM (Lima, Perú) - Sudamérica y Centroamérica
        if (destino.startsWith("S") || destino.startsWith("M") || destino.startsWith("T")) {
            // 90% probabilidad de usar SPIM para América
            if (random.nextDouble() < 0.9) {
                return 0; // SPIM
            }
        }
        
        // EBCI (Bruselas, Bélgica) - Europa y África
        if (destino.startsWith("E") || destino.startsWith("L") || 
            destino.startsWith("G") || destino.startsWith("F") || destino.startsWith("D")) {
            // 85% probabilidad de usar EBCI para Europa/África
            if (random.nextDouble() < 0.85) {
                return 1; // EBCI
            }
        }
        
        // UBBB (Baku, Azerbaiyán) - Asia, Medio Oriente, Oceanía
        if (destino.startsWith("O") || destino.startsWith("U") || destino.startsWith("Z") || 
            destino.startsWith("V") || destino.startsWith("R") || destino.startsWith("Y") ||
            destino.startsWith("P") || destino.startsWith("A") || destino.startsWith("N")) {
            // 85% probabilidad de usar UBBB para Asia/Oceanía
            if (random.nextDouble() < 0.85) {
                return 2; // UBBB
            }
        }
        
        // Para casos sin coincidencia clara, elegir la sede más probable
        // Distribuir proporcionalmente: SPIM 40%, EBCI 35%, UBBB 25%
        double probabilidad = random.nextDouble();
        if (probabilidad < 0.40) return 0; // SPIM
        else if (probabilidad < 0.75) return 1; // EBCI 
        else return 2; // UBBB
    }
    
    /**
     * Evalúa el fitness usando la misma función que el algoritmo genético
     */
    public void evaluarFitness() {
        if (fitnessCalculado) return;
        
        fitness = calcularFitness();
        fitnessCalculado = true;
    }
    
    /**
     * Calcula el fitness basado en planificación real de rutas
     * ✅ COPIADO del algoritmo genético para ser idéntico
     */
    private double calcularFitness() {
        double fitness = 0.0;
        String[] sedes = {"SPIM", "EBCI", "UBBB"};
        
        for (int i = 0; i < rutasCompletas.size(); i++) {
            RutaCompleta ruta = rutasCompletas.get(i);
            if (ruta != null) {
                // Bonus base por ruta planificada
                double bonusBase = 1000.0;
                
                // Factor continental/intercontinental
                String origen = sedes[asignacionSedes[i]]; // Sede asignada
                String destino = pedidos.get(i).getAeropuertoDestinoId();
                double factorContinental = esVueloContinental(origen, destino) ? 1.0 : 0.5;
                
                // Aplicar factor continental al bonus base
                fitness += bonusBase * factorContinental;
                
                // Penalizar por tiempo de viaje (también con factor continental)
                double tiempoTotal = ruta.calcularTiempoTotal();
                fitness -= (tiempoTotal * 0.5 * factorContinental); // Menos penalización para intercontinentales
                
                // Bonus por tipo de ruta (priorizar directos)
                String tipoRuta = ruta.getTipoRuta();
                double bonusTipoRuta = 0.0;
                if ("DIRECTO".equals(tipoRuta)) {
                    bonusTipoRuta = 400.0; // Máxima prioridad para directos
                } else if ("UNA_CONEXION".equals(tipoRuta)) {
                    bonusTipoRuta = 150.0; // Buena prioridad para una conexión
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
     * ✅ COPIADO: Determina si un vuelo es continental usando datos del CSV
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
     * ✅ COPIADO: Carga aeropuertos del CSV la primera vez que se necesiten
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
     * ✅ COPIADO: Métodos auxiliares para determinar continentes por código ICAO
     */
    private boolean esAeropuertoSudamericano(String codigo) {
        return codigo.startsWith("S");
    }
    
    private boolean esAeropuertoEuropeo(String codigo) {
        return codigo.startsWith("E") || codigo.startsWith("L") || codigo.startsWith("G") || 
               codigo.startsWith("F") || codigo.startsWith("D");
    }
    
    private boolean esAeropuertoAsiatico(String codigo) {
        return codigo.startsWith("Z") || codigo.startsWith("V") || codigo.startsWith("R") || 
               codigo.startsWith("U") || codigo.startsWith("O");
    }
    
    private boolean esAeropuertoNorteamericano(String codigo) {
        return codigo.startsWith("K") || codigo.startsWith("C") || codigo.startsWith("M");
    }
    
    private boolean esAeropuertoAfricano(String codigo) {
        return codigo.startsWith("H") || codigo.startsWith("D");
    }
    
    private boolean esAeropuertoOceania(String codigo) {
        return codigo.startsWith("Y") || codigo.startsWith("A") || codigo.startsWith("N");
    }
    
    /**
     * Crea una copia del individuo
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
    public void replanificarRuta(int indice) {
        if (indice >= 0 && indice < pedidos.size()) {
            Pedido pedido = pedidos.get(indice);
            Random random = new Random();
            String[] sedes = {"SPIM", "EBCI", "UBBB"};
            
            // Usar asignación inteligente en lugar de aleatoria
            int nuevaSedeIndex = asignarSedeInteligente(pedido.getAeropuertoDestinoId(), random);
            asignacionSedes[indice] = nuevaSedeIndex;
            
            // Re-planificar ruta completa usando la NUEVA sede asignada
            String origen = sedes[nuevaSedeIndex]; // Usar la misma sede asignada
            String destino = pedido.getAeropuertoDestinoId();
            
            try {
                RutaCompleta nuevaRuta = planificador.planificarRuta(origen, destino, pedido.getCantidadProductos());
                rutasCompletas.set(indice, nuevaRuta);
            } catch (Exception e) {
                rutasCompletas.set(indice, null);
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
}
