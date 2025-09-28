package morapack.core.solucion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Representa una solución construida por una hormiga.
 * Mantiene la secuencia de nodos visitados y proporciona
 * métodos para manipular y consultar la solución.
 */
public class Solucion implements Cloneable {

    private List<Integer> secuenciaNodos;
    private double fitness;
    private boolean fitnessCalculado;
    private long tiempoCreacion;

    /**
     * Constructor por defecto
     */
    public Solucion() {
        this.secuenciaNodos = new ArrayList<>();
        this.fitness = Double.MAX_VALUE;
        this.fitnessCalculado = false;
        this.tiempoCreacion = System.currentTimeMillis();
    }

    /**
     * Constructor con secuencia inicial
     * @param secuenciaInicial Array con la secuencia inicial de nodos
     */
    public Solucion(int[] secuenciaInicial) {
        this();
        if (secuenciaInicial != null) {
            for (int nodo : secuenciaInicial) {
                secuenciaNodos.add(nodo);
            }
        }
    }

    /**
     * Constructor copia
     * @param otra Solución a copiar
     */
    public Solucion(Solucion otra) {
        this.secuenciaNodos = new ArrayList<>(otra.secuenciaNodos);
        this.fitness = otra.fitness;
        this.fitnessCalculado = otra.fitnessCalculado;
        this.tiempoCreacion = System.currentTimeMillis();
    }

    /**
     * Agrega un nodo al final de la secuencia
     * @param nodo Nodo a agregar
     */
    public void agregarNodo(int nodo) {
        if (nodo < 0) {
            throw new IllegalArgumentException("El nodo debe ser no negativo: " + nodo);
        }
        secuenciaNodos.add(nodo);
        invalidarFitness();
    }

    /**
     * Inserta un nodo en una posición específica
     * @param posicion Posición donde insertar
     * @param nodo Nodo a insertar
     */
    public void insertarNodo(int posicion, int nodo) {
        if (posicion < 0 || posicion > secuenciaNodos.size()) {
            throw new IndexOutOfBoundsException("Posición inválida: " + posicion);
        }
        if (nodo < 0) {
            throw new IllegalArgumentException("El nodo debe ser no negativo: " + nodo);
        }
        secuenciaNodos.add(posicion, nodo);
        invalidarFitness();
    }

    /**
     * Elimina un nodo de la secuencia
     * @param nodo Nodo a eliminar
     * @return true si se eliminó, false si no existía
     */
    public boolean eliminarNodo(int nodo) {
        boolean eliminado = secuenciaNodos.remove(Integer.valueOf(nodo));
        if (eliminado) {
            invalidarFitness();
        }
        return eliminado;
    }

    /**
     * Elimina el nodo en una posición específica
     * @param posicion Posición del nodo a eliminar
     * @return El nodo eliminado
     */
    public int eliminarNodoEnPosicion(int posicion) {
        if (posicion < 0 || posicion >= secuenciaNodos.size()) {
            throw new IndexOutOfBoundsException("Posición inválida: " + posicion);
        }
        int nodoEliminado = secuenciaNodos.remove(posicion);
        invalidarFitness();
        return nodoEliminado;
    }

    /**
     * Obtiene el nodo en una posición específica
     * @param posicion Posición del nodo
     * @return El nodo en esa posición
     */
    public int getNodo(int posicion) {
        if (posicion < 0 || posicion >= secuenciaNodos.size()) {
            throw new IndexOutOfBoundsException("Posición inválida: " + posicion);
        }
        return secuenciaNodos.get(posicion);
    }

    /**
     * Obtiene el primer nodo de la secuencia
     * @return El primer nodo, o -1 si está vacía
     */
    public int getPrimerNodo() {
        return secuenciaNodos.isEmpty() ? -1 : secuenciaNodos.get(0);
    }

    /**
     * Obtiene el último nodo de la secuencia
     * @return El último nodo, o -1 si está vacía
     */
    public int getUltimoNodo() {
        return secuenciaNodos.isEmpty() ? -1 : secuenciaNodos.get(secuenciaNodos.size() - 1);
    }

    /**
     * Obtiene el tamaño de la solución (número de nodos)
     * @return Número de nodos en la secuencia
     */
    public int getTamaño() {
        return secuenciaNodos.size();
    }

    /**
     * Verifica si la solución está vacía
     * @return true si no tiene nodos
     */
    public boolean estaVacia() {
        return secuenciaNodos.isEmpty();
    }

    /**
     * Verifica si un nodo específico está en la solución
     * @param nodo Nodo a buscar
     * @return true si el nodo está presente
     */
    public boolean contieneNodo(int nodo) {
        return secuenciaNodos.contains(nodo);
    }

    /**
     * Obtiene la posición de un nodo en la secuencia
     * @param nodo Nodo a buscar
     * @return Posición del nodo, o -1 si no está presente
     */
    public int getPosicionNodo(int nodo) {
        return secuenciaNodos.indexOf(nodo);
    }

    /**
     * Obtiene una copia de la secuencia como array
     * @return Array con la secuencia de nodos
     */
    public int[] getSecuenciaComoArray() {
        return secuenciaNodos.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * Obtiene una copia de la lista de nodos
     * @return Lista con los nodos
     */
    public List<Integer> getSecuenciaComoLista() {
        return new ArrayList<>(secuenciaNodos);
    }

    /**
     * Intercambia dos nodos en la secuencia
     * @param posicion1 Primera posición
     * @param posicion2 Segunda posición
     */
    public void intercambiarNodos(int posicion1, int posicion2) {
        if (posicion1 < 0 || posicion1 >= secuenciaNodos.size() ||
            posicion2 < 0 || posicion2 >= secuenciaNodos.size()) {
            throw new IndexOutOfBoundsException("Posiciones inválidas: " + posicion1 + ", " + posicion2);
        }

        Collections.swap(secuenciaNodos, posicion1, posicion2);
        invalidarFitness();
    }

    /**
     * Invierte un segmento de la secuencia
     * @param inicio Posición inicial del segmento
     * @param fin Posición final del segmento (inclusiva)
     */
    public void invertirSegmento(int inicio, int fin) {
        if (inicio < 0 || fin >= secuenciaNodos.size() || inicio > fin) {
            throw new IllegalArgumentException("Segmento inválido: " + inicio + " a " + fin);
        }

        while (inicio < fin) {
            Collections.swap(secuenciaNodos, inicio, fin);
            inicio++;
            fin--;
        }
        invalidarFitness();
    }

    /**
     * Aplica una mejora local usando 2-opt
     * @param pos1 Primera posición para 2-opt
     * @param pos2 Segunda posición para 2-opt
     */
    public void aplicar2Opt(int pos1, int pos2) {
        if (pos1 > pos2) {
            int temp = pos1;
            pos1 = pos2;
            pos2 = temp;
        }
        invertirSegmento(pos1 + 1, pos2);
    }

    /**
     * Limpia la solución
     */
    public void limpiar() {
        secuenciaNodos.clear();
        invalidarFitness();
    }

    /**
     * Invalida el fitness calculado
     */
    private void invalidarFitness() {
        this.fitnessCalculado = false;
        this.fitness = Double.MAX_VALUE;
    }

    /**
     * Establece el fitness de la solución
     * @param fitness Valor del fitness
     */
    public void setFitness(double fitness) {
        this.fitness = fitness;
        this.fitnessCalculado = true;
    }

    /**
     * Obtiene el fitness de la solución
     * @return Valor del fitness
     */
    public double getFitness() {
        return fitness;
    }

    /**
     * Verifica si el fitness ha sido calculado
     * @return true si el fitness es válido
     */
    public boolean esFitnessValido() {
        return fitnessCalculado;
    }

    /**
     * Obtiene el tiempo de creación de la solución
     * @return Timestamp de creación
     */
    public long getTiempoCreacion() {
        return tiempoCreacion;
    }

    @Override
    public Solucion clone() {
        return new Solucion(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Solucion solucion = (Solucion) obj;
        return secuenciaNodos.equals(solucion.secuenciaNodos);
    }

    @Override
    public int hashCode() {
        return secuenciaNodos.hashCode();
    }

    @Override
    public String toString() {
        if (secuenciaNodos.isEmpty()) {
            return "Solución vacía";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Solución[");
        for (int i = 0; i < secuenciaNodos.size(); i++) {
            if (i > 0) sb.append(" -> ");
            sb.append(secuenciaNodos.get(i));
        }
        sb.append("]");

        if (fitnessCalculado) {
            sb.append(" fitness=").append(String.format("%.2f", fitness));
        }

        return sb.toString();
    }

    /**
     * Genera una representación compacta de la solución
     * @return String compacto con la secuencia
     */
    public String toStringCompacto() {
        return secuenciaNodos.toString();
    }
}