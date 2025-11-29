package com.morapack.skyroute.plan.service;

import com.morapack.skyroute.models.Route;
import com.morapack.skyroute.models.RouteSegment;
import com.morapack.skyroute.plan.repository.RouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RouteService {

    private final RouteRepository routeRepository;

    public List<Route> findAll() {
        return routeRepository.findAll();
    }

    public Optional<Route> findById(Long id) {
        return routeRepository.findById(id);
    }

    public List<Route> findByQuantity(int quantity) {
        return routeRepository.findByQuantity(quantity);
    }

    public List<Route> findByQuantityGreaterThanEqual(int quantity) {
        return routeRepository.findByQuantityGreaterThanEqual(quantity);
    }

    public List<Route> findRoutesWithFinalLeg() {
        return routeRepository.findRoutesWithFinalLeg();
    }

    @Transactional
    public Route save(Route route) {
        return routeRepository.save(route);
    }

    @Transactional
    public Route create(int quantity) {
        Route route = new Route(quantity);
        return routeRepository.save(route);
    }

    @Transactional
    public void deleteById(Long id) {
        Optional<Route> route = routeRepository.findById(id);
        if (route.isPresent()) {
            route.get().releaseResources();
            routeRepository.deleteById(id);
        }
    }

    @Transactional
    public Route addSegment(Long routeId, RouteSegment segment) {
        Optional<Route> optionalRoute = routeRepository.findById(routeId);
        if (optionalRoute.isPresent()) {
            Route route = optionalRoute.get();
            route.add(segment);
            return routeRepository.save(route);
        }
        return null;
    }

    @Transactional
    public Route updateSlack(Long routeId, Duration slack) {
        Optional<Route> optionalRoute = routeRepository.findById(routeId);
        if (optionalRoute.isPresent()) {
            Route route = optionalRoute.get();
            route.setSlack(slack);
            return routeRepository.save(route);
        }
        return null;
    }

    @Transactional
    public void releaseResources(Long routeId) {
        Optional<Route> optionalRoute = routeRepository.findById(routeId);
        if (optionalRoute.isPresent()) {
            Route route = optionalRoute.get();
            route.releaseResources();
            routeRepository.save(route);
        }
    }
}
