package com.morapack.skyroute.orders.service;

import com.morapack.skyroute.base.repository.AirportRepository;
import com.morapack.skyroute.models.Airport;
import com.morapack.skyroute.models.Order;
import com.morapack.skyroute.models.OrderScope;
import com.morapack.skyroute.orders.dto.OrderRequest;
import com.morapack.skyroute.orders.repository.OrderRepository;
import com.morapack.skyroute.config.Config;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final AirportRepository airportRepository;

    public OrderService(OrderRepository orderRepository, AirportRepository airportRepository) {
        this.orderRepository = orderRepository;
        this.airportRepository = airportRepository;
    }

    private static final int DEFAULT_LIMIT = 500;
    private static final int MAX_LIMIT = 2000;

    public Page<Order> getPage(OrderScope scope, Integer page, Integer size) {
        int pageIndex = page == null || page < 0 ? 0 : page;
        int pageSize = normalizeLimit(size);
        Sort sort = Sort.by(Sort.Direction.DESC, "creationUtc");
        Pageable pageable = PageRequest.of(pageIndex, pageSize, sort);
        if (scope == null) {
            return orderRepository.findAll(pageable);
        }
        return orderRepository.findAllByScope(scope, pageable);
    }

    public long count(OrderScope scope) {
        if (scope == null) {
            return orderRepository.count();
        }
        return orderRepository.countByScope(scope);
    }

    public Order create(OrderRequest request) {
        request = ensureId(request);
        validateRequest(request, false);
        if (orderRepository.existsById(request.id())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order " + request.id() + " already exists");
        }
        Order order = buildOrderFromRequest(request);
        return orderRepository.save(order);
    }

    public List<Order> createAll(List<OrderRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orders list is required");
        }

        Set<String> seenIds = new HashSet<>();
        List<OrderRequest> withIds = new ArrayList<>(requests.size());
        for (OrderRequest original : requests) {
            OrderRequest request = ensureId(original);
            validateRequest(request, false);
            if (!seenIds.add(request.id())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Duplicated order id in payload: " + request.id()
                );
            }
            withIds.add(request);
        }

        var existing = orderRepository.findAllById(seenIds);
        if (!existing.isEmpty()) {
            String conflicting = existing.stream().map(Order::getId).findFirst().orElse("unknown");
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Order " + conflicting + " already exists"
            );
        }

        List<Order> toPersist = new ArrayList<>(withIds.size());
        for (OrderRequest request : withIds) {
            toPersist.add(buildOrderFromRequest(request));
        }
        return orderRepository.saveAll(toPersist);
    }

    public Order update(String id, OrderRequest request) {
        Order existing = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order " + id + " not found"));
        validateRequest(request, false);
        Airport destination = airportRepository.findById(request.destinationAirportCode())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Destination airport not found: " + request.destinationAirportCode()));

        Instant creationUtc = toUtc(request.creationLocal(), destination);
        Instant dueUtc = creationUtc.plus(Config.INTERCONTINENTAL_SLA_HOURS, ChronoUnit.HOURS);

        existing.setCustomerReference(request.customerReference());
        existing.setDestinationAirport(destination);
        existing.setQuantity(request.quantity());
        existing.setCreationUtc(creationUtc);
        existing.setDueUtc(dueUtc);
        existing.setScope(resolveScope(request.projected()));
        return orderRepository.save(existing);
    }

    public void delete(String id) {
        if (!orderRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order " + id + " not found");
        }
        orderRepository.deleteById(id);
    }

    private Order buildOrderFromRequest(OrderRequest request) {
        if (isBlank(request.id())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "id generation failed");
        }
        Airport destination = airportRepository.findById(request.destinationAirportCode())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Destination airport not found: " + request.destinationAirportCode()));
        Instant creationUtc = toUtc(request.creationLocal(), destination);
        Instant dueUtc = creationUtc.plus(Config.INTERCONTINENTAL_SLA_HOURS, ChronoUnit.HOURS);
        return new Order(
                request.id(),
                request.customerReference(),
                destination,
                request.quantity(),
                creationUtc,
                dueUtc,
                resolveScope(request.projected())
        );
    }

    private void validateRequest(OrderRequest request, boolean requireId) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        if (requireId && isBlank(request.id())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "id is required");
        }
        if (isBlank(request.customerReference())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "customerReference is required");
        }
        if (isBlank(request.destinationAirportCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "destinationAirportCode is required");
        }
        if (request.quantity() == null || request.quantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity must be greater than zero");
        }
        if (request.creationLocal() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "creationLocal is required");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private Instant toUtc(java.time.LocalDateTime creationLocal, Airport destination) {
        ZoneOffset offset = destination.getZoneOffset();
        return creationLocal.atOffset(offset).toInstant();
    }

    private OrderScope resolveScope(Boolean projectedFlag) {
        return Boolean.TRUE.equals(projectedFlag) ? OrderScope.PROJECTED : OrderScope.REAL;
    }

    private OrderRequest ensureId(OrderRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        if (!isBlank(request.id())) {
            return request;
        }
        if (isBlank(request.destinationAirportCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "destinationAirportCode is required to generate id");
        }
        String generatedId = generateOrderId(request);
        return new OrderRequest(
                generatedId,
                request.customerReference(),
                request.destinationAirportCode(),
                request.quantity(),
                request.creationLocal(),
                request.projected()
        );
    }

    private String generateOrderId(OrderRequest request) {
        OrderScope scope = resolveScope(request.projected());
        String prefix = scope == OrderScope.REAL ? "1" : "0";
        String destination = request.destinationAirportCode().toUpperCase();
        long next = orderRepository.countByScope(scope) + 1;
        String candidate;
        do {
            candidate = String.format("%s%08d_%s", prefix, next, destination);
            next++;
        } while (orderRepository.existsById(candidate));
        return candidate;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
