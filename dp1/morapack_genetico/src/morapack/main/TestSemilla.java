package morapack.main;

import morapack.modelo.*;
import morapack.datos.CargadorDatosCSV;
import morapack.datos.CargadorPedidos;
import morapack.genetico.core.algoritmo.AlgoritmoGeneticoIntegrado;
import morapack.genetico.core.algoritmo.IndividuoIntegrado;
import java.util.*;

/**
 * Test para verificar que la semilla funciona correctamente
 */
public class TestSemilla {
    
    public static void main(String[] args) {
        try {
            System.out.println("VERIFICANDO QUE LA SEMILLA FUNCIONA CORRECTAMENTE");
            System.out.println("================================================");
            
            // 1. CARGAR DATOS
            List<Vuelo> vuelos = CargadorDatosCSV.cargarVuelos();
            List<Pedido> pedidos = CargadorPedidos.cargarDesdeArchivo("datos/pedidos/pedidos_pares_01.csv");
            
            long semillaPrueba = 1234L;
            
            System.out.println("Ejecutando 3 veces con la MISMA semilla: " + semillaPrueba);
            System.out.println("Si la semilla funciona, los 3 fitness deben ser EXACTAMENTE iguales");
            System.out.println();
            
            for (int i = 1; i <= 3; i++) {
                System.out.printf("Ejecución %d con semilla %d: ", i, semillaPrueba);
                
                AlgoritmoGeneticoIntegrado algoritmo = new AlgoritmoGeneticoIntegrado(
                    pedidos, vuelos, 
                    25,  // Población
                    30,  // Generaciones
                    semillaPrueba // MISMA semilla siempre
                );
                
                IndividuoIntegrado resultado = algoritmo.ejecutar();
                System.out.printf("Fitness=%.2f%n", resultado.getFitness());
            }
            
            System.out.println();
            System.out.println("Ahora ejecutando con 3 semillas DIFERENTES:");
            
            long[] semillasDiferentes = {1111L, 2222L, 3333L};
            
            for (int i = 0; i < 3; i++) {
                System.out.printf("Ejecución %d con semilla %d: ", i+1, semillasDiferentes[i]);
                
                AlgoritmoGeneticoIntegrado algoritmo = new AlgoritmoGeneticoIntegrado(
                    pedidos, vuelos, 
                    25,  // Población
                    30,  // Generaciones
                    semillasDiferentes[i]
                );
                
                IndividuoIntegrado resultado = algoritmo.ejecutar();
                System.out.printf("Fitness=%.2f%n", resultado.getFitness());
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
