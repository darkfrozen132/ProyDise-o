package com.morapack.skyroute.algorithm;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import com.morapack.skyroute.config.*;
import com.morapack.skyroute.io.*;
import com.morapack.skyroute.models.*;

class RouteBuilder {
    private static final List<String> PRODUCTION_HUBS = List.of("SPIM", "EBCI", "UBBB");
    private static final int MAX_HOPS = 8;
    private static final int MAX_DAY_LOOKAHEAD = 7;

    private final Flights flights;
    private final Airports airports;
    private final FlightSchedule flightSchedule;
    private final AirportSchedule airportSchedule;
    private final Map<String, List<String>> reverseGraph;
    private final Random rnd;
    private final SelectionMode mode;

    RouteBuilder(World world,
                 FlightSchedule flightSchedule,
                 AirportSchedule airportSchedule,
                 Random rnd,
                 SelectionMode mode) {
        this.flights = Objects.requireNonNull(world, "world").getFlights();
        this.airports = world.getAirports();
        this.flightSchedule = Objects.requireNonNull(flightSchedule, "flightSchedule");
        this.airportSchedule = Objects.requireNonNull(airportSchedule, "airportSchedule");
        this.reverseGraph = buildReverseGraph();
        this.rnd = Objects.requireNonNull(rnd, "rnd");
        this.mode = Objects.requireNonNull(mode, "mode");
    }

    Route buildRoute(Order order, String originHub, int quantity) {
        String destination = order.getDestinationCode();
        Route route = new Route(quantity);
        if (originHub.equals(destination)) {
            return route;
        }

        Airport currentAirport = airports.get(originHub);
        if (currentAirport == null) {
            return null;
        }

        Duration overallSla = Duration.ZERO;
        Instant dueInstant = null;
        LocalDateTime readyTime = LocalDateTime.ofInstant(order.getCreationUtc(), currentAirport.getZoneOffset())
                .plus(Config.WAREHOUSE_DWELL);
        Map<String, Integer> distances = computeHopDistances(destination);

        String current = originHub;
        Set<String> visited = new HashSet<>();
        visited.add(current);
        int hops = 0;

        int currentRouteQty = quantity;

        while (!current.equals(destination) && hops < MAX_HOPS) {
            if (dueInstant == null) {
                Duration hopSla = slaFor(originHub, destination);
                overallSla = overallSla.compareTo(hopSla) > 0 ? overallSla : hopSla;
                dueInstant = order.getCreationUtc().plus(overallSla);
            }

            List<Flight> options = new ArrayList<>(flights.getByOriginCode(current));
            if (options.isEmpty()) {
                releaseAllocated(route);
                return null;
            }

            int currentDist = distances.getOrDefault(current, Integer.MAX_VALUE);
            List<Flight> candidates = rankCandidates(options, currentDist, destination, distances, visited);
            boolean reserved = false;

            for (Flight candidate : candidates) {
                LocalDate date = readyTime.toLocalDate();
                int attempts = 0;
                while (attempts < MAX_DAY_LOOKAHEAD) {
                    LocalDateTime departureLocal = toLocal(candidate.getDepartureInstant(date), currentAirport.getZoneOffset());
                    if (departureLocal.isBefore(readyTime)) {
                        date = date.plusDays(1);
                        attempts++;
                        continue;
                    }

                    int availableFlight = flightSchedule.getRemainingCapacity(candidate, date);
                    if (availableFlight <= 0) {
                        date = date.plusDays(1);
                        attempts++;
                        continue;
                    }

                    int sendQty = Math.min(currentRouteQty, availableFlight);
                    if (sendQty <= 0) {
                        date = date.plusDays(1);
                        attempts++;
                        continue;
                    }

                    boolean finalLeg = candidate.getDestinationCode().equals(destination);
                    if (reserveSegment(route, candidate, date, sendQty, finalLeg, dueInstant)) {
                        current = candidate.getDestinationCode();
                        currentAirport = airports.get(current);
                        LocalDateTime arrivalLocal = toLocal(candidate.getArrivalInstant(date), currentAirport.getZoneOffset());
                        readyTime = arrivalLocal.plus(finalLeg ? Config.WAREHOUSE_DWELL : Config.TRANSFER_BUFFER);
                        visited.add(current);
                        currentRouteQty = sendQty;
                        reserved = true;
                        break;
                    }

                    date = date.plusDays(1);
                    attempts++;
                }
                if (reserved) {
                    break;
                }
            }

            if (!reserved) {
                releaseAllocated(route);
                return null;
            }

            hops++;
        }

        if (!current.equals(destination)) {
            releaseAllocated(route);
            return null;
        }

        route.setQuantity(currentRouteQty);

        return route;
    }

    List<String> productionHubs() {
        return Collections.unmodifiableList(PRODUCTION_HUBS);
    }

    private Map<String, Integer> computeHopDistances(String destination) {
        Map<String, Integer> distances = new HashMap<>();
        Deque<String> queue = new ArrayDeque<>();
        distances.put(destination, 0);
        queue.add(destination);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            int base = distances.get(current);
            for (String origin : reverseGraph.getOrDefault(current, List.of())) {
                if (!distances.containsKey(origin)) {
                    distances.put(origin, base + 1);
                    queue.add(origin);
                }
            }
        }
        return distances;
    }

    private Map<String, List<String>> buildReverseGraph() {
        Map<String, List<String>> reverse = new HashMap<>();
        for (Flight flight : flights.getAll()) {
            reverse.computeIfAbsent(flight.getDestinationCode(), key -> new ArrayList<>())
                    .add(flight.getOriginCode());
        }
        return reverse;
    }

    private List<Flight> rankCandidates(List<Flight> options,
                                        int currentDist,
                                        String destination,
                                        Map<String, Integer> distances,
                                        Set<String> visited) {
        if (mode == SelectionMode.RANDOM_APPROACH) {
            List<Flight> prioritized = new ArrayList<>();
            for (Flight option : options) {
                int nextDist = distances.getOrDefault(option.getDestinationCode(), Integer.MAX_VALUE);
                if (nextDist < currentDist || option.getDestinationCode().equals(destination)) {
                    prioritized.add(option);
                }
            }

            if (prioritized.isEmpty()) {
                for (Flight option : options) {
                    if (!visited.contains(option.getDestinationCode())) {
                        prioritized.add(option);
                    }
                }
            }

            if (prioritized.isEmpty()) {
                prioritized.addAll(options);
            }

            Collections.shuffle(prioritized, rnd);
            return prioritized;
        }

        List<Flight> prioritized = new ArrayList<>(options);
        prioritized.removeIf(f -> visited.contains(f.getDestinationCode()));
        if (prioritized.isEmpty()) prioritized.addAll(options);

        prioritized.sort((a, b) -> Double.compare(
                slackScore(a, destination, distances),
                slackScore(b, destination, distances)));
        return prioritized;
    }

    private double slackScore(Flight flight,
                              String destination,
                              Map<String, Integer> distances) {
        int nextDist = distances.getOrDefault(flight.getDestinationCode(), Integer.MAX_VALUE);
        int directBonus = flight.getDestinationCode().equals(destination) ? -10 : 0;
        double heuristic = heuristic(flight.getDestinationCode(), destination);
        return nextDist * 10 + heuristic + directBonus;
    }

    private double heuristic(String airportCode, String destinationCode) {
        Airport target = airports.get(destinationCode);
        Airport candidate = airports.get(airportCode);
        if (target == null || candidate == null) {
            return Double.MAX_VALUE;
        }
        int diffOffset = Math.abs(target.getZoneOffset().getTotalSeconds() - candidate.getZoneOffset().getTotalSeconds());
        return diffOffset;
    }

    private boolean reserveSegment(Route route, Flight flight, LocalDate date, int quantity, boolean finalLeg, Instant dueInstant) {
        if (!flightSchedule.tryReserve(flight, date, quantity)) {
            return false;
        }

        Airport destinationAirport = airports.get(flight.getDestinationCode());
        LocalDateTime arrivalLocal = toLocal(flight.getArrivalInstant(date), destinationAirport.getZoneOffset());
        LocalDateTime departureLocal = arrivalLocal.plus(finalLeg ? Config.WAREHOUSE_DWELL : Config.TRANSFER_BUFFER);

        if (!airportSchedule.tryReserveTransit(destinationAirport.code, arrivalLocal, departureLocal, quantity)) {
            flightSchedule.release(flight, date, quantity);
            return false;
        }

        RouteSegment segment = new RouteSegment(flight, date, quantity, finalLeg);
        if (finalLeg) {
            Instant arrivalInstant = flight.getArrivalInstant(date);
            Duration slack = Duration.between(arrivalInstant, dueInstant);
            segment.setSlack(slack);
            route.setSlack(slack);
        }
        route.add(segment);
        return true;
    }

    private void releaseAllocated(Route route) {
        for (RouteSegment segment : route.getSegments()) {
            Flight flight = segment.getFlight();
            LocalDate date = segment.getDate();
            int qty = segment.getRouteQuantity();
            flightSchedule.release(flight, date, qty);

            Airport destinationAirport = airports.get(flight.getDestinationCode());
            LocalDateTime arrivalLocal = toLocal(flight.getArrivalInstant(date), destinationAirport.getZoneOffset());
            LocalDateTime departureLocal = arrivalLocal.plus(segment.isFinalLeg() ? Config.WAREHOUSE_DWELL : Config.TRANSFER_BUFFER);
            airportSchedule.releaseTransit(destinationAirport.code, arrivalLocal, departureLocal, qty);
        }
    }

    private LocalDateTime toLocal(Instant instant, ZoneOffset offset) {
        return LocalDateTime.ofInstant(instant, offset);
    }

    private Duration slaFor(String originCode, String destinationCode) {
        Airport origin = airports.get(originCode);
        Airport destination = airports.get(destinationCode);
        if (origin == null || destination == null) {
            return Duration.ofHours(Config.INTERCONTINENTAL_SLA_HOURS);
        }
        String originContinent = origin.getContinent();
        String destinationContinent = destination.getContinent();
        if (originContinent != null && originContinent.equalsIgnoreCase(destinationContinent)) {
            return Duration.ofHours(Config.CONTINENTAL_SLA_HOURS);
        }
        return Duration.ofHours(Config.INTERCONTINENTAL_SLA_HOURS);
    }

    enum SelectionMode {
        RANDOM_APPROACH,
        HEURISTIC_APPROACH
    }
}