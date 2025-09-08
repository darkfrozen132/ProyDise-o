package genetico.operators.mutacion;

import genetico.Individuo;

/**
 * Interfaz para operadores de mutación
 */
public interface OperadorMutacion<T> {
    /**
     * Aplica mutación a un individuo
     * @param individuo El individuo a mutar
     * @param probabilidad La probabilidad de mutación (0.0 - 1.0)
     */
    void mutar(Individuo<T> individuo, double probabilidad);
}
