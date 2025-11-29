package com.morapack.skyroute.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "current_plan")
public class CurrentPlan {

    @Id
    private Long id = 1L; // Singleton: siempre será el registro único

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(nullable = false)
    private double fitness;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderPlan> orderPlans;

    public CurrentPlan(LocalDateTime generatedAt, double fitness, List<OrderPlan> orderPlans) {
        this.generatedAt = generatedAt;
        this.fitness = fitness;
        this.orderPlans = orderPlans;
    }
}
