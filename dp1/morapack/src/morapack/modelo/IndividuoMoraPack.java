package morapack.modelo;

import morapack.genetico.IndividuoBase;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

/**
 * Representa un individuo para el problema MoraPack
 * Cada individuo es una asignación de pedidos a sedes
 */
public class IndividuoMoraPack extends IndividuoBase {
    private List<Pedido> pedidos;
    private List<Aeropuerto> sedes;
    private int[] asignaciones; // asignaciones[i] = índice de sede para pedido i
    
    public IndividuoMoraPack(List<Pedido> pedidos, List<Aeropuerto> sedes) {
        this.pedidos = pedidos;
        this.sedes = sedes;
        this.asignaciones = new int[pedidos.size()];
        this.fitness = 0.0;
        this.evaluado = false;
        
        // Inicialización aleatoria
        Random random = new Random();
        for (int i = 0; i < asignaciones.length; i++) {
            asignaciones[i] = random.nextInt(sedes.size());
        }
    }
    
    public IndividuoMoraPack(List<Pedido> pedidos, List<Aeropuerto> sedes, int[] asignaciones) {
        this.pedidos = pedidos;
        this.sedes = sedes;
        this.asignaciones = asignaciones.clone();
        this.fitness = 0.0;
        this.evaluado = false;
    }
    
    @Override
    public void evaluar() {
        if (evaluado) return;
        
        double costoTotal = 0.0;
        double penalizacion = 0.0;
        int violacionesFactibilidad = 0;
        
        // Estadísticas por sede
        int[] pedidosPorSede = new int[sedes.size()];
        
        for (int i = 0; i < asignaciones.length; i++) {
            Pedido pedido = pedidos.get(i);
            Aeropuerto sede = sedes.get(asignaciones[i]);
            
            pedidosPorSede[asignaciones[i]]++;
            
            // Calcular distancia
            double distancia = sede.calcularDistancia(pedido.getDestino());
            
            // Calcular costo del envío
            double costo = pedido.calcularCosto(sede, distancia);
            costoTotal += costo;
            
            // Verificar factibilidad (cumplimiento de plazos)
            if (!pedido.esFactible(sede)) {
                violacionesFactibilidad++;
                penalizacion += costo * 2.0; // Penalización por incumplimiento
            }
        }
        
        // Penalización por desbalance de carga entre sedes
        double desbalance = calcularDesbalanceSedes(pedidosPorSede);
        penalizacion += desbalance * 1000.0; // Factor de penalización por desbalance
        
        // El fitness es el inverso del costo total (minimización)
        // Sumamos penalizaciones para favorecer soluciones factibles y balanceadas
        double costoConPenalizacion = costoTotal + penalizacion;
        this.fitness = 1.0 / (1.0 + costoConPenalizacion);
        
        this.evaluado = true;
    }
    
    /**
     * Calcula el desbalance de carga entre sedes
     */
    private double calcularDesbalanceSedes(int[] pedidosPorSede) {
        int totalPedidos = pedidos.size();
        int promedioPorSede = totalPedidos / sedes.size();
        
        double desbalance = 0.0;
        for (int count : pedidosPorSede) {
            desbalance += Math.pow(count - promedioPorSede, 2);
        }
        
        return Math.sqrt(desbalance / sedes.size());
    }
    
    @Override
    public IndividuoBase clonar() {
        return new IndividuoMoraPack(pedidos, sedes, asignaciones);
    }
    
    @Override
    public void mutar(double probabilidad) {
        Random random = new Random();
        
        for (int i = 0; i < asignaciones.length; i++) {
            if (random.nextDouble() < probabilidad) {
                // Mutación: cambiar la sede asignada al pedido
                int nuevaSede;
                do {
                    nuevaSede = random.nextInt(sedes.size());
                } while (nuevaSede == asignaciones[i] && sedes.size() > 1);
                
                asignaciones[i] = nuevaSede;
                this.evaluado = false; // Marcar para reevaluación
            }
        }
    }
    
    /**
     * Cruce específico para MoraPack: intercambio de asignaciones
     */
    public static List<IndividuoMoraPack> cruzar(IndividuoMoraPack padre1, IndividuoMoraPack padre2) {
        Random random = new Random();
        int tamano = padre1.asignaciones.length;
        
        int[] hijo1 = new int[tamano];
        int[] hijo2 = new int[tamano];
        
        // Cruce en un punto
        int puntoCorte = random.nextInt(tamano);
        
        for (int i = 0; i < tamano; i++) {
            if (i < puntoCorte) {
                hijo1[i] = padre1.asignaciones[i];
                hijo2[i] = padre2.asignaciones[i];
            } else {
                hijo1[i] = padre2.asignaciones[i];
                hijo2[i] = padre1.asignaciones[i];
            }
        }
        
        List<IndividuoMoraPack> hijos = new ArrayList<>();
        hijos.add(new IndividuoMoraPack(padre1.pedidos, padre1.sedes, hijo1));
        hijos.add(new IndividuoMoraPack(padre1.pedidos, padre1.sedes, hijo2));
        
        return hijos;
    }
    
    /**
     * Obtiene estadísticas de la solución
     */
    public EstadisticasSolucion obtenerEstadisticas() {
        if (!evaluado) evaluar();
        
        double costoTotal = 0.0;
        int violacionesFactibilidad = 0;
        int[] pedidosPorSede = new int[sedes.size()];
        
        for (int i = 0; i < asignaciones.length; i++) {
            Pedido pedido = pedidos.get(i);
            Aeropuerto sede = sedes.get(asignaciones[i]);
            
            pedidosPorSede[asignaciones[i]]++;
            
            double distancia = sede.calcularDistancia(pedido.getDestino());
            double costo = pedido.calcularCosto(sede, distancia);
            costoTotal += costo;
            
            if (!pedido.esFactible(sede)) {
                violacionesFactibilidad++;
            }
        }
        
        return new EstadisticasSolucion(costoTotal, violacionesFactibilidad, pedidosPorSede, fitness);
    }
    
    // Getters
    public int[] getAsignaciones() { return asignaciones.clone(); }
    public List<Pedido> getPedidos() { return pedidos; }
    public List<Aeropuerto> getSedes() { return sedes; }
    
    public Aeropuerto getSedeAsignada(int indicePedido) {
        return sedes.get(asignaciones[indicePedido]);
    }
    
    @Override
    public String toString() {
        if (!evaluado) evaluar();
        EstadisticasSolucion stats = obtenerEstadisticas();
        return String.format("MoraPack[Fitness=%.6f, Costo=%.2f, Violaciones=%d]", 
                           fitness, stats.costoTotal, stats.violacionesFactibilidad);
    }
    
    /**
     * Clase interna para estadísticas de la solución
     */
    public static class EstadisticasSolucion {
        public final double costoTotal;
        public final int violacionesFactibilidad;
        public final int[] pedidosPorSede;
        public final double fitness;
        
        public EstadisticasSolucion(double costoTotal, int violacionesFactibilidad, 
                                 int[] pedidosPorSede, double fitness) {
            this.costoTotal = costoTotal;
            this.violacionesFactibilidad = violacionesFactibilidad;
            this.pedidosPorSede = pedidosPorSede.clone();
            this.fitness = fitness;
        }
    }
}
