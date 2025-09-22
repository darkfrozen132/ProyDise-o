package morapack.dao.impl;

import morapack.dao.SedeDAO;
import morapack.modelo.Sede;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementación en memoria del DAO de sedes
 */
public class SedeDAOImpl implements SedeDAO {
    
    private final Map<String, Sede> sedes = new HashMap<>();
    
    @Override
    public void crear(Sede sede) {
        if (sede.getId() == null) {
            throw new IllegalArgumentException("El ID de la sede no puede ser null");
        }
        if (sedes.containsKey(sede.getId())) {
            throw new IllegalArgumentException("Ya existe una sede con ID: " + sede.getId());
        }
        sedes.put(sede.getId(), sede);
    }
    
    @Override
    public Optional<Sede> obtenerPorId(String id) {
        return Optional.ofNullable(sedes.get(id));
    }
    
    @Override
    public List<Sede> obtenerTodos() {
        return new ArrayList<>(sedes.values());
    }
    
    @Override
    public void actualizar(Sede sede) {
        if (!sedes.containsKey(sede.getId())) {
            throw new IllegalArgumentException("No existe sede con ID: " + sede.getId());
        }
        sedes.put(sede.getId(), sede);
    }
    
    @Override
    public void eliminar(String id) {
        if (!sedes.containsKey(id)) {
            throw new IllegalArgumentException("No existe sede con ID: " + id);
        }
        sedes.remove(id);
    }
    
    @Override
    public boolean existe(String id) {
        return sedes.containsKey(id);
    }
    
    @Override
    public List<Sede> obtenerPorAeropuerto(String aeropuertoId) {
        return sedes.values().stream()
                .filter(s -> aeropuertoId.equals(s.getAeropuertoId()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Sede> obtenerPorTipo(String tipo) {
        return sedes.values().stream()
                .filter(s -> tipo.equals(s.getTipo()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Sede> obtenerPorEstado(String estado) {
        return sedes.values().stream()
                .filter(s -> estado.equals(s.getEstado()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Sede> obtenerActivas() {
        return sedes.values().stream()
                .filter(s -> s.estaActiva())
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Sede> obtenerDisponibles() {
        return sedes.values().stream()
                .filter(s -> s.estaActiva() && s.puedeAtender())
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Sede> obtenerConCapacidad(int minimaCapacidad) {
        return sedes.values().stream()
                .filter(s -> s.getCapacidadMaxima() >= minimaCapacidad)
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean tieneCapacidadDisponible(String sedeId) {
        Sede sede = sedes.get(sedeId);
        return sede != null && sede.puedeAtender();
    }
    
    @Override
    public int obtenerCapacidadDisponible(String sedeId) {
        Sede sede = sedes.get(sedeId);
        if (sede != null) {
            return (int) (sede.getCapacidadMaxima() - sede.getCantidadPedidosAsignados());
        }
        return 0;
    }
    
    @Override
    public long contarTotal() {
        return sedes.size();
    }
    
    @Override
    public long contarActivas() {
        return sedes.values().stream()
                .filter(Sede::estaActiva)
                .count();
    }
    
    @Override
    public long contarPorTipo(String tipo) {
        return sedes.values().stream()
                .filter(s -> tipo.equals(s.getTipo()))
                .count();
    }
    
    @Override
    public double calcularCapacidadTotal() {
        return sedes.values().stream()
                .mapToDouble(Sede::getCapacidadMaxima)
                .sum();
    }
    
    @Override
    public double calcularCapacidadDisponible() {
        return sedes.values().stream()
                .filter(Sede::estaActiva)
                .mapToDouble(s -> s.getCapacidadMaxima() - s.getCantidadPedidosAsignados())
                .sum();
    }
}
