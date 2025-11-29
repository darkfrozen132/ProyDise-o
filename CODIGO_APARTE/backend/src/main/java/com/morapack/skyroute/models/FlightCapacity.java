package com.morapack.skyroute.models;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@Data
@NoArgsConstructor
@Entity
@Table(name = "flight_capacity")
public class FlightCapacity {

    @EmbeddedId
    private FlightCapacityKey id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Flight flight;

    @Column(name = "used_capacity", nullable = false)
    private int usedCapacity;

    public FlightCapacity(Flight flight, LocalDate date, int usedCapacity) {
        this.id = new FlightCapacityKey(flight.getId(), date);
        this.flight = flight;
        this.usedCapacity = usedCapacity;
    }

    @Embeddable
    @Data
    @NoArgsConstructor
    public static class FlightCapacityKey implements Serializable {

        @Column(name = "flight_id", nullable = false)
        private String flightId;

        @Column(name = "date", nullable = false)
        private LocalDate date;

        public FlightCapacityKey(String flightId, LocalDate date) {
            this.flightId = flightId;
            this.date = date;
        }
    }
}
