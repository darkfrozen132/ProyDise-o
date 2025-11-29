package com.morapack.skyroute.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;


@Data
@Entity
@Table(name = "route_segment")
@NoArgsConstructor
public class RouteSegment{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "flight_id" , referencedColumnName = "id")
    private Flight flight;

    private LocalDate date;
    private int routeQuantity;
    private boolean departed;
    private boolean arrived;
    private boolean receivedByNext = false;
    private boolean finalLeg;
    private Duration slack = Duration.ZERO;

    public RouteSegment(Flight flight, LocalDate date, int quantity, boolean finalLeg) {
        this.flight = flight;
        this.date = date;
        this.routeQuantity = quantity;
        this.finalLeg = finalLeg;
    }

    public void markRecievedByNext() {
        this.receivedByNext = true;
    }

    public LocalDateTime getExactDepDateTime() {
        return LocalDateTime.ofInstant(flight.getDepartureInstant(date), ZoneOffset.ofHours(flight.getOrigin().getGmtOffsetHours()));
    }

    public LocalDateTime getExactArrDateTime() {
        return LocalDateTime.ofInstant(flight.getArrivalInstant(date), ZoneOffset.ofHours(flight.getDestination().getGmtOffsetHours()));
    }

     public void refreshStatus(Instant now) {
        if (flight.isCancelled(date)) {
            departed = false;
            arrived = false;
            return;
        }
        if (!departed) {
            Instant departureInstant = flight.getDepartureInstant(date);
            if (!departureInstant.isAfter(now)) {
                departed = true;
            }
        }
        if (!arrived) {
            Instant arrivalInstant = flight.getArrivalInstant(date);
            if (!arrivalInstant.isAfter(now)) {
                arrived = true;
                markRecievedByNext();
                slack = Duration.ZERO;
            }
        }
    }

    public void release() {
    }

    public void setSlack(Duration slack) {
        this.slack = slack;
    }
}