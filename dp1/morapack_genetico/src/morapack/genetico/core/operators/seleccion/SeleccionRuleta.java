package genetico.operators.seleccion;

import genetico.Individuo;
import genetico.Poblacion;
import java.util.Random;

/**
 * Selección por ruleta - selección proporcional al fitness
 */
public class SeleccionRuleta<T> implements OperadorSeleccion<T> {
    private Random random = new Random();
    
    @Override
    public Individuo<T> seleccionar(Poblacion<T> poblacion) {
        // Calcular fitness total
        double fitnessTotal = 0;
        for (Individuo<T> individuo : poblacion) {
            // Asegurar que el fitness sea positivo
            double fitness = Math.max(0, individuo.getFitness());
            fitnessTotal += fitness;
        }
        
        if (fitnessTotal == 0) {
            // Si todos tienen fitness 0, selección aleatoria
            int indiceAleatorio = random.nextInt(poblacion.getTamaño());
            return poblacion.getIndividuo(indiceAleatorio);
        }
        
        // Generar número aleatorio
        double valorAleatorio = random.nextDouble() * fitnessTotal;
        double acumulado = 0;
        
        for (Individuo<T> individuo : poblacion) {
            acumulado += Math.max(0, individuo.getFitness());
            if (acumulado >= valorAleatorio) {
                return individuo;
            }
        }
        
        // En caso de error de punto flotante, devolver el último
        return poblacion.getIndividuo(poblacion.getTamaño() - 1);
    }
}
