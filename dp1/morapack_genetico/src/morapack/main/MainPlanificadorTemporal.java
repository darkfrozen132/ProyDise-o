package morapack.main;

import morapack.modelo.*;
import morapack.datos.*;
import morapack.planificacion.*;
import java.util.*;

/**
 * Demostración del Planificador Temporal Mejorado
 * 
 * NUEVAS CARACTERÍSTICAS:
 * ⏰ Horarios realistas de pedidos vs vuelos
 * 📅 Plan de vuelos diario repetitivo
 * ⌛ Tiempo de preparación de 30 minutos
 * 🌙 Lógica nocturna/día siguiente
 * 📦 Gestión de capacidad
 */
public class MainPlanificadorTemporal {
    
    public static void main(String[] args) {
        System.out.println("🚀 MORAPACK GENETICO - PLANIFICADOR TEMPORAL MEJORADO");
        System.out.println("===================================================");
        System.out.println("⏰ Incorpora horarios realistas y lógica temporal");
        System.out.println();
        
        try {
            // 1️⃣ CARGAR DATOS
            System.out.println("📂 PASO 1: Carga de datos");
            List<Vuelo> vuelos = CargadorDatosCSV.cargarVuelos();
            System.out.println("   Vuelos cargados: " + vuelos.size());
            
            // 2️⃣ CREAR PLANIFICADOR MEJORADO
            System.out.println("\n🧠 PASO 2: Inicialización del planificador");
            PlanificadorTemporalMejorado planificador = new PlanificadorTemporalMejorado(vuelos);
            
            // 3️⃣ CREAR PEDIDOS DE PRUEBA CON HORARIOS DIVERSOS
            System.out.println("\n📦 PASO 3: Generación de pedidos de prueba");
            List<Pedido> pedidosPrueba = generarPedidosDiversos();
            
            // 4️⃣ PROCESAR PEDIDOS CON LÓGICA TEMPORAL
            System.out.println("\n🔄 PASO 4: Procesamiento temporal de pedidos");
            procesarPedidosConLogicaTemporal(planificador, pedidosPrueba);
            
            // 5️⃣ MOSTRAR ESTADÍSTICAS
            System.out.println("\n📊 PASO 5: Estadísticas finales");
            planificador.mostrarEstadisticas();
            
        } catch (Exception e) {
            System.err.println("❌ ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Genera pedidos con horarios diversos para probar todas las lógicas
     */
    private static List<Pedido> generarPedidosDiversos() {
        List<Pedido> pedidos = new ArrayList<>();
        
        // 🌅 PEDIDOS MATUTINOS (06:00 - 11:59)
        pedidos.add(crearPedido(1, 8, 30, "SKBO", 50));    // 08:30 → Bogotá
        pedidos.add(crearPedido(1, 10, 15, "SBBR", 25));   // 10:15 → Brasilia
        
        // ☀️ PEDIDOS MEDIODÍA (12:00 - 17:59)
        pedidos.add(crearPedido(1, 14, 45, "LOWW", 75));   // 14:45 → Viena
        pedidos.add(crearPedido(1, 16, 20, "EHAM", 40));   // 16:20 → Amsterdam
        
        // 🌆 PEDIDOS TARDÍOS (18:00 - 21:59)
        pedidos.add(crearPedido(1, 19, 30, "OMDB", 60));   // 19:30 → Dubai
        pedidos.add(crearPedido(1, 20, 45, "VIDP", 35));   // 20:45 → Delhi
        
        // 🌙 PEDIDOS NOCTURNOS (22:00 - 05:59) - CASOS ESPECIALES
        pedidos.add(crearPedido(1, 23, 15, "OAKB", 45));   // 23:15 → Kabul (nocturno)
        pedidos.add(crearPedido(2, 2, 30, "OSDI", 30));    // 02:30 → Damasco (madrugada)
        pedidos.add(crearPedido(2, 4, 45, "OERK", 55));    // 04:45 → Riad (muy temprano)
        
        // 📦 PEDIDOS CON GRANDES VOLÚMENES (probar capacidad)
        pedidos.add(crearPedido(1, 12, 0, "SUAA", 200));   // 12:00 → Uruguayana (gran volumen)
        
        System.out.println("   Pedidos generados: " + pedidos.size());
        for (Pedido pedido : pedidos) {
            System.out.println("     📦 " + pedido.getId() + " (" + 
                             String.format("%02d:%02d", pedido.getHora(), pedido.getMinuto()) + 
                             " → " + pedido.getAeropuertoDestinoId() + ")");
        }
        
        return pedidos;
    }
    
    /**
     * Crea un pedido con parámetros específicos
     */
    private static Pedido crearPedido(int dia, int hora, int minuto, String destino, int cantidad) {
        String id = Pedido.crearId(dia, hora, minuto, destino, cantidad, "1234567");
        // Usar el constructor que acepta ID para que se parseen correctamente todos los valores
        Pedido pedido = new Pedido(id);
        return pedido;
    }
    
    /**
     * Procesa los pedidos usando la lógica temporal mejorada
     */
    private static void procesarPedidosConLogicaTemporal(PlanificadorTemporalMejorado planificador, 
                                                        List<Pedido> pedidos) {
        
        int rutasExitosas = 0;
        int rutasDirectas = 0;
        int rutasConEscalas = 0;
        int rutasFallidas = 0;
        
        for (int i = 0; i < pedidos.size(); i++) {
            Pedido pedido = pedidos.get(i);
            
            System.out.println("\n" + (i+1) + ". 📦 PROCESANDO PEDIDO: " + pedido.getId());
            System.out.println("   ⏰ Hora del pedido: " + 
                             String.format("%02d:%02d", pedido.getHora(), pedido.getMinuto()));
            System.out.println("   🎯 Destino: " + pedido.getAeropuertoDestinoId());
            System.out.println("   📦 Cantidad: " + pedido.getCantidadProductos());
            
            // Determinar sede origen según lógica continental
            String sedeOrigen = determinarSedeOptima(pedido.getAeropuertoDestinoId());
            System.out.println("   🏢 Sede asignada: " + sedeOrigen);
            
            // ⏰ APLICAR LÓGICA TEMPORAL MEJORADA
            RutaCompleta ruta = planificador.planificarRutaTemporal(pedido, sedeOrigen);
            
            if (ruta != null && ruta.esViable()) {
                rutasExitosas++;
                
                int numVuelos = ruta.getVuelos().size();
                if (numVuelos == 1) {
                    rutasDirectas++;
                    System.out.println("   ✅ RESULTADO: RUTA DIRECTA");
                } else {
                    rutasConEscalas++;
                    System.out.println("   ✅ RESULTADO: RUTA CON " + (numVuelos-1) + " ESCALAS");
                }
                
                System.out.println("   📋 Detalles: " + ruta.obtenerDescripcion());
                
                // Mostrar todos los vuelos de la ruta
                for (int j = 0; j < ruta.getVuelos().size(); j++) {
                    Vuelo vuelo = ruta.getVuelos().get(j);
                    String tipoSegmento = (j == 0) ? "🛫 SALIDA" : "🔄 CONEXIÓN " + j;
                    System.out.println("       " + tipoSegmento + ": " + vuelo.getOrigen() + 
                                     " → " + vuelo.getDestino() + 
                                     " (" + vuelo.getHoraSalida() + "-" + vuelo.getHoraLlegada() + ")");
                }
                
            } else {
                rutasFallidas++;
                System.out.println("   ❌ RESULTADO: SIN RUTA DISPONIBLE");
                
                // Diagnosticar el problema
                if (pedido.getHora() >= 22 || pedido.getHora() < 6) {
                    System.out.println("       💡 Posible causa: Pedido nocturno sin conexiones disponibles");
                } else {
                    System.out.println("       💡 Posible causa: Capacidad insuficiente o sin vuelos al destino");
                }
            }
        }
        
        // 📊 RESUMEN FINAL DE RESULTADOS
        System.out.println("\n" + "=".repeat(50));
        System.out.println("📊 RESUMEN DE RESULTADOS CON LÓGICA TEMPORAL:");
        System.out.println("   ✅ Rutas exitosas: " + rutasExitosas + "/" + pedidos.size() + 
                         " (" + String.format("%.1f%%", 100.0 * rutasExitosas / pedidos.size()) + ")");
        System.out.println("   ✈️ Rutas directas: " + rutasDirectas + 
                         " (" + String.format("%.1f%%", 100.0 * rutasDirectas / rutasExitosas) + ")");
        System.out.println("   🔄 Rutas con escalas: " + rutasConEscalas + 
                         " (" + String.format("%.1f%%", 100.0 * rutasConEscalas / rutasExitosas) + ")");
        System.out.println("   ❌ Rutas fallidas: " + rutasFallidas);
    }
    
    /**
     * Determina la sede óptima según el destino (lógica continental)
     */
    private static String determinarSedeOptima(String destino) {
        // Lógica simplificada basada en códigos ICAO
        if (destino.startsWith("S")) {
            return "SPIM"; // Lima para Sudamérica
        } else if (destino.startsWith("O") || destino.startsWith("V") || destino.startsWith("U")) {
            return "UBBB"; // Baku para Asia/Medio Oriente
        } else {
            return "EBCI"; // Bruselas para Europa
        }
    }
}
