package com.morapack;

import com.morapack.model.*;
import com.morapack.repository.jpa.*;
import com.morapack.repository.impl.*;
import com.morapack.service.*;
import java.util.List;
import java.util.Random;

/**
 * Aplicación principal para demostrar la nueva arquitectura MoraPack
 */
public class MoraPackApplication {
    
    public static void main(String[] args) {
        System.out.println("=== SISTEMA MORAPACK - ARQUITECTURA PROFESIONAL ===");
        
        // 1. Inicializar repositorios (en memoria)
        PedidoRepository pedidoRepository = new PedidoRepositoryImpl();
        // AeropuertoRepository aeropuertoRepository = new AeropuertoRepositoryImpl();
        // SedeRepository sedeRepository = new SedeRepositoryImpl();
        
        // 2. Inicializar servicios
        PedidoService pedidoService = new PedidoService(pedidoRepository);
        
        // 3. Crear datos de ejemplo
        crearDatosEjemplo(pedidoService);
        
        // 4. Mostrar estadísticas
        mostrarEstadisticas(pedidoService);
        
        // 5. Demostrar funcionalidades
        demostrarFuncionalidades(pedidoService);
        
        System.out.println("\n=== APLICACIÓN MORAPACK COMPLETADA ===");
    }
    
    private static void crearDatosEjemplo(PedidoService pedidoService) {
        System.out.println("\n--- Creando datos de ejemplo ---");
        
        Random random = new Random();
        String[] aeropuertos = {"LIM", "BRU", "BAK", "MAD", "NRT", "JFK"};
        
        for (int i = 1; i <= 15; i++) {
            String id = "PED" + String.format("%03d", i);
            String clienteId = "CLI" + String.format("%03d", random.nextInt(50) + 1);
            String aeropuertoDestino = aeropuertos[random.nextInt(aeropuertos.length)];
            int cantidadProductos = random.nextInt(20) + 1;
            int prioridad = random.nextInt(3) + 1;
            
            Pedido pedido = new Pedido(id, clienteId, aeropuertoDestino, cantidadProductos, prioridad);
            pedidoService.crearPedido(pedido);
        }
        
        System.out.println("✓ Creados 15 pedidos de ejemplo");
    }
    
    private static void mostrarEstadisticas(PedidoService pedidoService) {
        System.out.println("\n--- Estadísticas del Sistema ---");
        
        PedidoService.EstadisticasPedidos stats = pedidoService.obtenerEstadisticas();
        System.out.println("Total de pedidos: " + stats.getTotal());
        System.out.println("Pendientes: " + stats.getPendientes());
        System.out.println("Asignados: " + stats.getAsignados());
        System.out.println("En ruta: " + stats.getEnRuta());
        System.out.println("Entregados: " + stats.getEntregados());
        System.out.println("Cancelados: " + stats.getCancelados());
    }
    
    private static void demostrarFuncionalidades(PedidoService pedidoService) {
        System.out.println("\n--- Demostrando Funcionalidades ---");
        
        // Obtener pedidos pendientes
        List<Pedido> pedidosPendientes = pedidoService.obtenerPedidosPendientes();
        System.out.println("Pedidos pendientes: " + pedidosPendientes.size());
        
        // Asignar algunos pedidos a sedes
        if (!pedidosPendientes.isEmpty()) {
            Pedido primerPedido = pedidosPendientes.get(0);
            try {
                Pedido pedidoAsignado = pedidoService.asignarSedeAPedido(primerPedido.getId(), "SEDE_LIM");
                System.out.println("✓ Pedido " + primerPedido.getId() + " asignado a SEDE_LIM");
                
                // Cambiar estado a EN_RUTA
                Pedido pedidoEnRuta = pedidoService.cambiarEstadoPedido(primerPedido.getId(), EstadoPedido.EN_RUTA);
                System.out.println("✓ Pedido " + primerPedido.getId() + " ahora está EN_RUTA");
                
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
        
        // Obtener pedidos de alta prioridad
        List<Pedido> altaPrioridad = pedidoService.obtenerPedidosAltaPrioridad();
        System.out.println("Pedidos de alta prioridad: " + altaPrioridad.size());
        
        // Mostrar algunos pedidos
        System.out.println("\n--- Primeros 5 pedidos ---");
        List<Pedido> todosPedidos = pedidoService.obtenerTodosPedidos();
        for (int i = 0; i < Math.min(5, todosPedidos.size()); i++) {
            System.out.println((i+1) + ". " + todosPedidos.get(i));
        }
    }
}
