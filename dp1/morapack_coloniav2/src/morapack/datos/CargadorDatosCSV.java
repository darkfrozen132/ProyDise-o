package morapack.datos;

import morapack.modelo.Vuelo;
import morapack.modelo.Aeropuerto;
import java.io.*;
import java.util.*;

/**
 * Cargador de datos CSV simplificado - Solo vuelos
 */
public class CargadorDatosCSV {
    
    private static final String RUTA_DATOS = "datos/";
    
    /**
     * Carga vuelos desde el archivo CSV completo (2866 vuelos) y retorna la lista
     */
    public static List<Vuelo> cargarVuelos() {
        String archivo = RUTA_DATOS + "vuelos_completos.csv";
        List<Vuelo> vuelos = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            boolean primeraLinea = true;
            
            while ((linea = br.readLine()) != null) {
                if (primeraLinea) {
                    primeraLinea = false; // Saltar cabecera
                    continue;
                }
                
                String[] campos = linea.split(",");
                if (campos.length >= 5) {
                    String origen = campos[0].trim();
                    String destino = campos[1].trim();
                    String horaSalida = campos[2].trim();
                    String horaLlegada = campos[3].trim();
                    int capacidad = Integer.parseInt(campos[4].trim());
                    
                    Vuelo vuelo = new Vuelo(origen, destino, horaSalida, horaLlegada, capacidad);
                    vuelos.add(vuelo);
                }
            }
            
            System.out.println("📊 Vuelos disponibles en CSV: " + vuelos.size());
            
        } catch (IOException e) {
            System.err.println("❌ Error al leer vuelos: " + e.getMessage());
        }
        
        return vuelos;
    }
    
    /**
     * Carga aeropuertos desde el archivo CSV con coordenadas geográficas
     */
    public static List<Aeropuerto> cargarAeropuertos() {
        String archivo = RUTA_DATOS + "aeropuertos_simple.csv";
        List<Aeropuerto> aeropuertos = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            boolean primeraLinea = true;
            int numeroLinea = 0;
            int lineasProcesadas = 0;
            
            while ((linea = br.readLine()) != null) {
                numeroLinea++;
                
                if (primeraLinea) {
                    primeraLinea = false; // Saltar cabecera: ICAO,Ciudad,Pais,Codigo,Huso,Capacidad,Latitud,Longitud
                    continue;
                }
                
                // Verificar línea vacía
                if (linea.trim().isEmpty()) {
                    continue;
                }
                
                try {
                    String[] campos = linea.split(",");
                    if (campos.length >= 8) {
                        String codigoICAO = campos[0].trim();
                        String ciudad = campos[1].trim();
                        String pais = campos[2].trim();
                        String codigoCorto = campos[3].trim();
                        int husoHorario = Integer.parseInt(campos[4].trim());
                        int capacidad = Integer.parseInt(campos[5].trim());
                        double latitud = Double.parseDouble(campos[6].trim());
                        double longitud = Double.parseDouble(campos[7].trim());
                        
                        Aeropuerto aeropuerto = new Aeropuerto(codigoICAO, ciudad, pais, codigoCorto, 
                                                              husoHorario, capacidad, latitud, longitud);
                        aeropuertos.add(aeropuerto);
                        lineasProcesadas++;
                    } else {
                        System.err.println("⚠️ Línea " + numeroLinea + " tiene " + campos.length + " campos, se esperan 8: " + linea);
                    }
                } catch (NumberFormatException e) {
                    System.err.println("❌ Error de formato en línea " + numeroLinea + ": " + linea);
                    System.err.println("    Error: " + e.getMessage());
                }
            }
            
            System.out.println("📊 Líneas leídas: " + numeroLinea + ", procesadas: " + lineasProcesadas + ", aeropuertos cargados: " + aeropuertos.size());
            
        } catch (IOException e) {
            System.err.println("❌ Error al leer aeropuertos: " + e.getMessage());
        }
        
        return aeropuertos;
    }
}
