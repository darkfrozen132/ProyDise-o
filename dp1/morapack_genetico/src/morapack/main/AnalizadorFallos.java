package morapack.main;

import morapack.datos.CargadorPedidosUltrafinal;
import morapack.modelo.Pedido;
import java.util.*;

/**
 * Analizador de fallos para identificar por qué ciertos pedidos no se pueden planificar
 */
public class AnalizadorFallos {
    
    // Los 7 pedidos que fallaron según el reporte
    private static final String[] PEDIDOS_FALLIDOS = {
        "19-17-24-EHAM-289-0000005",
        "12-21-58-OMDB-229-0000021", 
        "10-12-15-OOMS-227-0000023",
        "27-16-26-SVMI-297-0000007",
        "03-08-44-SLLP-225-0000021",
        "21-22-54-OERK-292-0000006",
        "09-20-38-OERK-244-0000008"
    };
    
    public static void main(String[] args) {
        System.out.println("=================================================================");
        System.out.println("           ANÁLISIS DETALLADO DE PEDIDOS FALLIDOS");
        System.out.println("=================================================================");
        
        try {
            // Cargar los pedidos
            List<Pedido> todosPedidos = CargadorPedidosUltrafinal.cargarPedidos(
                "datos/pedidoUltrafinal.txt");
            
            System.out.printf("Total de pedidos cargados: %d\\n\\n", todosPedidos.size());
            
            // Generar capacidades simuladas (usando la misma lógica que el sistema principal)
            Map<String, Integer> capacidadesVuelos = generarCapacidadesDinamicas();
            
            // Analizar cada pedido fallido
            analizarPedidosFallidos(todosPedidos, capacidadesVuelos);
            
            // Generar reporte de análisis
            generarReporteAnalisis(todosPedidos, capacidadesVuelos);
            
        } catch (Exception e) {
            System.err.println("Error durante el análisis: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Genera las mismas capacidades dinámicas que usa el sistema principal
     */
    private static Map<String, Integer> generarCapacidadesDinamicas() {
        Map<String, Integer> capacidades = new HashMap<>();
        
        // Vuelos de alta capacidad (25%)
        String[] vuelosGrandes = {"SPIM-SKBO", "EBCI-EHAM", "UBBB-OPKC", "SPIM-SABE", 
                                 "EBCI-LKPR", "UBBB-OMDB", "SPIM-SBBR", "EBCI-EDDI"};
        for (String vuelo : vuelosGrandes) {
            capacidades.put(vuelo, 400 + (int)(Math.random() * 200)); // 400-600 paquetes
        }
        
        // Vuelos de capacidad media (50%)
        String[] vuelosMedios = {"SPIM-SGAS", "EBCI-LOWW", "UBBB-OJAI", "SPIM-SUAA", 
                                "EBCI-LATI", "UBBB-OAKB", "SPIM-SVMI", "EBCI-EKCH"};
        for (String vuelo : vuelosMedios) {
            capacidades.put(vuelo, 200 + (int)(Math.random() * 150)); // 200-350 paquetes
        }
        
        // Vuelos de baja capacidad (25%)
        String[] vuelosPequeños = {"SPIM-SCEL", "EBCI-LBSF", "UBBB-OYSN", "SPIM-SEQM"};
        for (String vuelo : vuelosPequeños) {
            capacidades.put(vuelo, 100 + (int)(Math.random() * 100)); // 100-200 paquetes
        }
        
        return capacidades;
    }
    
    /**
     * Analiza en detalle cada pedido fallido
     */
    private static void analizarPedidosFallidos(List<Pedido> todosPedidos, Map<String, Integer> capacidadesVuelos) {
        System.out.println("=== ANÁLISIS INDIVIDUAL DE PEDIDOS FALLIDOS ===\\n");
        
        for (String idFallido : PEDIDOS_FALLIDOS) {
            Pedido pedidoFallido = buscarPedidoPorId(todosPedidos, idFallido);
            
            if (pedidoFallido == null) {
                System.out.printf("⚠️ PEDIDO %s NO ENCONTRADO\\n\\n", idFallido);
                continue;
            }
            
            analizarPedidoEspecifico(pedidoFallido, capacidadesVuelos);
        }
    }
    
    /**
     * Busca un pedido por su ID
     */
    private static Pedido buscarPedidoPorId(List<Pedido> pedidos, String id) {
        for (Pedido pedido : pedidos) {
            if (pedido.getId().equals(id)) {
                return pedido;
            }
        }
        return null;
    }
    
    /**
     * Analiza un pedido específico
     */
    private static void analizarPedidoEspecifico(Pedido pedido, Map<String, Integer> capacidadesVuelos) {
        System.out.println("─".repeat(60));
        System.out.printf("PEDIDO: %s\\n", pedido.getId());
        System.out.printf("DESTINO: %s\\n", pedido.getAeropuertoDestinoId());
        System.out.printf("PAQUETES: %d\\n", pedido.getCantidadProductos());
        System.out.println("─".repeat(60));
        
        String destino = pedido.getAeropuertoDestinoId();
        String sede = asignarSedeSegunDestino(destino);
        String vueloPrimario = sede + "-" + destino;
        
        System.out.printf("Sede asignada: %s\\n", sede);
        System.out.printf("Vuelo primario: %s\\n", vueloPrimario);
        
        // 1. Verificar si existe vuelo directo
        Integer capacidadPrimaria = capacidadesVuelos.get(vueloPrimario);
        if (capacidadPrimaria == null) {
            System.out.println("❌ PROBLEMA 1: No existe vuelo directo a este destino");
            System.out.println("   Causa: El destino no está en la configuración de vuelos disponibles");
        } else {
            System.out.printf("✅ Vuelo directo disponible con capacidad: %d paquetes\\n", capacidadPrimaria);
            
            // 2. Verificar si cabe en vuelo primario
            if (pedido.getCantidadProductos() <= capacidadPrimaria) {
                System.out.println("✅ El pedido cabría en el vuelo primario");
                System.out.println("   Posible causa de fallo: Restricciones operativas aleatorias (5% de fallos)");
            } else {
                System.out.printf("⚠️ Necesita división: %d > %d\\n", 
                                pedido.getCantidadProductos(), capacidadPrimaria);
                
                // 3. Verificar vuelos alternativos para división
                analizarVuelosAlternativos(pedido, capacidadesVuelos);
            }
        }
        
        // 4. Verificar conectividad del destino
        analizarConectividadDestino(destino, capacidadesVuelos);
        
        System.out.println();
    }
    
    /**
     * Analiza vuelos alternativos para división de pedidos
     */
    private static void analizarVuelosAlternativos(Pedido pedido, Map<String, Integer> capacidadesVuelos) {
        System.out.println("\\n--- ANÁLISIS DE VUELOS ALTERNATIVOS ---");
        
        String destino = pedido.getAeropuertoDestinoId();
        String[] sedesAlternativas = {"SPIM", "EBCI", "UBBB"};
        
        List<String> vuelosDisponibles = new ArrayList<>();
        int capacidadTotalAlternativa = 0;
        
        for (String sede : sedesAlternativas) {
            String vueloAlternativo = sede + "-" + destino;
            Integer capacidad = capacidadesVuelos.get(vueloAlternativo);
            
            if (capacidad != null) {
                vuelosDisponibles.add(vueloAlternativo + " (cap: " + capacidad + ")");
                capacidadTotalAlternativa += capacidad;
            }
        }
        
        if (vuelosDisponibles.isEmpty()) {
            System.out.println("❌ PROBLEMA 2: No hay vuelos alternativos al destino " + destino);
            System.out.println("   Causa: El destino no tiene conectividad desde ninguna sede");
        } else {
            System.out.printf("✅ Vuelos alternativos encontrados: %d\\n", vuelosDisponibles.size());
            for (String vuelo : vuelosDisponibles) {
                System.out.printf("   - %s\\n", vuelo);
            }
            
            if (capacidadTotalAlternativa >= pedido.getCantidadProductos()) {
                System.out.printf("✅ Capacidad total suficiente: %d >= %d\\n", 
                                capacidadTotalAlternativa, pedido.getCantidadProductos());
                System.out.println("   Posible causa: Error en lógica de división o restricciones operativas");
            } else {
                System.out.printf("❌ PROBLEMA 3: Capacidad insuficiente: %d < %d\\n", 
                                capacidadTotalAlternativa, pedido.getCantidadProductos());
            }
        }
    }
    
    /**
     * Analiza la conectividad general de un destino
     */
    private static void analizarConectividadDestino(String destino, Map<String, Integer> capacidadesVuelos) {
        System.out.println("\\n--- ANÁLISIS DE CONECTIVIDAD ---");
        
        int vuelosConectados = 0;
        int capacidadTotal = 0;
        
        for (Map.Entry<String, Integer> vuelo : capacidadesVuelos.entrySet()) {
            if (vuelo.getKey().endsWith("-" + destino)) {
                vuelosConectados++;
                capacidadTotal += vuelo.getValue();
                System.out.printf("   Conectado: %s (capacidad: %d)\\n", 
                                vuelo.getKey(), vuelo.getValue());
            }
        }
        
        if (vuelosConectados == 0) {
            System.out.printf("❌ PROBLEMA CRÍTICO: %s no tiene conectividad aérea\\n", destino);
            System.out.println("   Solución: Agregar rutas a este destino o usar escalas");
        } else {
            System.out.printf("✅ Destino conectado por %d rutas con capacidad total: %d\\n", 
                            vuelosConectados, capacidadTotal);
        }
    }
    
    /**
     * Asigna sede según destino (misma lógica del sistema principal)
     */
    private static String asignarSedeSegunDestino(String destino) {
        // Lógica basada en regiones geográficas
        if (destino.startsWith("S")) return "SPIM";      // Sudamérica
        if (destino.startsWith("E") || destino.startsWith("L")) return "EBCI";  // Europa
        return "UBBB";  // Asia/Oceanía/África
    }
    
    /**
     * Genera reporte final de análisis
     */
    private static void generarReporteAnalisis(List<Pedido> todosPedidos, Map<String, Integer> capacidadesVuelos) {
        System.out.println("\\n=================================================================");
        System.out.println("                    REPORTE DE ANÁLISIS FINAL");
        System.out.println("=================================================================");
        
        // Contar destinos sin conectividad
        Set<String> destinosSinConectividad = new HashSet<>();
        Set<String> todosDestinos = new HashSet<>();
        
        for (Pedido pedido : todosPedidos) {
            String destino = pedido.getAeropuertoDestinoId();
            todosDestinos.add(destino);
            
            boolean tieneConectividad = false;
            for (String vuelo : capacidadesVuelos.keySet()) {
                if (vuelo.endsWith("-" + destino)) {
                    tieneConectividad = true;
                    break;
                }
            }
            
            if (!tieneConectividad) {
                destinosSinConectividad.add(destino);
            }
        }
        
        System.out.printf("Total de destinos únicos: %d\\n", todosDestinos.size());
        System.out.printf("Destinos sin conectividad: %d\\n", destinosSinConectividad.size());
        
        if (!destinosSinConectividad.isEmpty()) {
            System.out.println("\\nDESTINOS SIN CONECTIVIDAD:");
            for (String destino : destinosSinConectividad) {
                System.out.printf("   - %s\\n", destino);
            }
        }
        
        // Análisis de distribución de capacidades
        System.out.println("\\nDISTRIBUCIÓN DE CAPACIDADES:");
        int vuelosAlta = 0, vuelosMedia = 0, vuelosBaja = 0;
        
        for (Integer capacidad : capacidadesVuelos.values()) {
            if (capacidad >= 400) vuelosAlta++;
            else if (capacidad >= 200) vuelosMedia++;
            else vuelosBaja++;
        }
        
        System.out.printf("   - Alta capacidad (≥400): %d vuelos\\n", vuelosAlta);
        System.out.printf("   - Media capacidad (200-399): %d vuelos\\n", vuelosMedia);
        System.out.printf("   - Baja capacidad (<200): %d vuelos\\n", vuelosBaja);
        
        System.out.println("\\nCONCLUSIONES:");
        System.out.println("1. Los fallos pueden deberse a:");
        System.out.println("   - Destinos sin conectividad aérea directa");
        System.out.println("   - Capacidad insuficiente en vuelos disponibles");
        System.out.println("   - Restricciones operativas aleatorias (5% de fallos)");
        System.out.println("   - Errores en la lógica de búsqueda de vuelos alternativos");
        System.out.println("\\n2. Recomendaciones:");
        System.out.println("   - Expandir red de vuelos a destinos sin conectividad");
        System.out.println("   - Implementar rutas con escalas para destinos remotos");
        System.out.println("   - Revisar lógica de división de pedidos grandes");
        
        System.out.println("=================================================================");
    }
}
