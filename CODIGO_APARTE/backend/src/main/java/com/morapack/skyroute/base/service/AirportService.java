package com.morapack.skyroute.base.service;

import com.morapack.skyroute.models.Airport;
import com.morapack.skyroute.base.repository.AirportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AirportService {

    @Autowired
    private AirportRepository airportRepository;

    public List<Airport> getAll() {
        return airportRepository.findAll();
    }

    public Optional<Airport> findById(String code) {
        return airportRepository.findById(code);
    }

    public Airport create(Airport airport) {
        return airportRepository.save(airport);
    }

    public void deleteById(String code) {
        airportRepository.deleteById(code);
    }
}
