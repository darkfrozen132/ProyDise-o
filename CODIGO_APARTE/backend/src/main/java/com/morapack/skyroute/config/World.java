package com.morapack.skyroute.config;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;

import com.morapack.skyroute.base.repository.AirportRepository;
import com.morapack.skyroute.base.repository.FlightRepository;
import com.morapack.skyroute.io.*;
import com.morapack.skyroute.models.*;
import com.morapack.skyroute.orders.repository.OrderRepository;

public class World implements Orders.Observer {
    private static World instance;
    private final Airports airports;
    private final Flights flights;
    private final Orders orders;
    private final AirportSchedule airportSchedule;
    private Instant currentInstant = Instant.EPOCH;

    public static synchronized World getInstance() {
        if (instance == null) {
            instance = new World();
        }
        return instance;
    }

    public World() {
        this(loadAirportsFromFiles(), null, null, null, null);
    }

    public static void setRepositories(AirportRepository airportRepo,
                                   FlightRepository flightRepo,
                                   OrderRepository orderRepo) {
    airportRepository = airportRepo;
    flightRepository = flightRepo;
    orderRepository = orderRepo;
}

    private World(Airports airports,
                  Flights flights,
                  Orders orders,
                  AirportSchedule airportSchedule,
                  Instant currentInstant) {
        this.airports = Objects.requireNonNull(airports, "airports");
        this.flights = flights != null ? flights : createFlightsFromFiles(this.airports);
        this.orders = orders != null ? orders : createOrdersFromFiles(this.airports);
        this.airportSchedule = airportSchedule != null
                ? airportSchedule
                : new AirportSchedule(this.airports.asMap());
        this.currentInstant = currentInstant != null ? currentInstant : Instant.EPOCH;
        if (this.orders != null) {
            this.orders.setObserver(this);
        }
    }

    public Airports getAirports() {
        return airports;
    }

    public Flights getFlights() {
        return flights;
    }

    public Orders getOrders() {
        return orders;
    }

    public AirportSchedule getAirportSchedule() {
        return airportSchedule;
    }

    public Instant getCurrentInstant() {
        return currentInstant;
    }

    @Override
    public synchronized void onOrderRead(Order order) {
        advanceTo(order.getCreationUtc());
    }

    public synchronized void advanceTo(Instant instant) {
        if (instant == null) {
            return;
        }
        currentInstant = instant;
        cleanupState(currentInstant);
    }

    private void cleanupState(Instant now) {
        cleanupWarehouses(now);
        cleanupFlights(now);
    }

    private void cleanupWarehouses(Instant now) {
        for (Airport airport : airports.asMap().values()) {
            LocalDateTime localNow = LocalDateTime.ofInstant(now, airport.getZoneOffset());
            airportSchedule.cleanupUntil(airport.code, localNow);
        }
    }

    private void cleanupFlights(Instant now) {
        var schedule = flights.getSchedule();
        for (Flight flight : flights.getAll()) {
            ZoneOffset offset = flight.getOrigin().getZoneOffset();
            LocalDate currentDate = LocalDateTime.ofInstant(now, offset).toLocalDate();
            schedule.purgeBefore(flight, currentDate);
        }
    }

    public static World fromData(Airports airports,
                                 Flights flights,
                                 AirportSchedule airportSchedule,
                                 Instant currentInstant) {
        return new World(airports,
                flights,
                null,
                airportSchedule,
                currentInstant);
    }

    @Autowired
    private static AirportRepository airportRepository;

    private static Airports loadAirportsFromFiles() {
        /*try {
            return Airports.load(Config.PATH_AIRPORTS);
        } catch (IOException e) {
            throw new RuntimeException("Error loading airports", e);
        }*/
        List<Airport> airportList = airportRepository.findAll();
        return Airports.fromEntities(airportList);
    }

    @Autowired
    private static FlightRepository flightRepository;

    private static Flights createFlightsFromFiles(Airports airports) {
        /*try {
            return Flights.load(Config.PATH_FLIGHTS, airports);
        } catch (IOException e) {
            throw new RuntimeException("Error loading flights", e);
        }*/
        List<Flight> flightList = flightRepository.findAll();
        return  Flights.fromEntities(flightList, airports);
    }


    @Autowired
    private static OrderRepository orderRepository;
    private static Orders createOrdersFromFiles(Airports airports) {
        /*try {
            return Orders.open(Config.PATH_ORDERS, airports);
        } catch (IOException e) {
            throw new RuntimeException("Error loading orders", e);
        }*/

        List<Order> orderList = orderRepository.findAllByScope(OrderScope.REAL);
        return OrderLoader.loadFromDatabase(orderList, airports);
    }
}
