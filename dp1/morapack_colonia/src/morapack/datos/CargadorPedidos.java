package morapack.datos;

import morapack.modelo.Pedido;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Cargador simple de pedidos desde CSV
 */
public class CargadorPedidos {
    
    /**
     * Carga pedidos desde un archivo CSV
     * @param nombreArchivo ruta del archivo CSV
     * @return lista de pedidos
     */
    public static List<Pedido> cargarDesdeArchivo(String nombreArchivo) {
        List<Pedido> pedidos = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(nombreArchivo))) {
            String linea;
            boolean primeraLinea = true;
            
            while ((linea = br.readLine()) != null) {
                if (primeraLinea) {
                    primeraLinea = false; // Saltar cabecera
                    continue;
                }
                
                linea = linea.trim();
                if (!linea.isEmpty()) {
                    Pedido pedido = parsearPedido(linea);
                    if (pedido != null) {
                        pedidos.add(pedido);
                    }
                }
            }
            
        } catch (IOException e) {
            System.err.println("Error al cargar pedidos: " + e.getMessage());
        }
        
        return pedidos;
    }
    
    /**
     * Parsea una línea del CSV para crear un pedido
     * Formato esperado: dd-hh-mm-DEST-cantidad-clienteId
     */
    private static Pedido parsearPedido(String linea) {
        try {
            // Formato: 30-09-15-SEQM-145-0054321
            String[] partes = linea.split("-");
            
            if (partes.length >= 6) {
                String destino = partes[3]; // SEQM
                int cantidad = Integer.parseInt(partes[4]); // 145
                
                Pedido pedido = new Pedido();
                pedido.setId(linea); // Usar toda la línea como ID
                pedido.setAeropuertoDestinoId(destino);
                pedido.setCantidadProductos(cantidad);
                
                return pedido;
            }
            
        } catch (Exception e) {
            System.err.println("Error parseando pedido: " + linea + " - " + e.getMessage());
        }
        
        return null;
    }
}
