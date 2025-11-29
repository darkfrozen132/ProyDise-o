package com.morapack.skyroute.models;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "flight")
public class Flight {

    @Id
    private String id;

    @ManyToOne
    @JoinColumn(name = "origin_code", referencedColumnName = "code")
    private Airport origin;

    @ManyToOne
    @JoinColumn(name = "destination_code", referencedColumnName = "code")
    private Airport destination;

    private LocalTime depLocal;
    private LocalTime arrLocal;
    private int dailyCapacity;
    private Duration flightDuration = Duration.ZERO;

    @Transient
    private Set<LocalDate> cancelledDates = Set.of();

    public Flight(String id,
                  Airport origin,
                  Airport destination,
                  LocalTime depLocal,
                  LocalTime arrLocal,
                  int dailyCapacity,
                  Set<LocalDate> cancelledDates) {

        if (origin == null || destination == null) {
            throw new IllegalArgumentException("Origin and destination airports are required");
        }
        this.id = Objects.requireNonNull(id, "id");
        this.origin = origin;
        this.destination = destination;
        this.depLocal = Objects.requireNonNull(depLocal, "depLocal");
        this.arrLocal = Objects.requireNonNull(arrLocal, "arrLocal");
        this.dailyCapacity = dailyCapacity;
        this.flightDuration = computeDuration(origin, destination, depLocal, arrLocal);
        setCancelledDates(cancelledDates);
    }

    public String getId() {
        return id;
    }

    public Airport getOrigin() {
        return origin;
    }

    public Airport getDestination() {
        return destination;
    }

    public int getDailyCapacity() {
        return dailyCapacity;
    }

    public static String buildId(Airport origin, Airport destination, LocalTime depLocal) {
        return origin.code + "-" + destination.code + "-" + depLocal;
    }

    public String getOriginCode() {
        return origin.code;
    }

    public String getDestinationCode() {
        return destination.code;
    }

    public void setCancelledDates(Set<LocalDate> dates) {
        if (dates == null || dates.isEmpty()) {
            this.cancelledDates = Set.of();
        } else {
            this.cancelledDates = Collections.unmodifiableSet(new HashSet<>(dates));
        }
    }

    public Set<LocalDate> getCancelledDates() {
        return cancelledDates;
    }

    public boolean isCancelled(LocalDate date) {
        return cancelledDates.contains(date);
    }

    public Instant getDepartureInstant(LocalDate date) {
        if (isCancelled(date)) {
            throw new IllegalStateException("Flight is cancelled on " + date);
        }
        LocalDateTime local = LocalDateTime.of(date, depLocal);
        ZoneOffset offset = origin.getZoneOffset();
        return local.toInstant(offset);
    }

    public Instant getArrivalInstant(LocalDate date) {
        if (isCancelled(date)) {
            throw new IllegalStateException("Flight is cancelled on " + date);
        }
        Instant departureInstant = getDepartureInstant(date);
        return departureInstant.plus(flightDuration);
    }

    @PostLoad
    @PostPersist
    @PostUpdate
    private void refreshDuration() {
        if (origin != null && destination != null && depLocal != null && arrLocal != null) {
            this.flightDuration = computeDuration(origin, destination, depLocal, arrLocal);
        }
    }

    private Duration computeDuration(Airport origin,
                                     Airport destination,
                                     LocalTime departureLocal,
                                     LocalTime arrivalLocal) {
        LocalDate baseDate = LocalDate.of(2000, 1, 1);
        LocalDateTime depDateTime = LocalDateTime.of(baseDate, departureLocal);
        LocalDateTime arrDateTime = LocalDateTime.of(baseDate, arrivalLocal);
        ZoneOffset originOffset = origin.getZoneOffset();
        ZoneOffset destinationOffset = destination.getZoneOffset();

        Instant depInstant = depDateTime.toInstant(originOffset);
        Instant arrInstant = arrDateTime.toInstant(destinationOffset);

        while (!arrInstant.isAfter(depInstant)) {
            arrDateTime = arrDateTime.plusDays(1);
            arrInstant = arrDateTime.toInstant(destinationOffset);
        }
        return Duration.between(depInstant, arrInstant);
    }
}
