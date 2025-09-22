package genetico.operators.cruce;

import genetico.Individuo;
import java.util.Random;

/**
 * Cruce BLX-α (Blend Crossover) para optimización continua
 */
public class CruceBLX implements OperadorCruce<Double> {
    private Random random = new Random();
    private double alpha;
    
    public CruceBLX(double alpha) {
        this.alpha = alpha;
    }
    
    public CruceBLX() {
        this.alpha = 0.5; // Valor típico
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public Individuo<Double>[] cruzar(Individuo<Double> padre1, Individuo<Double> padre2) {
        int longitud = padre1.getTamaño();
        
        // Crear hijos
        Individuo<Double> hijo1 = padre1.clonar();
        Individuo<Double> hijo2 = padre2.clonar();
        
        // Aplicar cruce BLX-α
        for (int i = 0; i < longitud; i++) {
            double gen1 = padre1.getGen(i);
            double gen2 = padre2.getGen(i);
            
            double minVal = Math.min(gen1, gen2);
            double maxVal = Math.max(gen1, gen2);
            double rango = maxVal - minVal;
            
            // Crear rango extendido
            double rangoMin = minVal - alpha * rango;
            double rangoMax = maxVal + alpha * rango;
            
            // Generar valores aleatorios en el rango extendido
            hijo1.setGen(i, rangoMin + random.nextDouble() * (rangoMax - rangoMin));
            hijo2.setGen(i, rangoMin + random.nextDouble() * (rangoMax - rangoMin));
        }
        
        return new Individuo[]{hijo1, hijo2};
    }
}
