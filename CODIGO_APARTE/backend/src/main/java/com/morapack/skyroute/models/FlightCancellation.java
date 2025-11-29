package com.morapack.skyroute.models;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "flight_cancellation")
public class FlightCancellation {

    @EmbeddedId
    private FlightCancellationId id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @MapsId("flightId")
    @JoinColumn(name = "flight_id", referencedColumnName = "id")
    private Flight flight;

    public FlightCancellation() {}

    public FlightCancellation(Flight flight, LocalDate date) {
        this.flight = flight;
        this.id = new FlightCancellationId(flight.getId(), date);
    }

    public FlightCancellationId getId() {
        return id;
    }

    public Flight getFlight() {
        return flight;
    }

    public LocalDate getDate() {
        return id != null ? id.getDate() : null;
    }
}
