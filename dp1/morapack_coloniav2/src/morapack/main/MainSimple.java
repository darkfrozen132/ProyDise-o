package morapack.main;

import morapack.modelo.Aeropuerto;
import morapack.modelo.Pedido;  
import morapack.modelo.Vuelo;
import morapack.datos.CargadorDatosCSV;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Versión simplificada de Main para MoraPack Colonia v2
 * Esta versión utiliza solo las clases disponibles y es completamente funcional
 */
public class MainSimple {
    
    private static List<Aeropuerto> aeropuertos;
    private static List<Vuelo> vuelos;
    private static List<Pedido> pedidos;
    
    public static void main(String[] args) {
        System.out.println("==============================================================");
        System.out.println("          MORAPACK COLONIA V2 - ACO OPTIMIZER                ");
        System.out.println("   Sistema de Optimizacion de Rutas de Distribucion Global   ");
        System.out.println("==============================================================");
        System.out.println();
        
        try {
            System.out.println("=== PASO 1: CARGA DE DATOS ===");
            cargarDatos();
            
            System.out.println("=== PASO 2: ANÁLISIS DE LA RED ===");
            analizarRed();
            
            System.out.println("=== PASO 3: GENERACIÓN DE RUTAS SIMULADAS ===");
            generarRutasSimuladas();
            
            System.out.println("==============================================================");
            System.out.println("                  EJECUCIÓN COMPLETADA                        ");
            System.out.println("==============================================================");
            
        } catch (Exception e) {
            System.err.println("ERROR CRÍTICO: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Carga los datos desde archivos CSV
     */
    private static void cargarDatos() throws Exception {
        System.out.println("Cargando datos del sistema...");
        
        try {
            // Cargar vuelos usando el método estático
            System.out.println("Cargando vuelos...");
            vuelos = CargadorDatosCSV.cargarVuelos();
            System.out.println("   Vuelos cargados: " + vuelos.size());
            
            // Crear aeropuertos ficticios para la demo
            System.out.println("Generando aeropuertos...");
            aeropuertos = generarAeropuertos();
            System.out.println("   Aeropuertos generados: " + aeropuertos.size());
            
            // Crear pedidos ficticios para la demo
            System.out.println("Generando pedidos...");
            pedidos = generarPedidos();
            System.out.println("   Pedidos generados: " + pedidos.size());
            
            System.out.println("Datos cargados exitosamente");
            
        } catch (Exception e) {
            throw new Exception("Error cargando datos: " + e.getMessage(), e);
        }
    }
    
    /**
     * Genera aeropuertos ficticios para la demostración
     */
    private static List<Aeropuerto> generarAeropuertos() {
        List<Aeropuerto> lista = new ArrayList<>();
        
        // Sedes principales
        lista.add(new Aeropuerto("SPIM", "Lima", "Peru", "SAM"));
        lista.add(new Aeropuerto("EBCI", "Bruselas", "Belgica", "EUR"));
        lista.add(new Aeropuerto("UBBB", "Baku", "Azerbaiyan", "ASI"));
        
        // Otros aeropuertos importantes
        lista.add(new Aeropuerto("SKBO", "Bogota", "Colombia", "SAM"));
        lista.add(new Aeropuerto("SBBR", "Brasilia", "Brasil", "SAM"));
        lista.add(new Aeropuerto("LOWW", "Viena", "Austria", "EUR"));
        lista.add(new Aeropuerto("EHAM", "Amsterdam", "Holanda", "EUR"));
        lista.add(new Aeropuerto("OMDB", "Dubai", "UAE", "ASI"));
        lista.add(new Aeropuerto("VIDP", "Delhi", "India", "ASI"));
        
        return lista;
    }
    
    /**
     * Genera pedidos ficticios para la demostración
     */
    private static List<Pedido> generarPedidos() {
        List<Pedido> lista = new ArrayList<>();
        
        // Crear algunos pedidos de ejemplo
        for (int i = 1; i <= 20; i++) {
            Pedido pedido = new Pedido();
            pedido.setId(String.format("05-09-%02d-DEST-%03d-000%04d", i, i*5, i));
            pedido.setAeropuertoDestinoId(obtenerDestinoAleatorio(i));
            pedido.setCantidadProductos(i * 5);
            lista.add(pedido);
        }
        
        return lista;
    }
    
    /**
     * Obtiene un destino aleatorio basado en un índice
     */
    private static String obtenerDestinoAleatorio(int indice) {
        String[] destinos = {"SKBO", "SBBR", "LOWW", "EHAM", "OMDB", "VIDP"};
        return destinos[indice % destinos.length];
    }
    
    /**
     * Analiza la red de distribución cargada
     */
    private static void analizarRed() {
        System.out.println("Analizando red de distribución...");
        
        // Contar aeropuertos por continente
        Map<String, Integer> aeropuertosPorContinente = new HashMap<>();
        for (Aeropuerto aeropuerto : aeropuertos) {
            String continente = aeropuerto.getContinente();
            aeropuertosPorContinente.put(continente, 
                aeropuertosPorContinente.getOrDefault(continente, 0) + 1);
        }
        
        System.out.println("=== ESTADÍSTICAS DE LA RED ===");
        System.out.println("Aeropuertos totales: " + aeropuertos.size());
        System.out.println("Vuelos totales: " + vuelos.size());
        System.out.println("Pedidos totales: " + pedidos.size());
        
        System.out.println("Distribución por continente:");
        for (Map.Entry<String, Integer> entrada : aeropuertosPorContinente.entrySet()) {
            System.out.println("   " + entrada.getKey() + ": " + entrada.getValue() + " aeropuertos");
        }
        
        // Identificar sedes principales (las que más vuelos de salida tienen)
        Map<String, Integer> vuelosPorOrigen = new HashMap<>();
        for (Vuelo vuelo : vuelos) {
            String origen = vuelo.getOrigen();
            vuelosPorOrigen.put(origen, vuelosPorOrigen.getOrDefault(origen, 0) + 1);
        }
        
        System.out.println("Principales hubs de vuelos:");
        vuelosPorOrigen.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(5)
            .forEach(entry -> System.out.println("   " + entry.getKey() + ": " + entry.getValue() + " vuelos"));
        
        System.out.println();
    }
    
    /**
     * Genera un análisis simulado de rutas (ya que no tenemos el algoritmo ACO completo)
     */
    private static void generarRutasSimuladas() {
        System.out.println("Generando análisis de rutas...");
        
        // Simular análisis de conectividad
        Map<String, List<String>> conectividad = new HashMap<>();
        for (Vuelo vuelo : vuelos) {
            String origen = vuelo.getOrigen();
            String destino = vuelo.getDestino();
            
            conectividad.computeIfAbsent(origen, k -> new ArrayList<>()).add(destino);
        }
        
        System.out.println("RUTAS SIMULADAS - ANÁLISIS DE CONECTIVIDAD:");
        System.out.println("===============================================");
        
        int rutasDirectas = 0;
        int rutasConEscalas = 0;
        
        // Analizar cada pedido
        for (int i = 0; i < Math.min(10, pedidos.size()); i++) {
            Pedido pedido = pedidos.get(i);
            String origen = determinarSedeOrigen(pedido);
            String destino = pedido.getAeropuertoDestinoId();
            
            System.out.println("   Pedido " + (i+1) + ": " + origen + " → " + destino);
            
            // Verificar si hay conexión directa
            if (conectividad.containsKey(origen) && conectividad.get(origen).contains(destino)) {
                System.out.println("     ✈️ RUTA DIRECTA");
                rutasDirectas++;
            } else {
                // Buscar ruta con una escala
                boolean encontroEscala = false;
                if (conectividad.containsKey(origen)) {
                    for (String intermedio : conectividad.get(origen)) {
                        if (conectividad.containsKey(intermedio) && 
                            conectividad.get(intermedio).contains(destino)) {
                            System.out.println("     🔄 RUTA CON 1 ESCALA: " + origen + " → " + intermedio + " → " + destino);
                            rutasConEscalas++;
                            encontroEscala = true;
                            break;
                        }
                    }
                }
                if (!encontroEscala) {
                    System.out.println("     ❌ SIN RUTA DIRECTA DISPONIBLE");
                }
            }
        }
        
        if (pedidos.size() > 10) {
            System.out.println("   ... (mostrando solo primeros 10 pedidos de " + pedidos.size() + ")");
        }
        
        System.out.println();
        System.out.println("ESTADÍSTICAS DE RUTAS (muestra):");
        System.out.println("   Rutas directas: " + rutasDirectas);
        System.out.println("   Rutas con escalas: " + rutasConEscalas);
        System.out.println("   Total analizado: " + Math.min(10, pedidos.size()) + "/" + pedidos.size());
        
        double porcentajeDirectas = (double) rutasDirectas / Math.min(10, pedidos.size()) * 100;
        System.out.println("   Porcentaje directo: " + String.format("%.1f%%", porcentajeDirectas));
        System.out.println();
    }
    
    /**
     * Determina la sede de origen más probable para un pedido
     */
    private static String determinarSedeOrigen(Pedido pedido) {
        String destino = pedido.getAeropuertoDestinoId();
        
        // Buscar el continente del destino
        String continenteDestino = "EUR"; // Default
        for (Aeropuerto aeropuerto : aeropuertos) {
            if (aeropuerto.getCodigoICAO().equals(destino)) {
                continenteDestino = aeropuerto.getContinente();
                break;
            }
        }
        
        // Asignar sede según continente
        switch (continenteDestino) {
            case "SAM": return "SPIM"; // Lima para Sudamérica
            case "ASI": return "UBBB"; // Baku para Asia
            case "EUR": return "EBCI"; // Bruselas para Europa
            default: return "SPIM";   // Default Lima
        }
    }
}
