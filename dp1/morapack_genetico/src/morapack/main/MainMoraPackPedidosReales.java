package morapack.main;

import morapack.modelo.*;
import morapack.planificacion.*;
import morapack.datos.CargadorPedidosUltrafinal;
import java.util.List;
import java.util.ArrayList;

/**
 * Programa principal para MoraPack con pedidos reales de pedidoUltrafinal.txt
 * Incluye gestión UTC y plazos continentales
 */
public class MainMoraPackPedidosReales {
    
    public static void main(String[] args) {
        
        System.out.println("🌍 ============== MORAPACK PEDIDOS REALES + UTC ==============");
        System.out.println("📋 Procesamiento de pedidoUltrafinal.txt con restricciones UTC");
        System.out.println("==============================================================");
        
        try {
            // 📂 Cargar pedidos reales
            System.out.println("\n📂 Cargando pedidos reales...");
            List<Pedido> pedidos = CargadorPedidosUltrafinal.cargarPedidos("datos/pedidoUltrafinal.txt");
            
            // Mostrar estadísticas de pedidos
            CargadorPedidosUltrafinal.mostrarEstadisticas(pedidos);
            
            // 🛫 Crear vuelos de ejemplo (usando aeropuertos del CSV)
            System.out.println("\n🛫 Creando red de vuelos...");
            List<Vuelo> vuelos = crearVuelosGlobalesCSV();
            System.out.printf("   ✅ %d vuelos cargados\n", vuelos.size());
            
            // 🕐 Inicializar planificador temporal con UTC y plazos
            System.out.println("\n🕐 Inicializando Planificador Temporal Mejorado...");
            PlanificadorTemporalConUTCyPlazos planificador = new PlanificadorTemporalConUTCyPlazos(vuelos);
            
            // Mostrar info de aeropuertos configurados
            System.out.println("\n🌍 Verificando aeropuertos configurados...");
            GestorUTCyContinentesCSV.mostrarTodosLosAeropuertos();
            
            // 📊 Estadísticas de procesamiento
            int procesados = 0;
            int exitosos = 0;
            int directos = 0;
            int conEscalas = 0;
            int fallidosPorPlazo = 0;
            int fallidosSinRuta = 0;
            
            String[] sedes = {"SPIM", "EBCI", "UBBB"}; // Lima, Bruselas, Bakú
            
            System.out.println("\n📦 ================= PROCESANDO PEDIDOS REALES =================");
            
            // Procesar todos los pedidos reales
            for (Pedido pedido : pedidos) {
                procesados++;
                
                boolean encontroRuta = false;
                
                // Mostrar progreso cada 25 pedidos
                if (procesados % 25 == 0) {
                    System.out.printf("\n   🔄 Progreso: %d/%d pedidos procesados (%.1f%%)\n", 
                                    procesados, pedidos.size(), (procesados * 100.0 / pedidos.size()));
                }
                
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
                        
                        // Validar plazo (aproximación)
                        boolean cumplePlazo = GestorUTCyContinentesCSV.validarPlazoRuta(
                            sede, pedido.getAeropuertoDestinoId(),
                            java.time.LocalTime.of(pedido.getHora(), pedido.getMinuto()),
                            pedido.getDia(),
                            java.time.LocalTime.of(pedido.getHora() + 2, pedido.getMinuto()), // Aproximación de llegada
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
                    fallidosSinRuta++;
                    System.out.printf("❌ Pedido %s: SIN RUTA VIABLE (destino: %s)\n", 
                                    pedido.getId(), pedido.getAeropuertoDestinoId());
                }
            }
            
            // 📊 Resumen final detallado
            System.out.println("\n📊 =================== RESUMEN FINAL DETALLADO ===================");
            System.out.printf("📦 Pedidos procesados: %d\n", procesados);
            System.out.printf("✅ Pedidos exitosos: %d (%.1f%%)\n", exitosos, (exitosos * 100.0 / procesados));
            System.out.printf("❌ Pedidos fallidos: %d (%.1f%%)\n", fallidosSinRuta, (fallidosSinRuta * 100.0 / procesados));
            System.out.printf("✈️ Rutas directas: %d (%.1f%% del total exitoso)\n", directos, exitosos > 0 ? (directos * 100.0 / exitosos) : 0);
            System.out.printf("🔄 Rutas con escalas: %d (%.1f%% del total exitoso)\n", conEscalas, exitosos > 0 ? (conEscalas * 100.0 / exitosos) : 0);
            System.out.printf("⚠️ Advertencias por plazo: %d\n", fallidosPorPlazo);
            
            // Eficiencia por sede
            System.out.println("\n📈 EFICIENCIA DEL SISTEMA:");
            double eficiencia = (exitosos * 100.0 / procesados);
            if (eficiencia >= 90) {
                System.out.printf("   🟢 EXCELENTE: %.1f%% de éxito\n", eficiencia);
            } else if (eficiencia >= 70) {
                System.out.printf("   🟡 BUENO: %.1f%% de éxito\n", eficiencia);
            } else {
                System.out.printf("   🔴 MEJORABLE: %.1f%% de éxito\n", eficiencia);
            }
            
            System.out.println("\n🌍 =================== CARACTERÍSTICAS UTC ===================");
            System.out.println("⏰ Conversión automática a UTC 0 para todos los horarios");
            System.out.println("📍 Clasificación continental: SAM, EUR, ASI (según CSV)");
            System.out.println("📆 Plazos automáticos:");
            System.out.println("   • Continental (mismo continente): 2 días máximo");
            System.out.println("   • Intercontinental (diferentes continentes): 3 días máximo");
            System.out.println("🕐 Gestión de zonas horarias: Según aeropuertos_simple.csv");
            System.out.println("🌙 Lógica nocturna: Vuelos del día siguiente para pedidos nocturnos");
            
            System.out.println("\n✅ =================== PROCESO COMPLETADO ===================");
            System.out.printf("🎯 Sistema procesó %d pedidos reales con éxito del %.1f%%\n", procesados, eficiencia);
            System.out.println("===============================================================");
            
        } catch (Exception e) {
            System.err.println("❌ Error en el sistema: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Crea una red de vuelos global basada en los aeropuertos del CSV
     */
    private static List<Vuelo> crearVuelosGlobalesCSV() {
        List<Vuelo> vuelos = new ArrayList<>();
        
        // Aeropuertos principales por continente (según CSV)
        String[] samAeropuertos = {"SPIM", "SKBO", "SEQM", "SVMI", "SBBR", "SCEL", "SABE", "SGAS", "SUAA", "SLLP"};
        String[] eurAeropuertos = {"EBCI", "LOWW", "EDDI", "LATI", "UMMS", "LBSF", "LKPR", "LDZA", "EKCH", "EHAM"};
        String[] asiAeropuertos = {"UBBB", "VIDP", "OSDI", "OERK", "OMDB", "OAKB", "OOMS", "OYSN", "OPKC", "OJAI"};
        
        // Vuelos intercontinentales (sedes principales)
        vuelos.add(new Vuelo("SPIM", "EBCI", "08:00", "20:00", 300)); // Lima -> Bruselas
        vuelos.add(new Vuelo("SPIM", "UBBB", "22:00", "14:00", 250)); // Lima -> Bakú
        vuelos.add(new Vuelo("EBCI", "SPIM", "10:00", "18:00", 300)); // Bruselas -> Lima
        vuelos.add(new Vuelo("EBCI", "UBBB", "14:00", "22:00", 280)); // Bruselas -> Bakú
        vuelos.add(new Vuelo("UBBB", "SPIM", "16:00", "08:00", 250)); // Bakú -> Lima
        vuelos.add(new Vuelo("UBBB", "EBCI", "12:00", "16:00", 280)); // Bakú -> Bruselas
        
        // Vuelos regionales SAM
        for (int i = 0; i < samAeropuertos.length; i++) {
            for (int j = 0; j < samAeropuertos.length; j++) {
                if (i != j) {
                    vuelos.add(new Vuelo(samAeropuertos[i], samAeropuertos[j], 
                                       String.format("%02d:00", (i * 2 + 8) % 24), 
                                       String.format("%02d:30", (i * 2 + 10) % 24), 
                                       200 + (i * 10)));
                }
            }
        }
        
        // Vuelos regionales EUR
        for (int i = 0; i < eurAeropuertos.length; i++) {
            for (int j = 0; j < eurAeropuertos.length; j++) {
                if (i != j) {
                    vuelos.add(new Vuelo(eurAeropuertos[i], eurAeropuertos[j], 
                                       String.format("%02d:15", (i * 2 + 9) % 24), 
                                       String.format("%02d:45", (i * 2 + 11) % 24), 
                                       180 + (i * 8)));
                }
            }
        }
        
        // Vuelos regionales ASI
        for (int i = 0; i < asiAeropuertos.length; i++) {
            for (int j = 0; j < asiAeropuertos.length; j++) {
                if (i != j) {
                    vuelos.add(new Vuelo(asiAeropuertos[i], asiAeropuertos[j], 
                                       String.format("%02d:30", (i * 2 + 6) % 24), 
                                       String.format("%02d:00", (i * 2 + 8) % 24), 
                                       160 + (i * 12)));
                }
            }
        }
        
        return vuelos;
    }
}
