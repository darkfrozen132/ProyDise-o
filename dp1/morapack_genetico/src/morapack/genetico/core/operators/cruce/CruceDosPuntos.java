package genetico.operators.cruce;

import genetico.Individuo;
import java.util.Random;

/**
 * Cruce en dos puntos - combina los padres usando dos puntos de corte
 */
public class CruceDosPuntos<T> implements OperadorCruce<T> {
    private Random random = new Random();
    
    @Override
    @SuppressWarnings("unchecked")
    public Individuo<T>[] cruzar(Individuo<T> padre1, Individuo<T> padre2) {
        int longitud = padre1.getTamaño();
        
        if (longitud < 3) {
            // Si es muy pequeño, usar cruce de un punto
            CruceUnPunto<T> cruceUnPunto = new CruceUnPunto<>();
            return cruceUnPunto.cruzar(padre1, padre2);
        }
        
        // Dos puntos de corte aleatorios
        int punto1 = random.nextInt(longitud - 2) + 1;
        int punto2 = random.nextInt(longitud - punto1 - 1) + punto1 + 1;
        
        // Crear hijos
        Individuo<T> hijo1 = padre1.clonar();
        Individuo<T> hijo2 = padre2.clonar();
        
        // Intercambiar segmento entre los dos puntos
        for (int i = punto1; i <= punto2; i++) {
            hijo1.setGen(i, padre2.getGen(i));
            hijo2.setGen(i, padre1.getGen(i));
        }
        
        return new Individuo[]{hijo1, hijo2};
    }
}
