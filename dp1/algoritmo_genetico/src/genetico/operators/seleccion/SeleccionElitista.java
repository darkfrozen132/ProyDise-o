package genetico.operators.seleccion;

import genetico.Individuo;
import genetico.Poblacion;

/**
 * Selección elitista - selecciona siempre los mejores individuos
 */
public class SeleccionElitista<T> implements OperadorSeleccion<T> {
    
    @Override
    public Individuo<T> seleccionar(Poblacion<T> poblacion) {
        poblacion.ordenar();
        return poblacion.getMejorIndividuo();
    }
    
    /**
     * Selecciona los N mejores individuos
     */
    @Override
    public Individuo<T>[] seleccionarMultiples(Poblacion<T> poblacion, int cantidad) {
        poblacion.ordenar();
        
        @SuppressWarnings("unchecked")
        Individuo<T>[] seleccionados = new Individuo[cantidad];
        
        for (int i = 0; i < cantidad && i < poblacion.getTamaño(); i++) {
            seleccionados[i] = poblacion.getIndividuo(i);
        }
        
        return seleccionados;
    }
}
