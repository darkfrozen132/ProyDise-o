package morapack.main;

import morapack.datos.CargadorDatosCSV;
import morapack.datos.CargadorPedidos;
import morapack.modelo.Pedido;
import morapack.modelo.Vuelo;
import morapack.planificacion.PlanificadorTemporalMejorado;
import morapack.planificacion.RutaCompleta;

import java.util.List;

/**
 * Main para probar el planificador temporal con pedidos reales
 * Usa el archivo pedidoUltrafinal.txt con 212 pedidos reales
 */
public class MainPlanificadorPedidosReales {
    
    public static void main(String[] args) {
        System.out.println("🚀 MORAPACK GENETICO - PLANIFICADOR CON PEDIDOS REALES");
        System.out.println("====================================================");
        System.out.println("⏰ Procesando pedidos del archivo pedidoUltrafinal.txt");
        
        try {
            // PASO 1: Cargar datos de vuelos
            System.out.println("\n📂 PASO 1: Carga de datos");
            
            List<Vuelo> vuelos = CargadorDatosCSV.cargarVuelos();
            
            System.out.println("   Vuelos cargados: " + vuelos.size());
            
            // PASO 2: Inicializar planificador temporal
            System.out.println("\n🧠 PASO 2: Inicialización del planificador");
            PlanificadorTemporalMejorado planificador = new PlanificadorTemporalMejorado(vuelos);
            
            // PASO 3: Cargar pedidos reales desde archivo
            System.out.println("\n📦 PASO 3: Carga de pedidos reales");
            
            // Copiar archivo a la ubicación correcta
            String rutaPedidos = "../morapack_coloniav2/datos/pedidoUltrafinal.txt";
            List<Pedido> pedidos = CargadorPedidos.cargarPedidosDesdeArchivo(rutaPedidos);
            
            if (pedidos.isEmpty()) {
                System.err.println("❌ No se pudieron cargar pedidos del archivo: " + rutaPedidos);
                return;
            }
            
            System.out.println("   ✅ Pedidos cargados exitosamente: " + pedidos.size());
            
            // Mostrar estadísticas de pedidos
            CargadorPedidos.mostrarEstadisticasPedidos(pedidos);
            
            // PASO 4: Procesar pedidos con lógica temporal
            System.out.println("\n🔄 PASO 4: Procesamiento temporal de pedidos");
            System.out.println("Procesando " + pedidos.size() + " pedidos reales...\n");
            
            procesarPedidosReales(planificador, pedidos);
            
        } catch (Exception e) {
            System.err.println("❌ Error en el procesamiento: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Procesa los pedidos reales usando el planificador temporal
     */
    private static void procesarPedidosReales(PlanificadorTemporalMejorado planificador, List<Pedido> pedidos) {
        int exitosas = 0;
        int rutasDirectas = 0;
        int rutasConEscalas = 0;
        int fallidas = 0;
        
        System.out.println("📋 Iniciando procesamiento de " + pedidos.size() + " pedidos...\n");
        
        for (int i = 0; i < pedidos.size(); i++) {
            Pedido pedido = pedidos.get(i);
            
            // Mostrar progreso cada 20 pedidos
            if (i % 20 == 0) {
                System.out.printf("📊 Progreso: %d/%d pedidos procesados (%.1f%%)\n", 
                                i, pedidos.size(), (i * 100.0) / pedidos.size());
            }
            
            try {
                System.out.printf("\n%d. 📦 PROCESANDO PEDIDO: %s\n", i + 1, pedido.getId());
                System.out.printf("   ⏰ Hora del pedido: %02d:%02d\n", pedido.getHora(), pedido.getMinuto());
                System.out.printf("   🎯 Destino: %s\n", pedido.getAeropuertoDestinoId());
                System.out.printf("   📦 Cantidad: %d\n", pedido.getCantidadProductos());
                
                // Determinar sede óptima (lógica simple por región)
                String sedeOptima = determinarSedeOptima(pedido.getAeropuertoDestinoId());
                pedido.setSedeAsignadaId(sedeOptima);
                System.out.printf("   🏢 Sede asignada: %s\n", sedeOptima);
                
                // Planificar ruta con lógica temporal
                RutaCompleta ruta = planificador.planificarRutaTemporal(pedido, sedeOptima);
                
                if (ruta != null && ruta.esViable()) {
                    exitosas++;
                    
                    if (ruta.getVuelos().size() == 1) {
                        rutasDirectas++;
                        System.out.println("   ✅ RESULTADO: RUTA DIRECTA");
                    } else {
                        rutasConEscalas++;
                        System.out.printf("   ✅ RESULTADO: RUTA CON %d ESCALAS\n", ruta.getVuelos().size() - 1);
                    }
                    
                    System.out.printf("   📋 Detalles: %s\n", ruta.obtenerDescripcion());
                    
                    // Mostrar cada vuelo de la ruta
                    ruta.getVuelos().forEach(vuelo -> {
                        System.out.printf("       🛫 %s: %s → %s (%s-%s)\n",
                            ruta.getVuelos().size() == 1 ? "SALIDA" : "ESCALA",
                            vuelo.getOrigen(),
                            vuelo.getDestino(),
                            vuelo.getHoraSalida(),
                            vuelo.getHoraLlegada()
                        );
                    });
                    
                } else {
                    fallidas++;
                    System.out.println("   ❌ RESULTADO: NO SE ENCONTRÓ RUTA");
                    System.out.println("   📋 Motivo: Sin vuelos disponibles o capacidad insuficiente");
                }
                
            } catch (Exception e) {
                fallidas++;
                System.out.printf("   ❌ ERROR: %s\n", e.getMessage());
            }
        }
        
        // RESUMEN FINAL
        double tasaExito = (exitosas * 100.0) / pedidos.size();
        double porcentajeDirectas = exitosas > 0 ? (rutasDirectas * 100.0) / exitosas : 0;
        double porcentajeEscalas = exitosas > 0 ? (rutasConEscalas * 100.0) / exitosas : 0;
        
        System.out.println("\n==================================================");
        System.out.println("📊 RESUMEN FINAL - PEDIDOS REALES CON LÓGICA TEMPORAL:");
        System.out.printf("   ✅ Rutas exitosas: %d/%d (%.1f%%)\n", exitosas, pedidos.size(), tasaExito);
        System.out.printf("   ✈️ Rutas directas: %d (%.1f%%)\n", rutasDirectas, porcentajeDirectas);
        System.out.printf("   🔄 Rutas con escalas: %d (%.1f%%)\n", rutasConEscalas, porcentajeEscalas);
        System.out.printf("   ❌ Rutas fallidas: %d\n", fallidas);
        
        if (rutasConEscalas > 0) {
            System.out.println("\n📊 ANÁLISIS DE ESCALAS:");
            System.out.printf("   Las rutas con escalas representan el %.1f%% de las rutas exitosas\n", porcentajeEscalas);
            System.out.println("   Esto demuestra la capacidad del algoritmo para encontrar rutas complejas");
        }
        
        System.out.println("\n📊 PASO 5: Estadísticas finales");
        System.out.println("📊 ESTADÍSTICAS DEL PLANIFICADOR TEMPORAL:");
        
        // Calcular estadísticas adicionales
        int totalPaquetes = pedidos.stream().mapToInt(Pedido::getCantidadProductos).sum();
        System.out.printf("   Total paquetes a transportar: %d\n", totalPaquetes);
        System.out.printf("   Promedio paquetes por pedido: %.1f\n", totalPaquetes / (double) pedidos.size());
        System.out.printf("   Pedidos procesados exitosamente: %d\n", exitosas);
    }
    
    /**
     * Determina la sede óptima basada en el destino (lógica geográfica simplificada)
     */
    private static String determinarSedeOptima(String destino) {
        // Sede SPIM (Lima, Perú) para Sudamérica
        if (destino.startsWith("S") || destino.equals("SKBO") || destino.equals("SGAS") || 
            destino.equals("SBBR") || destino.equals("SEQM") || destino.equals("SUAA") || 
            destino.equals("SABE") || destino.equals("SVMI")) {
            return "SPIM";
        }
        
        // Sede EBCI (Bruselas, Bélgica) para Europa
        if (destino.startsWith("E") || destino.startsWith("L") || destino.equals("LOWW") || 
            destino.equals("EHAM") || destino.equals("LKPR") || destino.equals("EKCH") ||
            destino.equals("EDDI") || destino.equals("LDZA") || destino.equals("LATI")) {
            return "EBCI";
        }
        
        // Sede UBBB (Moscú, Rusia) para Asia, Medio Oriente y África
        if (destino.startsWith("O") || destino.startsWith("U") || destino.startsWith("V") || 
            destino.equals("OMDB") || destino.equals("OERK") || destino.equals("VIDP") || 
            destino.equals("OAKB") || destino.equals("OSDI") || destino.equals("OJAI") ||
            destino.equals("UMMS") || destino.equals("OOMS") || destino.equals("OPKC") ||
            destino.equals("OYSN")) {
            return "UBBB";
        }
        
        // Por defecto, asignar a SPIM
        return "SPIM";
    }
}
