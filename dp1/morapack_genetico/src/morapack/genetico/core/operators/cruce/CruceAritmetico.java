package genetico.operators.cruce;

import genetico.Individuo;

/**
 * Cruce aritmético para genes numéricos reales
 */
public class CruceAritmetico implements OperadorCruce<Double> {
    private double alpha;
    
    public CruceAritmetico(double alpha) {
        this.alpha = alpha;
    }
    
    public CruceAritmetico() {
        this.alpha = 0.5; // Por defecto promedio
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public Individuo<Double>[] cruzar(Individuo<Double> padre1, Individuo<Double> padre2) {
        int longitud = padre1.getTamaño();
        
        // Crear hijos
        Individuo<Double> hijo1 = padre1.clonar();
        Individuo<Double> hijo2 = padre2.clonar();
        
        // Aplicar cruce aritmético
        for (int i = 0; i < longitud; i++) {
            double gen1 = padre1.getGen(i);
            double gen2 = padre2.getGen(i);
            
            hijo1.setGen(i, alpha * gen1 + (1 - alpha) * gen2);
            hijo2.setGen(i, alpha * gen2 + (1 - alpha) * gen1);
        }
        
        return new Individuo[]{hijo1, hijo2};
    }
}
