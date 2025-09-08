package genetico.operators.cruce;

import genetico.Individuo;

/**
 * Interfaz para operadores de cruce
 */
public interface OperadorCruce<T> {
    /**
     * Aplica cruce entre dos individuos padres
     * @param padre1 Primer padre
     * @param padre2 Segundo padre
     * @return Array con uno o dos hijos (según la implementación)
     */
    Individuo<T>[] cruzar(Individuo<T> padre1, Individuo<T> padre2);
}
