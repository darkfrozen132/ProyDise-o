package com.proyecto.backend.algoritmo.core;

import java.util.Arrays;
import java.util.Random;

/**
 * Cromosoma del algoritmo genetico
 * Representa una solucion candidata mediante claves de prioridad
 * Cada gen corresponde a un plan de vuelo y su valor determina su prioridad de uso
 */
public class Chromosome {

    // Claves de prioridad (un gen por cada plan de vuelo)
    private final double[] genes;

    /**
     * Constructor con numero de genes
     *
     * @param numGenes Numero de planes de vuelo en el sistema
     */
    public Chromosome(int numGenes) {
        this.genes = new double[numGenes];
    }

    /**
     * Constructor con genes especificos
     *
     * @param genes Array de genes
     */
    public Chromosome(double[] genes) {
        this.genes = Arrays.copyOf(genes, genes.length);
    }

    /**
     * Inicializa el cromosoma con valores aleatorios [0, 1)
     *
     * @param random Generador de numeros aleatorios
     */
    public void inicializarAleatorio(Random random) {
        for (int i = 0; i < genes.length; i++) {
            genes[i] = random.nextDouble();
        }
    }

    /**
     * Obtiene el valor de un gen especifico
     *
     * @param indice Indice del gen
     * @return Valor del gen
     */
    public double getGen(int indice) {
        return genes[indice];
    }

    /**
     * Establece el valor de un gen especifico
     *
     * @param indice Indice del gen
     * @param valor Nuevo valor del gen
     */
    public void setGen(int indice, double valor) {
        genes[indice] = Math.max(0.0, Math.min(1.0, valor));
    }

    /**
     * Obtiene el numero de genes
     *
     * @return Numero de genes
     */
    public int getNumGenes() {
        return genes.length;
    }

    /**
     * Crea una copia profunda del cromosoma
     *
     * @return Nuevo cromosoma con los mismos genes
     */
    public Chromosome copiar() {
        return new Chromosome(Arrays.copyOf(genes, genes.length));
    }

    /**
     * Aplica mutacion gaussiana a los genes
     *
     * @param probabilidad Probabilidad de mutar cada gen
     * @param random Generador de numeros aleatorios
     */
    public void mutar(double probabilidad, Random random) {
        for (int i = 0; i < genes.length; i++) {
            if (random.nextDouble() < probabilidad) {
                // Mutacion gaussiana con desviacion estandar 0.1
                double mutacion = genes[i] + random.nextGaussian() * 0.1;
                genes[i] = Math.max(0.0, Math.min(1.0, mutacion));
            }
        }
    }

    /**
     * Realiza cruce uniforme con otro cromosoma
     *
     * @param otro Otro cromosoma padre
     * @param random Generador de numeros aleatorios
     * @return Nuevo cromosoma hijo
     */
    public Chromosome cruzar(Chromosome otro, Random random) {
        Chromosome hijo = new Chromosome(genes.length);
        for (int i = 0; i < genes.length; i++) {
            // Cruce uniforme: toma gen de uno u otro padre aleatoriamente
            hijo.genes[i] = random.nextBoolean() ? this.genes[i] : otro.genes[i];
        }
        return hijo;
    }

    @Override
    public String toString() {
        return String.format("Chromosome[genes=%d]", genes.length);
    }
}
