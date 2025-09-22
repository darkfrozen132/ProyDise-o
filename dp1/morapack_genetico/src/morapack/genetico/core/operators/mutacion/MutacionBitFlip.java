package genetico.operators.mutacion;

import genetico.Individuo;
import java.util.Random;

/**
 * Mutación BitFlip para cromosomas binarios
 */
public class MutacionBitFlip implements OperadorMutacion<Boolean> {
    private Random random = new Random();
    
    @Override
    public void mutar(Individuo<Boolean> individuo, double probabilidad) {
        for (int i = 0; i < individuo.getTamaño(); i++) {
            if (random.nextDouble() < probabilidad) {
                // Cambiar 0→1 o 1→0
                individuo.setGen(i, !individuo.getGen(i));
            }
        }
    }
}
