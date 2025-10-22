package com.proyecto.backend.algoritmo.core;

import com.proyecto.backend.model.Aeropuerto;
import com.proyecto.backend.model.PlanDeVuelo;
import lombok.Getter;

import java.util.*;

/**
 * Representa el "mundo" del algoritmo: aeropuertos, vuelos y sus relaciones
 * Esta clase actua como cache en memoria para optimizar el procesamiento
 * Usa directamente las entidades JPA existentes
 */
@Getter
public class World {

    // Mapa de aeropuertos por codigo ICAO
    private final Map<String, Aeropuerto> aeropuertos;

    // Lista de planes de vuelo (templates diarios)
    private final List<PlanDeVuelo> planesVuelo;

    // Indice: vuelos que salen de cada aeropuerto
    private final Map<String, List<PlanDeVuelo>> vuelosPorOrigen;

    // Lista de hubs (sedes principales con stock ilimitado)
    private final List<String> hubs;

    // Conjunto de sedes principales
    private static final Set<String> SEDES_PRINCIPALES = Set.of("SPIM", "EBCI", "UBBB");

    /**
     * Constructor del mundo
     *
     * @param aeropuertos Lista de aeropuertos desde la BD
     * @param planesVuelo Lista de planes de vuelo desde la BD
     */
    public World(List<Aeropuerto> aeropuertos, List<PlanDeVuelo> planesVuelo) {
        // Construir mapa de aeropuertos
        Map<String, Aeropuerto> mapAeropuertos = new HashMap<>();
        for (Aeropuerto aeropuerto : aeropuertos) {
            mapAeropuertos.put(aeropuerto.getCodigoICAO(), aeropuerto);
        }
        this.aeropuertos = Collections.unmodifiableMap(mapAeropuertos);

        // Guardar planes de vuelo
        this.planesVuelo = Collections.unmodifiableList(new ArrayList<>(planesVuelo));

        // Construir indice de vuelos por origen
        Map<String, List<PlanDeVuelo>> indicePorOrigen = new HashMap<>();
        for (PlanDeVuelo vuelo : planesVuelo) {
            indicePorOrigen
                    .computeIfAbsent(vuelo.getAeropuertoOrigen(), k -> new ArrayList<>())
                    .add(vuelo);
        }

        // Ordenar vuelos por hora de salida para cada origen
        for (List<PlanDeVuelo> vuelos : indicePorOrigen.values()) {
            vuelos.sort(Comparator.comparing(PlanDeVuelo::getHoraSalida));
        }

        // Hacer inmutable
        Map<String, List<PlanDeVuelo>> indiceInmutable = new HashMap<>();
        for (Map.Entry<String, List<PlanDeVuelo>> entry : indicePorOrigen.entrySet()) {
            indiceInmutable.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
        }
        this.vuelosPorOrigen = Collections.unmodifiableMap(indiceInmutable);

        // Identificar hubs
        List<String> hubsList = new ArrayList<>();
        for (Aeropuerto aeropuerto : aeropuertos) {
            if (aeropuerto.esSedePrincipal()) {
                hubsList.add(aeropuerto.getCodigoICAO());
            }
        }
        this.hubs = Collections.unmodifiableList(hubsList);
    }

    /**
     * Obtiene un aeropuerto por codigo ICAO
     *
     * @param codigoICAO Codigo del aeropuerto
     * @return Aeropuerto o null si no existe
     */
    public Aeropuerto getAeropuerto(String codigoICAO) {
        return aeropuertos.get(codigoICAO);
    }

    /**
     * Obtiene los vuelos que salen de un aeropuerto
     *
     * @param codigoICAO Codigo del aeropuerto origen
     * @return Lista de planes de vuelo (vacia si no hay)
     */
    public List<PlanDeVuelo> getVuelosDesde(String codigoICAO) {
        return vuelosPorOrigen.getOrDefault(codigoICAO, Collections.emptyList());
    }

    /**
     * Verifica si un aeropuerto es sede principal
     *
     * @param codigoICAO Codigo del aeropuerto
     * @return true si es sede principal
     */
    public boolean esSedePrincipal(String codigoICAO) {
        return SEDES_PRINCIPALES.contains(codigoICAO);
    }

    /**
     * Verifica si dos aeropuertos estan en el mismo continente
     *
     * @param codigo1 Primer aeropuerto
     * @param codigo2 Segundo aeropuerto
     * @return true si estan en el mismo continente
     */
    public boolean mismoContinente(String codigo1, String codigo2) {
        Aeropuerto a1 = aeropuertos.get(codigo1);
        Aeropuerto a2 = aeropuertos.get(codigo2);

        if (a1 == null || a2 == null) {
            return false;
        }

        return a1.esMismoContinente(a2);
    }

    /**
     * Calcula la distancia entre dos aeropuertos usando Haversine
     *
     * @param codigo1 Primer aeropuerto
     * @param codigo2 Segundo aeropuerto
     * @return Distancia en km
     */
    public double calcularDistanciaKm(String codigo1, String codigo2) {
        Aeropuerto a1 = aeropuertos.get(codigo1);
        Aeropuerto a2 = aeropuertos.get(codigo2);

        if (a1 == null || a2 == null) {
            return Double.MAX_VALUE;
        }

        return calcularHaversine(
            a1.getLatitud(), a1.getLongitud(),
            a2.getLatitud(), a2.getLongitud()
        );
    }

    /**
     * Calcula la distancia Haversine entre dos puntos geograficos
     *
     * @param lat1 Latitud del primer punto
     * @param lon1 Longitud del primer punto
     * @param lat2 Latitud del segundo punto
     * @param lon2 Longitud del segundo punto
     * @return Distancia en kilometros
     */
    private double calcularHaversine(double lat1, double lon1, double lat2, double lon2) {
        final double RADIO_TIERRA_KM = 6371.0;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return RADIO_TIERRA_KM * c;
    }

    /**
     * Informacion resumida del mundo
     *
     * @return String con estadisticas
     */
    public String getEstadisticas() {
        return String.format("World: %d aeropuertos, %d planes de vuelo, %d hubs",
                aeropuertos.size(), planesVuelo.size(), hubs.size());
    }
}
