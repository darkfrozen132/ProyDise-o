package com.morapack.skyroute.plan.repository;

import com.morapack.skyroute.models.OrderPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderPlanRepository extends JpaRepository<OrderPlan, Long> {
    
    @Query("SELECT op FROM OrderPlan op WHERE SIZE(op.routes) > 0")
    List<OrderPlan> findOrderPlansWithRoutes();
    
    @Query("SELECT op FROM OrderPlan op WHERE SIZE(op.routes) >= ?1")
    List<OrderPlan> findByMinimumRoutes(int minRoutes);
}
