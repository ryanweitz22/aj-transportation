package com.ajtransportation.app.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * GoogleMapsService — Phase 8
 *
 * Calls the Google Maps Distance Matrix API to get:
 *   - Distance in km between two addresses
 *   - Travel time in minutes (real-world ETA)
 *
 * The API key is stored in application-local.properties (never committed).
 */
@Service
public class GoogleMapsService {

    @Value("${google.maps.api-key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Result holder for a Distance Matrix lookup.
     */
    public static class DistanceResult {
        public final BigDecimal distanceKm;
        public final int etaMinutes;
        public final int bufferedDurationMinutes; // etaMinutes + 15

        public DistanceResult(BigDecimal distanceKm, int etaMinutes) {
            this.distanceKm = distanceKm;
            this.etaMinutes = etaMinutes;
            this.bufferedDurationMinutes = etaMinutes + 15;
        }
    }

    /**
     * Queries Google Maps Distance Matrix API.
     *
     * @param origin      Pickup address (e.g. "12 Long Street, Cape Town")
     * @param destination Dropoff address (e.g. "Cape Town International Airport")
     * @return DistanceResult with km, ETA and buffered duration, or null if API key
     *         is missing / call fails.
     */
    public DistanceResult getDistanceAndEta(String origin, String destination) {
        if (apiKey == null || apiKey.isBlank()) {
            // API key not configured — return null so callers can handle gracefully
            return null;
        }
        if (origin == null || origin.isBlank() || destination == null || destination.isBlank()) {
            return null;
        }

        try {
            String url = buildUrl(origin, destination);
            String response = restTemplate.getForObject(url, String.class);

            JsonNode root = objectMapper.readTree(response);

            // Check top-level status
            String topStatus = root.path("status").asText();
            if (!"OK".equals(topStatus)) {
                return null;
            }

            // Drill into rows[0].elements[0]
            JsonNode element = root
                    .path("rows").get(0)
                    .path("elements").get(0);

            String elementStatus = element.path("status").asText();
            if (!"OK".equals(elementStatus)) {
                return null;
            }

            // Distance in metres → km (2 decimal places)
            long distanceMetres = element.path("distance").path("value").asLong();
            BigDecimal distanceKm = BigDecimal.valueOf(distanceMetres)
                    .divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);

            // Duration in seconds → minutes (rounded up)
            long durationSeconds = element.path("duration").path("value").asLong();
            int etaMinutes = (int) Math.ceil(durationSeconds / 60.0);

            return new DistanceResult(distanceKm, etaMinutes);

        } catch (Exception e) {
            // Log and return null — callers must handle gracefully
            System.err.println("GoogleMapsService error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Calculates trip fee: max(distanceKm × ratePerKm, minimumFare).
     *
     * @param distanceKm   Distance from Google Maps
     * @param ratePerKm    Rate set by admin (e.g. R8.00/km)
     * @param minimumFare  Minimum charge regardless of distance
     * @return Calculated fee rounded to 2 decimal places
     */
    public BigDecimal calculateFee(BigDecimal distanceKm, BigDecimal ratePerKm, BigDecimal minimumFare) {
        if (distanceKm == null || ratePerKm == null) {
            return minimumFare != null ? minimumFare : BigDecimal.ZERO;
        }

        BigDecimal calculatedFee = distanceKm.multiply(ratePerKm).setScale(2, RoundingMode.HALF_UP);

        if (minimumFare != null && calculatedFee.compareTo(minimumFare) < 0) {
            return minimumFare.setScale(2, RoundingMode.HALF_UP);
        }

        return calculatedFee;
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private String buildUrl(String origin, String destination) {
        String encodedOrigin      = urlEncode(origin);
        String encodedDestination = urlEncode(destination);
        return "https://maps.googleapis.com/maps/api/distancematrix/json"
                + "?origins="       + encodedOrigin
                + "&destinations="  + encodedDestination
                + "&units=metric"
                + "&key="           + apiKey;
    }

    private String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value.replace(" ", "+");
        }
    }
}
