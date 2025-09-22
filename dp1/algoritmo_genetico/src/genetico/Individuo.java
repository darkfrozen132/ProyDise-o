package genetico;

import java.util.Arrays;
import java.util.Random;

/**
 * Representa un individuo en el algoritmo genético
 * @param <T> Tipo de dato del genotipo
 */
public abstract class Individuo<T> implements Comparable<Individuo<T>> {
    protected T[] genotipo;
    protected double fitness;
    protected boolean fitnessCalculado;
    protected static Random random = new Random();
    
    public Individuo(int tamaño) {
        this.genotipo = crearGenotipo(tamaño);
        this.fitnessCalculado = false;
    }
    
    public Individuo(T[] genotipo) {
        this.genotipo = genotipo.clone();
        this.fitnessCalculado = false;
    }
    
    /**
     * Crea un nuevo genotipo del tamaño especificado
     */
    protected abstract T[] crearGenotipo(int tamaño);
    
    /**
     * Calcula el fitness del individuo
     */
    public abstract double calcularFitness();
    
    /**
     * Crea una copia del individuo
     */
    public abstract Individuo<T> clonar();
    
    /**
     * Inicializa el genotipo aleatoriamente
     */
    public abstract void inicializarAleatorio();
    
    public T[] getGenotipo() {
        return genotipo.clone();
    }
    
    public void setGenotipo(T[] genotipo) {
        this.genotipo = genotipo.clone();
        this.fitnessCalculado = false;
    }
    
    public T getGen(int indice) {
        return genotipo[indice];
    }
    
    public void setGen(int indice, T valor) {
        genotipo[indice] = valor;
        this.fitnessCalculado = false;
    }

    public int getTamaño() {
        return genotipo.length;
    }

    /**
     * Indica si el fitness ya fue calculado para el individuo.
     */
    public boolean isFitnessCalculado() {
        return fitnessCalculado;
    }

    /**
     * Marca el fitness como desactualizado para que se vuelva a calcular.
     */
    public void invalidarFitness() {
        this.fitnessCalculado = false;
    }

    public double getFitness() {
        if (!fitnessCalculado) {
            fitness = calcularFitness();
            fitnessCalculado = true;
        }
        return fitness;
    }
    
    @Override
    public int compareTo(Individuo<T> otro) {
        return Double.compare(this.getFitness(), otro.getFitness());
    }
    
    @Override
    public String toString() {
        return "Individuo{genotipo=" + Arrays.toString(genotipo) + 
               ", fitness=" + getFitness() + "}";
    }
}
