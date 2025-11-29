package com.morapack.skyroute.plan.repository;

import com.morapack.skyroute.models.RouteSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface RouteSegmentRepository extends JpaRepository<RouteSegment, Long> {
    
    List<RouteSegment> findByDate(LocalDate date);
    
    List<RouteSegment> findByDeparted(boolean departed);
    
    List<RouteSegment> findByArrived(boolean arrived);
    
    List<RouteSegment> findByFinalLeg(boolean finalLeg);
}
