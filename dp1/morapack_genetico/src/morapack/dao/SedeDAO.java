package morapack.dao;

import morapack.modelo.Sede;
import java.util.List;
import java.util.Optional;

/**
 * Interfaz DAO para la gestión de sedes
 */
public interface SedeDAO {
    
    // Operaciones CRUD básicas
    void crear(Sede sede);
    Optional<Sede> obtenerPorId(String id);
    List<Sede> obtenerTodos();
    void actualizar(Sede sede);
    void eliminar(String id);
    boolean existe(String id);
    
    // Consultas específicas
    List<Sede> obtenerPorAeropuerto(String aeropuertoId);
    List<Sede> obtenerPorTipo(String tipo);
    List<Sede> obtenerPorEstado(String estado);
    List<Sede> obtenerActivas();
    List<Sede> obtenerDisponibles(); // Activas con capacidad disponible
    List<Sede> obtenerConCapacidad(int minimaCapacidad);
    
    // Operaciones de capacidad
    boolean tieneCapacidadDisponible(String sedeId);
    int obtenerCapacidadDisponible(String sedeId);
    
    // Estadísticas
    long contarTotal();
    long contarActivas();
    long contarPorTipo(String tipo);
    double calcularCapacidadTotal();
    double calcularCapacidadDisponible();
}
