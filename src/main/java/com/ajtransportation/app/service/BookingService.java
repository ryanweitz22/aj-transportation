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

    // Create a new booking for a user on a given trip
    // Returns the saved Booking, or throws if the trip is no longer available
    @Transactional
    public Booking createBooking(User user, UUID tripId) {
        // Double-check the trip is still available (race condition protection)
        if (!tripService.isTripAvailable(tripId)) {
            throw new RuntimeException("Sorry, that slot is no longer available.");
        }

        Trip trip = tripService.getTripById(tripId);

        // Check this user hasn't already booked this same trip
        boolean alreadyBooked = bookingRepository.existsByTripIdAndStatusNot(tripId, "CANCELLED");
        if (alreadyBooked) {
            throw new RuntimeException("This slot has already been booked.");
        }

        // Create the booking record
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setTrip(trip);
        booking.setStatus("PENDING");         // Awaiting payment
        booking.setPaymentStatus("UNPAID");
        booking.setCreatedAt(LocalDateTime.now());

        // Mark the trip as BOOKED so no one else can grab it
        tripService.updateTripStatus(tripId, "BOOKED");

        return bookingRepository.save(booking);
    }

    // Get all bookings for a specific user (newest first) — for user dashboard
    public List<Booking> getUserBookings(User user) {
        return bookingRepository.findByUserOrderByCreatedAtDesc(user);
    }

    // Get all bookings for a specific user — for past trips view
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

        // Free the trip slot back up
        tripService.updateTripStatus(booking.getTrip().getId(), "AVAILABLE");
    }

    // Confirm a booking after payment (called by payment controller in Phase 9)
    public void confirmBooking(UUID bookingId) {
        Booking booking = getBookingById(bookingId);
        booking.setStatus("CONFIRMED");
        booking.setPaymentStatus("PAID");
        bookingRepository.save(booking);
    }

    // Count confirmed/pending bookings for a user — for dashboard stats
    public long countActiveBookings(User user) {
        return bookingRepository.findByUser(user)
                .stream()
                .filter(b -> "CONFIRMED".equals(b.getStatus()) || "PENDING".equals(b.getStatus()))
                .count();
    }
}
