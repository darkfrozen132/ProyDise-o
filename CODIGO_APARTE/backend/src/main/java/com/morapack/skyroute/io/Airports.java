package com.morapack.skyroute.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import com.morapack.skyroute.models.*;;

public class Airports {
    private static final String LATITUDE_TOKEN = "Latitude:";

    private final Map<String, Airport> byCode = new HashMap<>();

    public Airport get(String code) {
        return byCode.get(code);
    }

    public Map<String, Airport> asMap() {
        return Collections.unmodifiableMap(byCode);
    }

    public static Airports load(Path path) throws IOException {
        Airports airports = new Airports();
        List<String> lines = Files.readAllLines(path);
        String currentRegion = null;
        for (String rawLine : lines) {
            String line = rawLine.strip();
            if (line.isEmpty() || !Character.isDigit(line.charAt(0))) {
                if (!line.isEmpty()) {
                    currentRegion = line.trim();
                }
                continue;
            }

            String[] tokens = line.split("\\s+");
            if (tokens.length < 3) {
                continue;
            }

            String code = tokens[1];
            int latitudeIndex = findLatitudeToken(tokens);
            int longitudeIndex = findLongitudeToken(tokens);
            if (latitudeIndex < 2) {
                continue;
            }

            int gmt = safeParse(tokens[latitudeIndex - 2]);
            int capacity = safeParse(tokens[latitudeIndex - 1]);
            if (gmt == Integer.MIN_VALUE || capacity == Integer.MIN_VALUE) {
                continue;
            }

            String latitudeText = latitudeIndex >= 0 && longitudeIndex > latitudeIndex
                    ? joinTokens(tokens, latitudeIndex + 1, longitudeIndex)
                    : "";
            String longitudeText = longitudeIndex >= 0
                    ? joinTokens(tokens, longitudeIndex + 1, tokens.length)
                    : "";

            airports.byCode.put(code, new Airport(
                    code,
                    code,
                    gmt,
                    capacity,
                    currentRegion,
                    parseCoordinate(latitudeText),
                    parseCoordinate(longitudeText)));
        }
        return airports;
    }

    private static int findLatitudeToken(String[] tokens) {
        for (int i = 0; i < tokens.length; i++) {
            if (LATITUDE_TOKEN.equalsIgnoreCase(tokens[i])) {
                return i;
            }
        }
        return -1;
    }

    private static int findLongitudeToken(String[] tokens) {
        for (int i = 0; i < tokens.length; i++) {
            if ("Longitude:".equalsIgnoreCase(tokens[i])) {
                return i;
            }
        }
        return -1;
    }

    private static int safeParse(String text) {
        try {
            return Integer.parseInt(text.replace("\"", ""));
        } catch (NumberFormatException ex) {
            return Integer.MIN_VALUE;
        }
    }

    private static String joinTokens(String[] tokens, int from, int toExclusive) {
        StringJoiner joiner = new StringJoiner(" ");
        for (int i = from; i < toExclusive && i < tokens.length; i++) {
            String token = tokens[i];
            if (token.equalsIgnoreCase("Longitude:")) {
                break;
            }
            joiner.add(token);
        }
        return joiner.toString().trim();
    }



    private static Double parseCoordinate(String text) {
        if (text == null) {
            return null;
        }
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }


    public static Airports fromEntities(List<Airport> entities) {
        Airports airports = new Airports();
        if (entities == null) {
            return airports;
        }
        for (Airport entity : entities) {
            if (entity == null || entity.getCode() == null) {
                continue;
            }
            airports.byCode.put(entity.getCode(), cloneAirport(entity));
        }
        return airports;
    }

    private static Airport cloneAirport(Airport source) {
        return new Airport(
                source.getCode(),
                source.getName(),
                source.getGmtOffsetHours(),
                source.getStorageCapacity(),
                source.getContinent(),
                source.getLatitude(),
                source.getLongitude()
        );
    }
}
