package com.morapack.skyroute.plan.controller;

import com.morapack.skyroute.models.RouteSegment;
import com.morapack.skyroute.plan.service.RouteSegmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/route-segments")
@RequiredArgsConstructor
public class RouteSegmentController {

    private final RouteSegmentService routeSegmentService;

    @GetMapping
    public ResponseEntity<List<RouteSegment>> getAllRouteSegments() {
        return ResponseEntity.ok(routeSegmentService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RouteSegment> getRouteSegmentById(@PathVariable Long id) {
        return routeSegmentService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-date/{date}")
    public ResponseEntity<List<RouteSegment>> getRouteSegmentsByDate(@PathVariable LocalDate date) {
        return ResponseEntity.ok(routeSegmentService.findByDate(date));
    }

    @GetMapping("/departed/{departed}")
    public ResponseEntity<List<RouteSegment>> getRouteSegmentsByDeparted(@PathVariable boolean departed) {
        return ResponseEntity.ok(routeSegmentService.findByDeparted(departed));
    }

    @GetMapping("/arrived/{arrived}")
    public ResponseEntity<List<RouteSegment>> getRouteSegmentsByArrived(@PathVariable boolean arrived) {
        return ResponseEntity.ok(routeSegmentService.findByArrived(arrived));
    }

    @PostMapping
    public ResponseEntity<RouteSegment> createRouteSegment(@RequestBody RouteSegment routeSegment) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(routeSegmentService.save(routeSegment));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RouteSegment> updateRouteSegment(@PathVariable Long id, @RequestBody RouteSegment routeSegment) {
        return routeSegmentService.findById(id)
                .map(existing -> ResponseEntity.ok(routeSegmentService.save(routeSegment)))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRouteSegment(@PathVariable Long id) {
        routeSegmentService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/refresh-status")
    public ResponseEntity<Void> refreshStatus(@PathVariable Long id) {
        routeSegmentService.refreshStatus(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/refresh-all-statuses")
    public ResponseEntity<Void> refreshAllStatuses() {
        routeSegmentService.refreshAllStatuses();
        return ResponseEntity.ok().build();
    }
}
