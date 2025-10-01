package morapack.main;

import morapack.datos.CargadorDatosCSV;
import morapack.datos.CargadorPedidos;
import morapack.modelo.Pedido;
import morapack.modelo.Vuelo;
import morapack.planificacion.PlanificadorTemporalMejorado;
import morapack.planificacion.RutaCompleta;

import java.util.List;

/**
 * Contador específico para analizar pedidos del archivo pedidoUltrafinal.txt
 * y determinar cuántos son directos vs con escalas
 */
public class ContadorRutasReales {
    
    public static void main(String[] args) {
        System.out.println("🚀 ANÁLISIS DE RUTAS - ARCHIVO PEDIDOULTRAFINAL.TXT");
        System.out.println("=====================================================");
        
        try {
            // PASO 1: Cargar datos de vuelos
            System.out.println("\n📂 PASO 1: Cargando datos de vuelos");
            List<Vuelo> vuelos = CargadorDatosCSV.cargarVuelos();
            System.out.println("   ✅ Vuelos cargados: " + vuelos.size());
            
            // PASO 2: Cargar pedidos desde pedidoUltrafinal.txt
            System.out.println("\n📦 PASO 2: Cargando pedidos desde pedidoUltrafinal.txt");
            String rutaPedidos = "datos/pedidoUltrafinal.txt";
            List<Pedido> pedidos = CargadorPedidos.cargarPedidosDesdeArchivo(rutaPedidos);
            
            if (pedidos.isEmpty()) {
                System.err.println("❌ No se pudieron cargar pedidos del archivo: " + rutaPedidos);
                return;
            }
            
            System.out.println("   ✅ Pedidos cargados: " + pedidos.size());
            
            // PASO 3: Inicializar planificador temporal
            System.out.println("\n🧠 PASO 3: Inicializando planificador temporal");
            PlanificadorTemporalMejorado planificador = new PlanificadorTemporalMejorado(vuelos);
            
            // PASO 4: Procesar y contar rutas
            System.out.println("\n🔄 PASO 4: Procesando y contando rutas");
            analizarRutas(planificador, pedidos);
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Analiza las rutas y cuenta directas vs escalas
     */
    private static void analizarRutas(PlanificadorTemporalMejorado planificador, List<Pedido> pedidos) {
        int rutasDirectas = 0;
        int rutasConEscalas = 0;
        int rutasFallidas = 0;
        int totalProcesados = 0;
        
        System.out.println("📊 Analizando " + pedidos.size() + " pedidos...\n");
        
        for (Pedido pedido : pedidos) {
            totalProcesados++;
            
            try {
                // Determinar sede óptima
                String sedeOptima = determinarSedeOptima(pedido.getAeropuertoDestinoId());
                pedido.setSedeAsignadaId(sedeOptima);
                
                // Planificar ruta con lógica temporal
                RutaCompleta ruta = planificador.planificarRutaTemporal(pedido, sedeOptima);
                
                if (ruta != null && ruta.esViable()) {
                    if (ruta.getVuelos().size() == 1) {
                        rutasDirectas++;
                        // Mostrar cada 50 procesados para seguimiento
                        if (totalProcesados % 50 == 0) {
                            System.out.printf("📈 Progreso: %d/%d pedidos - Directas: %d, Escalas: %d\n", 
                                totalProcesados, pedidos.size(), rutasDirectas, rutasConEscalas);
                        }
                    } else {
                        rutasConEscalas++;
                        // Mostrar ejemplo de ruta con escalas
                        System.out.printf("🔄 Ruta con escalas: %s → %s [%d escalas]\n", 
                            sedeOptima, pedido.getAeropuertoDestinoId(), ruta.getVuelos().size() - 1);
                        for (int i = 0; i < ruta.getVuelos().size(); i++) {
                            Vuelo vuelo = ruta.getVuelos().get(i);
                            System.out.printf("   %d. %s → %s (%s-%s)\n", 
                                i + 1, vuelo.getOrigen(), vuelo.getDestino(),
                                vuelo.getHoraSalida(), vuelo.getHoraLlegada());
                        }
                    }
                } else {
                    rutasFallidas++;
                    System.out.printf("❌ SIN RUTA: %s → %s\n", 
                        sedeOptima, pedido.getAeropuertoDestinoId());
                }
                
            } catch (Exception e) {
                rutasFallidas++;
                System.out.printf("❌ ERROR en pedido %s: %s\n", pedido.getId(), e.getMessage());
            }
        }
        
        // RESULTADOS FINALES
        int rutasExitosas = rutasDirectas + rutasConEscalas;
        double porcentajeExito = (rutasExitosas * 100.0) / totalProcesados;
        double porcentajeDirectas = rutasExitosas > 0 ? (rutasDirectas * 100.0) / rutasExitosas : 0;
        double porcentajeEscalas = rutasExitosas > 0 ? (rutasConEscalas * 100.0) / rutasExitosas : 0;
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("📊 RESUMEN FINAL - ARCHIVO PEDIDOULTRAFINAL.TXT");
        System.out.println("=".repeat(60));
        
        System.out.printf("📦 Total pedidos procesados: %d\n", totalProcesados);
        System.out.printf("✅ Rutas exitosas: %d (%.1f%%)\n", rutasExitosas, porcentajeExito);
        System.out.printf("❌ Rutas fallidas: %d\n", rutasFallidas);
        
        System.out.println("\n🎯 DESGLOSE DE RUTAS EXITOSAS:");
        System.out.printf("🚀 Rutas DIRECTAS: %d (%.1f%% del total exitoso)\n", rutasDirectas, porcentajeDirectas);
        System.out.printf("🔄 Rutas CON ESCALAS: %d (%.1f%% del total exitoso)\n", rutasConEscalas, porcentajeEscalas);
        
        // Análisis adicional
        System.out.println("\n📈 ANÁLISIS:");
        if (porcentajeDirectas > 90) {
            System.out.println("   ⭐ EXCELENTE: Más del 90% de rutas son directas");
        } else if (porcentajeDirectas > 80) {
            System.out.println("   ✅ MUY BUENO: Más del 80% de rutas son directas");
        } else {
            System.out.println("   ⚠️ MEJORABLE: Menos del 80% de rutas son directas");
        }
        
        if (rutasConEscalas > 0) {
            System.out.printf("   🔗 Promedio escalas por ruta compleja: %.1f\n", 
                rutasConEscalas > 0 ? rutasConEscalas / (double) rutasConEscalas : 0);
        }
        
        // Calcular total de paquetes
        int totalPaquetes = pedidos.stream().mapToInt(Pedido::getCantidadProductos).sum();
        System.out.printf("\n📦 Total paquetes a transportar: %d\n", totalPaquetes);
        System.out.printf("📊 Promedio paquetes por pedido: %.1f\n", totalPaquetes / (double) totalProcesados);
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
