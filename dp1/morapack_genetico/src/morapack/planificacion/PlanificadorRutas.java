package morapack.planificacion;

import morapack.modelo.*;
import morapack.dao.impl.*;
import morapack.optimizacion.FuncionObjetivoOptimizada;
import morapack.genetico.core.*;
import java.util.*;

/**
 * Planificador de rutas que utiliza algoritmo genético para optimizar
 * la asignación de productos a rutas de vuelo
 */
public class PlanificadorRutas {
    
    private final FuncionObjetivoOptimizada funcionObjetivo;
    private final AeropuertoDAOImpl aeropuertoDAO;
    private final List<Pedido> pedidos;
    private final List<Vuelo> vuelos;
    
    // Parámetros del algoritmo genético
    private int tamañoPoblacion = 100;
    private int numeroGeneraciones = 200;
    private double probabilidadCruce = 0.8;
    private double probabilidadMutacion = 0.15;
    private boolean elitismo = true;
    
    // Historial de iteraciones
    private List<String> historialIteraciones = new ArrayList<>();
    
    public PlanificadorRutas(FuncionObjetivoOptimizada funcionObjetivo, 
                            AeropuertoDAOImpl aeropuertoDAO,
                            List<Pedido> pedidos, 
                            List<Vuelo> vuelos) {
        this.funcionObjetivo = funcionObjetivo;
        this.aeropuertoDAO = aeropuertoDAO;
        this.pedidos = pedidos;
        this.vuelos = vuelos;
    }
    
    /**
     * Planifica las rutas óptimas utilizando algoritmo genético
     */
    public PlanRutas planificarRutas() {
        System.out.println("🧬 INICIANDO PLANIFICACIÓN CON ALGORITMO GENÉTICO");
        System.out.printf("📊 Parámetros: %d individuos, %d generaciones%n", 
                         tamañoPoblacion, numeroGeneraciones);
        System.out.printf("🔀 Probabilidades: %.1f%% cruce, %.1f%% mutación%n", 
                         probabilidadCruce * 100, probabilidadMutacion * 100);
        System.out.println();
        
        // 1. Inicializar población
        PoblacionRutas poblacionInicial = generarPoblacionInicial();
        
        // 2. Ejecutar algoritmo genético
        SolucionRuta mejorSolucion = ejecutarAlgoritmoGenetico(poblacionInicial);
        
        // 3. Generar plan de rutas final
        PlanRutas planFinal = generarPlanRutas(mejorSolucion);
        
        System.out.println("✅ PLANIFICACIÓN COMPLETADA");
        return planFinal;
    }
    
    /**
     * Genera población inicial de soluciones
     */
    private PoblacionRutas generarPoblacionInicial() {
        System.out.println("🎲 Generando población inicial...");
        List<SolucionRuta> poblacion = new ArrayList<>();
        
        for (int i = 0; i < tamañoPoblacion; i++) {
            int[] cromosoma = funcionObjetivo.generarSolucionAleatoria();
            double fitness = funcionObjetivo.calcularFitness(cromosoma);
            poblacion.add(new SolucionRuta(cromosoma, fitness));
        }
        
        System.out.printf("✅ Población inicial: %d individuos generados%n", poblacion.size());
        return new PoblacionRutas(poblacion);
    }
    
    /**
     * Ejecuta el algoritmo genético principal
     */
    private SolucionRuta ejecutarAlgoritmoGenetico(PoblacionRutas poblacionInicial) {
        System.out.println("🧬 Ejecutando evolución genética...");
        
        PoblacionRutas poblacionActual = poblacionInicial;
        SolucionRuta mejorGlobal = poblacionActual.getMejorIndividuo();
        
        System.out.printf("📊 Fitness inicial: $%.2f%n", mejorGlobal.getFitness());
        
        for (int generacion = 0; generacion < numeroGeneraciones; generacion++) {
            // Nueva población
            List<SolucionRuta> nuevaPoblacion = new ArrayList<>();
            
            // Elitismo: conservar los mejores
            if (elitismo) {
                int numeroElites = Math.max(1, tamañoPoblacion / 10);
                List<SolucionRuta> elites = poblacionActual.getMejoresIndividuos(numeroElites);
                nuevaPoblacion.addAll(elites);
            }
            
            // Generar resto de la población
            while (nuevaPoblacion.size() < tamañoPoblacion) {
                // Selección por torneo
                SolucionRuta padre1 = seleccionTorneo(poblacionActual);
                SolucionRuta padre2 = seleccionTorneo(poblacionActual);
                
                // Cruce
                SolucionRuta[] hijos = cruce(padre1, padre2);
                
                // Mutación
                for (SolucionRuta hijo : hijos) {
                    if (Math.random() < probabilidadMutacion) {
                        mutacion(hijo);
                    }
                    if (nuevaPoblacion.size() < tamañoPoblacion) {
                        nuevaPoblacion.add(hijo);
                    }
                }
            }
            
            poblacionActual = new PoblacionRutas(nuevaPoblacion);
            SolucionRuta mejorActual = poblacionActual.getMejorIndividuo();
            
            // Actualizar mejor global
            if (mejorActual.getFitness() > mejorGlobal.getFitness()) { // Cambiado a > porque ahora mayor es mejor
                mejorGlobal = mejorActual;
            }
            
            // Calcular productos a tiempo y retrasados
            int productosATiempo = funcionObjetivo.obtenerProductosATiempo(mejorGlobal.getCromosoma());
            int productosRetrasados = funcionObjetivo.obtenerProductosRetrasados(mejorGlobal.getCromosoma());
            
            // Mostrar progreso CADA iteración con detalles
            String iteracionInfo = String.format("🧬 Iteración %d: Fitness = %.0f (A tiempo: %d, Retrasados: %d)", 
                generacion + 1, mejorGlobal.getFitness(), productosATiempo, productosRetrasados);
            System.out.println(iteracionInfo);
            
            // Guardar en historial
            historialIteraciones.add(iteracionInfo);
        }
        
        System.out.printf("🎯 Evolución completada. Fitness final: $%.2f%n", mejorGlobal.getFitness());
        return mejorGlobal;
    }
    
    /**
     * Selección por torneo
     */
    private SolucionRuta seleccionTorneo(PoblacionRutas poblacion) {
        int tamañoTorneo = 5;
        SolucionRuta mejor = poblacion.getIndividuoAleatorio();
        
        for (int i = 1; i < tamañoTorneo; i++) {
            SolucionRuta candidato = poblacion.getIndividuoAleatorio();
            if (candidato.getFitness() > mejor.getFitness()) { // Cambiado a > porque mayor es mejor
                mejor = candidato;
            }
        }
        
        return mejor;
    }
    
    /**
     * Operador de cruce - Cruce uniforme
     */
    private SolucionRuta[] cruce(SolucionRuta padre1, SolucionRuta padre2) {
        if (Math.random() > probabilidadCruce) {
            return new SolucionRuta[]{padre1.clone(), padre2.clone()};
        }
        
        int[] cromosoma1 = padre1.getCromosoma().clone();
        int[] cromosoma2 = padre2.getCromosoma().clone();
        
        // Cruce uniforme
        for (int i = 0; i < cromosoma1.length; i++) {
            if (Math.random() < 0.5) {
                int temp = cromosoma1[i];
                cromosoma1[i] = cromosoma2[i];
                cromosoma2[i] = temp;
            }
        }
        
        double fitness1 = funcionObjetivo.calcularFitness(cromosoma1);
        double fitness2 = funcionObjetivo.calcularFitness(cromosoma2);
        
        return new SolucionRuta[]{
            new SolucionRuta(cromosoma1, fitness1),
            new SolucionRuta(cromosoma2, fitness2)
        };
    }
    
    /**
     * Operador de mutación - Mutación por intercambio
     */
    private void mutacion(SolucionRuta individuo) {
        int[] cromosoma = individuo.getCromosoma();
        int numSedes = aeropuertoDAO.obtenerSedes().size();
        
        // Mutación por intercambio de genes
        int pos1 = (int) (Math.random() * cromosoma.length);
        int pos2 = (int) (Math.random() * cromosoma.length);
        
        // Intercambiar valores
        int temp = cromosoma[pos1];
        cromosoma[pos1] = cromosoma[pos2];
        cromosoma[pos2] = temp;
        
        // También mutación por cambio aleatorio
        if (Math.random() < 0.3) {
            int pos = (int) (Math.random() * cromosoma.length);
            cromosoma[pos] = (int) (Math.random() * numSedes);
        }
        
        // Recalcular fitness
        double nuevoFitness = funcionObjetivo.calcularFitness(cromosoma);
        individuo.setFitness(nuevoFitness);
    }
    
    /**
     * Genera el plan de rutas final a partir de la mejor solución
     */
    private PlanRutas generarPlanRutas(SolucionRuta mejorSolucion) {
        System.out.println("📋 Generando plan de rutas optimizado...");
        
        PlanRutas plan = new PlanRutas();
        plan.setFitnessTotal(mejorSolucion.getFitness());
        plan.setCapacidadTotalDiaria(funcionObjetivo.getCapacidadTotalDiaria());
        
        // Generar rutas individuales
        Map<String, Aeropuerto> mapaAeropuertos = new HashMap<>();
        for (Aeropuerto aeropuerto : aeropuertoDAO.obtenerTodos()) {
            mapaAeropuertos.put(aeropuerto.getCodigoICAO(), aeropuerto);
        }
        
        for (int i = 0; i < pedidos.size(); i++) {
            Pedido pedido = pedidos.get(i);
            // Usar el aeropuerto origen del pedido en lugar de las sedes
            Aeropuerto origen = mapaAeropuertos.get(pedido.getAeropuertoOrigenId());
            Aeropuerto destino = mapaAeropuertos.get(pedido.getAeropuertoDestinoId());
            
            if (origen != null && destino != null) {
                RutaOptimizada ruta = new RutaOptimizada();
                ruta.setPedidoId(pedido.getId());
                ruta.setOrigen(origen);
                ruta.setDestino(destino);
                ruta.setCantidadProductos(pedido.getCantidadProductos());
                ruta.setPrioridad(pedido.getPrioridad());
                ruta.setHoraEstimada(pedido.getHora());
                ruta.setDistanciaGMT(origen.calcularDistancia(destino));
                
                plan.agregarRuta(ruta);
            } else if (origen == null) {
                System.err.printf("⚠️  Aeropuerto origen %s no encontrado para pedido %s%n", 
                                pedido.getAeropuertoOrigenId(), pedido.getId());
            } else {
                System.err.printf("⚠️  Aeropuerto destino %s no encontrado para pedido %s%n", 
                                pedido.getAeropuertoDestinoId(), pedido.getId());
            }
        }
        
        // Calcular estadísticas
        plan.calcularEstadisticas();
        
        // Añadir información adicional al plan
        int productosATiempo = funcionObjetivo.obtenerProductosATiempo(mejorSolucion.getCromosoma());
        int productosRetrasados = funcionObjetivo.obtenerProductosRetrasados(mejorSolucion.getCromosoma());
        plan.setProductosATiempo(productosATiempo);
        plan.setProductosRetrasados(productosRetrasados);
        plan.setHistorialIteraciones(historialIteraciones);
        
        System.out.printf("✅ Plan generado: %d rutas optimizadas%n", plan.getRutas().size());
        return plan;
    }
    
    // Getters y setters para parámetros del AG
    public void setTamañoPoblacion(int tamañoPoblacion) {
        this.tamañoPoblacion = tamañoPoblacion;
    }
    
    public void setNumeroGeneraciones(int numeroGeneraciones) {
        this.numeroGeneraciones = numeroGeneraciones;
    }
    
    public void setProbabilidadCruce(double probabilidadCruce) {
        this.probabilidadCruce = probabilidadCruce;
    }
    
    public void setProbabilidadMutacion(double probabilidadMutacion) {
        this.probabilidadMutacion = probabilidadMutacion;
    }
    
    public List<String> getHistorialIteraciones() {
        return new ArrayList<>(historialIteraciones);
    }
}
