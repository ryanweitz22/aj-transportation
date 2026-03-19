package com.ajtransportation.app.service;

import com.ajtransportation.app.model.Booking;
import com.ajtransportation.app.model.Trip;
import com.ajtransportation.app.model.User;
import com.ajtransportation.app.repository.BookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final TripService tripService;

    public BookingService(BookingRepository bookingRepository, TripService tripService) {
        this.bookingRepository = bookingRepository;
        this.tripService = tripService;
    }

    /**
     * Create a booking for a user on a given trip.
     * Booking is immediately CONFIRMED — no pending state.
     * Payment via Ozow will be handled in Phase 9.
     */
    @Transactional
    public Booking createBooking(User user, UUID tripId, String pickupAddress, String dropoffAddress) {
        // Double-check the trip is still available (race condition protection)
        if (!tripService.isTripAvailable(tripId)) {
            throw new RuntimeException("Sorry, that slot is no longer available.");
        }

        Trip trip = tripService.getTripById(tripId);

        // Ensure nobody else has already booked this trip
        boolean alreadyBooked = bookingRepository.existsByTripIdAndStatusNot(tripId, "CANCELLED");
        if (alreadyBooked) {
            throw new RuntimeException("This slot has already been booked.");
        }

        // Build the booking — immediately CONFIRMED (payment is Phase 9)
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setTrip(trip);
        booking.setStatus("CONFIRMED");
        booking.setPaymentStatus("UNPAID");
        booking.setCreatedAt(LocalDateTime.now());

        // Store pickup/dropoff on the trip so admin can see the route
        if (pickupAddress != null && !pickupAddress.isBlank()) {
            trip.setPickupAddress(pickupAddress);
        }
        if (dropoffAddress != null && !dropoffAddress.isBlank()) {
            trip.setDropoffAddress(dropoffAddress);
        }

        // Mark the trip as BOOKED so nobody else can grab it
        tripService.updateTripStatus(tripId, "BOOKED");

        return bookingRepository.save(booking);
    }

    // Backwards-compatible overload (used by admin private booking)
    @Transactional
    public Booking createBooking(User user, UUID tripId) {
        return createBooking(user, tripId, null, null);
    }

    // Get all bookings for a user (newest first) — for user dashboard
    public List<Booking> getUserBookings(User user) {
        return bookingRepository.findByUserOrderByCreatedAtDesc(user);
    }

    // Get all bookings for a specific user — past trips view
    public List<Booking> getPastBookings(User user) {
        return bookingRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .filter(b -> "CONFIRMED".equals(b.getStatus()) || "CANCELLED".equals(b.getStatus()))
                .toList();
    }

    // Get a single booking by ID
    public Booking getBookingById(UUID id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + id));
    }

    // Cancel a booking — frees the trip slot back to AVAILABLE
    @Transactional
    public void cancelBooking(UUID bookingId) {
        Booking booking = getBookingById(bookingId);
        booking.setStatus("CANCELLED");
        bookingRepository.save(booking);
        tripService.updateTripStatus(booking.getTrip().getId(), "AVAILABLE");
    }

    // Confirm a booking after payment (called by payment controller in Phase 9)
    public void confirmBooking(UUID bookingId) {
        Booking booking = getBookingById(bookingId);
        booking.setStatus("CONFIRMED");
        booking.setPaymentStatus("PAID");
        bookingRepository.save(booking);
    }

    // Count confirmed bookings for a user — for dashboard stats
    public long countActiveBookings(User user) {
        return bookingRepository.findByUser(user)
                .stream()
                .filter(b -> "CONFIRMED".equals(b.getStatus()))
                .count();
    }

    // Cancel a booking by trip ID — used by admin dashboard
    @Transactional
    public void cancelBookingByTripId(UUID tripId) {
        bookingRepository.findByTripIdAndStatusNot(tripId, "CANCELLED")
            .ifPresent(booking -> {
                booking.setStatus("CANCELLED");
                bookingRepository.save(booking);
                tripService.updateTripStatus(tripId, "AVAILABLE");
            });
    }
}