package morapack.datos;

import morapack.modelo.Pedido;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Cargador simple solo para pedidos
 */
public class CargadorPedidosSimple {
    
    public static List<Pedido> cargarPedidosDesdeArchivo(String rutaCompleta) {
        List<Pedido> pedidos = new ArrayList<>();
        
        System.out.println("🔍 Intentando cargar desde: " + rutaCompleta);
        
        try (BufferedReader br = new BufferedReader(new FileReader(rutaCompleta))) {
            String linea;
            boolean primeraLinea = true;
            
            while ((linea = br.readLine()) != null) {
                if (primeraLinea) {
                    primeraLinea = false; // Saltar cabecera
                    continue;
                }
                
                String idPedido = linea.trim();
                
                // Validar formato del ID
                if (Pedido.esFormatoValido(idPedido)) {
                    Pedido pedido = new Pedido(idPedido);
                    // Asignar aeropuerto origen aleatorio entre los 3 disponibles
                    String[] aeropuertosOrigen = {"SPIM", "EBCI", "UBBB"};
                    pedido.setAeropuertoOrigenId(aeropuertosOrigen[(int)(Math.random() * 3)]);
                    // Asignar prioridad aleatoria para pruebas
                    pedido.setPrioridad(1 + (int)(Math.random() * 3));
                    pedido.setEstado("PENDIENTE");
                    
                    pedidos.add(pedido);
                } else {
                    System.err.println("⚠️  Formato inválido de pedido: " + idPedido);
                }
            }
            
            System.out.println("✅ Pedidos cargados exitosamente: " + pedidos.size());
            
        } catch (IOException e) {
            System.err.println("❌ Error al cargar pedidos desde " + rutaCompleta + ": " + e.getMessage());
        }
        
        return pedidos;
    }
}
