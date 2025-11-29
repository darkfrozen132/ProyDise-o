package com.morapack.skyroute.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "orders")
@Getter @Setter
@NoArgsConstructor
public class Order {

    @Id
    private String id;
    private String customerReference;
    @ManyToOne
    @JoinColumn(name = "destination_airport_code", referencedColumnName = "code")
    private Airport destinationAirport;
    private int quantity;
    private Instant creationUtc;
    private Instant dueUtc;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderScope scope = OrderScope.REAL;

     public Order(String id,
                 String customerReference,
                 Airport destination,
                 int quantity,
                 Instant creationUtc,
                 Instant dueUtc) {
        this(id, customerReference, destination, quantity, creationUtc, dueUtc, OrderScope.REAL);
    }

    public Order(String id,
                String customerReference,
                Airport destination,
                int quantity,
                Instant creationUtc,
                Instant dueUtc,
                OrderScope scope) {
        if (destination == null) {
            throw new IllegalArgumentException("Destination airport is required");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        this.id = Objects.requireNonNull(id, "id");
        this.customerReference = Objects.requireNonNull(customerReference, "customerReference");
        this.destinationAirport = destination;
        this.quantity = quantity;
        this.creationUtc = Objects.requireNonNull(creationUtc, "creationUtc");
        this.dueUtc = Objects.requireNonNull(dueUtc, "dueUtc");
        this.scope = scope == null ? OrderScope.REAL : scope;
    }

    public String getDestinationCode() {
        return destinationAirport.code;
    }

    public void setScope(OrderScope scope) {
        this.scope = scope == null ? OrderScope.REAL : scope;
    }
}
