package morapack.datos;

import morapack.modelo.Vuelo;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Cargador de vuelos reales desde vuelos_completos.csv
 * Formato CSV: Origen,Destino,HoraSalida,HoraLlegada,Capacidad
 */
public class CargadorVuelosCompletos {
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm");
    
    /**
     * Carga vuelos desde el archivo vuelos_completos.csv
     */
    public static List<Vuelo> cargarVuelos(String rutaArchivo) throws IOException {
        List<Vuelo> vuelos = new ArrayList<>();
        
        System.out.println("✈️ Cargando vuelos desde: " + rutaArchivo);
        
        try (BufferedReader br = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea = br.readLine(); // Saltar header
            
            int contador = 0;
            while ((linea = br.readLine()) != null) {
                try {
                    Vuelo vuelo = parsearVuelo(linea.trim());
                    if (vuelo != null) {
                        vuelos.add(vuelo);
                        contador++;
                        
                        // Progreso cada 500 vuelos
                        if (contador % 500 == 0) {
                            System.out.println("   ✅ Cargados " + contador + " vuelos...");
                        }
                    }
                } catch (Exception e) {
                    System.err.println("⚠️ Error procesando línea: " + linea + " - " + e.getMessage());
                }
            }
            
            System.out.println("✅ Total vuelos cargados: " + contador);
        }
        
        return vuelos;
    }
    
    /**
     * Parsea una línea del CSV y crea un objeto Vuelo
     */
    private static Vuelo parsearVuelo(String linea) {
        if (linea.isEmpty()) return null;
        
        String[] partes = linea.split(",");
        if (partes.length < 5) {
            throw new IllegalArgumentException("Línea inválida, faltan columnas: " + linea);
        }
        
        try {
            String origen = partes[0].trim();
            String destino = partes[1].trim();
            
            String horaSalida = validarHora(partes[2].trim());
            String horaLlegada = validarHora(partes[3].trim());
            
            int capacidad = Integer.parseInt(partes[4].trim());
            
            return new Vuelo(origen, destino, horaSalida, horaLlegada, capacidad);
            
        } catch (Exception e) {
            throw new RuntimeException("Error parseando vuelo: " + linea + " - " + e.getMessage(), e);
        }
    }
    
    /**
     * Valida y parsea una hora en formato H:mm, retorna String
     */
    private static String validarHora(String horaStr) {
        try {
            LocalTime time = LocalTime.parse(horaStr, TIME_FORMATTER);
            // Convertir a formato HH:mm para consistencia
            return String.format("%02d:%02d", time.getHour(), time.getMinute());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Formato de hora inválido: '" + horaStr + "', esperado H:mm", e);
        }
    }
    
    /**
     * Valida que los vuelos usen solo aeropuertos válidos
     */
    public static void validarAeropuertos(List<Vuelo> vuelos, Set<String> aeropuertosValidos) {
        Set<String> aeropuertosEncontrados = new java.util.HashSet<>();
        
        System.out.println("\n🔍 =========== VALIDANDO AEROPUERTOS ===========");
        
        for (Vuelo vuelo : vuelos) {
            aeropuertosEncontrados.add(vuelo.getOrigen());
            aeropuertosEncontrados.add(vuelo.getDestino());
        }
        
        System.out.println("✅ Aeropuertos encontrados en vuelos: " + aeropuertosEncontrados.size());
        System.out.println("✅ Aeropuertos válidos: " + aeropuertosValidos.size());
        
        // Verificar aeropuertos inválidos
        Set<String> invalidos = new java.util.HashSet<>(aeropuertosEncontrados);
        invalidos.removeAll(aeropuertosValidos);
        
        if (!invalidos.isEmpty()) {
            System.err.println("❌ Aeropuertos INVÁLIDOS encontrados: " + invalidos);
            throw new IllegalStateException("Se encontraron aeropuertos inválidos en los vuelos");
        }
        
        // Verificar cobertura
        Set<String> faltantes = new java.util.HashSet<>(aeropuertosValidos);
        faltantes.removeAll(aeropuertosEncontrados);
        
        if (!faltantes.isEmpty()) {
            System.out.println("⚠️ Aeropuertos válidos sin vuelos: " + faltantes);
        }
        
        System.out.println("✅ Todos los aeropuertos en vuelos son válidos");
        System.out.println("=============================================");
    }
    
    /**
     * Muestra estadísticas detalladas de los vuelos cargados
     */
    public static void mostrarEstadisticas(List<Vuelo> vuelos) {
        if (vuelos.isEmpty()) {
            System.out.println("❌ No hay vuelos para mostrar estadísticas");
            return;
        }
        
        System.out.println("\n📊 ============= ESTADÍSTICAS DE VUELOS =============");
        
        // Estadísticas básicas
        System.out.println("✈️ Total vuelos: " + vuelos.size());
        
        Set<String> origenes = new java.util.HashSet<>();
        Set<String> destinos = new java.util.HashSet<>();
        long capacidadTotal = 0;
        
        for (Vuelo vuelo : vuelos) {
            origenes.add(vuelo.getOrigen());
            destinos.add(vuelo.getDestino());
            capacidadTotal += vuelo.getCapacidad();
        }
        
        System.out.println("🛫 Aeropuertos de origen únicos: " + origenes.size());
        System.out.println("🛬 Aeropuertos de destino únicos: " + destinos.size());
        System.out.println("📦 Capacidad total: " + capacidadTotal + " pasajeros/paquetes");
        System.out.println("📊 Capacidad promedio por vuelo: " + (capacidadTotal / (double) vuelos.size()));
        
        // Top rutas más frecuentes
        java.util.Map<String, Integer> frecuenciaRutas = new java.util.HashMap<>();
        for (Vuelo vuelo : vuelos) {
            String ruta = vuelo.getOrigen() + " → " + vuelo.getDestino();
            frecuenciaRutas.put(ruta, frecuenciaRutas.getOrDefault(ruta, 0) + 1);
        }
        
        System.out.println("\n🎯 TOP 10 RUTAS MÁS FRECUENTES:");
        frecuenciaRutas.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
            .limit(10)
            .forEach(entry -> System.out.println("   " + entry.getKey() + ": " + entry.getValue() + " vuelos"));
        
        System.out.println("===================================================");
    }
}
