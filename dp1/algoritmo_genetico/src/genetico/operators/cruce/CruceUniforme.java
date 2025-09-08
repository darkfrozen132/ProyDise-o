package genetico.operators.cruce;

import genetico.Individuo;
import java.util.Random;

/**
 * Cruce uniforme - cada gen se toma aleatoriamente de uno de los padres
 */
public class CruceUniforme<T> implements OperadorCruce<T> {
    private Random random = new Random();
    private double probabilidad;
    
    public CruceUniforme(double probabilidad) {
        this.probabilidad = probabilidad;
    }
    
    public CruceUniforme() {
        this.probabilidad = 0.5; // Por defecto 50% de probabilidad
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public Individuo<T>[] cruzar(Individuo<T> padre1, Individuo<T> padre2) {
        int longitud = padre1.getTamaño();
        
        // Crear hijos
        Individuo<T> hijo1 = padre1.clonar();
        Individuo<T> hijo2 = padre2.clonar();
        
        // Para cada posición, decidir aleatoriamente de qué padre tomar el gen
        for (int i = 0; i < longitud; i++) {
            if (random.nextDouble() < probabilidad) {
                // Intercambiar genes en esta posición
                hijo1.setGen(i, padre2.getGen(i));
                hijo2.setGen(i, padre1.getGen(i));
            }
        }
        
        return new Individuo[]{hijo1, hijo2};
    }
}
