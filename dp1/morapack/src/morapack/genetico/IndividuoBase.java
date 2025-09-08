package morapack.genetico;

/**
 * Clase base para individuos específicos del problema MoraPack
 * Hereda de la clase base del algoritmo genético
 */
public abstract class IndividuoBase {
    protected double fitness;
    protected boolean evaluado;
    
    public IndividuoBase() {
        this.fitness = 0.0;
        this.evaluado = false;
    }
    
    public abstract void evaluar();
    public abstract IndividuoBase clonar();
    public abstract void mutar(double probabilidad);
    
    public double getFitness() {
        if (!evaluado) evaluar();
        return fitness;
    }
    
    public boolean isEvaluado() {
        return evaluado;
    }
    
    public void setFitness(double fitness) {
        this.fitness = fitness;
        this.evaluado = true;
    }
    
    public void invalidar() {
        this.evaluado = false;
    }
    
    @Override
    public abstract String toString();
}
