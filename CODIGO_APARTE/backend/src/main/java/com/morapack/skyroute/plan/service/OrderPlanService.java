package com.morapack.skyroute.plan.service;

import com.morapack.skyroute.models.OrderPlan;
import com.morapack.skyroute.models.Route;
import com.morapack.skyroute.plan.repository.OrderPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderPlanService {

    private final OrderPlanRepository orderPlanRepository;

    public List<OrderPlan> findAll() {
        return orderPlanRepository.findAll();
    }

    public Optional<OrderPlan> findById(Long id) {
        return orderPlanRepository.findById(id);
    }


    public List<OrderPlan> findOrderPlansWithRoutes() {
        return orderPlanRepository.findOrderPlansWithRoutes();
    }

    public List<OrderPlan> findByMinimumRoutes(int minRoutes) {
        return orderPlanRepository.findByMinimumRoutes(minRoutes);
    }

    @Transactional
    public OrderPlan save(OrderPlan orderPlan) {
        return orderPlanRepository.save(orderPlan);
    }

    @Transactional
    public OrderPlan create(String status) {
        OrderPlan orderPlan = new OrderPlan(status);
        return orderPlanRepository.save(orderPlan);
    }

    @Transactional
    public OrderPlan updateStatus(Long id, String status) {
        Optional<OrderPlan> optionalOrderPlan = orderPlanRepository.findById(id);
        if (optionalOrderPlan.isPresent()) {
            OrderPlan orderPlan = optionalOrderPlan.get();
            return orderPlanRepository.save(orderPlan);
        }
        return null;
    }

    @Transactional
    public OrderPlan addRoute(Long orderPlanId, Route route) {
        Optional<OrderPlan> optionalOrderPlan = orderPlanRepository.findById(orderPlanId);
        if (optionalOrderPlan.isPresent()) {
            OrderPlan orderPlan = optionalOrderPlan.get();
            orderPlan.addRoute(route);
            return orderPlanRepository.save(orderPlan);
        }
        return null;
    }

    @Transactional
    public OrderPlan removeRoute(Long orderPlanId, Route route) {
        Optional<OrderPlan> optionalOrderPlan = orderPlanRepository.findById(orderPlanId);
        if (optionalOrderPlan.isPresent()) {
            OrderPlan orderPlan = optionalOrderPlan.get();
            return orderPlanRepository.save(orderPlan);
        }
        return null;
    }

    @Transactional
    public void deleteById(Long id) {
        orderPlanRepository.deleteById(id);
    }
}
