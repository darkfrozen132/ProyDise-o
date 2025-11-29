package com.morapack.skyroute.plan.service;

import com.morapack.skyroute.base.repository.AirportRepository;
import com.morapack.skyroute.base.repository.FlightRepository;
import com.morapack.skyroute.capacity.repository.FlightCancellationRepository;
import com.morapack.skyroute.io.Airports;
import com.morapack.skyroute.io.Flights;
import com.morapack.skyroute.models.Airport;
import com.morapack.skyroute.models.AirportSchedule;
import com.morapack.skyroute.models.FlightCancellation;
import com.morapack.skyroute.models.Order;
import com.morapack.skyroute.models.OrderScope;
import com.morapack.skyroute.orders.repository.OrderRepository;
import com.morapack.skyroute.config.World;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class WorldBuilder {

    private final AirportRepository airportRepository;
    private final FlightRepository flightRepository;
    private final FlightCancellationRepository cancellationRepository;
    private final OrderRepository orderRepository;

    public WorldBuilder(AirportRepository airportRepository,
                        FlightRepository flightRepository,
                        FlightCancellationRepository cancellationRepository,
                        OrderRepository orderRepository) {
        this.airportRepository = airportRepository;
        this.flightRepository = flightRepository;
        this.cancellationRepository = cancellationRepository;
        this.orderRepository = orderRepository;
    }

    public record Snapshot(World world, List<Order> demand) {}

    @Transactional(readOnly = true)
    public Snapshot buildOperationalSnapshot() {
        Airports airports = Airports.fromEntities(airportRepository.findAll());
        Map<String, Set<LocalDate>> cancellationsByFlight = loadCancellations();

        Flights flights = Flights.fromEntities(flightRepository.findAll(), airports, cancellationsByFlight);
        AirportSchedule airportSchedule = new AirportSchedule(airports.asMap());
        World world = World.fromData(airports, flights, airportSchedule, Instant.now());

        List<Order> clonedOrders = orderRepository.findAllByScope(OrderScope.REAL).stream()
                .map(order -> cloneOrder(order, airports))
                .filter(Objects::nonNull)
                .toList();

        return new Snapshot(world, clonedOrders);
    }

    @Transactional(readOnly = true)
    public World buildBaseWorld() {
        return buildBaseWorld(Instant.now());
    }

    public World buildBaseWorld(Instant initialInstant) {
        Airports airports = Airports.fromEntities(airportRepository.findAll());
        Map<String, Set<LocalDate>> cancellationsByFlight = loadCancellations();
        Flights flights = Flights.fromEntities(flightRepository.findAll(), airports, cancellationsByFlight);
        AirportSchedule airportSchedule = new AirportSchedule(airports.asMap());
        Instant seed = initialInstant != null ? initialInstant : Instant.now();
        return World.fromData(airports, flights, airportSchedule, seed);
    }

    private static Order cloneOrder(Order source, Airports airports) {
        if (source == null) {
            return null;
        }
        Airport destination = airports.get(source.getDestinationCode());
        if (destination == null) {
            return null;
        }
        return new Order(
                source.getId(),
                source.getCustomerReference(),
                destination,
                source.getQuantity(),
                source.getCreationUtc(),
                source.getDueUtc(),
                source.getScope()
        );
    }

    private Map<String, Set<LocalDate>> loadCancellations() {
        return cancellationRepository.findAll()
                .stream()
                .collect(Collectors.groupingBy(
                        cancellation -> cancellation.getId().getFlightId(),
                        Collectors.mapping(FlightCancellation::getDate, Collectors.toSet())
                ));
    }
}
