package morapack.planificacion;

import java.util.*;

/**
 * Representa una población de soluciones para el algoritmo genético
 */
public class PoblacionRutas {
    private List<SolucionRuta> individuos;
    private Random random = new Random();
    
    public PoblacionRutas(List<SolucionRuta> individuos) {
        this.individuos = new ArrayList<>(individuos);
        // Ordenar por fitness (menor es mejor)
        // Ordenar de mayor a menor fitness (mayor es mejor ahora)
        this.individuos.sort(Comparator.comparingDouble(SolucionRuta::getFitness).reversed());
    }
    
    public SolucionRuta getMejorIndividuo() {
        return individuos.isEmpty() ? null : individuos.get(0);
    }
    
    public List<SolucionRuta> getMejoresIndividuos(int cantidad) {
        int limite = Math.min(cantidad, individuos.size());
        return new ArrayList<>(individuos.subList(0, limite));
    }
    
    public SolucionRuta getIndividuoAleatorio() {
        if (individuos.isEmpty()) return null;
        return individuos.get(random.nextInt(individuos.size()));
    }
    
    public double getFitnessPromedio() {
        return individuos.stream()
                .mapToDouble(SolucionRuta::getFitness)
                .average()
                .orElse(Double.MAX_VALUE);
    }
    
    public int getTamaño() {
        return individuos.size();
    }
    
    public List<SolucionRuta> getIndividuos() {
        return new ArrayList<>(individuos);
    }
}
