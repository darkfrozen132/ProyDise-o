package morapack.dao.impl;

import morapack.dao.AeropuertoDAO;
import morapack.modelo.Aeropuerto;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementación en memoria del DAO de aeropuertos
 */
public class AeropuertoDAOImpl implements AeropuertoDAO {
    
    private final Map<String, Aeropuerto> aeropuertos = new HashMap<>();
    
    @Override
    public void crear(Aeropuerto aeropuerto) {
        if (aeropuerto.getId() == null) {
            throw new IllegalArgumentException("El ID del aeropuerto no puede ser null");
        }
        if (aeropuertos.containsKey(aeropuerto.getId())) {
            throw new IllegalArgumentException("Ya existe un aeropuerto con ID: " + aeropuerto.getId());
        }
        aeropuertos.put(aeropuerto.getId(), aeropuerto);
    }
    
    @Override
    public Optional<Aeropuerto> obtenerPorId(String id) {
        return Optional.ofNullable(aeropuertos.get(id));
    }
    
    @Override
    public List<Aeropuerto> obtenerTodos() {
        return new ArrayList<>(aeropuertos.values());
    }
    
    @Override
    public void actualizar(Aeropuerto aeropuerto) {
        if (!aeropuertos.containsKey(aeropuerto.getId())) {
            throw new IllegalArgumentException("No existe aeropuerto con ID: " + aeropuerto.getId());
        }
        aeropuertos.put(aeropuerto.getId(), aeropuerto);
    }
    
    @Override
    public void eliminar(String id) {
        if (!aeropuertos.containsKey(id)) {
            throw new IllegalArgumentException("No existe aeropuerto con ID: " + id);
        }
        aeropuertos.remove(id);
    }
    
    @Override
    public boolean existe(String id) {
        return aeropuertos.containsKey(id);
    }
    
    @Override
    public Optional<Aeropuerto> obtenerPorCodigo(String codigo) {
        return aeropuertos.values().stream()
                .filter(a -> codigo.equals(a.getCodigo()) || codigo.equals(a.getCodigoICAO()))
                .findFirst();
    }
    
    @Override
    public List<Aeropuerto> obtenerPorContinente(String continente) {
        return aeropuertos.values().stream()
                .filter(a -> continente.equals(a.getContinente()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Aeropuerto> obtenerPorPais(String pais) {
        return aeropuertos.values().stream()
                .filter(a -> pais.equals(a.getPais()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Aeropuerto> obtenerSedes() {
        return aeropuertos.values().stream()
                .filter(Aeropuerto::esSede)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Aeropuerto> obtenerDestinos() {
        return aeropuertos.values().stream()
                .filter(a -> !a.esSede())
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Aeropuerto> obtenerCercanos(double latitud, double longitud, double radioKm) {
        // Como ahora usamos GMT, buscar aeropuertos en el mismo huso horario o cercano
        int gmtBuscado = (int) Math.round(longitud / 15.0); // Aproximación de longitud a GMT
        return aeropuertos.values().stream()
                .filter(a -> Math.abs(a.getHusoHorario() - gmtBuscado) <= 2) // ±2 horas de diferencia
                .collect(Collectors.toList());
    }
    
    @Override
    public long contarTotal() {
        return aeropuertos.size();
    }
    
    @Override
    public long contarSedes() {
        return aeropuertos.values().stream()
                .filter(Aeropuerto::esSede)
                .count();
    }
    
    @Override
    public long contarDestinos() {
        return aeropuertos.values().stream()
                .filter(a -> !a.esSede())
                .count();
    }
    
    /**
     * Calcula la distancia entre dos puntos usando la fórmula de Haversine
     */
    private double calcularDistanciaHaversine(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371; // Radio de la Tierra en km
        
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(deltaLat/2) * Math.sin(deltaLat/2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLon/2) * Math.sin(deltaLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        
        return R * c;
    }
}
