package com.morapack.skyroute.models;


public enum ShipmentStatus {
    PLANNED,     // Existe en plan, aún no enviado
    IN_TRANSIT,  // Ya partió, puede actualizarse su progreso
    DELIVERED,   // Llegó a destino (inmutable)
    CANCELLED    // Se eliminó antes de partir
}