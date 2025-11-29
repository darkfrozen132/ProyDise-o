package com.morapack.skyroute.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

public class Config {
    public static final int CONTINENTAL_SLA_HOURS = 48;
    public static final int INTERCONTINENTAL_SLA_HOURS = 72;
    public static final Duration TRANSFER_BUFFER = Duration.ofMinutes(30); // Nota: Revisar si duration es necesario
    public static final Duration WAREHOUSE_DWELL = Duration.ofHours(2); // Nota: Revisar si duration es necesario
    public static final int SIMULATION_WINDOW_MINUTES = 10;

    public static final Path PATH_AIRPORTS = Paths.get("src/com/morapack/config/data/aeropuertos.txt");
    public static final Path PATH_FLIGHTS = Paths.get("src/com/morapack/config/data/vuelos.txt");
    public static final Path PATH_ORDERS = Paths.get("src/com/morapack/config/data/pedidos.txt");
    public static final Path DIR_LOGS = Paths.get("logs");
    public static final Path DIR_EXPORTS = Paths.get("exports");

    // GA
    public static final int POP_SIZE = 15;
    public static final int MAX_GEN = 15;
    public static final double P_CROSS = 0.5;
    public static final double P_MUT = 0.5;
}
