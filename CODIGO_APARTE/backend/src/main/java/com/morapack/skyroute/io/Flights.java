package com.morapack.skyroute.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.morapack.skyroute.models.*;

public class Flights {
    private final Airports airportsIndex;
    private final List<Flight> all = new ArrayList<>();
    private final Map<Airport, List<Flight>> byOrigin = new HashMap<>();
    private final Map<String, Flight> byId = new HashMap<>();
    private final FlightSchedule schedule = new FlightSchedule();

    private Flights(Airports airports) {
        if (airports == null) {
            throw new IllegalArgumentException("Airports index is required");
        }
        this.airportsIndex = airports;
    }

    public List<Flight> getAll() {
        return Collections.unmodifiableList(all);
    }

    public int size() {
        return all.size();
    }

    public List<Flight> getByOrigin(Airport origin) {
        if (origin == null) {
            return Collections.emptyList();
        }
        List<Flight> flights = byOrigin.get(origin);
        return flights == null ? Collections.emptyList() : Collections.unmodifiableList(flights);
    }

    public List<Flight> getByOriginCode(String originCode) {
        if (originCode == null) {
            return Collections.emptyList();
        }
        return getByOrigin(airportsIndex.get(originCode));
    }

    public Flight getById(String flightId) {
        return byId.get(flightId);
    }

    public FlightSchedule getSchedule() {
        return schedule;
    }

    public static Flights load(Path path, Airports airports) throws IOException {
        Flights flights = new Flights(airports);
        for (String rawLine : Files.readAllLines(path)) {
            String line = rawLine.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] parts = line.split("-");
            if (parts.length != 5) {
                continue;
            }

            String originCode = parts[0];
            String destCode = parts[1];
            try {
                LocalTime departure = LocalTime.parse(parts[2]);
                LocalTime arrival = LocalTime.parse(parts[3]);
                int capacity = Integer.parseInt(parts[4]);

                Airport origin = airports.get(originCode);
                Airport destination = airports.get(destCode);
                if (origin == null || destination == null) {
                    continue;
                }

                String id = Flight.buildId(origin, destination, departure);
                Flight flight = new Flight(id, origin, destination, departure, arrival, capacity, Set.of());
                flights.register(flight);
            } catch (DateTimeParseException | NumberFormatException ignored) {
                // Ignorar líneas inválidas; se puede registrar si se necesita depuración
            }
        }
        return flights;
    }

    public static Flights fromEntities(List<Flight> entities,
                                       Airports airports,
                                       Map<String, Set<LocalDate>> cancellations) {
        Flights flights = new Flights(airports);
        if (entities == null) {
            return flights;
        }
        for (Flight entity : entities) {
            if (entity == null) {
                continue;
            }
            Airport origin = airports.get(entity.getOriginCode());
            Airport destination = airports.get(entity.getDestinationCode());
            if (origin == null || destination == null) {
                continue;
            }
            Set<LocalDate> cancelledDates = cancellations == null
                    ? Set.of()
                    : cancellations.getOrDefault(entity.getId(), Set.of());

            Flight flightCopy = new Flight(
                    entity.getId(),
                    origin,
                    destination,
                    entity.getDepLocal(),
                    entity.getArrLocal(),
                    entity.getDailyCapacity(),
                    cancelledDates
            );
            flights.register(flightCopy);
        }
        return flights;
    }

    public static Flights fromEntities(List<Flight> entities, Airports airports) {
        return fromEntities(entities, airports, Map.of());
    }

    private void register(Flight flight) {
        all.add(flight);
        byOrigin.computeIfAbsent(flight.getOrigin(), key -> new ArrayList<>()).add(flight);
        byId.put(flight.getId(), flight);
    }
}
