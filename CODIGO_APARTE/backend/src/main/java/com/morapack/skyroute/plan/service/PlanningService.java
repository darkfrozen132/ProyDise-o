package com.morapack.skyroute.plan.service;

import com.morapack.skyroute.algorithm.GeneticAlgorithm;
import com.morapack.skyroute.algorithm.Individual;
import com.morapack.skyroute.config.Config;
import com.morapack.skyroute.models.CurrentPlan;
import com.morapack.skyroute.models.Flight;
import com.morapack.skyroute.models.FlightCapacity;
import com.morapack.skyroute.models.Route;
import com.morapack.skyroute.models.RouteSegment;
import com.morapack.skyroute.plan.repository.CurrentPlanRepository;
import com.morapack.skyroute.capacity.repository.FlightCapacityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PlanningService {

    private final WorldBuilder worldBuilder;
    private final Mapper mapper;
    private final CurrentPlanRepository planRepository;
    private final FlightCapacityRepository flightCapacityRepository;

    public PlanningService(WorldBuilder worldBuilder,
                           Mapper mapper,
                           CurrentPlanRepository planRepository,
                           FlightCapacityRepository flightCapacityRepository) {
        this.worldBuilder = worldBuilder;
        this.mapper = mapper;
        this.planRepository = planRepository;
        this.flightCapacityRepository = flightCapacityRepository;
    }

    @Transactional
    public CurrentPlan run() {
        WorldBuilder.Snapshot snapshot = worldBuilder.buildOperationalSnapshot();
        if (snapshot.demand().isEmpty()) {
            throw new IllegalStateException("No orders available to run the genetic algorithm.");
        }

        GeneticAlgorithm geneticAlgorithm = new GeneticAlgorithm(snapshot.world(), snapshot.demand());
        Individual best = geneticAlgorithm.run(Config.POP_SIZE, Config.MAX_GEN);
        CurrentPlan entity = mapper.toEntity(best);
        persistFlightCapacities(entity);
        planRepository.save(entity);
        return entity;
    }

    private void persistFlightCapacities(CurrentPlan plan) {
        var capacities = aggregateFlightCapacities(plan);
        flightCapacityRepository.deleteAll();
        flightCapacityRepository.saveAll(capacities);
    }

    private List<FlightCapacity> aggregateFlightCapacities(CurrentPlan plan) {
        Map<FlightCapacity.FlightCapacityKey, Integer> totals = new HashMap<>();
        Map<FlightCapacity.FlightCapacityKey, Flight> flights = new HashMap<>();

        if (plan.getOrderPlans() != null) {
            plan.getOrderPlans().forEach(orderPlan -> {
                if (orderPlan.getRoutes() == null) {
                    return;
                }
                for (Route route : orderPlan.getRoutes()) {
                    if (route.getSegments() == null) {
                        continue;
                    }
                    for (RouteSegment segment : route.getSegments()) {
                        Flight flight = segment.getFlight();
                        LocalDate date = segment.getDate();
                        int quantity = segment.getRouteQuantity();
                        if (flight == null || date == null || quantity <= 0) {
                            continue;
                        }
                        FlightCapacity.FlightCapacityKey key = new FlightCapacity.FlightCapacityKey(flight.getId(), date);
                        totals.merge(key, quantity, Integer::sum);
                        flights.putIfAbsent(key, flight);
                    }
                }
            });
        }

        List<FlightCapacity> capacities = new ArrayList<>(totals.size());
        totals.forEach((key, used) -> {
            Flight flight = flights.get(key);
            if (flight != null) {
                capacities.add(new FlightCapacity(flight, key.getDate(), used));
            }
        });
        return capacities;
    }

    @Transactional(readOnly = true)
    public Optional<CurrentPlan> getCurrentPlan() {
        return planRepository.findById(1L);
    }

    @Transactional
    public void clearCurrentPlan() {
        flightCapacityRepository.deleteAll();
        planRepository.deleteById(1L);
    }
}
