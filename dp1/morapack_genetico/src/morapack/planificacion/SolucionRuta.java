package morapack.planificacion;

/**
 * Representa una solución individual en el algoritmo genético
 * Cromosoma: array de índices de aeropuertos para cada pedido
 */
public class SolucionRuta implements Cloneable {
    private int[] cromosoma;
    private double fitness;
    
    public SolucionRuta(int[] cromosoma, double fitness) {
        this.cromosoma = cromosoma.clone();
        this.fitness = fitness;
    }
    
    public int[] getCromosoma() {
        return cromosoma;
    }
    
    public void setCromosoma(int[] cromosoma) {
        this.cromosoma = cromosoma.clone();
    }
    
    public double getFitness() {
        return fitness;
    }
    
    public void setFitness(double fitness) {
        this.fitness = fitness;
    }
    
    @Override
    public SolucionRuta clone() {
        return new SolucionRuta(this.cromosoma, this.fitness);
    }
    
    @Override
    public String toString() {
        return String.format("SolucionRuta[fitness=%.2f, genes=%d]", fitness, cromosoma.length);
    }
}
