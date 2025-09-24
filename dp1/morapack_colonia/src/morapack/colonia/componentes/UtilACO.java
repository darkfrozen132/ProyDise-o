package morapack.colonia.componentes;

import java.util.Random;

/**
 * Utilidades para el algoritmo de colonia de hormigas
 */
public class UtilACO {
    private static final Random random = new Random();
    
    /**
     * Selección proporcional basada en probabilidades (ruleta)
     */
    public static int seleccionRuleta(double[] probabilidades) {
        double suma = 0.0;
        for (double prob : probabilidades) {
            suma += prob;
        }
        
        if (suma == 0.0) {
            // Si todas las probabilidades son 0, selección aleatoria
            return random.nextInt(probabilidades.length);
        }
        
        double r = random.nextDouble() * suma;
        double acumulado = 0.0;
        
        for (int i = 0; i < probabilidades.length; i++) {
            acumulado += probabilidades[i];
            if (r <= acumulado) {
                return i;
            }
        }
        
        return probabilidades.length - 1;
    }
    
    /**
     * Selección elitista (mejor opción)
     */
    public static int seleccionElitista(double[] valores) {
        int mejorIndice = 0;
        double mejorValor = valores[0];
        
        for (int i = 1; i < valores.length; i++) {
            if (valores[i] > mejorValor) {
                mejorValor = valores[i];
                mejorIndice = i;
            }
        }
        
        return mejorIndice;
    }
    
    /**
     * Normaliza un array de valores entre 0 y 1
     */
    public static double[] normalizar(double[] valores) {
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        
        for (double valor : valores) {
            if (valor < min) min = valor;
            if (valor > max) max = valor;
        }
        
        if (max - min == 0) {
            double[] resultado = new double[valores.length];
            for (int i = 0; i < valores.length; i++) {
                resultado[i] = 1.0 / valores.length;
            }
            return resultado;
        }
        
        double[] normalizados = new double[valores.length];
        for (int i = 0; i < valores.length; i++) {
            normalizados[i] = (valores[i] - min) / (max - min);
        }
        
        return normalizados;
    }
    
    /**
     * Calcula probabilidades basadas en deseabilidad
     */
    public static double[] calcularProbabilidades(double[] deseabilidades, boolean[] disponibles) {
        double[] probabilidades = new double[deseabilidades.length];
        double suma = 0.0;
        
        // Calcular suma de deseabilidades disponibles
        for (int i = 0; i < deseabilidades.length; i++) {
            if (disponibles[i] && deseabilidades[i] > 0) {
                probabilidades[i] = deseabilidades[i];
                suma += deseabilidades[i];
            }
        }
        
        // Normalizar probabilidades
        if (suma > 0) {
            for (int i = 0; i < probabilidades.length; i++) {
                probabilidades[i] /= suma;
            }
        } else {
            // Si no hay deseabilidades válidas, distribución uniforme
            int disponiblesCount = 0;
            for (boolean disponible : disponibles) {
                if (disponible) disponiblesCount++;
            }
            
            if (disponiblesCount > 0) {
                double probUniforme = 1.0 / disponiblesCount;
                for (int i = 0; i < probabilidades.length; i++) {
                    if (disponibles[i]) {
                        probabilidades[i] = probUniforme;
                    }
                }
            }
        }
        
        return probabilidades;
    }
    
    /**
     * Aplica mutación a una solución
     */
    public static void mutar(double[] solucion, double probabilidadMutacion, int rangoMutacion) {
        for (int i = 0; i < solucion.length; i++) {
            if (random.nextDouble() < probabilidadMutacion) {
                solucion[i] = random.nextInt(rangoMutacion);
            }
        }
    }
    
    /**
     * Crea una copia de un array
     */
    public static double[] copiar(double[] original) {
        if (original == null) return null;
        
        double[] copia = new double[original.length];
        System.arraycopy(original, 0, copia, 0, original.length);
        return copia;
    }
    
    /**
     * Intercambia dos elementos en un array
     */
    public static void intercambiar(double[] array, int i, int j) {
        double temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }
    
    /**
     * Genera un número aleatorio en un rango
     */
    public static int enteroAleatorio(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }
    
    public static double realAleatorio(double min, double max) {
        return random.nextDouble() * (max - min) + min;
    }
    
    /**
     * Verifica si una solución es válida
     */
    public static boolean esSolucionValida(double[] solucion, int valorMaximo) {
        if (solucion == null) return false;
        
        for (double valor : solucion) {
            if (valor < 0 || valor >= valorMaximo) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Calcula la distancia euclidiana entre dos puntos
     */
    public static double distanciaEuclidiana(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Establece semilla para reproducibilidad
     */
    public static void establecerSemilla(long semilla) {
        random.setSeed(semilla);
    }
}
