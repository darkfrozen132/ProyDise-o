package genetico.operators.seleccion;

import genetico.Individuo;
import genetico.Poblacion;
import java.util.Random;

/**
 * Selección por torneo - selecciona el mejor de un subconjunto aleatorio
 */
public class SeleccionTorneo<T> implements OperadorSeleccion<T> {
    private Random random = new Random();
    private int tamañoTorneo;
    
    public SeleccionTorneo(int tamañoTorneo) {
        this.tamañoTorneo = tamañoTorneo;
    }
    
    public SeleccionTorneo() {
        this.tamañoTorneo = 3; // Valor por defecto
    }
    
    @Override
    public Individuo<T> seleccionar(Poblacion<T> poblacion) {
        Individuo<T> mejor = null;
        
        for (int i = 0; i < tamañoTorneo; i++) {
            int indiceAleatorio = random.nextInt(poblacion.getTamaño());
            Individuo<T> candidato = poblacion.getIndividuo(indiceAleatorio);
            
            if (mejor == null || candidato.getFitness() > mejor.getFitness()) {
                mejor = candidato;
            }
        }
        
        return mejor;
    }
}
