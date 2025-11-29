package com.morapack.skyroute.plan.service;

import com.morapack.skyroute.algorithm.Individual;
import com.morapack.skyroute.base.repository.FlightRepository;
import com.morapack.skyroute.models.CurrentPlan;
import com.morapack.skyroute.models.Route;
import com.morapack.skyroute.models.RouteSegment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class Mapper {

    private final FlightRepository flightRepository;

    public Mapper(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }

    public CurrentPlan toEntity(Individual individual) {
        if (individual == null) {
            throw new IllegalArgumentException("Individual is required to build CurrentPlan.");
        }
        CurrentPlan plan = new CurrentPlan();
        plan.setGeneratedAt(LocalDateTime.now());
        plan.setFitness(individual.getFitness());

        List<com.morapack.skyroute.models.OrderPlan> mappedPlans = individual.getPlans().stream()
                .map(original -> mapOrderPlan(original, plan))
                .collect(Collectors.toCollection(ArrayList::new));
        plan.setOrderPlans(mappedPlans);
        return plan;
    }

    private com.morapack.skyroute.models.OrderPlan mapOrderPlan(
            com.morapack.skyroute.models.OrderPlan original,
            CurrentPlan parentPlan) {
        com.morapack.skyroute.models.OrderPlan copy = new com.morapack.skyroute.models.OrderPlan();
        copy.setOrderId(original.getOrderId());
        copy.setSlack(original.getSlack());
        copy.setPlan(parentPlan);

        List<Route> routes = original.getRoutes() == null
                ? new ArrayList<>()
                : original.getRoutes().stream()
                         .map(this::mapRoute)
                         .collect(Collectors.toCollection(ArrayList::new));
        copy.setRoutes(routes);
        return copy;
    }

    private Route mapRoute(com.morapack.skyroute.models.Route original) {
        Route copy = new Route();
        copy.setQuantity(original.getQuantity());
        copy.setSlack(original.getSlack());

        List<RouteSegment> segments = original.getSegments() == null
                ? new ArrayList<>()
                : original.getSegments().stream()
                        .map(this::mapRouteSegment)
                        .collect(Collectors.toCollection(ArrayList::new));
        copy.setSegments(segments);
        return copy;
    }

    private RouteSegment mapRouteSegment(com.morapack.skyroute.models.RouteSegment original) {
        RouteSegment copy = new RouteSegment();
        String flightId = original.getFlight().getId();
        var persistedFlight = flightRepository.findById(flightId)
                .orElseThrow(() -> new IllegalStateException("Flight not found for segment: " + flightId));

        copy.setFlight(persistedFlight);
        copy.setDate(original.getDate());
        copy.setRouteQuantity(original.getRouteQuantity());
        copy.setDeparted(original.isDeparted());
        copy.setArrived(original.isArrived());
        copy.setReceivedByNext(original.isReceivedByNext());
        copy.setFinalLeg(original.isFinalLeg());
        copy.setSlack(original.getSlack());
        return copy;
    }
}
