package com.morapack.skyroute.models;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "order_plan")
public class OrderPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderId;

    // Relación con Route: un plan puede tener varias rutas
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_plan_id" , referencedColumnName = "id" )
    private List<Route> routes = new ArrayList<>();

    private Duration slack = Duration.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_plan_id")
    private CurrentPlan plan;

    // Constructor adicional (para uso de lógica)
    public OrderPlan(String orderId) {
        this.orderId = orderId;
    }

    // Métodos de conveniencia
    public void addRoute(Route route) {
        routes.add(route);
    }

    public int plannedQuantity() {
        return routes.stream().mapToInt(Route::getQuantity).sum();
    }
}