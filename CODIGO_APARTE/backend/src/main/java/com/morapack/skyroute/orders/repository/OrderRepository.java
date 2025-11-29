package com.morapack.skyroute.orders.repository;

import com.morapack.skyroute.models.Order;
import com.morapack.skyroute.models.OrderScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    List<Order> findAllByScope(OrderScope scope);

    List<Order> findAllByScopeOrderByCreationUtcAsc(OrderScope scope);

    List<Order> findAllByScopeAndCreationUtcBetweenOrderByCreationUtcAsc(OrderScope scope,
                                                                         Instant start,
                                                                         Instant end);

    Optional<Order> findFirstByScopeOrderByCreationUtcAsc(OrderScope scope);

    Optional<Order> findFirstByScopeOrderByCreationUtcDesc(OrderScope scope);

    List<Order> findAllByScopeOrderByCreationUtcDesc(OrderScope scope);

    Page<Order> findAllByScope(OrderScope scope, Pageable pageable);

    long countByScope(OrderScope scope);
}
