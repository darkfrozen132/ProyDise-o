package morapack.main;

import morapack.modelo.*;
import morapack.planificacion.*;
import java.util.List;
import java.util.ArrayList;

/**
 * Programa principal para MoraPack con gestión UTC y plazos continentales
 */
public class MainMoraPackUTCyPlazos {
    
    public static void main(String[] args) {
        
        System.out.println("🌍 ============== MORAPACK UTC Y PLAZOS CONTINENTALES ==============");
        System.out.println("📦 Sistema de Logística Global con Restricciones Temporales");
        System.out.println("================================================================");
        
        try {
            // 📂 Crear datos de ejemplo
            System.out.println("\n📂 Creando datos de ejemplo del sistema...");
            
            List<Vuelo> vuelos = crearVuelosEjemplo();
            List<Pedido> pedidos = crearPedidosEjemplo();
            
            System.out.printf("   ✅ %d vuelos cargados\n", vuelos.size());
            System.out.printf("   ✅ %d pedidos cargados\n", pedidos.size());
            
            // 🕐 Inicializar planificador temporal con UTC y plazos
            System.out.println("\n🕐 Inicializando Planificador Temporal con UTC y Plazos...");
            PlanificadorTemporalConUTCyPlazos planificador = new PlanificadorTemporalConUTCyPlazos(vuelos);
            
            // 📊 Estadísticas de procesamiento
            int procesados = 0;
            int exitosos = 0;
            int directos = 0;
            int conEscalas = 0;
            int fallidosPorPlazo = 0;
            
            String[] sedes = {"SPIM", "EBCI", "UBBB"}; // Lima, Bruselas, Bakú
            
            System.out.println("\n📦 =================== PROCESANDO PEDIDOS ===================");
            
            // Procesar todos los pedidos
            for (Pedido pedido : pedidos) {
                procesados++;
                
                boolean encontroRuta = false;
                
                // Intentar desde cada sede
                for (String sede : sedes) {
                    RutaCompleta ruta = planificador.planificarRutaTemporal(pedido, sede);
                    
                    if (ruta != null && ruta.esViable()) {
                        exitosos++;
                        encontroRuta = true;
                        
                        // Clasificar tipo de ruta
                        if (ruta.getTipoRuta().equals("DIRECTO")) {
                            directos++;
                            System.out.printf("✅ Pedido %s: %s → %s (DIRECTO)\n", 
                                            pedido.getId(), sede, pedido.getAeropuertoDestinoId());
                        } else {
                            conEscalas++;
                            System.out.printf("🔄 Pedido %s: %s → %s (CON ESCALAS: %s)\n", 
                                            pedido.getId(), sede, pedido.getAeropuertoDestinoId(),
                                            String.join("→", ruta.getEscalas()));
                        }
                        
                        // Validar plazo
                        boolean cumplePlazo = GestorUTCyContinentesCSV.validarPlazoRuta(
                            sede, pedido.getAeropuertoDestinoId(),
                            java.time.LocalTime.of(pedido.getHora(), pedido.getMinuto()),
                            pedido.getDia(),
                            java.time.LocalTime.now(), // Aproximación de llegada
                            pedido.getDia() + 1
                        );
                        
                        if (!cumplePlazo) {
                            fallidosPorPlazo++;
                            System.out.println("   ⚠️  ADVERTENCIA: Ruta excede plazo continental/intercontinental");
                        }
                        
                        break; // Encontró ruta desde esta sede
                    }
                }
                
                if (!encontroRuta) {
                    System.out.printf("❌ Pedido %s: SIN RUTA VIABLE (destino: %s)\n", 
                                    pedido.getId(), pedido.getAeropuertoDestinoId());
                }
            }
            
            // 📊 Resumen final
            System.out.println("\n📊 =================== RESUMEN FINAL ===================");
            System.out.printf("📦 Pedidos procesados: %d\n", procesados);
            System.out.printf("✅ Pedidos exitosos: %d (%.1f%%)\n", exitosos, (exitosos * 100.0 / procesados));
            System.out.printf("✈️ Rutas directas: %d (%.1f%%)\n", directos, (directos * 100.0 / exitosos));
            System.out.printf("🔄 Rutas con escalas: %d (%.1f%%)\n", conEscalas, (conEscalas * 100.0 / exitosos));
            System.out.printf("⚠️ Advertencias por plazo: %d\n", fallidosPorPlazo);
            
            System.out.println("\n🌍 =================== CARACTERÍSTICAS UTC ===================");
            System.out.println("⏰ Conversión automática a UTC 0 para todos los horarios");
            System.out.println("📍 Clasificación continental: Sudamérica, Europa, Asia");
            System.out.println("📆 Plazos automáticos:");
            System.out.println("   • Continental (mismo continente): 2 días máximo");
            System.out.println("   • Intercontinental (diferentes continentes): 3 días máximo");
            System.out.println("🕐 Gestión de zonas horarias: UTC-5 a UTC+8");
            System.out.println("🌙 Lógica nocturna: Vuelos del día siguiente para pedidos nocturnos");
            
            System.out.println("\n✅ =================== PROCESO COMPLETADO ===================");
            
        } catch (Exception e) {
            System.err.println("❌ Error en el sistema: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Crea vuelos de ejemplo entre las sedes y destinos principales
     */
    private static List<Vuelo> crearVuelosEjemplo() {
        List<Vuelo> vuelos = new ArrayList<>();
        
        // Vuelos desde SPIM (Lima)
        vuelos.add(new Vuelo("SPIM", "SCEL", "08:00", "10:30", 50)); // Lima → Santiago
        vuelos.add(new Vuelo("SPIM", "SBGR", "14:00", "18:00", 100)); // Lima → São Paulo
        vuelos.add(new Vuelo("SPIM", "LEMD", "22:00", "16:30", 200)); // Lima → Madrid
        vuelos.add(new Vuelo("SPIM", "LFPG", "23:30", "18:00", 180)); // Lima → París
        
        // Vuelos desde EBCI (Bruselas)
        vuelos.add(new Vuelo("EBCI", "LEMD", "09:00", "11:00", 120)); // Bruselas → Madrid
        vuelos.add(new Vuelo("EBCI", "LFPG", "10:30", "11:30", 100)); // Bruselas → París
        vuelos.add(new Vuelo("EBCI", "EDDF", "15:00", "16:30", 80)); // Bruselas → Frankfurt
        vuelos.add(new Vuelo("EBCI", "OMDB", "20:00", "06:00", 250)); // Bruselas → Dubái
        
        // Vuelos desde UBBB (Bakú)
        vuelos.add(new Vuelo("UBBB", "OMDB", "12:00", "14:30", 150)); // Bakú → Dubái
        vuelos.add(new Vuelo("UBBB", "VIDP", "16:00", "21:30", 200)); // Bakú → Nueva Delhi
        vuelos.add(new Vuelo("UBBB", "LTBA", "08:00", "09:30", 100)); // Bakú → Estambul
        
        // Conexiones intercontinentales
        vuelos.add(new Vuelo("LEMD", "ZBAA", "14:00", "08:00", 300)); // Madrid → Pekín
        vuelos.add(new Vuelo("EDDF", "RJAA", "11:00", "06:00", 280)); // Frankfurt → Tokio
        vuelos.add(new Vuelo("OMDB", "WSSS", "02:00", "12:00", 200)); // Dubái → Singapur
        
        return vuelos;
    }
    
    /**
     * Crea pedidos de ejemplo con diferentes horarios y destinos
     */
    private static List<Pedido> crearPedidosEjemplo() {
        List<Pedido> pedidos = new ArrayList<>();
        
        // Pedidos continentales (2 días máximo) - formato: dd-hh-mm-dest-###-IdClien
        pedidos.add(new Pedido("01-08-30-SCEL-010-0000001"));  // Lima → Santiago (continental)
        pedidos.add(new Pedido("01-14-00-LEMD-025-0000002"));  // Bruselas → Madrid (continental)
        pedidos.add(new Pedido("01-18-45-OMDB-015-0000003"));  // Bakú → Dubái (continental)
        
        // Pedidos intercontinentales (3 días máximo)
        pedidos.add(new Pedido("01-10-15-ZBAA-030-0000004"));  // Lima → Pekín (intercontinental)
        pedidos.add(new Pedido("01-16-30-VIDP-020-0000005"));  // Bruselas → Nueva Delhi (intercontinental)
        pedidos.add(new Pedido("01-22-00-WSSS-035-0000006"));  // Bakú → Singapur (intercontinental)
        
        // Pedidos nocturnos
        pedidos.add(new Pedido("01-23-30-LFPG-012-0000007"));  // Lima → París (nocturno)
        pedidos.add(new Pedido("01-02-15-LTBA-018-0000008"));  // Bakú → Estambul (nocturno)
        
        return pedidos;
    }
}
