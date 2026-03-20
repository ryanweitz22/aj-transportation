package com.ajtransportation.app.service;

import com.ajtransportation.app.model.PricingConfig;
import com.ajtransportation.app.model.Trip;
import com.ajtransportation.app.repository.PricingConfigRepository;
import com.ajtransportation.app.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
public class TripService {

    private static final BigDecimal DEFAULT_RATE_PER_KM  = new BigDecimal("8.00");
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

    public List<Trip> getTripsForWeek(LocalDate weekStart) {
        return tripRepository.findByDateBetween(weekStart, weekStart.plusDays(6));
    }

    public List<Trip> getTripsForDay(LocalDate date) {
        return tripRepository.findByDate(date);
    }

    public List<Trip> getTripsForRange(LocalDate from, LocalDate to) {
        return tripRepository.findByDateBetween(from, to);
    }

    /**
     * Returns ALL trips for the week including BLOCKED ones.
     *
     * Previously this used findByDateBetweenAndStatusNot(..., "BLOCKED") which
     * excluded BLOCKED trips entirely. When a blocked trip was excluded, the
     * ghost-slot logic in calendar.js saw nothing covering that time and rendered
     * a bookable teal slot there — making blocked times appear available to users.
     *
     * Fix: return every status. The JS calendar renders BLOCKED as grey and
     * suppresses ghost slots for that time window.
     */
    public List<Trip> getVisibleTripsForWeek(LocalDate weekStart) {
        return tripRepository.findByDateBetween(weekStart, weekStart.plusDays(6));
    }

    public Trip getTripById(UUID id) {
        return tripRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trip not found: " + id));
    }

    public boolean isTripAvailable(UUID id) {
        return "AVAILABLE".equals(getTripById(id).getStatus());
    }

    @Transactional
    public Trip createTrip(Trip trip) {
        if (trip.getStatus() == null || trip.getStatus().isBlank()) {
            trip.setStatus("AVAILABLE");
        }
        if (hasAddresses(trip)) {
            enrichTripWithGoogleMaps(trip);
        }
        return tripRepository.save(trip);
    }

    @Transactional
    public Trip createOnTheFlyTrip(LocalDate date, LocalTime startTime,
                                    String pickupAddress, String dropoffAddress) {
        Trip trip = new Trip();
        trip.setDate(date);
        trip.setStartTime(startTime);
        trip.setPickupAddress(pickupAddress);
        trip.setDropoffAddress(dropoffAddress);
        trip.setStatus("PENDING");
        trip.setLabel("User Request");

        if (hasAddresses(trip)) {
            enrichTripWithGoogleMaps(trip);
        }

        if (trip.getFee() == null) {
            trip.setFee(getPricingConfig().getMinimumFare());
        }

        return tripRepository.save(trip);
    }

    @Transactional
    public void updateTripStatus(UUID id, String status) {
        Trip trip = getTripById(id);
        trip.setStatus(status);
        tripRepository.save(trip);
    }

    @Transactional
    public void blockTrip(UUID id, String reason) {
        Trip trip = getTripById(id);
        trip.setStatus("BLOCKED");
        trip.setBlockedReason(reason);
        tripRepository.save(trip);
    }

    @Transactional
    public void unblockTrip(UUID id) {
        Trip trip = getTripById(id);
        trip.setStatus("AVAILABLE");
        trip.setBlockedReason(null);
        tripRepository.save(trip);
    }

    @Transactional
    public void deleteTrip(UUID id) {
        tripRepository.deleteById(id);
    }

    public PricingConfig getPricingConfig() {
        return pricingConfigRepository.findById(1).orElseGet(() -> {
            PricingConfig defaults = new PricingConfig();
            defaults.setId(1);
            defaults.setRatePerKm(DEFAULT_RATE_PER_KM);
            defaults.setMinimumFare(DEFAULT_MINIMUM_FARE);
            return defaults;
        });
    }

    @Transactional
    public void savePricingConfig(BigDecimal ratePerKm, BigDecimal minimumFare) {
        PricingConfig config = getPricingConfig();
        config.setRatePerKm(ratePerKm);
        config.setMinimumFare(minimumFare);
        config.setUpdatedAt(java.time.LocalDateTime.now());
        pricingConfigRepository.save(config);
    }

    private void enrichTripWithGoogleMaps(Trip trip) {
        GoogleMapsService.DistanceResult result =
                googleMapsService.getDistanceAndEta(trip.getPickupAddress(), trip.getDropoffAddress());

        if (result == null) return;

        trip.setDistanceKm(result.distanceKm);
        trip.setGoogleEtaMinutes(result.etaMinutes);
        trip.setBufferedDurationMinutes(result.bufferedDurationMinutes);

        if (trip.getStartTime() != null) {
            trip.setEndTime(trip.getStartTime().plusMinutes(result.bufferedDurationMinutes));
        }

        PricingConfig config = getPricingConfig();
        trip.setFee(googleMapsService.calculateFee(
                result.distanceKm, config.getRatePerKm(), config.getMinimumFare()));
    }

    private boolean hasAddresses(Trip trip) {
        return trip.getPickupAddress() != null && !trip.getPickupAddress().isBlank()
                && trip.getDropoffAddress() != null && !trip.getDropoffAddress().isBlank();
    }
}