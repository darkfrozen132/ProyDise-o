package morapack.demo;

import morapack.dao.impl.*;
import morapack.modelo.*;
import java.util.*;

/**
 * Demostración del sistema MoraPack con el nuevo formato de pedidos
 */
public class DemoMoraPackGenetico {
    
    public static void main(String[] args) {
        System.out.println("=== DEMO MORAPACK CON ALGORITMO GENÉTICO ===\n");
        
        // Inicializar DAOs
        PedidoDAOImpl pedidoDAO = new PedidoDAOImpl();
        AeropuertoDAOImpl aeropuertoDAO = new AeropuertoDAOImpl();
        SedeDAOImpl sedeDAO = new SedeDAOImpl();
        
        // 1. Crear aeropuertos de ejemplo
        System.out.println("1. Creando aeropuertos...");
        crearAeropuertosEjemplo(aeropuertoDAO);
        System.out.println("   Aeropuertos creados: " + aeropuertoDAO.contarTotal());
        System.out.println();
        
        // 2. Crear sedes en algunos aeropuertos
        System.out.println("2. Creando sedes...");
        crearSedesEjemplo(sedeDAO);
        System.out.println("   Sedes creadas: " + sedeDAO.contarTotal());
        System.out.println("   Sedes activas: " + sedeDAO.contarActivas());
        System.out.println();
        
        // 3. Generar pedidos con el nuevo formato
        System.out.println("3. Generando pedidos mensuales...");
        generarPedidosEjemplo(pedidoDAO);
        System.out.println("   Pedidos creados: " + pedidoDAO.contarTotal());
        System.out.println("   Pendientes: " + pedidoDAO.contarPorEstado("PENDIENTE"));
        System.out.println();
        
        // 4. Mostrar estadísticas por día/hora
        System.out.println("4. Estadísticas por tiempo:");
        mostrarEstadisticasTiempo(pedidoDAO);
        System.out.println();
        
        // 5. Mostrar pedidos por destino
        System.out.println("5. Pedidos por destino:");
        mostrarPedidosPorDestino(pedidoDAO);
        System.out.println();
        
        // 6. Demostrar consultas específicas del nuevo formato
        System.out.println("6. Consultas específicas del nuevo formato:");
        demostrarConsultasEspecificas(pedidoDAO);
        
        System.out.println("\n=== SISTEMA LISTO PARA OPTIMIZACIÓN GENÉTICA ===");
        System.out.println("Los datos están preparados para ser procesados por el algoritmo genético");
        System.out.println("que optimizará la asignación de pedidos a sedes.");
    }
    
    private static void crearAeropuertosEjemplo(AeropuertoDAOImpl aeropuertoDAO) {
        // Aeropuertos venezolanos
        Aeropuerto svmi = new Aeropuerto("SVMI", "Caracas", "Venezuela", "América del Sur", 10.6014, -66.9908);
        svmi.setCodigoCorto("CCS");
        svmi.setSede(true);
        aeropuertoDAO.crear(svmi);
        
        Aeropuerto svmg = new Aeropuerto("SVMG", "Maracaibo", "Venezuela", "América del Sur", 10.5582, -71.7278);
        svmg.setCodigoCorto("MAR");
        svmg.setSede(true);
        aeropuertoDAO.crear(svmg);
        
        // Aeropuertos internacionales
        aeropuertoDAO.crear(new Aeropuerto("SBBR", "Brasília", "Brasil", "América del Sur", -15.8697, -47.9208));
        aeropuertoDAO.crear(new Aeropuerto("KJFK", "New York", "Estados Unidos", "América del Norte", 40.6413, -73.7781));
        aeropuertoDAO.crear(new Aeropuerto("SCFA", "Antofagasta", "Chile", "América del Sur", -23.4445, -70.4451));
    }
    
    private static void crearSedesEjemplo(SedeDAOImpl sedeDAO) {
        sedeDAO.crear(new Sede("SEDE001", "Sede Central Caracas", "SVMI", 200, "PRINCIPAL"));
        sedeDAO.crear(new Sede("SEDE002", "Sede Maracaibo", "SVMG", 150, "DEPOSITO"));
        
        // Configurar algunas sedes como activas
        sedeDAO.obtenerTodos().forEach(sede -> {
            sede.setEstado("ACTIVA");
            sedeDAO.actualizar(sede);
        });
    }
    
    private static void generarPedidosEjemplo(PedidoDAOImpl pedidoDAO) {
        String[] aeropuertos = {"SBBR", "KJFK", "SCFA", "SVMG"};
        String[] clientes = {"0000001", "0000123", "0001234", "0005678", "0009876"};
        Random random = new Random();
        
        // Generar pedidos para diferentes días y horas
        for (int dia = 1; dia <= 5; dia++) {
            for (int hora = 8; hora < 18; hora += 2) {
                for (int i = 0; i < 3; i++) {
                    int minuto = random.nextInt(60);
                    String aeropuerto = aeropuertos[random.nextInt(aeropuertos.length)];
                    int cantidad = random.nextInt(300) + 1;
                    String cliente = clientes[random.nextInt(clientes.length)];
                    
                    String idPedido = Pedido.crearId(dia, hora, minuto, aeropuerto, cantidad, cliente);
                    Pedido pedido = new Pedido(idPedido);
                    pedido.setPrioridad(random.nextInt(3) + 1);
                    pedido.setEstado("PENDIENTE");
                    
                    pedidoDAO.crear(pedido);
                }
            }
        }
    }
    
    private static void mostrarEstadisticasTiempo(PedidoDAOImpl pedidoDAO) {
        for (int dia = 1; dia <= 5; dia++) {
            long pedidosDia = pedidoDAO.contarPorDia(dia);
            double promedioCantidad = pedidoDAO.calcularPromedioProductosPorDia(dia);
            int maxCantidad = pedidoDAO.obtenerCantidadMaximaPorDia(dia);
            
            System.out.printf("   Día %02d: %d pedidos, promedio %.1f productos, máximo %d productos%n", 
                            dia, pedidosDia, promedioCantidad, maxCantidad);
        }
        
        System.out.println("   Pedidos por hora:");
        for (int hora = 8; hora < 18; hora += 2) {
            long pedidosHora = pedidoDAO.contarPorHora(hora);
            System.out.printf("     %02d:xx - %d pedidos%n", hora, pedidosHora);
        }
    }
    
    private static void mostrarPedidosPorDestino(PedidoDAOImpl pedidoDAO) {
        String[] destinos = {"SBBR", "KJFK", "SCFA", "SVMG"};
        
        for (String destino : destinos) {
            List<Pedido> pedidos = pedidoDAO.obtenerPorAeropuertoDestino(destino);
            System.out.printf("   %s: %d pedidos%n", destino, pedidos.size());
        }
    }
    
    private static void demostrarConsultasEspecificas(PedidoDAOImpl pedidoDAO) {
        // Pedidos de alta cantidad
        List<Pedido> altaCantidad = pedidoDAO.obtenerPorCantidadMinima(200);
        System.out.println("   Pedidos con 200+ productos: " + altaCantidad.size());
        
        // Pedidos de un cliente específico
        List<Pedido> pedidosCliente = pedidoDAO.obtenerPorCliente("0001234");
        System.out.println("   Pedidos del cliente 0001234: " + pedidosCliente.size());
        
        // Pedidos en horario matutino
        List<Pedido> matutinos = pedidoDAO.obtenerPorRangoHoras(8, 12);
        System.out.println("   Pedidos matutinos (8-12h): " + matutinos.size());
        
        // Mostrar algunos ejemplos
        if (!altaCantidad.isEmpty()) {
            System.out.println("   Ejemplo de pedido grande: " + altaCantidad.get(0));
        }
    }
}
