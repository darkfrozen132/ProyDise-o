package com.morapack.skyroute.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import com.morapack.skyroute.base.repository.AirportRepository;
import com.morapack.skyroute.base.repository.FlightRepository;
import com.morapack.skyroute.orders.repository.OrderRepository;

@Configuration
public class WorldConfig {
    @Autowired
    public void configureWorld(AirportRepository airportRepo,
                               FlightRepository flightRepo,
                               OrderRepository orderRepo) {
        World.setRepositories(airportRepo, flightRepo, orderRepo);
    }
}