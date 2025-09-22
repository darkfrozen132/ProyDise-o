package morapack.dao.impl;

import morapack.dao.PedidoDAO;
import morapack.modelo.Pedido;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementación en memoria del DAO de pedidos
 */
public class PedidoDAOImpl implements PedidoDAO {
    
    private final Map<String, Pedido> pedidos = new HashMap<>();
    
    @Override
    public void crear(Pedido pedido) {
        if (pedido.getId() == null) {
            throw new IllegalArgumentException("El ID del pedido no puede ser null");
        }
        if (pedidos.containsKey(pedido.getId())) {
            throw new IllegalArgumentException("Ya existe un pedido con ID: " + pedido.getId());
        }
        pedidos.put(pedido.getId(), pedido);
    }
    
    @Override
    public Optional<Pedido> obtenerPorId(String id) {
        return Optional.ofNullable(pedidos.get(id));
    }
    
    @Override
    public List<Pedido> obtenerTodos() {
        return new ArrayList<>(pedidos.values());
    }
    
    @Override
    public void actualizar(Pedido pedido) {
        if (!pedidos.containsKey(pedido.getId())) {
            throw new IllegalArgumentException("No existe pedido con ID: " + pedido.getId());
        }
        pedidos.put(pedido.getId(), pedido);
    }
    
    @Override
    public void eliminar(String id) {
        if (!pedidos.containsKey(id)) {
            throw new IllegalArgumentException("No existe pedido con ID: " + id);
        }
        pedidos.remove(id);
    }
    
    @Override
    public boolean existe(String id) {
        return pedidos.containsKey(id);
    }
    
    @Override
    public List<Pedido> obtenerPorEstado(String estado) {
        return pedidos.values().stream()
                .filter(p -> estado.equals(p.getEstado()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Pedido> obtenerPendientes() {
        return obtenerPorEstado("PENDIENTE");
    }
    
    @Override
    public List<Pedido> obtenerSinAsignar() {
        return pedidos.values().stream()
                .filter(p -> p.getSedeAsignadaId() == null || p.getSedeAsignadaId().isEmpty())
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Pedido> obtenerPorSede(String sedeId) {
        return pedidos.values().stream()
                .filter(p -> sedeId.equals(p.getSedeAsignadaId()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Pedido> obtenerPorAeropuertoDestino(String aeropuertoId) {
        return pedidos.values().stream()
                .filter(p -> aeropuertoId.equals(p.getAeropuertoDestinoId()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Pedido> obtenerPorPrioridad(int prioridad) {
        return pedidos.values().stream()
                .filter(p -> p.getPrioridad() == prioridad)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Pedido> obtenerAltaPrioridad() {
        return obtenerPorPrioridad(1);
    }
    
    @Override
    public List<Pedido> obtenerVencidos() {
        LocalDateTime ahora = LocalDateTime.now();
        return pedidos.values().stream()
                .filter(p -> p.getFechaLimiteEntrega().isBefore(ahora))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Pedido> obtenerParaOptimizacion() {
        LocalDateTime ahora = LocalDateTime.now();
        return pedidos.values().stream()
                .filter(p -> "PENDIENTE".equals(p.getEstado()))
                .filter(p -> p.getFechaLimiteEntrega().isAfter(ahora))
                .collect(Collectors.toList());
    }
    
    @Override
    public long contarTotal() {
        return pedidos.size();
    }
    
    @Override
    public long contarPorEstado(String estado) {
        return pedidos.values().stream()
                .filter(p -> estado.equals(p.getEstado()))
                .count();
    }
    
    @Override
    public double calcularCostoTotal() {
        return pedidos.values().stream()
                .mapToDouble(p -> p.getCantidadProductos() * 10.0) // Costo base simplificado
                .sum();
    }
    
    @Override
    public void crearVarios(List<Pedido> listaPedidos) {
        for (Pedido pedido : listaPedidos) {
            crear(pedido);
        }
    }
    
    @Override
    public void actualizarVarios(List<Pedido> listaPedidos) {
        for (Pedido pedido : listaPedidos) {
            actualizar(pedido);
        }
    }
    
    // Métodos basados en el nuevo formato de ID
    @Override
    public List<Pedido> obtenerPorDia(int dia) {
        return pedidos.values().stream()
                .filter(p -> p.getDia() == dia)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Pedido> obtenerPorHora(int hora) {
        return pedidos.values().stream()
                .filter(p -> p.getHora() == hora)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Pedido> obtenerPorRangoHoras(int horaInicio, int horaFin) {
        return pedidos.values().stream()
                .filter(p -> p.getHora() >= horaInicio && p.getHora() <= horaFin)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Pedido> obtenerPorCliente(String clienteId) {
        return pedidos.values().stream()
                .filter(p -> clienteId.equals(p.getClienteId()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Pedido> obtenerPorCantidadMinima(int cantidadMinima) {
        return pedidos.values().stream()
                .filter(p -> p.getCantidadProductos() >= cantidadMinima)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Pedido> obtenerPorCantidadRango(int cantidadMin, int cantidadMax) {
        return pedidos.values().stream()
                .filter(p -> p.getCantidadProductos() >= cantidadMin && 
                           p.getCantidadProductos() <= cantidadMax)
                .collect(Collectors.toList());
    }
    
    @Override
    public long contarPorDia(int dia) {
        return pedidos.values().stream()
                .filter(p -> p.getDia() == dia)
                .count();
    }
    
    @Override
    public long contarPorHora(int hora) {
        return pedidos.values().stream()
                .filter(p -> p.getHora() == hora)
                .count();
    }
    
    @Override
    public long contarPorCliente(String clienteId) {
        return pedidos.values().stream()
                .filter(p -> clienteId.equals(p.getClienteId()))
                .count();
    }
    
    @Override
    public double calcularPromedioProductosPorDia(int dia) {
        return pedidos.values().stream()
                .filter(p -> p.getDia() == dia)
                .mapToInt(Pedido::getCantidadProductos)
                .average()
                .orElse(0.0);
    }
    
    @Override
    public int obtenerCantidadMaximaPorDia(int dia) {
        return pedidos.values().stream()
                .filter(p -> p.getDia() == dia)
                .mapToInt(Pedido::getCantidadProductos)
                .max()
                .orElse(0);
    }
}
