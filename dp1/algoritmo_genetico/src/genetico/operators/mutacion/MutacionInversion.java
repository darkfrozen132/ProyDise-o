package genetico.operators.mutacion;

import genetico.Individuo;
import java.util.Random;

/**
 * Mutación por inversión - invierte un segmento del genotipo
 */
public class MutacionInversion<T> implements OperadorMutacion<T> {
    private Random random = new Random();
    
    @Override
    public void mutar(Individuo<T> individuo, double probabilidad) {
        if (random.nextDouble() < probabilidad) {
            int tamaño = individuo.getTamaño();
            if (tamaño >= 2) {
                int pos1 = random.nextInt(tamaño);
                int pos2 = random.nextInt(tamaño);
                
                if (pos1 > pos2) {
                    int temp = pos1;
                    pos1 = pos2;
                    pos2 = temp;
                }
                
                // Invertir el segmento
                while (pos1 < pos2) {
                    T temp = individuo.getGen(pos1);
                    individuo.setGen(pos1, individuo.getGen(pos2));
                    individuo.setGen(pos2, temp);
                    pos1++;
                    pos2--;
                }
            }
        }
    }
}
