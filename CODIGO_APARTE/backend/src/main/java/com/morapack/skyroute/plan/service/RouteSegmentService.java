package com.morapack.skyroute.plan.service;

import com.morapack.skyroute.models.RouteSegment;
import com.morapack.skyroute.plan.repository.RouteSegmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RouteSegmentService {

    private final RouteSegmentRepository routeSegmentRepository;

    public List<RouteSegment> findAll() {
        return routeSegmentRepository.findAll();
    }

    public Optional<RouteSegment> findById(Long id) {
        return routeSegmentRepository.findById(id);
    }

    public List<RouteSegment> findByDate(LocalDate date) {
        return routeSegmentRepository.findByDate(date);
    }

    public List<RouteSegment> findByDeparted(boolean departed) {
        return routeSegmentRepository.findByDeparted(departed);
    }

    public List<RouteSegment> findByArrived(boolean arrived) {
        return routeSegmentRepository.findByArrived(arrived);
    }

    @Transactional
    public RouteSegment save(RouteSegment routeSegment) {
        return routeSegmentRepository.save(routeSegment);
    }

    @Transactional
    public void deleteById(Long id) {
        routeSegmentRepository.deleteById(id);
    }

    @Transactional
    public void refreshStatus(Long id) {
        Optional<RouteSegment> optionalSegment = routeSegmentRepository.findById(id);
        if (optionalSegment.isPresent()) {
            RouteSegment segment = optionalSegment.get();
            segment.refreshStatus(Instant.now());
            routeSegmentRepository.save(segment);
        }
    }

    @Transactional
    public void refreshAllStatuses() {
        Instant now = Instant.now();
        List<RouteSegment> segments = routeSegmentRepository.findAll();
        segments.forEach(segment -> {
            segment.refreshStatus(now);
            routeSegmentRepository.save(segment);
        });
    }
}
