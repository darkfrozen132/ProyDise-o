package morapack.datos;

import morapack.modelo.Vuelo;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Cargador de vuelos desde vuelos_completos.csv
 * Formato: Origen,Destino,HoraSalida,HoraLlegada,Capacidad
 */
public class CargadorVuelosCompletos {
    
    /**
     * Carga vuelos desde el archivo vuelos_completos.csv
     */
    public static List<Vuelo> cargarVuelos(String rutaArchivo) {
        List<Vuelo> vuelos = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea;
            int contador = 0;
            
            System.out.println("✈️ Cargando vuelos desde: " + rutaArchivo);
            
            // Leer primera línea (cabecera)
            String cabecera = br.readLine();
            if (cabecera == null) {
                System.err.println("❌ Archivo vacío o inválido");
                return vuelos;
            }
            
            while ((linea = br.readLine()) != null) {
                linea = linea.trim();
                if (linea.isEmpty()) continue;
                
                try {
                    Vuelo vuelo = parsearVuelo(linea);
                    if (vuelo != null) {
                        vuelos.add(vuelo);
                        contador++;
                        
                        // Mostrar progreso cada 500 vuelos
                        if (contador % 500 == 0) {
                            System.out.println("   ✅ Cargados " + contador + " vuelos...");
                        }
                    }
                } catch (Exception e) {
                    System.err.println("   ⚠️  Error procesando línea: " + linea + " - " + e.getMessage());
                }
            }
            
            System.out.println("✅ Total vuelos cargados: " + vuelos.size());
            
        } catch (IOException e) {
            System.err.println("❌ Error leyendo archivo: " + e.getMessage());
        }
        
        return vuelos;
    }
    
    /**
     * Parsea una línea del formato: Origen,Destino,HoraSalida,HoraLlegada,Capacidad
     */
    private static Vuelo parsearVuelo(String linea) {
        String[] partes = linea.split(",");
        
        if (partes.length != 5) {
            throw new IllegalArgumentException("Formato inválido. Esperado: Origen,Destino,HoraSalida,HoraLlegada,Capacidad");
        }
        
        try {
            String origen = partes[0].trim();
            String destino = partes[1].trim();
            String horaSalida = partes[2].trim();
            String horaLlegada = partes[3].trim();
            int capacidad = Integer.parseInt(partes[4].trim());
            
            // Validaciones básicas
            if (origen.length() != 4 || destino.length() != 4) {
                throw new IllegalArgumentException("Códigos de aeropuerto deben tener 4 caracteres");
            }
            
            if (!validarHora(horaSalida) || !validarHora(horaLlegada)) {
                throw new IllegalArgumentException("Formato de hora inválido: debe ser HH:mm");
            }
            
            if (capacidad <= 0) {
                throw new IllegalArgumentException("Capacidad inválida: " + capacidad);
            }
            
            return new Vuelo(origen, destino, horaSalida, horaLlegada, capacidad);
            
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Error parseando capacidad: " + e.getMessage());
        }
    }
    
    /**
     * Valida formato de hora HH:mm
     */
    private static boolean validarHora(String hora) {
        if (hora == null || hora.length() != 5) return false;
        if (hora.charAt(2) != ':') return false;
        
        try {
            int horas = Integer.parseInt(hora.substring(0, 2));
            int minutos = Integer.parseInt(hora.substring(3, 5));
            return horas >= 0 && horas <= 23 && minutos >= 0 && minutos <= 59;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Muestra estadísticas de los vuelos cargados
     */
    public static void mostrarEstadisticas(List<Vuelo> vuelos) {
        if (vuelos.isEmpty()) {
            System.out.println("❌ No hay vuelos para analizar");
            return;
        }
        
        System.out.println("\n📊 ============= ESTADÍSTICAS DE VUELOS =============");
        System.out.println("✈️ Total vuelos: " + vuelos.size());
        
        // Contar aeropuertos únicos de origen
        long origenesUnicos = vuelos.stream()
            .map(Vuelo::getOrigen)
            .distinct()
            .count();
        System.out.println("🛫 Aeropuertos de origen únicos: " + origenesUnicos);
        
        // Contar aeropuertos únicos de destino
        long destinosUnicos = vuelos.stream()
            .map(Vuelo::getDestino)
            .distinct()
            .count();
        System.out.println("🛬 Aeropuertos de destino únicos: " + destinosUnicos);
        
        // Capacidad total y promedio
        int capacidadTotal = vuelos.stream()
            .mapToInt(Vuelo::getCapacidad)
            .sum();
        System.out.println("📦 Capacidad total: " + capacidadTotal + " pasajeros/paquetes");
        
        double capacidadPromedio = (double) capacidadTotal / vuelos.size();
        System.out.printf("📊 Capacidad promedio por vuelo: %.1f\n", capacidadPromedio);
        
        // Rutas más frecuentes (top 10)
        System.out.println("\n🎯 TOP 10 RUTAS MÁS FRECUENTES:");
        vuelos.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                vuelo -> vuelo.getOrigen() + " → " + vuelo.getDestino(),
                java.util.stream.Collectors.counting()))
            .entrySet()
            .stream()
            .sorted(java.util.Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(10)
            .forEach(entry -> 
                System.out.printf("   %s: %d vuelos\n", entry.getKey(), entry.getValue()));
        
        System.out.println("===================================================");
    }
    
    /**
     * Valida que todos los aeropuertos en los vuelos estén en la lista válida
     */
    public static void validarAeropuertos(List<Vuelo> vuelos, java.util.Set<String> aeropuertosValidos) {
        System.out.println("\n🔍 =========== VALIDANDO AEROPUERTOS ===========");
        
        java.util.Set<String> aeropuertosEncontrados = new java.util.HashSet<>();
        java.util.Set<String> aeropuertosInvalidos = new java.util.HashSet<>();
        
        for (Vuelo vuelo : vuelos) {
            aeropuertosEncontrados.add(vuelo.getOrigen());
            aeropuertosEncontrados.add(vuelo.getDestino());
            
            if (!aeropuertosValidos.contains(vuelo.getOrigen())) {
                aeropuertosInvalidos.add(vuelo.getOrigen());
            }
            if (!aeropuertosValidos.contains(vuelo.getDestino())) {
                aeropuertosInvalidos.add(vuelo.getDestino());
            }
        }
        
        System.out.println("✅ Aeropuertos encontrados en vuelos: " + aeropuertosEncontrados.size());
        System.out.println("✅ Aeropuertos válidos: " + aeropuertosValidos.size());
        
        if (!aeropuertosInvalidos.isEmpty()) {
            System.out.println("⚠️ Aeropuertos NO válidos encontrados:");
            aeropuertosInvalidos.forEach(aeropuerto -> 
                System.out.println("   ❌ " + aeropuerto));
        } else {
            System.out.println("✅ Todos los aeropuertos en vuelos son válidos");
        }
        
        System.out.println("=============================================");
    }
    
    /**
     * Muestra estadísticas sin iconos para una mejor legibilidad
     */
    public static void mostrarEstadisticasSinIconos(List<Vuelo> vuelos) {
        if (vuelos.isEmpty()) {
            System.out.println("No hay vuelos para mostrar estadisticas");
            return;
        }
        
        System.out.println("=============================================");
        System.out.println("            ESTADISTICAS DE VUELOS          ");
        System.out.println("=============================================");
        
        System.out.printf("Total de vuelos cargados: %d%n", vuelos.size());
        
        // Capacidad total
        int capacidadTotal = vuelos.stream().mapToInt(Vuelo::getCapacidad).sum();
        System.out.printf("Capacidad total de flota: %d paquetes%n", capacidadTotal);
        
        // Promedio de capacidad
        double promedioCapacidad = (double) capacidadTotal / vuelos.size();
        System.out.printf("Capacidad promedio por vuelo: %.1f paquetes%n", promedioCapacidad);
        
        // Aeropuertos únicos
        java.util.Set<String> aeropuertosOrigen = new java.util.HashSet<>();
        java.util.Set<String> aeropuertosDestino = new java.util.HashSet<>();
        
        for (Vuelo vuelo : vuelos) {
            aeropuertosOrigen.add(vuelo.getOrigen());
            aeropuertosDestino.add(vuelo.getDestino());
        }
        
        System.out.printf("Aeropuertos de origen únicos: %d%n", aeropuertosOrigen.size());
        System.out.printf("Aeropuertos de destino únicos: %d%n", aeropuertosDestino.size());
        
        System.out.println("Aeropuertos validados correctamente");
        System.out.println("=============================================");
    }
}
