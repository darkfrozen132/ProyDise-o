package genetico.operators.seleccion;

import genetico.Individuo;
import genetico.Poblacion;

/**
 * Interfaz para operadores de selección
 */
public interface OperadorSeleccion<T> {
    /**
     * Selecciona un individuo de la población
     * @param poblacion La población de la cual seleccionar
     * @return El individuo seleccionado
     */
    Individuo<T> seleccionar(Poblacion<T> poblacion);
    
    /**
     * Selecciona múltiples individuos de la población
     * @param poblacion La población de la cual seleccionar
     * @param cantidad Número de individuos a seleccionar
     * @return Array de individuos seleccionados
     */
    default Individuo<T>[] seleccionarMultiples(Poblacion<T> poblacion, int cantidad) {
        @SuppressWarnings("unchecked")
        Individuo<T>[] seleccionados = new Individuo[cantidad];
        for (int i = 0; i < cantidad; i++) {
            seleccionados[i] = seleccionar(poblacion);
        }
        return seleccionados;
    }
}
