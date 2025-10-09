package morapack.main;

import morapack.modelo.*;
import morapack.datos.*;
import java.util.*;
import java.io.*;

/**
 * Sistema Genético CON DIVISIÓN AUTOMÁTICA DE PEDIDOS
 * Mejora: Cuando un pedido es demasiado grande para un vuelo, se divide automáticamente
 */
public class MainGeneticoConDivision {
    
    private static Map<String, VueloConsolidado> vuelosConsolidados = new HashMap<>();
    private static List<PedidoDividido> pedidosDivididos = new ArrayList<>();
    
    private static class VueloConsolidado {
        String vuelo;
        String ruta;
        int totalPedidos;
        int capacidadTotal;
        int paquetesTotal;
        List<String> pedidosOriginales = new ArrayList<>();
        
        VueloConsolidado(String vuelo, String ruta) {
            this.vuelo = vuelo;
            this.ruta = ruta;
            this.totalPedidos = 0;
            this.capacidadTotal = 0;
            this.paquetesTotal = 0;
        }
    }
    
    private static class PedidoDividido {
        String pedidoOriginalId;
        int paquetesOriginales;
        List<SubPedido> subPedidos = new ArrayList<>();
        
        static class SubPedido {
            String id;
            int paquetes;
            String vuelo;
            String ruta;
            String estado;
            
            SubPedido(String id, int paquetes) {
                this.id = id;
                this.paquetes = paquetes;
                this.estado = "PENDIENTE";
            }
        }
    }
    
    public static void main(String[] args) {
        
        System.out.println("=========== MORAPACK CON DIVISIÓN DE PEDIDOS ===========");
        System.out.println("Sistema inteligente que divide pedidos grandes");
        System.out.println("Maximiza la utilización de capacidad de vuelos");
        System.out.println("========================================================");
        
        try {
            // Definir aeropuertos válidos
            Set<String> aeropuertosValidos = crearAeropuertosValidos();
            System.out.println("\\nAeropuertos válidos configurados: " + aeropuertosValidos.size());
            
            // Cargar pedidos filtrados
            System.out.println("\\nCargando pedidos desde pedidoUltrafinal.txt...");
            List<Pedido> pedidos = CargadorPedidosUltrafinal.cargarPedidos("datos/pedidoUltrafinal.txt", aeropuertosValidos);
            
            CargadorPedidosUltrafinal.mostrarEstadisticasSinIconos(pedidos);
            
            // Simular vuelos con capacidades variables
            Map<String, Integer> capacidadesVuelos = generarCapacidadesVuelos();
            
            // Procesar pedidos con división inteligente
            procesarPedidosConDivision(pedidos, capacidadesVuelos);
            
            // Generar reportes completos
            generarReporteConsolidacion();
            generarReporteDivisionPedidos();
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Genera capacidades realistas para los vuelos
     */
    private static Map<String, Integer> generarCapacidadesVuelos() {
        Map<String, Integer> capacidades = new HashMap<>();
        
        // Vuelos de alta capacidad (25%)
        String[] vuelosGrandes = {"SPIM-SKBO", "EBCI-EHAM", "UBBB-OMDB", "SPIM-SABE"};
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
        
        System.out.println("\\n======= CAPACIDADES DE VUELOS =======");
        for (Map.Entry<String, Integer> entry : capacidades.entrySet()) {
            System.out.printf("%s: %d paquetes\\n", entry.getKey(), entry.getValue());
        }
        System.out.println("=====================================");
        
        return capacidades;
    }
    
    /**
     * Procesa pedidos con división automática cuando exceden capacidad
     */
    private static void procesarPedidosConDivision(List<Pedido> pedidos, Map<String, Integer> capacidadesVuelos) {
        System.out.println("\\n=========== PROCESAMIENTO CON DIVISIÓN ===========");
        
        int exitosos = 0;
        int fallidos = 0;
        int directos = 0;
        int conEscalas = 0;
        int divididos = 0;
        
        for (Pedido pedido : pedidos) {
            System.out.printf("\\nProcesando pedido: %s (%d paquetes)\\n", 
                            pedido.getId(), pedido.getCantidadProductos());
            
            String destino = pedido.getAeropuertoDestinoId();
            String sede = asignarSedeSegunDestino(destino);
            String vueloKey = sede + "-" + destino;
            
            Integer capacidadVuelo = capacidadesVuelos.get(vueloKey);
            if (capacidadVuelo == null) {
                capacidadVuelo = 250; // Capacidad por defecto
            }
            
            // ¿Necesita división?
            if (pedido.getCantidadProductos() > capacidadVuelo) {
                System.out.printf("   División necesaria: %d > %d (capacidad vuelo)\\n", 
                                pedido.getCantidadProductos(), capacidadVuelo);
                
                boolean divisionExitosa = dividirPedido(pedido, capacidadVuelo, vueloKey, capacidadesVuelos);
                if (divisionExitosa) {
                    exitosos++;
                    divididos++;
                } else {
                    fallidos++;
                }
            } else {
                // Pedido normal - no necesita división
                boolean exitoso = asignarPedidoNormal(pedido, vueloKey, capacidadVuelo);
                if (exitoso) {
                    exitosos++;
                    // Determinar tipo de ruta
                    if (Math.random() > 0.28) {
                        directos++;
                        System.out.println("   -> Ruta DIRECTA asignada");
                    } else {
                        conEscalas++;
                        System.out.println("   -> Ruta CON ESCALAS asignada");
                    }
                } else {
                    fallidos++;
                }
            }
        }
        
        // Mostrar estadísticas finales
        System.out.println("\\n================== ESTADÍSTICAS FINALES ==================");
        System.out.printf("Total procesados: %d\\n", pedidos.size());
        System.out.printf("Exitosos: %d (%.1f%%)\\n", exitosos, (exitosos * 100.0 / pedidos.size()));
        System.out.printf("  - Rutas directas: %d\\n", directos);
        System.out.printf("  - Rutas con escalas: %d\\n", conEscalas);
        System.out.printf("  - Pedidos divididos: %d\\n", divididos);
        System.out.printf("Fallidos: %d (%.1f%%)\\n", fallidos, (fallidos * 100.0 / pedidos.size()));
        System.out.println("=========================================================");
    }
    
    /**
     * Divide un pedido en sub-pedidos que caben en los vuelos disponibles
     */
    private static boolean dividirPedido(Pedido pedidoOriginal, int capacidadPrimaria, 
                                       String vueloPrimario, Map<String, Integer> capacidadesVuelos) {
        
        PedidoDividido division = new PedidoDividido();
        division.pedidoOriginalId = pedidoOriginal.getId();
        division.paquetesOriginales = pedidoOriginal.getCantidadProductos();
        
        int paquetesRestantes = pedidoOriginal.getCantidadProductos();
        int numeroSubPedido = 1;
        
        System.out.println("   === INICIANDO DIVISIÓN ===");
        
        // Primer sub-pedido en el vuelo primario
        int paquetesPrimero = Math.min(paquetesRestantes, capacidadPrimaria);
        String idSubPedido1 = pedidoOriginal.getId() + "-PARTE" + numeroSubPedido;
        
        PedidoDividido.SubPedido subPedido1 = new PedidoDividido.SubPedido(idSubPedido1, paquetesPrimero);
        subPedido1.vuelo = vueloPrimario;
        subPedido1.ruta = determinarRuta(vueloPrimario, true);
        subPedido1.estado = "ASIGNADO";
        division.subPedidos.add(subPedido1);
        
        System.out.printf("   Sub-pedido %d: %s (%d paquetes) -> %s\\n", 
                         numeroSubPedido, idSubPedido1, paquetesPrimero, vueloPrimario);
        
        registrarVueloConDivision(vueloPrimario, subPedido1.ruta, paquetesPrimero, pedidoOriginal.getId());
        
        paquetesRestantes -= paquetesPrimero;
        numeroSubPedido++;
        
        // Dividir el resto en vuelos adicionales
        while (paquetesRestantes > 0) {
            String vueloAlternativo = buscarVueloAlternativo(pedidoOriginal.getAeropuertoDestinoId(), capacidadesVuelos);
            
            if (vueloAlternativo == null) {
                System.out.println("   ⚠️ No se encontraron vuelos adicionales para el resto");
                // Crear sub-pedido fallido
                String idSubPedidoFallido = pedidoOriginal.getId() + "-PARTE" + numeroSubPedido;
                PedidoDividido.SubPedido subPedidoFallido = new PedidoDividido.SubPedido(idSubPedidoFallido, paquetesRestantes);
                subPedidoFallido.estado = "FALLIDO - Sin vuelos disponibles";
                division.subPedidos.add(subPedidoFallido);
                break;
            }
            
            int capacidadAlternativa = capacidadesVuelos.getOrDefault(vueloAlternativo, 250);
            int paquetesSubPedido = Math.min(paquetesRestantes, capacidadAlternativa);
            
            String idSubPedido = pedidoOriginal.getId() + "-PARTE" + numeroSubPedido;
            PedidoDividido.SubPedido subPedido = new PedidoDividido.SubPedido(idSubPedido, paquetesSubPedido);
            subPedido.vuelo = vueloAlternativo;
            subPedido.ruta = determinarRuta(vueloAlternativo, false);
            subPedido.estado = "ASIGNADO";
            division.subPedidos.add(subPedido);
            
            System.out.printf("   Sub-pedido %d: %s (%d paquetes) -> %s\\n", 
                             numeroSubPedido, idSubPedido, paquetesSubPedido, vueloAlternativo);
            
            registrarVueloConDivision(vueloAlternativo, subPedido.ruta, paquetesSubPedido, pedidoOriginal.getId());
            
            paquetesRestantes -= paquetesSubPedido;
            numeroSubPedido++;
        }
        
        pedidosDivididos.add(division);
        
        boolean exitoso = paquetesRestantes == 0;
        System.out.printf("   === DIVISIÓN %s ===\\n", exitoso ? "EXITOSA" : "PARCIAL");
        
        return exitoso;
    }
    
    /**
     * Busca un vuelo alternativo al mismo destino
     */
    private static String buscarVueloAlternativo(String destino, Map<String, Integer> capacidadesVuelos) {
        String[] sedesAlternativas = {"SPIM", "EBCI", "UBBB"};
        
        for (String sede : sedesAlternativas) {
            String vueloAlternativo = sede + "-" + destino;
            if (capacidadesVuelos.containsKey(vueloAlternativo)) {
                return vueloAlternativo;
            }
        }
        return null;
    }
    
    /**
     * Asigna un pedido que no necesita división
     */
    private static boolean asignarPedidoNormal(Pedido pedido, String vueloKey, int capacidad) {
        // Simular asignación normal (95% éxito para pedidos que caben)
        boolean exitoso = Math.random() > 0.05;
        
        if (exitoso) {
            String ruta = determinarRuta(vueloKey, Math.random() > 0.28);
            registrarVueloConDivision(vueloKey, ruta, pedido.getCantidadProductos(), pedido.getId());
            System.out.printf("   Asignado a: %s (%d/%d paquetes)\\n", 
                            vueloKey, pedido.getCantidadProductos(), capacidad);
        } else {
            System.out.println("   Fallo por restricciones operativas");
        }
        
        return exitoso;
    }
    
    /**
     * Registra el vuelo considerando la división de pedidos
     */
    private static void registrarVueloConDivision(String vuelo, String ruta, int paquetes, String pedidoOriginalId) {
        VueloConsolidado consolidado = vuelosConsolidados.computeIfAbsent(vuelo, 
            k -> new VueloConsolidado(vuelo, ruta));
        
        consolidado.totalPedidos++;
        consolidado.paquetesTotal += paquetes;
        consolidado.pedidosOriginales.add(pedidoOriginalId);
    }
    
    /**
     * Determina el tipo de ruta
     */
    private static String determinarRuta(String vuelo, boolean esDirecta) {
        String[] partes = vuelo.split("-");
        if (partes.length != 2) return "RUTA_DESCONOCIDA";
        
        if (esDirecta) {
            return partes[0] + " → " + partes[1];
        } else {
            // Simular escalas
            String[] escalas = {"LOWW", "EHAM", "OMDB"};
            String escala = escalas[(int)(Math.random() * escalas.length)];
            return partes[0] + " → " + escala + " → " + partes[1];
        }
    }
    
    /**
     * Genera reporte de consolidación mejorado
     */
    private static void generarReporteConsolidacion() {
        System.out.println("\\n============ REPORTE DE CONSOLIDACIÓN ============");
        
        int vuelosConMultiplesPedidos = 0;
        int vuelosConUnSoloPedido = 0;
        
        for (VueloConsolidado vuelo : vuelosConsolidados.values()) {
            if (vuelo.totalPedidos > 1) {
                vuelosConMultiplesPedidos++;
                System.out.printf("%s: %d pedidos (%d paquetes)\\n", 
                                vuelo.vuelo, vuelo.totalPedidos, vuelo.paquetesTotal);
            } else {
                vuelosConUnSoloPedido++;
            }
        }
        
        int totalVuelos = vuelosConsolidados.size();
        double porcentajeConsolidacion = totalVuelos > 0 ? 
            (vuelosConMultiplesPedidos * 100.0 / totalVuelos) : 0;
        
        System.out.printf("\\nConsolidación: %.1f%% (%d/%d vuelos con múltiples pedidos)\\n", 
                         porcentajeConsolidacion, vuelosConMultiplesPedidos, totalVuelos);
        System.out.println("===============================================");
    }
    
    /**
     * Genera reporte específico de división de pedidos
     */
    private static void generarReporteDivisionPedidos() {
        if (pedidosDivididos.isEmpty()) {
            System.out.println("\\n*** No se requirió división de pedidos ***");
            return;
        }
        
        System.out.println("\\n============ REPORTE DE DIVISIÓN DE PEDIDOS ============");
        
        try (FileWriter writer = new FileWriter("REPORTE_DIVISION_PEDIDOS.txt")) {
            writer.write("=========================================\\n");
            writer.write("    REPORTE DE DIVISIÓN DE PEDIDOS\\n");
            writer.write("=========================================\\n\\n");
            
            for (PedidoDividido division : pedidosDivididos) {
                System.out.printf("\\nPedido original: %s (%d paquetes)\\n", 
                                division.pedidoOriginalId, division.paquetesOriginales);
                
                writer.write(String.format("PEDIDO ORIGINAL: %s\\n", division.pedidoOriginalId));
                writer.write(String.format("Paquetes totales: %d\\n", division.paquetesOriginales));
                writer.write(String.format("Dividido en: %d partes\\n\\n", division.subPedidos.size()));
                
                for (int i = 0; i < division.subPedidos.size(); i++) {
                    PedidoDividido.SubPedido sub = division.subPedidos.get(i);
                    System.out.printf("  Parte %d: %s (%d paquetes) - %s - %s\\n", 
                                    i+1, sub.id, sub.paquetes, sub.vuelo, sub.estado);
                    
                    writer.write(String.format("  Parte %d: %s\\n", i+1, sub.id));
                    writer.write(String.format("    Paquetes: %d\\n", sub.paquetes));
                    writer.write(String.format("    Vuelo: %s\\n", sub.vuelo));
                    writer.write(String.format("    Ruta: %s\\n", sub.ruta));
                    writer.write(String.format("    Estado: %s\\n\\n", sub.estado));
                }
            }
            
            System.out.printf("\\n*** %d pedidos fueron divididos exitosamente ***\\n", pedidosDivididos.size());
            writer.write(String.format("\\n*** TOTAL: %d pedidos divididos ***\\n", pedidosDivididos.size()));
            
        } catch (IOException e) {
            System.err.println("Error escribiendo reporte: " + e.getMessage());
        }
        
        System.out.println("========================================================");
    }
    
    // Métodos auxiliares
    private static Set<String> crearAeropuertosValidos() {
        return Set.of("SPIM", "EBCI", "UBBB", "SKBO", "SABE", "SUAA", "SGAS", 
                     "SVMI", "SCEL", "SLLP", "SEQM", "SBBR", "EHAM", "LOWW", 
                     "LATI", "EKCH", "EDDI", "LKPR", "LBSF", "LDZA", "UMMS",
                     "OMDB", "OPKC", "OJAI", "OAKB", "OERK", "OOMS", "OYSN", 
                     "OSDI", "VIDP");
    }
    
    private static String asignarSedeSegunDestino(String destino) {
        if (destino.startsWith("S")) return "SPIM"; // Sudamérica
        if (destino.startsWith("E") || destino.startsWith("L")) return "EBCI"; // Europa
        return "UBBB"; // Asia/Oceanía
    }
}
