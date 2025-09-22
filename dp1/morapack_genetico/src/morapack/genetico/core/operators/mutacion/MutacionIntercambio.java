package genetico.operators.mutacion;

import genetico.Individuo;
import java.util.Random;

/**
 * Mutación por intercambio de genes - intercambia dos genes aleatorios
 */
public class MutacionIntercambio<T> implements OperadorMutacion<T> {
    private Random random = new Random();
    
    @Override
    public void mutar(Individuo<T> individuo, double probabilidad) {
        if (random.nextDouble() < probabilidad) {
            int tamaño = individuo.getTamaño();
            if (tamaño >= 2) {
                int pos1 = random.nextInt(tamaño);
                int pos2 = random.nextInt(tamaño);
                
                // Asegurar que las posiciones sean diferentes
                while (pos1 == pos2) {
                    pos2 = random.nextInt(tamaño);
                }
                
                T temp = individuo.getGen(pos1);
                individuo.setGen(pos1, individuo.getGen(pos2));
                individuo.setGen(pos2, temp);
            }
        }
    }
}
