package morapack.datos;

import morapack.modelo.Aeropuerto;
import morapack.modelo.Vuelo;
import morapack.modelo.Pedido;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

/**
 * Cargador de datos para el sistema MoraPack desde archivos CSV
 */
public class CargadorDatosMoraPack {
    
    /**
     * Carga lista de aeropuertos desde archivo CSV
     */
    public static List<Aeropuerto> cargarAeropuertos(String rutaArchivo) throws IOException {
        List<Aeropuerto> aeropuertos = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea;
            boolean primeraLinea = true;
            
            while ((linea = br.readLine()) != null) {
                if (primeraLinea) {
                    primeraLinea = false;
                    continue; // Saltar encabezados
                }
                
                String[] campos = linea.split(",");
                if (campos.length >= 8) {
                    try {
                        String codigoICAO = campos[0].trim();
                        String ciudad = campos[1].trim();
                        String pais = campos[2].trim();
                        String codigoCorto = campos[3].trim();
                        int husoHorario = Integer.parseInt(campos[4].trim());
                        int capacidad = Integer.parseInt(campos[5].trim());
                        double latitud = Double.parseDouble(campos[6].trim());
                        double longitud = Double.parseDouble(campos[7].trim());
                        
                        Aeropuerto aeropuerto = new Aeropuerto(codigoICAO, ciudad, pais, 
                                                              codigoCorto, husoHorario, 
                                                              capacidad, latitud, longitud);
                        aeropuertos.add(aeropuerto);
                        
                    } catch (NumberFormatException e) {
                        System.err.println("Error parseando línea: " + linea);
                    }
                }
            }
        }
        
        System.out.println("Cargados " + aeropuertos.size() + " aeropuertos");
        return aeropuertos;
    }
    
    /**
     * Carga lista de vuelos desde archivo CSV
     */
    public static List<Vuelo> cargarVuelos(String rutaArchivo) throws IOException {
        List<Vuelo> vuelos = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea;
            boolean primeraLinea = true;
            
            while ((linea = br.readLine()) != null) {
                if (primeraLinea) {
                    primeraLinea = false;
                    continue; // Saltar encabezados
                }
                
                String[] campos = linea.split(",");
                if (campos.length >= 7) {
                    try {
                        String numeroVuelo = campos[0].trim();
                        String aerolinea = campos[1].trim();
                        String origen = campos[2].trim();
                        String destino = campos[3].trim();
                        String horaSalida = campos[4].trim();
                        String horaLlegada = campos[5].trim();
                        int capacidadCarga = Integer.parseInt(campos[6].trim());
                        
                        Vuelo vuelo = new Vuelo(origen, destino, horaSalida, horaLlegada, capacidadCarga);
                        vuelos.add(vuelo);
                        
                    } catch (NumberFormatException e) {
                        System.err.println("Error parseando línea de vuelo: " + linea);
                    }
                }
            }
        }
        
        System.out.println("Cargados " + vuelos.size() + " vuelos");
        return vuelos;
    }
    
    /**
     * Genera lista de pedidos de ejemplo para testing
     */
    public static List<Pedido> generarPedidosEjemplo(List<Aeropuerto> aeropuertos, int cantidad) {
        List<Pedido> pedidos = new ArrayList<>();
        List<Aeropuerto> destinos = new ArrayList<>();
        
        // Filtrar aeropuertos que no son sedes como destinos
        for (Aeropuerto aeropuerto : aeropuertos) {
            if (!aeropuerto.esSede()) {
                destinos.add(aeropuerto);
            }
        }
        
        java.util.Random random = new java.util.Random();
        
        for (int i = 0; i < cantidad; i++) {
            String id = "PED" + String.format("%04d", i + 1);
            String clienteId = "CLI" + String.format("%03d", random.nextInt(100) + 1);
            
            // Seleccionar destino aleatorio
            Aeropuerto destino = destinos.get(random.nextInt(destinos.size()));
            
            // Generar cantidad de productos (1-20)
            int cantidadProductos = random.nextInt(20) + 1;
            
            // Generar prioridad (1=alta, 2=media, 3=baja)
            int prioridad = random.nextInt(3) + 1;
            
            Pedido pedido = new Pedido(id, clienteId, destino, cantidadProductos, prioridad);
            pedidos.add(pedido);
        }
        
        System.out.println("Generados " + pedidos.size() + " pedidos de ejemplo");
        return pedidos;
    }
    
    /**
     * Obtiene lista de aeropuertos que son sedes MoraPack
     */
    public static List<Aeropuerto> obtenerSedes(List<Aeropuerto> aeropuertos) {
        List<Aeropuerto> sedes = new ArrayList<>();
        
        for (Aeropuerto aeropuerto : aeropuertos) {
            if (aeropuerto.esSede()) {
                sedes.add(aeropuerto);
            }
        }
        
        System.out.println("Identificadas " + sedes.size() + " sedes MoraPack");
        return sedes;
    }
    
    /**
     * Muestra estadísticas de los datos cargados
     */
    public static void mostrarEstadisticas(List<Aeropuerto> aeropuertos, List<Vuelo> vuelos, List<Pedido> pedidos) {
        System.out.println("\n=== ESTADÍSTICAS DE DATOS MORAPACK ===");
        System.out.println("Aeropuertos totales: " + aeropuertos.size());
        
        List<Aeropuerto> sedes = obtenerSedes(aeropuertos);
        System.out.println("Sedes MoraPack: " + sedes.size());
        for (Aeropuerto sede : sedes) {
            System.out.println("  - " + sede.getCiudad() + " (" + sede.getCodigoICAO() + ")");
        }
        
        System.out.println("Vuelos disponibles: " + vuelos.size());
        System.out.println("Pedidos a procesar: " + pedidos.size());
        
        // Estadísticas por continente
        java.util.Map<String, Integer> porContinente = new java.util.HashMap<>();
        for (Aeropuerto aeropuerto : aeropuertos) {
            String continente = aeropuerto.getContinente();
            porContinente.put(continente, porContinente.getOrDefault(continente, 0) + 1);
        }
        
        System.out.println("\nDistribución por continente:");
        for (java.util.Map.Entry<String, Integer> entry : porContinente.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue() + " aeropuertos");
        }
        System.out.println("=====================================\n");
    }
}
