package com.morapack.skyroute.models;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

public class AirportSchedule {
        private final Map<String, Integer> capacityByAirport;
    private final NavigableMap<Key, Integer> transitDeltas = new TreeMap<>();
    private final NavigableMap<Key, Integer> finalDeltas = new TreeMap<>();

    public AirportSchedule(Map<String, Airport> airports) {
        Map<String, Integer> capacities = new HashMap<>();
        for (Airport airport : airports.values()) {
            capacities.put(airport.code, airport.getStorageCapacity());
        }
        this.capacityByAirport = Collections.unmodifiableMap(capacities);
    }

    private AirportSchedule(Map<String, Integer> capacities,
                             NavigableMap<Key, Integer> transit,
                             NavigableMap<Key, Integer> finals) {
        this.capacityByAirport = capacities;
        this.transitDeltas.putAll(transit);
        this.finalDeltas.putAll(finals);
    }

    public synchronized boolean tryReserveTransit(String airportId, LocalDateTime start, LocalDateTime end, int qty) {
        if (qty <= 0) return true;
        Objects.requireNonNull(airportId, "airportId");
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");

        mergeDelta(transitDeltas, new Key(start, airportId), qty);
        mergeDelta(transitDeltas, new Key(end, airportId), -qty);
        if (violatesCapacity()) {
            mergeDelta(transitDeltas, new Key(start, airportId), -qty);
            mergeDelta(transitDeltas, new Key(end, airportId), +qty);
            cleanup(transitDeltas);
            return false;
        }
        cleanup(transitDeltas);
        return true;
    }

    public synchronized boolean tryReserveFinal(String airportId, LocalDateTime start, int qty) {
        if (qty <= 0) return true;
        Objects.requireNonNull(airportId, "airportId");
        Objects.requireNonNull(start, "start");

        mergeDelta(finalDeltas, new Key(start, airportId), qty);
        if (violatesCapacity()) {
            mergeDelta(finalDeltas, new Key(start, airportId), -qty);
            cleanup(finalDeltas);
            return false;
        }
        cleanup(finalDeltas);
        return true;
    }

    public synchronized void releaseTransit(String airportId, LocalDateTime start, LocalDateTime end, int qty) {
        if (qty <= 0) return;
        mergeDelta(transitDeltas, new Key(start, airportId), -qty);
        mergeDelta(transitDeltas, new Key(end, airportId), +qty);
        cleanup(transitDeltas);
    }

    public synchronized void releaseFinal(String airportId, LocalDateTime start, int qty) {
        if (qty <= 0) return;
        mergeDelta(finalDeltas, new Key(start, airportId), -qty);
        cleanup(finalDeltas);
    }

    public synchronized int getOccupied(String airportId, LocalDateTime instant) {
        Objects.requireNonNull(airportId, "airportId");
        Objects.requireNonNull(instant, "instant");
        int occ = 0;
        for (var entry : transitDeltas.entrySet()) {
            if (entry.getKey().airportId.equals(airportId)
                    && !entry.getKey().instant.isAfter(instant)) {
                occ += entry.getValue();
            }
        }
        for (var entry : finalDeltas.entrySet()) {
            if (entry.getKey().airportId.equals(airportId)
                    && !entry.getKey().instant.isAfter(instant)) {
                occ += entry.getValue();
            }
        }
        return Math.max(0, occ);
    }

    public synchronized int getAvailable(String airportId, LocalDateTime instant) {
        Integer capacity = capacityByAirport.get(airportId);
        if (capacity == null) return Integer.MAX_VALUE;
        return capacity - getOccupied(airportId, instant);
    }

    public synchronized void cleanupUntil(String airportId, LocalDateTime instant) {
        Objects.requireNonNull(airportId, "airportId");
        Objects.requireNonNull(instant, "instant");
        cleanupMapUntil(transitDeltas, airportId, instant);
        cleanupMapUntil(finalDeltas, airportId, instant);
    }

    public synchronized void purgeBefore(LocalDateTime instant) {
        Objects.requireNonNull(instant, "instant");
        transitDeltas.keySet().removeIf(key -> key.instant.isBefore(instant));
        finalDeltas.keySet().removeIf(key -> key.instant.isBefore(instant));
    }

    public AirportSchedule copy() {
        return new AirportSchedule(capacityByAirport, new TreeMap<>(transitDeltas), new TreeMap<>(finalDeltas));
    }

    public synchronized void applyFrom(AirportSchedule other) {
        transitDeltas.clear();
        finalDeltas.clear();
        transitDeltas.putAll(other.transitDeltas);
        finalDeltas.putAll(other.finalDeltas);
    }

    private void cleanupMapUntil(Map<Key, Integer> map, String airportId, LocalDateTime instant) {
        map.entrySet().removeIf(entry ->
                entry.getKey().airportId.equals(airportId)
                        && entry.getKey().instant.isBefore(instant)
                        && entry.getValue() == 0);
    }

    private void mergeDelta(Map<Key, Integer> map, Key key, int delta) {
        map.merge(key, delta, Integer::sum);
    }

    private void cleanup(Map<Key, Integer> map) {
        map.entrySet().removeIf(entry -> entry.getValue() == 0);
    }

    private boolean violatesCapacity() {
        Map<String, Integer> occupancy = new HashMap<>();
        NavigableMap<Key, Integer> combined = new TreeMap<>(transitDeltas);
        finalDeltas.forEach((key, value) -> combined.merge(key, value, Integer::sum));
        for (var entry : combined.entrySet()) {
            String airportId = entry.getKey().airportId;
            int updated = occupancy.getOrDefault(airportId, 0) + entry.getValue();
            int capacity = capacityByAirport.getOrDefault(airportId, Integer.MAX_VALUE);
            if (updated > capacity) {
                return true;
            }
            occupancy.put(airportId, updated);
        }
        return false;
    }

    private static final class Key implements Comparable<Key> {
        private final LocalDateTime instant;
        private final String airportId;

        private Key(LocalDateTime instant, String airportId) {
            this.instant = instant;
            this.airportId = airportId;
        }

        @Override
        public int compareTo(Key other) {
            int cmp = this.instant.compareTo(other.instant);
            if (cmp != 0) return cmp;
            return this.airportId.compareTo(other.airportId);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Key other)) return false;
            return instant.equals(other.instant) && airportId.equals(other.airportId);
        }

        @Override
        public int hashCode() {
            return instant.hashCode() * 31 + airportId.hashCode();
        }
    }
}
