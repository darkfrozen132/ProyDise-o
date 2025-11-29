package com.morapack.skyroute.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Data
@NoArgsConstructor
@Entity
@Table(name = "airport_operation")
public class AirportOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "airport_code", referencedColumnName = "code")
    private Airport airport;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private int delta; // + depósito, - retiro

    public AirportOperation(Airport airport, LocalDateTime timestamp, int delta) {
        this.airport = Objects.requireNonNull(airport, "airport");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
        this.delta = delta;
    }
}
