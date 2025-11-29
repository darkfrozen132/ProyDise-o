package com.morapack.skyroute.models;

/**
 * Identifica si un pedido es parte de la operación real
 * o corresponde a una proyección usada por la simulación semanal.
 */
public enum OrderScope {
    REAL,
    PROJECTED
}
