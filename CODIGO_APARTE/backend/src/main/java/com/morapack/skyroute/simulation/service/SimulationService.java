package com.morapack.skyroute.simulation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.morapack.skyroute.algorithm.GeneticAlgorithm;
import com.morapack.skyroute.algorithm.Individual;
import com.morapack.skyroute.config.Config;
import com.morapack.skyroute.config.World;
import com.morapack.skyroute.models.Order;
import com.morapack.skyroute.models.OrderScope;
import com.morapack.skyroute.models.Route;
import com.morapack.skyroute.models.RouteSegment;
import com.morapack.skyroute.plan.service.WorldBuilder;
import com.morapack.skyroute.orders.repository.OrderRepository;
import com.morapack.skyroute.simulation.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SimulationService {

    private static final String TOPIC_PREFIX = "/topic/simulations/";
    private final Random random = new Random();
    private final WorldBuilder worldBuilder;
    private final OrderRepository orderRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final Path snapshotsDir = Paths.get("snapshots");
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Map<UUID, SimulationSession> sessions = new ConcurrentHashMap<>();

    public SimulationService(WorldBuilder worldBuilder,
                             OrderRepository orderRepository,
                             SimpMessagingTemplate messagingTemplate,
                             ObjectMapper objectMapper) {
        this.worldBuilder = worldBuilder;
        this.orderRepository = orderRepository;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    public SimulationStartResponse startSimulation(SimulationStartRequest request) {
        TimeRange range = resolveRange(request);
        long fetchStart = System.nanoTime();
        List<Order> projectedOrders = orderRepository
                .findAllByScopeAndCreationUtcBetweenOrderByCreationUtcAsc(
                        OrderScope.PROJECTED,
                        range.start(),
                        range.end()
                );
        log.info("[SIM] Loaded {} projected orders in {} ms (range {} - {})",
                projectedOrders.size(),
                nanosToMillis(System.nanoTime() - fetchStart),
                range.start(),
                range.end());
        if (projectedOrders.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No projected orders found for the selected range"
            );
        }

        UUID simulationId = UUID.randomUUID();
        SimulationSession session = new SimulationSession(simulationId, projectedOrders.size());
        sessions.put(simulationId, session);

        long worldBuildStart = System.nanoTime();
        World simulationWorld = worldBuilder.buildBaseWorld(range.start());
        log.info("[SIM] Base world built in {} ms", nanosToMillis(System.nanoTime() - worldBuildStart));
        try {
            Files.createDirectories(snapshotsDir);
            Path sessionFile = snapshotsDir.resolve(simulationId + ".jsonl");
            Files.deleteIfExists(sessionFile);
            session.snapshotFile = sessionFile;
        } catch (IOException ex) {
            log.warn("[SIM:{}] Unable to prepare snapshot file: {}", simulationId, ex.getMessage());
        }

        int windowMinutes = request != null && request.windowMinutes() != null
                ? request.windowMinutes()
                : Config.SIMULATION_WINDOW_MINUTES;
        if (windowMinutes < 0) {
            windowMinutes = 0;
        }

        final int finalWindowMinutes = windowMinutes;
        if (finalWindowMinutes > 0) {
            executorService.submit(() -> runSimulationBatched(session, projectedOrders, simulationWorld, finalWindowMinutes, range.start()));
        } else {
            executorService.submit(() -> runSimulationLegacy(session, projectedOrders, simulationWorld));
        }

        return new SimulationStartResponse(simulationId.toString());
    }

    public SimulationStatus getStatus(UUID simulationId) {
        SimulationSession session = sessions.get(simulationId);
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Simulation not found");
    }
        return session.toStatus();
    }

    public void cancel(UUID simulationId) {
        SimulationSession session = sessions.get(simulationId);
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Simulation not found");
        }
        session.cancel();
    }

    private void runSimulationLegacy(SimulationSession session, List<Order> orders, World world) {
        List<Order> demand = new ArrayList<>();
        Individual previousBest = null;
        try {
            log.info("[SIM:{}] Starting legacy simulation with {} orders", session.id, orders.size());
            for (Order order : orders) {
                if (session.cancelled.get()) {
                    log.warn("[SIM:{}] Simulation cancelled by user after processing {} orders", session.id, demand.size());
                    break;
                }
                previousBest = processOrder(session, world, previousBest, demand, order, orders.size(), true, order.getCreationUtc());
            }

            session.complete();
            SimulationSnapshot finalSnapshot = session.lastSnapshot;
            log.info("[SIM:{}] Simulation completed. Processed {}/{} orders. GA runs={}", session.id, session.processed.get(), session.totalOrders, session.gaRuns.get());
            persistSnapshot(session, finalSnapshot);
            messagingTemplate.convertAndSend(
                    topic(session.id),
                    SimulationMessage.completed(session.id.toString(), finalSnapshot)
            );
        } catch (Exception ex) {
            session.error(ex.getMessage());
            log.error("[SIM:{}] Simulation failed: {}", session.id, ex.getMessage(), ex);
            messagingTemplate.convertAndSend(topic(session.id), SimulationMessage.error(session.id.toString(), ex.getMessage()));
        }
    }

    private void runSimulationBatched(SimulationSession session,
                                      List<Order> orders,
                                      World world,
                                      int windowMinutes,
                                      Instant windowStart) {
        List<OrderWindow> batches = groupOrdersByWindow(orders, windowMinutes, windowStart);
        List<Order> demand = new ArrayList<>();
        Individual previousBest = null;
        boolean cancelled = false;
        Duration windowDuration = Duration.ofMinutes(windowMinutes);
        try {
            log.info("[SIM:{}] Starting batched simulation: {} orders grouped in {} windows of {} minutes",
                    session.id, orders.size(), batches.size(), windowMinutes);
            for (int batchIndex = 0; batchIndex < batches.size(); batchIndex++) {
                OrderWindow window = batches.get(batchIndex);
                List<Order> batch = window.orders();
                log.debug("[SIM:{}] Processing batch {} of {} ({} orders)",
                        session.id, batchIndex + 1, batches.size(), batch.size());
                Instant snapshotInstant = window.windowStart().plus(windowDuration);
                for (int i = 0; i < batch.size(); i++) {
                    if (session.cancelled.get()) {
                        cancelled = true;
                        break;
                    }
                    Order order = batch.get(i);
                    boolean emitSnapshot = (i == batch.size() - 1);
                    Instant emitInstant = emitSnapshot ? snapshotInstant : null;
                    previousBest = processOrder(session, world, previousBest, demand, order, orders.size(), emitSnapshot, emitInstant);
                }
                if (cancelled) {
                    log.warn("[SIM:{}] Simulation cancelled during batch {}", session.id, batchIndex + 1);
                    break;
                }
            }

            session.complete();
            SimulationSnapshot finalSnapshot = session.lastSnapshot;
            log.info("[SIM:{}] Simulation completed. Processed {}/{} orders. GA runs={}", session.id, session.processed.get(), session.totalOrders, session.gaRuns.get());
            persistSnapshot(session, finalSnapshot);
            messagingTemplate.convertAndSend(
                    topic(session.id),
                    SimulationMessage.completed(session.id.toString(), finalSnapshot)
            );
        } catch (Exception ex) {
            session.error(ex.getMessage());
            log.error("[SIM:{}] Simulation failed: {}", session.id, ex.getMessage(), ex);
            messagingTemplate.convertAndSend(topic(session.id), SimulationMessage.error(session.id.toString(), ex.getMessage()));
        }
    }

    private Individual processOrder(SimulationSession session,
                                    World world,
                                    Individual previousBest,
                                    List<Order> demand,
                                    Order order,
                                    int totalOrders,
                                    boolean emitSnapshot,
                                    Instant snapshotInstant) {
        long iterationStart = System.nanoTime();
        log.debug("[SIM:{}] Processing order {} (current demand size={})", session.id, order.getId(), demand.size());
        demand.add(order);

        world.advanceTo(order.getCreationUtc());
        Individual best = null;
        if (previousBest != null) {
            best = previousBest.tryInsertOrder(world, order, random);
            if (best != null) {
                best.applyToWorld(world);
                log.debug("[SIM:{}] Order {} patched via heuristic", session.id, order.getId());
            }
        }

        long gaDuration = 0;
        if (best == null) {
            GeneticAlgorithm ga = new GeneticAlgorithm(world, List.copyOf(demand));
            long start = System.nanoTime();
            best = ga.run(Config.POP_SIZE, Config.MAX_GEN, previousBest);
            gaDuration = System.nanoTime() - start;
            log.info("[SIM:{}] GA done for {} (took {} ms)", session.id, order.getId(), gaDuration / 1_000_000);
            session.gaRuns.incrementAndGet();
        }

        log.info("[SIM:{}] Metrics for {}: gaRun={} ms, iterationTotal={} ms",
                session.id,
                order.getId(),
                nanosToMillis(gaDuration),
                nanosToMillis(System.nanoTime() - iterationStart));

        log.debug("[SIM:{}] Plan ready for order {} (fitness={}, gaTime={} ms)",
                session.id,
                order.getId(),
                best.getFitness(),
                gaDuration / 1_000_000);

        if (emitSnapshot) {
            if (snapshotInstant != null) {
                Instant currentInstant = world.getCurrentInstant();
                if (currentInstant == null || snapshotInstant.isAfter(currentInstant)) {
                    world.advanceTo(snapshotInstant);
                }
            }
            SimulationSnapshot snapshot = toSnapshot(session.id, demand.size(), totalOrders, best, world, demand);
            session.update(snapshot);
            persistSnapshot(session, snapshot);
            log.trace("[SIM:{}] Snapshot created: processed={}/{}", session.id, demand.size(), totalOrders);
            messagingTemplate.convertAndSend(topic(session.id), SimulationMessage.progress(session.id.toString(), snapshot));
        }

        return best;
    }

    private SimulationSnapshot toSnapshot(UUID simulationId,
                                          int processed,
                                          int total,
                                          Individual best,
                                          World world,
                                          List<Order> demand) {

        Instant currentSimTime = world.getCurrentInstant();
        Map<String, Instant> completionTimes = best.computeCompletionTimes(world, demand);

        List<SimulationOrderPlan> orderPlans = best.getPlans().stream()
                .filter(plan -> {
                    Instant completion = completionTimes.get(plan.getOrderId());
                    return completion == null || completion.isAfter(currentSimTime);
                })
                .map(this::toOrderPlanDto)
                .collect(Collectors.toList());

        return new SimulationSnapshot(
                simulationId.toString(),
                processed,
                total,
                best.getFitness(),
                currentSimTime,
                orderPlans
        );
    }

    private SimulationOrderPlan toOrderPlanDto(com.morapack.skyroute.models.OrderPlan plan) {
        long slackMinutes = optionalDurationMinutes(plan.getSlack());
        List<SimulationRoute> routes = plan.getRoutes() == null
                ? List.of()
                : plan.getRoutes().stream().map(this::toRouteDto).toList();
        return new SimulationOrderPlan(plan.getOrderId(), slackMinutes, routes);
    }

    private SimulationRoute toRouteDto(Route route) {
        long slackMinutes = optionalDurationMinutes(route.getSlack());
        List<SimulationSegment> segments = route.getSegments() == null
                ? List.of()
                : route.getSegments().stream().map(this::toSegmentDto).toList();
        return new SimulationRoute(route.getQuantity(), slackMinutes, segments);
    }

    private SimulationSegment toSegmentDto(RouteSegment segment) {
        var flight = segment.getFlight();
        LocalDate date = segment.getDate();
        Instant departureUtc = flight.getDepartureInstant(date);
        Instant arrivalUtc = flight.getArrivalInstant(date);

        return new SimulationSegment(
                flight.getId(),
                flight.getOriginCode(),
                flight.getDestinationCode(),
                date,
                segment.getRouteQuantity(),
                departureUtc,
                arrivalUtc
        );
    }

    private long optionalDurationMinutes(Duration duration) {
        return duration == null ? 0 : duration.toMinutes();
    }

    private TimeRange resolveRange(SimulationStartRequest request) {
        Order firstProjected = orderRepository.findFirstByScopeOrderByCreationUtcAsc(OrderScope.PROJECTED)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "No projected orders available. Load projections before running the simulation."
                ));
        Order lastProjected = orderRepository.findFirstByScopeOrderByCreationUtcDesc(OrderScope.PROJECTED)
                .orElse(firstProjected);

        Instant start = request != null && request.startDate() != null
                ? toUtc(request.startDate())
                : firstProjected.getCreationUtc();
        Instant end = request != null && request.endDate() != null
                ? toUtc(request.endDate())
                : lastProjected.getCreationUtc();

        if (end.isBefore(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endDate must be greater than startDate");
        }

        return new TimeRange(start, end);
    }

    private Instant toUtc(LocalDateTime value) {
        return value.atOffset(ZoneOffset.UTC).toInstant();
    }

    private record TimeRange(Instant start, Instant end) {}

    private static long nanosToMillis(long nanos) {
        return nanos / 1_000_000;
    }

    private Instant alignToWindow(Instant instant, Duration window) {
        if (instant == null || window == null || window.isZero() || window.isNegative()) {
            return instant;
        }
        long windowMillis = window.toMillis();
        long alignedMillis = (instant.toEpochMilli() / windowMillis) * windowMillis;
        return Instant.ofEpochMilli(alignedMillis);
    }

    private List<OrderWindow> groupOrdersByWindow(List<Order> orders,
                                                  int windowMinutes,
                                                  Instant startInstant) {
        if (orders.isEmpty()) {
            return List.of();
        }
        Duration window = Duration.ofMinutes(windowMinutes);
        long windowMillis = window.toMillis();
        Instant alignedStart = alignToWindow(startInstant, window);
        long referenceMillis = alignedStart.toEpochMilli();
        Map<Long, List<Order>> buckets = new LinkedHashMap<>();
        for (Order order : orders) {
            long creationMillis = order.getCreationUtc().toEpochMilli();
            long delta = Math.max(0, creationMillis - referenceMillis);
            long bucketIndex = delta / windowMillis;
            long bucketKey = referenceMillis + bucketIndex * windowMillis;
            buckets.computeIfAbsent(bucketKey, k -> new ArrayList<>()).add(order);
        }
        List<OrderWindow> result = new ArrayList<>();
        for (Map.Entry<Long, List<Order>> entry : buckets.entrySet()) {
            result.add(new OrderWindow(Instant.ofEpochMilli(entry.getKey()), entry.getValue()));
        }
        return result;
    }

    private void persistSnapshot(SimulationSession session, SimulationSnapshot snapshot) {
        if (snapshot == null || session.snapshotFile == null) {
            return;
        }
        try {
            String line = objectMapper.writeValueAsString(snapshot) + System.lineSeparator();
            Files.write(session.snapshotFile,
                    line.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException ex) {
            log.warn("[SIM:{}] Unable to persist snapshot: {}", session.id, ex.getMessage());
        }
    }

    private String topic(UUID simulationId) {
        return TOPIC_PREFIX + simulationId;
    }

    private static class SimulationSession {
        private final UUID id;
        private final int totalOrders;
        private final AtomicInteger processed = new AtomicInteger();
        private final AtomicBoolean completed = new AtomicBoolean(false);
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private volatile SimulationSnapshot lastSnapshot;
        private volatile String error;
        private volatile Path snapshotFile;
        private final AtomicInteger gaRuns = new AtomicInteger();

        private SimulationSession(UUID id, int totalOrders) {
            this.id = id;
            this.totalOrders = totalOrders;
        }

        private void update(SimulationSnapshot snapshot) {
            this.lastSnapshot = snapshot;
            this.processed.set(snapshot.processedOrders());
        }

        private void complete() {
            this.completed.set(true);
        }

        private void cancel() {
            this.cancelled.set(true);
        }

        private void error(String message) {
            this.error = message;
        }

        private SimulationStatus toStatus() {
            return new SimulationStatus(
                    id.toString(),
                    processed.get(),
                    totalOrders,
                    completed.get(),
                    cancelled.get(),
                    error,
                    lastSnapshot
            );
        }
    }

    private record OrderWindow(Instant windowStart, List<Order> orders) {}
}
