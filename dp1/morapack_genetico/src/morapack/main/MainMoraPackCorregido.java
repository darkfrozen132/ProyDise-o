package morapack.main;

import morapack.modelo.*;
import morapack.planificacion.*;
import morapack.datos.*;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * Programa principal CORREGIDO para MoraPack 
 * Usa vuelos_completos.csv y filtra pedidos válidos según aeropuertos_simple.csv
 */
public class MainMoraPackCorregido {
    
    public static void main(String[] args) {
        
        System.out.println("🌍 ============ MORAPACK SISTEMA CORREGIDO ============");
        System.out.println("✅ Vuelos reales desde vuelos_completos.csv");
        System.out.println("✅ Pedidos filtrados por aeropuertos_simple.csv");
        System.out.println("✅ Gestión UTC y plazos continentales");
        System.out.println("======================================================");
        
        try {
            // 📋 Definir aeropuertos válidos (según aeropuertos_simple.csv)
            Set<String> aeropuertosValidos = crearAeropuertosValidos();
            System.out.println("\n📋 Aeropuertos válidos configurados: " + aeropuertosValidos.size());
            
            // ✈️ Cargar vuelos reales
            System.out.println("\n✈️ Cargando vuelos reales...");
            List<Vuelo> vuelos = CargadorVuelosCompletos.cargarVuelos("datos/vuelos_completos.csv");
            
            // Validar que los vuelos usen aeropuertos válidos
            CargadorVuelosCompletos.validarAeropuertos(vuelos, aeropuertosValidos);
            CargadorVuelosCompletos.mostrarEstadisticas(vuelos);
            
            // 📦 Cargar pedidos filtrados
            System.out.println("\n📦 Cargando pedidos filtrados...");
            List<Pedido> pedidos = CargadorPedidosUltrafinal.cargarPedidos("datos/pedidoUltrafinal.txt", aeropuertosValidos);
            
            // Mostrar estadísticas de pedidos
            CargadorPedidosUltrafinal.mostrarEstadisticas(pedidos);
            
            // 🕐 Inicializar planificador temporal con UTC y plazos
            System.out.println("\n🕐 Inicializando Planificador Temporal Mejorado...");
            PlanificadorTemporalConUTCyPlazos planificador = new PlanificadorTemporalConUTCyPlazos(vuelos);
            
            // Mostrar aeropuertos configurados para UTC
            System.out.println("\n🌍 Aeropuertos configurados para UTC:");
            GestorUTCyContinentesCSV.mostrarTodosLosAeropuertos();
            
            // 📊 Estadísticas de procesamiento
            int procesados = 0;
            int exitosos = 0;
            int directos = 0;
            int conEscalas = 0;
            int fallidosPorPlazo = 0;
            int fallidosSinRuta = 0;
            
            String[] sedes = {"SPIM", "EBCI", "UBBB"}; // Lima, Bruselas, Bakú
            
            System.out.println("\n📦 ============= PROCESANDO PEDIDOS FILTRADOS =============");
            
            // Procesar pedidos válidos
            for (Pedido pedido : pedidos) {
                procesados++;
                
                boolean encontroRuta = false;
                
                // Mostrar progreso cada 20 pedidos
                if (procesados % 20 == 0) {
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
                        
                        // Validar plazo (aproximación) - manejar horas válidas
                        int horaLlegada = Math.min(pedido.getHora() + 2, 23); // Máximo 23:59
                        boolean cumplePlazo = GestorUTCyContinentesCSV.validarPlazoRuta(
                            sede, pedido.getAeropuertoDestinoId(),
                            java.time.LocalTime.of(pedido.getHora(), pedido.getMinuto()),
                            pedido.getDia(),
                            java.time.LocalTime.of(horaLlegada, pedido.getMinuto()),
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
            System.out.println("\n📊 ================ RESUMEN FINAL CORREGIDO ================");
            System.out.printf("📦 Pedidos procesados: %d\n", procesados);
            System.out.printf("✅ Pedidos exitosos: %d (%.1f%%)\n", exitosos, (exitosos * 100.0 / procesados));
            System.out.printf("❌ Pedidos fallidos: %d (%.1f%%)\n", fallidosSinRuta, (fallidosSinRuta * 100.0 / procesados));
            System.out.printf("✈️ Rutas directas: %d (%.1f%% del total exitoso)\n", directos, exitosos > 0 ? (directos * 100.0 / exitosos) : 0);
            System.out.printf("🔄 Rutas con escalas: %d (%.1f%% del total exitoso)\n", conEscalas, exitosos > 0 ? (conEscalas * 100.0 / exitosos) : 0);
            System.out.printf("⚠️ Advertencias por plazo: %d\n", fallidosPorPlazo);
            
            // Eficiencia del sistema
            System.out.println("\n📈 EFICIENCIA DEL SISTEMA CORREGIDO:");
            double eficiencia = (exitosos * 100.0 / procesados);
            if (eficiencia >= 90) {
                System.out.printf("   🟢 EXCELENTE: %.1f%% de éxito\n", eficiencia);
            } else if (eficiencia >= 70) {
                System.out.printf("   🟡 BUENO: %.1f%% de éxito\n", eficiencia);
            } else if (eficiencia >= 40) {
                System.out.printf("   🟠 MODERADO: %.1f%% de éxito\n", eficiencia);
            } else {
                System.out.printf("   🔴 MEJORABLE: %.1f%% de éxito\n", eficiencia);
            }
            
            System.out.println("\n✅ ================== CORRECCIONES APLICADAS ==================");
            System.out.println("🔧 1. Vuelos cargados desde vuelos_completos.csv (vuelos reales)");
            System.out.println("🔧 2. Pedidos filtrados por destinos válidos en aeropuertos_simple.csv");
            System.out.println("🔧 3. Gestor UTC actualizado con husos exactos del CSV");
            System.out.println("🔧 4. Validación completa de aeropuertos en vuelos y pedidos");
            
            System.out.println("\n✅ =================== PROCESO COMPLETADO ===================");
            System.out.printf("🎯 Sistema procesó %d pedidos válidos con éxito del %.1f%%\n", procesados, eficiencia);
            System.out.println("===============================================================");
            
        } catch (Exception e) {
            System.err.println("❌ Error en el sistema: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Crea el conjunto de aeropuertos válidos según aeropuertos_simple.csv
     */
    private static Set<String> crearAeropuertosValidos() {
        Set<String> aeropuertos = new HashSet<>();
        
        // SUDAMÉRICA (SAM)
        aeropuertos.add("SKBO"); // Bogotá, Colombia
        aeropuertos.add("SEQM"); // Quito, Ecuador
        aeropuertos.add("SVMI"); // Caracas, Venezuela
        aeropuertos.add("SBBR"); // Brasilia, Brasil
        aeropuertos.add("SPIM"); // Lima, Perú (sede)
        aeropuertos.add("SLLP"); // La Paz, Bolivia
        aeropuertos.add("SCEL"); // Santiago de Chile, Chile
        aeropuertos.add("SABE"); // Buenos Aires, Argentina
        aeropuertos.add("SGAS"); // Asunción, Paraguay
        aeropuertos.add("SUAA"); // Montevideo, Uruguay
        
        // EUROPA (EUR)
        aeropuertos.add("LATI"); // Tirana, Albania
        aeropuertos.add("EDDI"); // Berlín, Alemania
        aeropuertos.add("LOWW"); // Viena, Austria
        aeropuertos.add("EBCI"); // Bruselas, Bélgica (sede)
        aeropuertos.add("UMMS"); // Minsk, Bielorrusia
        aeropuertos.add("LBSF"); // Sofía, Bulgaria
        aeropuertos.add("LKPR"); // Praga, República Checa
        aeropuertos.add("LDZA"); // Zagreb, Croacia
        aeropuertos.add("EKCH"); // Copenhague, Dinamarca
        aeropuertos.add("EHAM"); // Ámsterdam, Holanda
        
        // ASIA (ASI)
        aeropuertos.add("VIDP"); // Delhi, India
        aeropuertos.add("OSDI"); // Damasco, Siria
        aeropuertos.add("OERK"); // Riad, Arabia Saudita
        aeropuertos.add("OMDB"); // Dubai, Emiratos A.U.
        aeropuertos.add("OAKB"); // Kabul, Afganistán
        aeropuertos.add("OOMS"); // Mascate, Omán
        aeropuertos.add("OYSN"); // Sana, Yemen
        aeropuertos.add("OPKC"); // Karachi, Pakistán
        aeropuertos.add("UBBB"); // Baku, Azerbaiyán (sede)
        aeropuertos.add("OJAI"); // Amán, Jordania
        
        return aeropuertos;
    }
}
