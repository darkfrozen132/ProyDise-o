package com.morapack.skyroute.models;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@Embeddable
public class FlightCancellationId implements Serializable {

    @Column(name = "flight_id", nullable = false)
    private String flightId;

    @Column(name = "cancellation_date", nullable = false)
    private LocalDate date;

    public FlightCancellationId() {}

    public FlightCancellationId(String flightId, LocalDate date) {
        this.flightId = flightId;
        this.date = date;
    }

    public String getFlightId() {
        return flightId;
    }

    public LocalDate getDate() {
        return date;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FlightCancellationId that)) return false;
        return Objects.equals(flightId, that.flightId) && Objects.equals(date, that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flightId, date);
    }
}
