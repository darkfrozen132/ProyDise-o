package morapack.optimizacion;

import morapack.modelo.*;
import morapack.planificacion.RutaCompleta;
import java.util.*;

/**
 * Función objetivo simplificada para el algoritmo genético integrado
 */
public class FuncionObjetivoSimple {
    
    private final List<Pedido> pedidos;
    private final List<Vuelo> vuelos;
    private final Map<String, String> sedesDisponibles;
    
    public FuncionObjetivoSimple(List<Pedido> pedidos, List<Vuelo> vuelos) {
        this.pedidos = pedidos;
        this.vuelos = vuelos;
        this.sedesDisponibles = new HashMap<>();
        this.sedesDisponibles.put("SPIM", "Lima");
        this.sedesDisponibles.put("EBCI", "Charleroi");
        this.sedesDisponibles.put("UBBB", "Baku");
    }
    
    /**
     * Evalúa una solución simple (asignación de pedidos a sedes)
     */
    public double evaluarSolucionSimple(int[] solucion) {
        double fitness = 0.0;
        
        // Contadores por sede
        Map<Integer, Integer> contadores = new HashMap<>();
        contadores.put(0, 0); // SPIM
        contadores.put(1, 0); // EBCI
        contadores.put(2, 0); // UBBB
        
        // Contar asignaciones
        for (int asignacion : solucion) {
            contadores.put(asignacion, contadores.get(asignacion) + 1);
        }
        
        // Premiar distribución equilibrada
        double total = solucion.length;
        double equilibrio = 0.0;
        for (int count : contadores.values()) {
            double proporcion = count / total;
            equilibrio += Math.abs(proporcion - 0.333); // Deseamos 33.3% cada una
        }
        
        fitness += (1.0 - equilibrio) * 1000; // Mayor fitness = mejor equilibrio
        
        // Bonificación por volumen
        fitness += total * 10;
        
        return fitness;
    }
    
    /**
     * Evalúa una solución completa (con rutas)
     */
    public double evaluarSolucionCompleta(List<RutaCompleta> rutas) {
        double fitness = 0.0;
        
        if (rutas == null || rutas.isEmpty()) {
            return -10000; // Penalización severa por solución vacía
        }
        
        int rutasExitosas = 0;
        int totalVuelos = 0;
        int tiempoTotal = 0;
        
        for (RutaCompleta ruta : rutas) {
            if (ruta != null && ruta.esViable()) {
                rutasExitosas++;
                totalVuelos += ruta.getVuelos().size();
                tiempoTotal += ruta.calcularTiempoTotal();
            }
        }
        
        // Fitness basado en éxito de planificación
        fitness += rutasExitosas * 1000; // 1000 puntos por cada ruta exitosa
        
        // Bonificación por vuelos utilizados
        fitness += totalVuelos * 100;
        
        // Penalización por tiempo excesivo (preferir rutas más rápidas)
        fitness -= tiempoTotal * 0.5;
        
        // Bonificación por porcentaje de éxito
        double porcentajeExito = (double) rutasExitosas / rutas.size();
        fitness += porcentajeExito * 5000;
        
        return fitness;
    }
    
    /**
     * Evalúa una solución híbrida (combinación de simple y completa)
     */
    public double evaluarSolucionHibrida(int[] solucionSimple, List<RutaCompleta> rutasCompletas, 
                                       double pesoSimple, double pesoCompleto) {
        double fitnessSimple = evaluarSolucionSimple(solucionSimple);
        double fitnessCompleto = evaluarSolucionCompleta(rutasCompletas);
        
        return (fitnessSimple * pesoSimple) + (fitnessCompleto * pesoCompleto);
    }
    
    /**
     * Genera una solución aleatoria simple
     */
    public int[] generarSolucionAleatoria() {
        Random random = new Random();
        int[] solucion = new int[pedidos.size()];
        
        for (int i = 0; i < solucion.length; i++) {
            solucion[i] = random.nextInt(3); // 0=SPIM, 1=EBCI, 2=UBBB
        }
        
        return solucion;
    }
    
    /**
     * Obtiene el nombre de la sede por índice
     */
    public String obtenerNombreSede(int indice) {
        switch (indice) {
            case 0: return "SPIM";
            case 1: return "EBCI";
            case 2: return "UBBB";
            default: return "DESCONOCIDO";
        }
    }
    
    /**
     * Obtiene estadísticas de una solución
     */
    public void mostrarEstadisticas(int[] solucion) {
        Map<Integer, Integer> contadores = new HashMap<>();
        contadores.put(0, 0);
        contadores.put(1, 0);
        contadores.put(2, 0);
        
        for (int asignacion : solucion) {
            contadores.put(asignacion, contadores.get(asignacion) + 1);
        }
        
        System.out.println("📊 Estadísticas de asignación:");
        System.out.println("   SPIM: " + contadores.get(0) + " pedidos");
        System.out.println("   EBCI: " + contadores.get(1) + " pedidos");
        System.out.println("   UBBB: " + contadores.get(2) + " pedidos");
        System.out.println("   Total: " + solucion.length + " pedidos");
    }
}
