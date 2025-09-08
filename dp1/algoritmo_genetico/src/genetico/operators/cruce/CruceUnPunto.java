package genetico.operators.cruce;

import genetico.Individuo;
import java.util.Random;

/**
 * Cruce en un punto - combina los padres en un punto de corte aleatorio
 */
public class CruceUnPunto<T> implements OperadorCruce<T> {
    private Random random = new Random();
    
    @Override
    @SuppressWarnings("unchecked")
    public Individuo<T>[] cruzar(Individuo<T> padre1, Individuo<T> padre2) {
        int longitud = padre1.getTamaño();
        
        // Punto de corte aleatorio
        int punto = random.nextInt(longitud - 1) + 1;
        
        // Crear hijos
        Individuo<T> hijo1 = padre1.clonar();
        Individuo<T> hijo2 = padre2.clonar();
        
        // Intercambiar segmentos después del punto de corte
        for (int i = punto; i < longitud; i++) {
            hijo1.setGen(i, padre2.getGen(i));
            hijo2.setGen(i, padre1.getGen(i));
        }
        
        return new Individuo[]{hijo1, hijo2};
    }
}
