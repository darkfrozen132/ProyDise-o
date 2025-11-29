package com.morapack.skyroute.models;


import java.util.List;

import com.morapack.skyroute.io.*;

public class Network {
    private final Airports airports;
    private final Flights flights;

    public Network(Airports airports, Flights flights) {
        this.airports = airports;
        this.flights = flights;
    }

    public Airport getAirport(String code) {
        return airports.get(code);
    }

    public Airports getAirports() {
        return airports;
    }

    public Flights getFlights() {
        return flights;
    }

    public List<Flight> getFlightsFrom(String code) {
        return flights.getByOriginCode(code);
    }

    @Deprecated
    public List<Flight> getFlight(String code) {
        return getFlightsFrom(code);
    }
}
