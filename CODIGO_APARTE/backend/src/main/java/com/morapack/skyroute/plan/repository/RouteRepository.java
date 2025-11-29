package com.morapack.skyroute.plan.repository;

import com.morapack.skyroute.models.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {
    
    List<Route> findByQuantity(int quantity);
    
    @Query("SELECT r FROM Route r WHERE r.quantity >= ?1")
    List<Route> findByQuantityGreaterThanEqual(int quantity);
    
    @Query("SELECT r FROM Route r JOIN r.segments s WHERE s.finalLeg = true")
    List<Route> findRoutesWithFinalLeg();
}
