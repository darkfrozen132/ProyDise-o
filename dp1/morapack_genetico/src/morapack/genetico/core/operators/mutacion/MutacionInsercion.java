package genetico.operators.mutacion;

import genetico.Individuo;
import java.util.Random;

/**
 * Mutación por inserción - mueve un gen a otra posición
 */
public class MutacionInsercion<T> implements OperadorMutacion<T> {
    private Random random = new Random();
    
    @Override
    public void mutar(Individuo<T> individuo, double probabilidad) {
        if (random.nextDouble() < probabilidad) {
            int tamaño = individuo.getTamaño();
            if (tamaño >= 2) {
                int pos1 = random.nextInt(tamaño);
                int pos2 = random.nextInt(tamaño);
                
                if (pos1 != pos2) {
                    T gen = individuo.getGen(pos1);
                    
                    // Mover genes hacia la izquierda o derecha
                    if (pos1 < pos2) {
                        for (int i = pos1; i < pos2; i++) {
                            individuo.setGen(i, individuo.getGen(i + 1));
                        }
                    } else {
                        for (int i = pos1; i > pos2; i--) {
                            individuo.setGen(i, individuo.getGen(i - 1));
                        }
                    }
                    
                    // Insertar el gen en la nueva posición
                    individuo.setGen(pos2, gen);
                }
            }
        }
    }
}
