package com.morapack.skyroute.plan.controller;

import com.morapack.skyroute.models.CurrentPlan;
import com.morapack.skyroute.plan.service.PlanningService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plan")
public class PlanController {

    private final PlanningService planningService;

    public PlanController(PlanningService planningService) {
        this.planningService = planningService;
    }

    @PostMapping("/run")
    public ResponseEntity<CurrentPlan> runAlgorithm() {
        CurrentPlan plan = planningService.run();
        return ResponseEntity.ok(plan);
    }

    @GetMapping("/current")
    public ResponseEntity<CurrentPlan> getCurrentPlan() {
        return planningService.getCurrentPlan()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/current")
    public ResponseEntity<Void> deleteCurrentPlan() {
        var current = planningService.getCurrentPlan();
        if (current.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        planningService.clearCurrentPlan();
        return ResponseEntity.noContent().build();
    }
}
