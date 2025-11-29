package com.morapack.skyroute.plan.repository;

import com.morapack.skyroute.models.CurrentPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CurrentPlanRepository extends JpaRepository<CurrentPlan, Long> {
}
