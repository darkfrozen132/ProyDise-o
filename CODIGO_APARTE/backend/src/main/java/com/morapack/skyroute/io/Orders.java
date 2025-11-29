package com.morapack.skyroute.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.morapack.skyroute.config.*;
import com.morapack.skyroute.models.*;

public class Orders implements AutoCloseable {
    private final BufferedReader reader;
    private final Airports airports;
    private final List<Order> activeOrders = new ArrayList<>();
    private final Map<String, Order> byId = new HashMap<>();
    private Observer observer;

    public Orders(BufferedReader reader, Airports airports) {
        this.reader = reader;
        this.airports = airports;
    }

    public static Orders open(Path path, Airports airports) throws IOException {
        BufferedReader bufferedReader = Files.newBufferedReader(path);
        return new Orders(bufferedReader, airports);
    }

    public Order next() throws IOException {
        String rawLine;
        while ((rawLine = reader.readLine()) != null) {
            String line = rawLine.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] parts = line.split("-");
            if (parts.length < 7) {
                continue;
            }

            try {
                String orderId = parts[0];
                String dateToken = parts[1];
                int hour = Integer.parseInt(parts[2]);
                int minute = Integer.parseInt(parts[3]);
                String destinationCode = parts[4];
                int quantity = Integer.parseInt(parts[5]);
                String customerReference = parts.length > 6 ? parts[6] : orderId;

                Airport destination = airports.get(destinationCode);
                if (destination == null) {
                    continue;
                }

                LocalDate date = parseDateToken(dateToken);
                if (date == null) {
                    continue;
                }

                LocalDateTime creationLocal = LocalDateTime.of(date.getYear(), date.getMonthValue(), date.getDayOfMonth(), hour, minute);
                ZoneOffset offset = destination.getZoneOffset();
                var creationUtc = creationLocal.toInstant(offset);
                var dueUtc = creationUtc.plus(Config.INTERCONTINENTAL_SLA_HOURS, ChronoUnit.HOURS);

                Order order = new Order(orderId, customerReference, destination, quantity, creationUtc, dueUtc);
                register(order);
                return order;
            } catch (NumberFormatException | DateTimeException ignored) {
                // Skip malformed lines.
            }
        }
        return null;
    }

    public void setObserver(Observer observer) {
        this.observer = observer;
    }

    public List<Order> getActiveOrders() {
        return Collections.unmodifiableList(activeOrders);
    }

    public Order getById(String id) {
        return byId.get(id);
    }

    public int size() {
        return activeOrders.size();
    }

    public List<Order> pruneExpired(Instant now) {
        List<Order> removed = new ArrayList<>();
        Iterator<Order> it = activeOrders.iterator();
        while (it.hasNext()) {
            Order order = it.next();
            if (order.getDueUtc().isBefore(now)) {
                removed.add(order);
                it.remove();
                byId.remove(order.getId());
            }
        }
        return removed;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    void register(Order order) {
        activeOrders.add(order);
        byId.put(order.getId(), order);
        if (observer != null) {
            observer.onOrderRead(order);
        }
    }

    private LocalDate parseDateToken(String token) {
        if (token == null || token.length() != 8) {
            return null;
        }
        try {
            int year = Integer.parseInt(token.substring(0, 4));
            int month = Integer.parseInt(token.substring(4, 6));
            int day = Integer.parseInt(token.substring(6, 8));
            return LocalDate.of(year, month, day);
        } catch (NumberFormatException | DateTimeException ex) {
            return null;
        }
    }

    public interface Observer {
        void onOrderRead(Order order);
    }
}
