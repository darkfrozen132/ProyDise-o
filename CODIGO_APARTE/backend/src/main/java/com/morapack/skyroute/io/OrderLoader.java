package com.morapack.skyroute.io;


import java.io.IOException;
import java.nio.file.Path;

import java.util.List;

import com.morapack.skyroute.models.*;
import com.morapack.skyroute.orders.repository.OrderRepository;

/**
 * @deprecated Usa {@link Orders} directamente para consumir pedidos. Se mantiene
 *             como adaptador temporal para código existente.
 */
@Deprecated
public class OrderLoader implements AutoCloseable {
    private final Orders delegate;

    private OrderLoader(Orders delegate) {
        this.delegate = delegate;
    }

    public static OrderLoader open(Path path, Airports airports) throws IOException {
        return new OrderLoader(Orders.open(path, airports));
    }

    public Order readNext() throws IOException {
        return delegate.next();
    }


    // --- Método nuevo: cargar desde BD ---
    public static Orders loadFromDatabase(List<Order> entities, Airports airports) {
        Orders orders = new Orders(null,airports);
        if (entities == null) {
            return orders;
        }
        for (Order entity : entities) {
            Airport destination = airports.get(entity.getDestinationCode());
            if (destination == null) continue;

            Order order = new Order(
                entity.getId(),
                entity.getCustomerReference(),
                destination,
                entity.getQuantity(),
                entity.getCreationUtc(),
                entity.getDueUtc(),
                entity.getScope()
            );
            orders.register(order);
        }
        return orders;
    }


    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
