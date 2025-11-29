package com.morapack.skyroute.algorithm;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.morapack.skyroute.config.*;
import com.morapack.skyroute.models.*;

public class Individual {
    private final List<OrderPlan> plans;
    private final FlightSchedule flightSchedule;
    private final AirportSchedule airportSchedule;
    private double fitness;

    private Individual(List<OrderPlan> plans, FlightSchedule flightSchedule, AirportSchedule airportSchedule) {
        this.plans = plans;
        this.flightSchedule = flightSchedule;
        this.airportSchedule = airportSchedule;
    }

    static Individual randomIndividual(World world, List<Order> orders, Random rnd) {
        FlightSchedule flightSchedule = world.getFlights().getSchedule().copy();
        AirportSchedule airportSchedule = world.getAirportSchedule().copy();
        RouteBuilder builder = new RouteBuilder(world, flightSchedule, airportSchedule, rnd, RouteBuilder.SelectionMode.RANDOM_APPROACH);

        List<OrderPlan> plans = new ArrayList<>();
        for (Order order : orders) {
            OrderPlan plan = buildPlanForOrder(order, builder, world, rnd);
            plans.add(plan);
        }

        Individual individual = new Individual(plans, flightSchedule, airportSchedule);
        individual.evaluate();
        return individual;
    }

    static Individual crossover(World world, List<Order> orders, Individual parentA, Individual parentB, Random rnd) {
        FlightSchedule flightSchedule = world.getFlights().getSchedule().copy();
        AirportSchedule airportSchedule = world.getAirportSchedule().copy();
        RouteBuilder builder = new RouteBuilder(world, flightSchedule, airportSchedule, rnd, RouteBuilder.SelectionMode.HEURISTIC_APPROACH);

        List<OrderPlan> plans = new ArrayList<>();
        for (Order order : orders) {
            OrderPlan childPlan = new OrderPlan(order.getId());
            int remaining = order.getQuantity();

            List<Route> parentRoutes = gatherParentRoutes(parentA, parentB, order.getId(), rnd);
            for (Route parentRoute : parentRoutes) {
                if (remaining <= 0) break;
                String origin = extractOrigin(parentRoute);
                if (origin == null || origin.isBlank()) {
                    continue;
                }
                Route rebuilt = builder.buildRoute(order, origin, remaining);
                if (rebuilt != null && rebuilt.getQuantity() > 0) {
                    childPlan.addRoute(rebuilt);
                    remaining -= rebuilt.getQuantity();
                }
        }

        while (remaining > 0) {
                List<String> hubs = builder.productionHubs();
                String origin = hubs.get(rnd.nextInt(hubs.size()));
                Route fallback = builder.buildRoute(order, origin, remaining);
                if (fallback == null || fallback.getQuantity() <= 0) {
                    throw new IllegalStateException("Unable to rebuild route for order " + order.getId());
                }
                childPlan.addRoute(fallback);
                remaining -= fallback.getQuantity();
            }

            childPlan.setSlack(determinePlanSlack(world, order, childPlan));
            plans.add(childPlan);
        }

        Individual child = new Individual(plans, flightSchedule, airportSchedule);
        child.evaluate();
        return child;
    }

    static Individual mutate(World world, List<Order> orders, Individual parent, Random rnd) {
        FlightSchedule flightSchedule = world.getFlights().getSchedule().copy();
        AirportSchedule airportSchedule = world.getAirportSchedule().copy();
        RouteBuilder builder = new RouteBuilder(world, flightSchedule, airportSchedule, rnd, RouteBuilder.SelectionMode.RANDOM_APPROACH);

        List<OrderPlan> plans = new ArrayList<>();
        for (Order order : orders) {
            OrderPlan mutatedPlan = new OrderPlan(order.getId());
            int remaining = order.getQuantity();

            List<String> preferredHubs = new ArrayList<>();
            OrderPlan parentPlan = findPlan(parent, order.getId());
            if (parentPlan != null) {
                for (Route route : parentPlan.getRoutes()) {
                    String origin = extractOrigin(route);
                    if (origin != null && !origin.isBlank()) {
                        preferredHubs.add(origin);
                    }
                }
            }
            Collections.shuffle(preferredHubs, rnd);

            int preferredIndex = 0;
            while (remaining > 0) {
                boolean built = false;
                while (preferredIndex < preferredHubs.size() && remaining > 0) {
                    String origin = preferredHubs.get(preferredIndex++);
                    Route rebuilt = builder.buildRoute(order, origin, remaining);
                    if (rebuilt != null && rebuilt.getQuantity() > 0) {
                        mutatedPlan.addRoute(rebuilt);
                        remaining -= rebuilt.getQuantity();
                        built = true;
                        break;
                    }
                }

                if (built) continue;

                List<String> hubs = new ArrayList<>(builder.productionHubs());
                Collections.shuffle(hubs, rnd);
                for (String origin : hubs) {
                    Route fallback = builder.buildRoute(order, origin, remaining);
                    if (fallback != null && fallback.getQuantity() > 0) {
                        mutatedPlan.addRoute(fallback);
                        remaining -= fallback.getQuantity();
                        built = true;
                        break;
                    }
                }

                if (!built) {
                    throw new IllegalStateException("Unable to mutate route for order " + order.getId());
                }
            }

            mutatedPlan.setSlack(determinePlanSlack(world, order, mutatedPlan));
            plans.add(mutatedPlan);
        }

        Individual mutant = new Individual(plans, flightSchedule, airportSchedule);
        mutant.evaluate();
        return mutant;
    }

    public Individual copy() {
        List<OrderPlan> planCopies = deepCopyPlans(this.plans);
        Individual clone = new Individual(planCopies, flightSchedule.copy(), airportSchedule.copy());
        clone.fitness = this.fitness;
        return clone;
    }

    public Individual tryInsertOrder(World world, Order newOrder, Random rnd) {
        FlightSchedule scheduleCopy = flightSchedule.copy();
        AirportSchedule airportCopy = airportSchedule.copy();
        RouteBuilder builder = new RouteBuilder(world, scheduleCopy, airportCopy, rnd, RouteBuilder.SelectionMode.HEURISTIC_APPROACH);

        try {
            OrderPlan newPlan = buildPlanForOrder(newOrder, builder, world, rnd);
            if (newPlan.getSlack() == null || newPlan.getSlack().isNegative()) {
                return null;
            }
            List<OrderPlan> planCopies = deepCopyPlans(this.plans);
            planCopies.add(newPlan);
            Individual patched = new Individual(planCopies, scheduleCopy, airportCopy);
            patched.evaluate();
            return patched;
        } catch (IllegalStateException ex) {
            return null;
        }
    }

    public void applyToWorld(World world) {
        world.getFlights().getSchedule().applyFrom(flightSchedule);
        world.getAirportSchedule().applyFrom(airportSchedule);
    }

    private static Duration determinePlanSlack(World world, Order order, OrderPlan plan) {
        if (plan.getRoutes().isEmpty()) {
            return Duration.ZERO;
        }

        Duration maxSla = Duration.ZERO;
        for (Route route : plan.getRoutes()) {
            String origin = extractOrigin(route);
            Duration routeSla = slaFor(world, origin, order.getDestinationCode());
            if (routeSla.compareTo(maxSla) > 0) {
                maxSla = routeSla;
            }
        }

        Duration best = null;
        for (Route route : plan.getRoutes()) {
            RouteSegment lastSegment = getLastSegment(route);
            if (lastSegment == null) {
                continue;
            }
            Duration routeSlack = computeSlack(world, order, lastSegment, maxSla);
            route.setSlack(routeSlack);
            lastSegment.setSlack(routeSlack);
            if (best == null || routeSlack.compareTo(best) < 0) {
                best = routeSlack;
            }
        }
        return best == null ? Duration.ZERO : best;
    }

    private void evaluate() {
        double total = 0;
        for (OrderPlan plan : plans) {
            total += plan.getSlack().toMinutes();
        }
        this.fitness = total;
    }

    Individual deepCopy() {
        List<OrderPlan> copyPlans = new ArrayList<>(plans);
        // schedules are already individual-specific; we don't reuse them when cloning for operators
        return new Individual(copyPlans, flightSchedule, airportSchedule);
    }

    public double getFitness() {
        return fitness;
    }

    public List<OrderPlan> getPlans() {
        return plans;
    }

    public FlightSchedule getFlightSchedule() {
        return flightSchedule;
    }

    public AirportSchedule getAirportSchedule() {
        return airportSchedule;
    }
    
    public Map<String, Instant> computeCompletionTimes(World world, List<Order> orders) {
        Map<String, Order> orderMap = new HashMap<>();
        for (Order order : orders) {
            orderMap.put(order.getId(), order);
        }

        Map<String, Instant> completion = new HashMap<>();
        for (OrderPlan plan : plans) {
            Order order = orderMap.get(plan.getOrderId());
            if (order == null) {
                continue;
            }
            Instant done = computeOrderCompletion(order, plan);
            completion.put(order.getId(), done);
        }
        return completion;
    }

    public void prettyPrint() {
        for (OrderPlan plan : plans) {
            System.out.println("  Order " + plan.getOrderId() + " qty=" + plan.plannedQuantity()
                    + " slack=" + plan.getSlack());
            int idx = 1;
            for (Route route : plan.getRoutes()) {
                System.out.println("    Route " + (idx++) + " qty=" + route.getQuantity());
                for (RouteSegment segment : route.getSegments()) {
                    System.out.println(String.format(
                            "      %s -> %s dep %s arr %s qty %d",
                            segment.getFlight().getOriginCode(),
                            segment.getFlight().getDestinationCode(),
                            segment.getExactDepDateTime(),
                            segment.getExactArrDateTime(),
                            segment.getRouteQuantity()));
                }
            }
        }
    }

    private static List<Route> gatherParentRoutes(Individual parentA, Individual parentB, String orderId, Random rnd) {
        List<Route> combined = new ArrayList<>();
        OrderPlan planA = findPlan(parentA, orderId);
        OrderPlan planB = findPlan(parentB, orderId);
        if (planA != null) combined.addAll(planA.getRoutes());
        if (planB != null) combined.addAll(planB.getRoutes());
        Collections.shuffle(combined, rnd);
        return combined;
    }

    private static OrderPlan findPlan(Individual individual, String orderId) {
        for (OrderPlan plan : individual.getPlans()) {
            if (plan.getOrderId().equals(orderId)) {
                return plan;
            }
        }
        return null;
    }

    private static String extractOrigin(Route route) {
        if (route.getSegments().isEmpty()) {
            return null;
        }
        return route.getSegments().get(0).getFlight().getOriginCode();
    }

    private static RouteSegment getLastSegment(Route route) {
        List<RouteSegment> segments = route.getSegments();
        if (segments.isEmpty()) {
            return null;
        }
        return segments.get(segments.size() - 1);
    }

    private static Duration computeSlack(World world, Order order, RouteSegment lastSegment, Duration sla) {
        Instant arrivalInstant = lastSegment.getFlight().getArrivalInstant(lastSegment.getDate());
        Instant completionInstant = arrivalInstant.plus(Config.WAREHOUSE_DWELL);
        Instant dueInstant = order.getCreationUtc().plus(sla);
        return Duration.between(completionInstant, dueInstant);
    }

    private static Instant computeOrderCompletion(Order order, OrderPlan plan) {
        Instant completion = null;
        for (Route route : plan.getRoutes()) {
            RouteSegment lastSegment = getLastSegment(route);
            if (lastSegment == null) continue;
            Instant arrival = lastSegment.getFlight().getArrivalInstant(lastSegment.getDate());
            Instant finalInstant = arrival.plus(Config.WAREHOUSE_DWELL);
            if (completion == null || finalInstant.isAfter(completion)) {
                completion = finalInstant;
            }
        }
        return completion == null ? order.getCreationUtc() : completion;
    }

    private static Duration slaFor(World world, String originCode, String destinationCode) {
        if (destinationCode == null) {
            return Duration.ZERO;
        }
        com.morapack.skyroute.io.Airports airports = world.getAirports();
        com.morapack.skyroute.models.Airport destination = airports.get(destinationCode);
        com.morapack.skyroute.models.Airport origin = originCode == null || originCode.isBlank() ? null : airports.get(originCode);

        if (destination == null) {
            return Duration.ofHours(Config.INTERCONTINENTAL_SLA_HOURS);
        }

        if (origin == null) {
            return Duration.ofHours(Config.INTERCONTINENTAL_SLA_HOURS);
        }

        String originContinent = origin.getContinent();
        String destinationContinent = destination.getContinent();
        if (originContinent != null && originContinent.equalsIgnoreCase(destinationContinent)) {
            return Duration.ofHours(Config.CONTINENTAL_SLA_HOURS);
        }
        return Duration.ofHours(Config.INTERCONTINENTAL_SLA_HOURS);
    }

    private static OrderPlan buildPlanForOrder(Order order,
                                               RouteBuilder builder,
                                               World world,
                                               Random rnd) {
        OrderPlan plan = new OrderPlan(order.getId());
        int remaining = order.getQuantity();
        int stagnation = 0;
        while (remaining > 0) {
            List<String> hubs = new ArrayList<>(builder.productionHubs());
            Collections.shuffle(hubs, rnd);
            boolean built = false;
            for (String origin : hubs) {
                Route route = builder.buildRoute(order, origin, remaining);
                if (route != null && route.getQuantity() > 0) {
                    plan.addRoute(route);
                    remaining -= route.getQuantity();
                    built = true;
                    break;
                }
            }
            if (!built) {
                stagnation++;
                if (stagnation > hubs.size()) {
                    throw new IllegalStateException("No feasible route for remaining quantity of order " + order.getId());
                }
            } else {
                stagnation = 0;
            }
        }
        plan.setSlack(determinePlanSlack(world, order, plan));
        if (plan.getSlack() == null || plan.getSlack().isNegative()) {
            throw new IllegalStateException("Plan violates SLA for order " + order.getId());
        }
        return plan;
    }

    private static List<OrderPlan> deepCopyPlans(List<OrderPlan> originals) {
        List<OrderPlan> copies = new ArrayList<>(originals.size());
        for (OrderPlan plan : originals) {
            OrderPlan copy = new OrderPlan(plan.getOrderId());
            copy.setSlack(plan.getSlack());
            List<Route> routeCopies = new ArrayList<>();
            for (Route route : plan.getRoutes()) {
                Route routeCopy = new Route(route.getQuantity());
                routeCopy.setSlack(route.getSlack());
                List<RouteSegment> segmentCopies = new ArrayList<>();
                for (RouteSegment segment : route.getSegments()) {
                    RouteSegment segCopy = new RouteSegment();
                    segCopy.setFlight(segment.getFlight());
                    segCopy.setDate(segment.getDate());
                    segCopy.setRouteQuantity(segment.getRouteQuantity());
                    segCopy.setFinalLeg(segment.isFinalLeg());
                    segCopy.setSlack(segment.getSlack());
                    segCopy.setDeparted(segment.isDeparted());
                    segCopy.setArrived(segment.isArrived());
                    segCopy.setReceivedByNext(segment.isReceivedByNext());
                    segmentCopies.add(segCopy);
                }
                routeCopy.setSegments(segmentCopies);
                routeCopies.add(routeCopy);
            }
            copy.setRoutes(routeCopies);
            copies.add(copy);
        }
        return copies;
    }

}
