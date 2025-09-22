package morapack.genetico.core.algoritmo;

import morapack.modelo.Pedido;
import morapack.planificacion.PlanificadorConexiones;
import morapack.planificacion.PlanificadorAvanzadoEscalas;
import morapack.planificacion.RutaCompleta;
import java.util.*;

/**
 * Individuo integrado que combina asignación simple con planificación completa avanzada
 */
public class IndividuoIntegrado {
    
    private final List<Pedido> pedidos;
    private final PlanificadorConexiones planificador;
    private final PlanificadorAvanzadoEscalas planificadorAvanzado;
    
    // Cromosoma híbrido: tanto asignación simple como rutas completas
    private int[] asignacionSedes;           // Asignación simple (compatibilidad)
    private List<RutaCompleta> rutasCompletas; // Planificación completa (avanzado)
    
    private double fitness;
    private boolean fitnessCalculado;
    
    public IndividuoIntegrado(List<Pedido> pedidos, PlanificadorConexiones planificador) {
        this.pedidos = pedidos;
        this.planificador = planificador;
        this.planificadorAvanzado = new PlanificadorAvanzadoEscalas(null); // Se inicializará después
        this.asignacionSedes = new int[pedidos.size()];
        this.rutasCompletas = new ArrayList<>(Collections.nCopies(pedidos.size(), null));
        this.fitness = 0.0;
        this.fitnessCalculado = false;
    }
    
    /**
     * Constructor con planificador avanzado
     */
    public IndividuoIntegrado(List<Pedido> pedidos, PlanificadorConexiones planificador, 
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
     * Inicializa el individuo con planificación real avanzada
     */
    public void inicializarConPlanificacion() {
        Random random = new Random();
        String[] sedes = {"SPIM", "EBCI", "UBBB"};
        
        // Reiniciar capacidades del planificador avanzado
        if (planificadorAvanzado != null) {
            planificadorAvanzado.reiniciarCapacidades();
        }
        
        for (int i = 0; i < pedidos.size(); i++) {
            Pedido pedido = pedidos.get(i);
            
            // 1. Asignación simple aleatoria
            int sedeIndex = random.nextInt(3);
            asignacionSedes[i] = sedeIndex;
            
            // 2. Planificación completa usando el planificador avanzado
            String origen = sedes[sedeIndex]; // Usar la misma sede asignada
            String destino = pedido.getAeropuertoDestinoId();
            
            try {
                RutaCompleta ruta = null;
                
                // Intentar primero con planificador avanzado (múltiples escalas)
                if (planificadorAvanzado != null) {
                    int cantidad = pedido.getCantidadProductos();
                    ruta = planificadorAvanzado.planificarRuta(origen, destino, cantidad);
                }
                
                // Si no funciona, usar planificador simple
                if (ruta == null) {
                    ruta = planificador.buscarMejorRuta(origen, destino, "08:00");
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
     * Evalúa el fitness combinando ambos enfoques
     */
    public void evaluarFitness() {
        if (fitnessCalculado) return;
        
        double fitnessSimple = calcularFitnessSimple();
        double fitnessCompleto = calcularFitnessCompleto();
        
        // Combinar ambos fitness con pesos
        double peso_simple = 0.3;   // 30% fitness simple (compatibilidad)
        double peso_completo = 0.7; // 70% fitness completo (avanzado)
        
        fitness = peso_simple * fitnessSimple + peso_completo * fitnessCompleto;
        fitnessCalculado = true;
    }
    
    /**
     * Fitness simple basado en asignación de sedes
     */
    private double calcularFitnessSimple() {
        double fitness = 0.0;
        
        // Simulación simple: penalizar por distancia aproximada
        for (int i = 0; i < pedidos.size(); i++) {
            Pedido pedido = pedidos.get(i);
            int sedeAsignada = asignacionSedes[i];
            
            // Bonus por entrega (valores simulados)
            fitness += 100.0;
            
            // Penalización por "distancia" (simulada)
            fitness -= sedeAsignada * 10.0;
        }
        
        return Math.max(0, fitness);
    }
    
    /**
     * Fitness completo basado en planificación real
     */
    private double calcularFitnessCompleto() {
        double fitness = 0.0;
        int rutasPlanificadas = 0;
        
        for (RutaCompleta ruta : rutasCompletas) {
            if (ruta != null) {
                rutasPlanificadas++;
                
                // Bonus por ruta planificada
                fitness += 1000.0;
                
                // Penalizar por tiempo de viaje
                double tiempoTotal = ruta.calcularTiempoTotal();
                fitness -= tiempoTotal * 0.5; // 0.5 puntos por minuto
                
                // Bonus por rutas directas
                String tipoRuta = ruta.getTipoRuta();
                if ("DIRECTO".equals(tipoRuta)) {
                    fitness += 200.0;
                } else if ("UNA_CONEXION".equals(tipoRuta)) {
                    fitness += 100.0;
                } else if ("DOS_CONEXIONES".equals(tipoRuta)) {
                    fitness += 50.0;
                }
            } else {
                // Penalización por pedido sin ruta
                fitness -= 500.0;
            }
        }
        
        return fitness;
    }
    
    /**
     * Crea una copia del individuo
     */
    public IndividuoIntegrado copiar() {
        IndividuoIntegrado copia = new IndividuoIntegrado(pedidos, planificador);
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
            
            // Cambiar asignación simple
            int nuevaSedeIndex = random.nextInt(3);
            asignacionSedes[indice] = nuevaSedeIndex;
            
            // Re-planificar ruta completa usando la NUEVA sede asignada
            String origen = sedes[nuevaSedeIndex]; // Usar la misma sede asignada
            String destino = pedido.getAeropuertoDestinoId();
            
            try {
                RutaCompleta nuevaRuta = planificador.buscarMejorRuta(origen, destino, "08:00");
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
        return String.format("Fitness: %.2f | Rutas: %d/%d | Simple: %.2f | Completo: %.2f",
                           getFitness(), contarRutasPlanificadas(), pedidos.size(),
                           calcularFitnessSimple(), calcularFitnessCompleto());
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
        sb.append("  • Rutas planificadas: ").append(contarRutasPlanificadas()).append("/").append(pedidos.size()).append("\n");
        sb.append("  • Fitness simple: ").append(String.format("%.2f", calcularFitnessSimple())).append("\n");
        sb.append("  • Fitness completo: ").append(String.format("%.2f", calcularFitnessCompleto())).append("\n\n");
        
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
