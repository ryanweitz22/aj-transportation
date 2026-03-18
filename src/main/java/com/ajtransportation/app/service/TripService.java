package com.ajtransportation.app.service;

import com.ajtransportation.app.model.Trip;
import com.ajtransportation.app.repository.TripRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class TripService {

    private final TripRepository tripRepository;

    public TripService(TripRepository tripRepository) {
        this.tripRepository = tripRepository;
    }

    // Fetch all trips for a full week (Mon–Sun) — used by the user-facing calendar
    public List<Trip> getTripsForWeek(LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);
        return tripRepository.findByDateBetween(weekStart, weekEnd);
    }

    // Fetch all trips for a single day — used by admin schedule view (Phase 7)
    public List<Trip> getTripsForDay(LocalDate date) {
        return tripRepository.findByDate(date);
    }

    // Fetch trips for a week, filtered to only AVAILABLE and BOOKED (hides BLOCKED from users)
    public List<Trip> getVisibleTripsForWeek(LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);
        return tripRepository.findByDateBetweenAndStatusNot(weekStart, weekEnd, "BLOCKED");
    }

    // Fetch a single trip by ID
    public Trip getTripById(UUID id) {
        return tripRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trip not found: " + id));
    }

    // Save a new trip to the database (admin only)
    public Trip createTrip(Trip trip) {
        // Default status to AVAILABLE if not set
        if (trip.getStatus() == null || trip.getStatus().isBlank()) {
            trip.setStatus("AVAILABLE");
        }
        return tripRepository.save(trip);
    }

    // Update a trip's status — AVAILABLE / BOOKED / BLOCKED
    public void updateTripStatus(UUID id, String status) {
        Trip trip = getTripById(id);
        trip.setStatus(status);
        tripRepository.save(trip);
    }

    // Block a slot (admin) — sets status to BLOCKED with an optional internal reason
    public void blockTrip(UUID id, String reason) {
        Trip trip = getTripById(id);
        trip.setStatus("BLOCKED");
        trip.setBlockedReason(reason);
        tripRepository.save(trip);
    }

    // Unblock a slot (admin) — sets status back to AVAILABLE
    public void unblockTrip(UUID id) {
        Trip trip = getTripById(id);
        trip.setStatus("AVAILABLE");
        trip.setBlockedReason(null);
        tripRepository.save(trip);
    }

    // Delete a trip entirely (admin only — use with caution)
    public void deleteTrip(UUID id) {
        tripRepository.deleteById(id);
    }

    // Check if a trip is still available to book
    public boolean isTripAvailable(UUID id) {
        Trip trip = getTripById(id);
        return "AVAILABLE".equals(trip.getStatus());
    }

    // Fetch all trips between any two dates — used by admin month view
    public List<Trip> getTripsForRange(LocalDate from, LocalDate to) {
        return tripRepository.findByDateBetween(from, to);
    }
}
