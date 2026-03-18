package com.ajtransportation.app.service;

import com.ajtransportation.app.model.PricingConfig;
import com.ajtransportation.app.model.Trip;
import com.ajtransportation.app.repository.PricingConfigRepository;
import com.ajtransportation.app.repository.TripRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * TripService — Phase 6 + Phase 8
 *
 * Handles all trip CRUD, status changes, and (Phase 8) Google Maps
 * distance/ETA/fee auto-calculation when pickup + dropoff addresses are supplied.
 */
@Service
public class TripService {

    // Default rate per km (R8.00) — used as fallback when pricing_config row is absent
    private static final BigDecimal DEFAULT_RATE_PER_KM = new BigDecimal("8.00");
    private static final BigDecimal DEFAULT_MINIMUM_FARE = new BigDecimal("50.00");

    private final TripRepository tripRepository;
    private final PricingConfigRepository pricingConfigRepository;
    private final GoogleMapsService googleMapsService;

    public TripService(TripRepository tripRepository,
                       PricingConfigRepository pricingConfigRepository,
                       GoogleMapsService googleMapsService) {
        this.tripRepository = tripRepository;
        this.pricingConfigRepository = pricingConfigRepository;
        this.googleMapsService = googleMapsService;
    }

    // ── Calendar queries ───────────────────────────────────────────────────────

    /** All trips for a full week (Mon–Sun) — used by admin schedule view. */
    public List<Trip> getTripsForWeek(LocalDate weekStart) {
        return tripRepository.findByDateBetween(weekStart, weekStart.plusDays(6));
    }

    /** All trips for a single day — used by admin day view. */
    public List<Trip> getTripsForDay(LocalDate date) {
        return tripRepository.findByDate(date);
    }

    /** Trips for any date range — admin month view. */
    public List<Trip> getTripsForRange(LocalDate from, LocalDate to) {
        return tripRepository.findByDateBetween(from, to);
    }

    /**
     * Visible trips for the user-facing calendar (hides BLOCKED slots).
     * Returns AVAILABLE and BOOKED only.
     */
    public List<Trip> getVisibleTripsForWeek(LocalDate weekStart) {
        return tripRepository.findByDateBetweenAndStatusNot(weekStart, weekStart.plusDays(6), "BLOCKED");
    }

    /** Single trip by ID — throws if not found. */
    public Trip getTripById(UUID id) {
        return tripRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trip not found: " + id));
    }

    // ── Create / Save ──────────────────────────────────────────────────────────

    /**
     * Saves a new trip.
     *
     * If pickup + dropoff addresses are provided:
     *   1. Calls Google Maps Distance Matrix API for real km and ETA.
     *   2. Adds 15-minute buffer to ETA to set endTime automatically.
     *   3. Calculates fee using price-per-km algorithm.
     *
     * If addresses are missing or Google Maps call fails, the trip is saved
     * with whatever values the admin manually entered.
     */
    public Trip createTrip(Trip trip) {
        if (trip.getStatus() == null || trip.getStatus().isBlank()) {
            trip.setStatus("AVAILABLE");
        }

        // Only attempt Google Maps enrichment when both addresses are present
        if (hasAddresses(trip)) {
            enrichTripWithGoogleMaps(trip);
        }

        return tripRepository.save(trip);
    }

    // ── Status updates ─────────────────────────────────────────────────────────

    public void updateTripStatus(UUID id, String status) {
        Trip trip = getTripById(id);
        trip.setStatus(status);
        tripRepository.save(trip);
    }

    public void blockTrip(UUID id, String reason) {
        Trip trip = getTripById(id);
        trip.setStatus("BLOCKED");
        trip.setBlockedReason(reason);
        tripRepository.save(trip);
    }

    public void unblockTrip(UUID id) {
        Trip trip = getTripById(id);
        trip.setStatus("AVAILABLE");
        trip.setBlockedReason(null);
        tripRepository.save(trip);
    }

    public void deleteTrip(UUID id) {
        tripRepository.deleteById(id);
    }

    public boolean isTripAvailable(UUID id) {
        return "AVAILABLE".equals(getTripById(id).getStatus());
    }

    // ── Google Maps enrichment ─────────────────────────────────────────────────

    /**
     * Calls Google Maps and enriches the trip with:
     *  - distanceKm
     *  - googleEtaMinutes
     *  - bufferedDurationMinutes (ETA + 15)
     *  - endTime (startTime + bufferedDurationMinutes)
     *  - fee (distanceKm × ratePerKm, floored at minimumFare)
     */
    private void enrichTripWithGoogleMaps(Trip trip) {
        GoogleMapsService.DistanceResult result =
                googleMapsService.getDistanceAndEta(trip.getPickupAddress(), trip.getDropoffAddress());

        if (result == null) {
            // Google Maps call failed or API key not set — keep admin-entered values as-is
            return;
        }

        // Store distance and timing
        trip.setDistanceKm(result.distanceKm);
        trip.setGoogleEtaMinutes(result.etaMinutes);
        trip.setBufferedDurationMinutes(result.bufferedDurationMinutes);

        // Auto-set endTime from startTime + buffered duration
        if (trip.getStartTime() != null) {
            LocalTime endTime = trip.getStartTime().plusMinutes(result.bufferedDurationMinutes);
            trip.setEndTime(endTime);
        }

        // Calculate fee using price-per-km algorithm
        PricingConfig config = getPricingConfig();
        BigDecimal fee = googleMapsService.calculateFee(
                result.distanceKm,
                config.getRatePerKm(),
                config.getMinimumFare()
        );
        trip.setFee(fee);
    }

    // ── Pricing config ─────────────────────────────────────────────────────────

    /** Loads the single pricing_config row (id=1), with safe defaults if absent. */
    public PricingConfig getPricingConfig() {
        return pricingConfigRepository.findById(1).orElseGet(() -> {
            PricingConfig defaults = new PricingConfig();
            defaults.setId(1);
            defaults.setRatePerKm(DEFAULT_RATE_PER_KM);
            defaults.setMinimumFare(DEFAULT_MINIMUM_FARE);
            return defaults;
        });
    }

    /** Saves updated pricing config (admin only). */
    public void savePricingConfig(BigDecimal ratePerKm, BigDecimal minimumFare) {
        PricingConfig config = getPricingConfig();
        config.setRatePerKm(ratePerKm);
        config.setMinimumFare(minimumFare);
        config.setUpdatedAt(java.time.LocalDateTime.now());
        pricingConfigRepository.save(config);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private boolean hasAddresses(Trip trip) {
        return trip.getPickupAddress() != null && !trip.getPickupAddress().isBlank()
                && trip.getDropoffAddress() != null && !trip.getDropoffAddress().isBlank();
    }
}
