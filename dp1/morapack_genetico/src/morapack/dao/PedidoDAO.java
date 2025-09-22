package morapack.dao;

import morapack.modelo.Pedido;
import java.util.List;
import java.util.Optional;

/**
 * Interfaz DAO para la gestión de pedidos
 */
public interface PedidoDAO {
    
    // Operaciones CRUD básicas
    void crear(Pedido pedido);
    Optional<Pedido> obtenerPorId(String id);
    List<Pedido> obtenerTodos();
    void actualizar(Pedido pedido);
    void eliminar(String id);
    boolean existe(String id);
    
    // Consultas específicas de negocio
    List<Pedido> obtenerPorEstado(String estado);
    List<Pedido> obtenerPendientes();
    List<Pedido> obtenerSinAsignar();
    List<Pedido> obtenerPorSede(String sedeId);
    List<Pedido> obtenerPorAeropuertoDestino(String aeropuertoId);
    List<Pedido> obtenerPorPrioridad(int prioridad);
    List<Pedido> obtenerAltaPrioridad(); // Prioridad 1
    List<Pedido> obtenerVencidos();
    List<Pedido> obtenerParaOptimizacion(); // Pendientes y factibles
    
    // Estadísticas
    long contarTotal();
    long contarPorEstado(String estado);
    double calcularCostoTotal();
    
    // Operaciones por lotes
    void crearVarios(List<Pedido> pedidos);
    void actualizarVarios(List<Pedido> pedidos);
    
    // Consultas basadas en el nuevo formato de ID
    List<Pedido> obtenerPorDia(int dia);
    List<Pedido> obtenerPorHora(int hora);
    List<Pedido> obtenerPorRangoHoras(int horaInicio, int horaFin);
    List<Pedido> obtenerPorCliente(String clienteId);
    List<Pedido> obtenerPorCantidadMinima(int cantidadMinima);
    List<Pedido> obtenerPorCantidadRango(int cantidadMin, int cantidadMax);
    
    // Estadísticas avanzadas con nuevo formato
    long contarPorDia(int dia);
    long contarPorHora(int hora);
    long contarPorCliente(String clienteId);
    double calcularPromedioProductosPorDia(int dia);
    int obtenerCantidadMaximaPorDia(int dia);
}
