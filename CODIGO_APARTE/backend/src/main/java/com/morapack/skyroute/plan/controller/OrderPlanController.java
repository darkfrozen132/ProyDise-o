package com.morapack.skyroute.plan.controller;

import com.morapack.skyroute.models.OrderPlan;
import com.morapack.skyroute.models.Route;
import com.morapack.skyroute.plan.service.OrderPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order-plans")
@RequiredArgsConstructor
public class OrderPlanController {

    private final OrderPlanService orderPlanService;

    @GetMapping
    public ResponseEntity<List<OrderPlan>> getAllOrderPlans() {
        return ResponseEntity.ok(orderPlanService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderPlan> getOrderPlanById(@PathVariable Long id) {
        return orderPlanService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

  /*   @GetMapping("/status/{status}")
    public ResponseEntity<List<OrderPlan>> getOrderPlansByStatus(@PathVariable String status) {
        return ResponseEntity.ok(orderPlanService.findByStatus(status));
    } */

  /*  @GetMapping("/date-range")
    public ResponseEntity<List<OrderPlan>> getOrderPlansByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(orderPlanService.findByCreatedAtBetween(startDate, endDate));
    } */

    @GetMapping("/with-routes")
    public ResponseEntity<List<OrderPlan>> getOrderPlansWithRoutes() {
        return ResponseEntity.ok(orderPlanService.findOrderPlansWithRoutes());
    }

    @GetMapping("/min-routes/{minRoutes}")
    public ResponseEntity<List<OrderPlan>> getOrderPlansByMinimumRoutes(@PathVariable int minRoutes) {
        return ResponseEntity.ok(orderPlanService.findByMinimumRoutes(minRoutes));
    }

    @PostMapping
    public ResponseEntity<OrderPlan> createOrderPlan(@RequestBody OrderPlan orderPlan) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderPlanService.save(orderPlan));
    }

    @PostMapping("/create/{status}")
    public ResponseEntity<OrderPlan> createOrderPlanWithStatus(@PathVariable String status) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderPlanService.create(status));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderPlan> updateOrderPlan(@PathVariable Long id, @RequestBody OrderPlan orderPlan) {
        return orderPlanService.findById(id)
                .map(existing -> {
                    orderPlan.setId(id);
                    return ResponseEntity.ok(orderPlanService.save(orderPlan));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrderPlan> updateStatus(@PathVariable Long id, @RequestParam String status) {
        OrderPlan orderPlan = orderPlanService.updateStatus(id, status);
        if (orderPlan != null) {
            return ResponseEntity.ok(orderPlan);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/routes")
    public ResponseEntity<OrderPlan> addRoute(@PathVariable Long id, @RequestBody Route route) {
        OrderPlan orderPlan = orderPlanService.addRoute(id, route);
        if (orderPlan != null) {
            return ResponseEntity.ok(orderPlan);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}/routes")
    public ResponseEntity<OrderPlan> removeRoute(@PathVariable Long id, @RequestBody Route route) {
        OrderPlan orderPlan = orderPlanService.removeRoute(id, route);
        if (orderPlan != null) {
            return ResponseEntity.ok(orderPlan);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrderPlan(@PathVariable Long id) {
        orderPlanService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
