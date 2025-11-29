package com.morapack.skyroute.models;

import java.time.Instant;

public class Shipment {
    private final int quantity;
    private Route route;
    private boolean delivered = false;
    private ShipmentStatus status = ShipmentStatus.PLANNED;

    public Shipment(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        this.quantity = quantity;
    }

    public void assignRoute(Route newRoute) {
        if (newRoute == null) {
            throw new IllegalArgumentException("Route cannot be null");
        }

        if (this.route != null && this.route != newRoute) {
            this.route.releaseResources(); // libera vuelos, almacenes, etc.
        }

        this.route = newRoute;
    }

    public void refreshStatus(Instant now) {
        if (route == null || delivered) {
            return;
        }

        boolean anyDeparted = false;
        boolean allArrived = true;
        for (RouteSegment segment : route.getSegments()) {
            segment.refreshStatus(now);
            if (segment.isDeparted()) {
                anyDeparted = true;
            }
            if (!segment.isArrived()) {
                allArrived = false;
            }
        }

        if (allArrived && anyDeparted) {
            markDelivered();
        } else if (anyDeparted) {
            status = ShipmentStatus.IN_TRANSIT;
        }
    }

    public void markDelivered() {
        this.delivered = true;
        this.status = ShipmentStatus.DELIVERED;
    }

    public void cancel() {
        this.status = ShipmentStatus.CANCELLED;
        if (route != null) {
            route.releaseResources();
        }
    }

    public boolean isDelivered() { return delivered; }

    public ShipmentStatus getStatus() { return status; }

    public int getQuantity() { return quantity; }

    public Route getRoute() { return route; }
}
