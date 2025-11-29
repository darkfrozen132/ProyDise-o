package com.morapack.skyroute.plan.controller;

import com.morapack.skyroute.models.Route;
import com.morapack.skyroute.models.RouteSegment;
import com.morapack.skyroute.plan.service.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    @GetMapping
    public ResponseEntity<List<Route>> getAllRoutes() {
        return ResponseEntity.ok(routeService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Route> getRouteById(@PathVariable Long id) {
        return routeService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/quantity/{quantity}")
    public ResponseEntity<List<Route>> getRoutesByQuantity(@PathVariable int quantity) {
        return ResponseEntity.ok(routeService.findByQuantity(quantity));
    }

    @GetMapping("/quantity-min/{quantity}")
    public ResponseEntity<List<Route>> getRoutesByQuantityGreaterThanEqual(@PathVariable int quantity) {
        return ResponseEntity.ok(routeService.findByQuantityGreaterThanEqual(quantity));
    }

    @GetMapping("/with-final-leg")
    public ResponseEntity<List<Route>> getRoutesWithFinalLeg() {
        return ResponseEntity.ok(routeService.findRoutesWithFinalLeg());
    }

    @PostMapping
    public ResponseEntity<Route> createRoute(@RequestBody Route route) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(routeService.save(route));
    }

    @PostMapping("/create/{quantity}")
    public ResponseEntity<Route> createRouteWithQuantity(@PathVariable int quantity) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(routeService.create(quantity));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Route> updateRoute(@PathVariable Long id, @RequestBody Route route) {
        return routeService.findById(id)
                .map(existing -> {
                    route.setId(id);
                    return ResponseEntity.ok(routeService.save(route));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoute(@PathVariable Long id) {
        routeService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/segments")
    public ResponseEntity<Route> addSegment(@PathVariable Long id, @RequestBody RouteSegment segment) {
        Route route = routeService.addSegment(id, segment);
        if (route != null) {
            return ResponseEntity.ok(route);
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}/slack")
    public ResponseEntity<Route> updateSlack(@PathVariable Long id, @RequestParam long seconds) {
        Duration slack = Duration.ofSeconds(seconds);
        Route route = routeService.updateSlack(id, slack);
        if (route != null) {
            return ResponseEntity.ok(route);
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}/release-resources")
    public ResponseEntity<Void> releaseResources(@PathVariable Long id) {
        routeService.releaseResources(id);
        return ResponseEntity.ok().build();
    }
}
