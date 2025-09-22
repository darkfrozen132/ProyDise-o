package morapack.dao;

import morapack.modelo.Aeropuerto;
import java.util.List;
import java.util.Optional;

/**
 * Interfaz DAO para la gestión de aeropuertos
 */
public interface AeropuertoDAO {
    
    // Operaciones CRUD básicas
    void crear(Aeropuerto aeropuerto);
    Optional<Aeropuerto> obtenerPorId(String id);
    List<Aeropuerto> obtenerTodos();
    void actualizar(Aeropuerto aeropuerto);
    void eliminar(String id);
    boolean existe(String id);
    
    // Consultas específicas
    Optional<Aeropuerto> obtenerPorCodigo(String codigo);
    List<Aeropuerto> obtenerPorContinente(String continente);
    List<Aeropuerto> obtenerPorPais(String pais);
    List<Aeropuerto> obtenerSedes(); // Solo aeropuertos que son sedes
    List<Aeropuerto> obtenerDestinos(); // Solo aeropuertos destino
    
    // Operaciones geográficas
    List<Aeropuerto> obtenerCercanos(double latitud, double longitud, double radioKm);
    
    // Estadísticas
    long contarTotal();
    long contarSedes();
    long contarDestinos();
}
