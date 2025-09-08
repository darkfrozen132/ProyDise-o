package genetico;

import java.util.*;

/**
 * Representa una población de individuos en el algoritmo genético
 */
public class Poblacion<T> implements Iterable<Individuo<T>> {
    private List<Individuo<T>> individuos;
    private int tamaño;
    private boolean ordenada;
    
    public Poblacion(int tamaño) {
        this.tamaño = tamaño;
        this.individuos = new ArrayList<>(tamaño);
        this.ordenada = false;
    }
    
    public Poblacion(List<Individuo<T>> individuos) {
        this.individuos = new ArrayList<>(individuos);
        this.tamaño = individuos.size();
        this.ordenada = false;
    }
    
    /**
     * Añade un individuo a la población
     */
    public void añadirIndividuo(Individuo<T> individuo) {
        if (individuos.size() < tamaño) {
            individuos.add(individuo);
            ordenada = false;
        }
    }
    
    /**
     * Obtiene un individuo por índice
     */
    public Individuo<T> getIndividuo(int indice) {
        return individuos.get(indice);
    }
    
    /**
     * Reemplaza un individuo en el índice especificado
     */
    public void setIndividuo(int indice, Individuo<T> individuo) {
        individuos.set(indice, individuo);
        ordenada = false;
    }
    
    /**
     * Ordena la población por fitness (de mayor a menor)
     */
    public void ordenar() {
        if (!ordenada) {
            individuos.sort(Collections.reverseOrder());
            ordenada = true;
        }
    }
    
    /**
     * Obtiene el mejor individuo (mayor fitness)
     */
    public Individuo<T> getMejorIndividuo() {
        if (!ordenada) {
            ordenar();
        }
        return individuos.get(0);
    }
    
    /**
     * Obtiene el peor individuo (menor fitness)
     */
    public Individuo<T> getPeorIndividuo() {
        if (!ordenada) {
            ordenar();
        }
        return individuos.get(individuos.size() - 1);
    }
    
    /**
     * Calcula el fitness promedio de la población
     */
    public double getFitnessPromedio() {
        return individuos.stream()
                .mapToDouble(Individuo::getFitness)
                .average()
                .orElse(0.0);
    }
    
    /**
     * Obtiene el fitness máximo de la población
     */
    public double getFitnessMaximo() {
        return individuos.stream()
                .mapToDouble(Individuo::getFitness)
                .max()
                .orElse(0.0);
    }
    
    /**
     * Obtiene el fitness mínimo de la población
     */
    public double getFitnessMinimo() {
        return individuos.stream()
                .mapToDouble(Individuo::getFitness)
                .min()
                .orElse(0.0);
    }
    
    public int getTamaño() {
        return individuos.size();
    }
    
    public int getTamañoMaximo() {
        return tamaño;
    }
    
    public boolean estaLlena() {
        return individuos.size() == tamaño;
    }
    
    public void limpiar() {
        individuos.clear();
        ordenada = false;
    }
    
    @Override
    public Iterator<Individuo<T>> iterator() {
        return individuos.iterator();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Población{tamaño=").append(individuos.size())
          .append(", fitnessPromedio=").append(String.format("%.2f", getFitnessPromedio()))
          .append(", fitnessMaximo=").append(String.format("%.2f", getFitnessMaximo()))
          .append("}\n");
        
        for (int i = 0; i < Math.min(5, individuos.size()); i++) {
            sb.append("  ").append(i).append(": ").append(individuos.get(i)).append("\n");
        }
        if (individuos.size() > 5) {
            sb.append("  ...\n");
        }
        return sb.toString();
    }
}
