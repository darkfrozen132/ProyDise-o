package genetico.operators.mutacion;

import genetico.Individuo;
import java.util.Random;

/**
 * Mutación punto a punto - cada gen tiene probabilidad de mutar
 */
public class MutacionPuntoAPunto implements OperadorMutacion<Character> {
    private Random random = new Random();
    private char[] alfabeto;
    
    public MutacionPuntoAPunto(char[] alfabeto) {
        this.alfabeto = alfabeto;
    }
    
    @Override
    public void mutar(Individuo<Character> individuo, double probabilidad) {
        for (int i = 0; i < individuo.getTamaño(); i++) {
            if (random.nextDouble() < probabilidad) {
                char nuevoGen = alfabeto[random.nextInt(alfabeto.length)];
                individuo.setGen(i, nuevoGen);
            }
        }
    }
}
