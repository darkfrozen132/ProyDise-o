package genetico.operators.mutacion;

import genetico.Individuo;
import java.util.Random;

/**
 * Mutación gaussiana para genes de números reales
 */
public class MutacionGaussiana implements OperadorMutacion<Double> {
    private Random random = new Random();
    private double sigma; // Desviación estándar
    
    public MutacionGaussiana(double sigma) {
        this.sigma = sigma;
    }
    
    public MutacionGaussiana() {
        this.sigma = 0.1; // Valor por defecto
    }
    
    @Override
    public void mutar(Individuo<Double> individuo, double probabilidad) {
        for (int i = 0; i < individuo.getTamaño(); i++) {
            if (random.nextDouble() < probabilidad) {
                double valorActual = individuo.getGen(i);
                double ruido = random.nextGaussian() * sigma;
                individuo.setGen(i, valorActual + ruido);
            }
        }
    }
}
