package com.morapack.skyroute.orders.controller;

import com.morapack.skyroute.models.Order;
import com.morapack.skyroute.models.OrderScope;
import com.morapack.skyroute.orders.dto.OrderBatchRequest;
import com.morapack.skyroute.orders.dto.OrderCountResponse;
import com.morapack.skyroute.orders.dto.OrderRequest;
import com.morapack.skyroute.orders.dto.PagedOrdersResponse;
import com.morapack.skyroute.orders.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @GetMapping
    public PagedOrdersResponse getAll(@RequestParam(name = "scope", required = false) OrderScope scope,
                                      @RequestParam(name = "page", required = false, defaultValue = "0") Integer page,
                                      @RequestParam(name = "size", required = false) Integer size) {
        var resultPage = service.getPage(scope, page, size);
        return new PagedOrdersResponse(
                resultPage.getContent(),
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements(),
                resultPage.getTotalPages()
        );
    }

    @PostMapping
    public ResponseEntity<Order> create(@RequestBody OrderRequest request) {
        Order created = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/batch")
    public ResponseEntity<List<Order>> createBatch(@RequestBody OrderBatchRequest batchRequest) {
        List<Order> created = service.createAll(batchRequest != null ? batchRequest.orders() : null);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public Order update(@PathVariable String id, @RequestBody OrderRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        service.delete(id);
    }

    @GetMapping("/count")
    public OrderCountResponse count(@RequestParam(name = "scope", required = false) OrderScope scope) {
        return new OrderCountResponse(scope, service.count(scope));
    }
}
