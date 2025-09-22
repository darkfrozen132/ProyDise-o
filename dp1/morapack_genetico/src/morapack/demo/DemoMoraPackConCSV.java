package morapack.demo;

import morapack.dao.impl.*;
import morapack.datos.CargadorDatosCSV;
import morapack.modelo.*;
import java.util.*;

/**
 * Demostración del sistema MoraPack usando datos reales desde CSV
 */
public class DemoMoraPackConCSV {
    
    public static void main(String[] args) {
        System.out.println("=== MORAPACK GENÉTICO CON DATOS CSV REALES ===\n");
        
        // Inicializar DAOs
        AeropuertoDAOImpl aeropuertoDAO = new AeropuertoDAOImpl();
        SedeDAOImpl sedeDAO = new SedeDAOImpl();
        PedidoDAOImpl pedidoDAO = new PedidoDAOImpl();
        
        // Cargar sistema completo desde CSV
        CargadorDatosCSV.cargarSistemaCompleto(aeropuertoDAO, sedeDAO, pedidoDAO);
        
        System.out.println("\n" + "=".repeat(50));
        System.out.println("📊 ANÁLISIS DE DATOS CARGADOS");
        System.out.println("=".repeat(50));
        
        // Análisis de aeropuertos
        analizarAeropuertos(aeropuertoDAO);
        
        // Análisis de sedes
        analizarSedes(sedeDAO);
        
        // Análisis de pedidos
        analizarPedidos(pedidoDAO);
        
        // Demostrar consultas específicas
        demostrarConsultasAvanzadas(pedidoDAO, aeropuertoDAO);
        
        System.out.println("\n" + "=".repeat(50));
        System.out.println("🎯 SISTEMA LISTO PARA OPTIMIZACIÓN GENÉTICA");
        System.out.println("=".repeat(50));
        System.out.println("Los datos están cargados y listos para ser procesados");
        System.out.println("por el algoritmo genético de optimización de rutas.");
    }
    
    private static void analizarAeropuertos(AeropuertoDAOImpl aeropuertoDAO) {
        System.out.println("\n🛫 ANÁLISIS DE AEROPUERTOS:");
        
        List<Aeropuerto> aeropuertos = aeropuertoDAO.obtenerTodos();
        Map<String, Integer> porContinente = new HashMap<>();
        
        for (Aeropuerto aeropuerto : aeropuertos) {
            String continente = aeropuerto.getContinente();
            porContinente.put(continente, porContinente.getOrDefault(continente, 0) + 1);
        }
        
        System.out.println("   Total de aeropuertos: " + aeropuertos.size());
        System.out.println("   Distribución por continente:");
        for (Map.Entry<String, Integer> entry : porContinente.entrySet()) {
            System.out.println("     • " + entry.getKey() + ": " + entry.getValue() + " aeropuertos");
        }
        
        List<Aeropuerto> sedes = aeropuertoDAO.obtenerSedes();
        System.out.println("   Aeropuertos que son sedes: " + sedes.size());
        for (Aeropuerto sede : sedes) {
            System.out.println("     • " + sede.getCodigoICAO() + " - " + sede.getCiudad() + ", " + sede.getPais());
        }
    }
    
    private static void analizarSedes(SedeDAOImpl sedeDAO) {
        System.out.println("\n🏢 ANÁLISIS DE SEDES:");
        
        List<Sede> sedes = sedeDAO.obtenerTodos();
        double capacidadTotal = sedeDAO.calcularCapacidadTotal();
        double capacidadDisponible = sedeDAO.calcularCapacidadDisponible();
        
        System.out.println("   Total de sedes: " + sedes.size());
        System.out.println("   Sedes activas: " + sedeDAO.contarActivas());
        System.out.println("   Capacidad total: " + (int)capacidadTotal + " pedidos");
        System.out.println("   Capacidad disponible: " + (int)capacidadDisponible + " pedidos");
        System.out.println("   Utilización: " + String.format("%.1f%%", 
                          ((capacidadTotal - capacidadDisponible) / capacidadTotal) * 100));
        
        System.out.println("   Detalle de sedes:");
        for (Sede sede : sedes) {
            System.out.println("     • " + sede.getId() + " - " + sede.getNombre() + 
                             " (Cap: " + (int)sede.getCapacidadMaxima() + ", " + sede.getEstado() + ")");
        }
    }
    
    private static void analizarPedidos(PedidoDAOImpl pedidoDAO) {
        System.out.println("\n📦 ANÁLISIS DE PEDIDOS:");
        
        long totalPedidos = pedidoDAO.contarTotal();
        long pendientes = pedidoDAO.contarPorEstado("PENDIENTE");
        
        System.out.println("   Total de pedidos: " + totalPedidos);
        System.out.println("   Pedidos pendientes: " + pendientes);
        
        // Análisis por día
        System.out.println("   Distribución por día:");
        for (int dia = 1; dia <= 7; dia++) {
            long pedidosDia = pedidoDAO.contarPorDia(dia);
            if (pedidosDia > 0) {
                double promedio = pedidoDAO.calcularPromedioProductosPorDia(dia);
                int maximo = pedidoDAO.obtenerCantidadMaximaPorDia(dia);
                System.out.println("     • Día " + dia + ": " + pedidosDia + " pedidos" +
                                 " (promedio: " + String.format("%.0f", promedio) + 
                                 " productos, máximo: " + maximo + ")");
            }
        }
        
        // Análisis por destino
        System.out.println("   Destinos más solicitados:");
        Map<String, Integer> destinosCount = new HashMap<>();
        List<Pedido> pedidos = pedidoDAO.obtenerTodos();
        
        for (Pedido pedido : pedidos) {
            String destino = pedido.getAeropuertoDestinoId();
            destinosCount.put(destino, destinosCount.getOrDefault(destino, 0) + 1);
        }
        
        destinosCount.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(5)
            .forEach(entry -> System.out.println("     • " + entry.getKey() + ": " + entry.getValue() + " pedidos"));
    }
    
    private static void demostrarConsultasAvanzadas(PedidoDAOImpl pedidoDAO, AeropuertoDAOImpl aeropuertoDAO) {
        System.out.println("\n🔍 CONSULTAS AVANZADAS:");
        
        // Pedidos de alta prioridad
        List<Pedido> altaPrioridad = pedidoDAO.obtenerPorPrioridad(1);
        System.out.println("   Pedidos de alta prioridad: " + altaPrioridad.size());
        
        // Pedidos grandes (más de 200 productos)
        List<Pedido> pedidosGrandes = pedidoDAO.obtenerPorCantidadMinima(200);
        System.out.println("   Pedidos grandes (200+ productos): " + pedidosGrandes.size());
        
        // Pedidos matutinos
        List<Pedido> matutinos = pedidoDAO.obtenerPorRangoHoras(6, 12);
        System.out.println("   Pedidos matutinos (6-12h): " + matutinos.size());
        
        // Cliente más activo
        Map<String, Integer> clientesCount = new HashMap<>();
        List<Pedido> pedidos = pedidoDAO.obtenerTodos();
        
        for (Pedido pedido : pedidos) {
            String cliente = pedido.getClienteId();
            clientesCount.put(cliente, clientesCount.getOrDefault(cliente, 0) + 1);
        }
        
        Optional<Map.Entry<String, Integer>> clienteTop = clientesCount.entrySet().stream()
            .max(Map.Entry.comparingByValue());
        
        if (clienteTop.isPresent()) {
            System.out.println("   Cliente más activo: " + clienteTop.get().getKey() + 
                             " (" + clienteTop.get().getValue() + " pedidos)");
        }
        
        // Aeropuertos destino más cercanos a sedes venezolanas
        System.out.println("   Rutas recomendadas desde Venezuela:");
        List<Aeropuerto> sedesVE = aeropuertoDAO.obtenerPorPais("Venezuela");
        List<Aeropuerto> destinos = aeropuertoDAO.obtenerDestinos();
        
        for (Aeropuerto sede : sedesVE) {
            // Buscar destino más cercano
            Aeropuerto destinoCercano = null;
            double distanciaMinima = Double.MAX_VALUE;
            
            for (Aeropuerto destino : destinos) {
                double distancia = calcularDistancia(sede, destino);
                if (distancia < distanciaMinima) {
                    distanciaMinima = distancia;
                    destinoCercano = destino;
                }
            }
            
            if (destinoCercano != null) {
                System.out.println("     • " + sede.getCodigoICAO() + " → " + 
                                 destinoCercano.getCodigoICAO() + " (" + 
                                 String.format("%.0f", distanciaMinima) + " km)");
            }
        }
    }
    
    private static double calcularDistancia(Aeropuerto a1, Aeropuerto a2) {
        double lat1 = Math.toRadians(a1.getLatitud());
        double lon1 = Math.toRadians(a1.getLongitud());
        double lat2 = Math.toRadians(a2.getLatitud());
        double lon2 = Math.toRadians(a2.getLongitud());
        
        double dlat = lat2 - lat1;
        double dlon = lon2 - lon1;
        
        double a = Math.sin(dlat/2) * Math.sin(dlat/2) +
                   Math.cos(lat1) * Math.cos(lat2) *
                   Math.sin(dlon/2) * Math.sin(dlon/2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return 6371 * c; // Radio de la Tierra en km
    }
}
