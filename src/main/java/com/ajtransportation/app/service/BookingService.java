package com.ajtransportation.app.service;

import com.ajtransportation.app.model.Booking;
import com.ajtransportation.app.model.Trip;
import com.ajtransportation.app.model.User;
import com.ajtransportation.app.repository.BookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
     * User books an existing admin-created trip (green slot).
     */
    @Transactional
    public Booking createBooking(User user, UUID tripId, String pickupAddress, String dropoffAddress) {
        if (!tripService.isTripAvailable(tripId)) {
            throw new RuntimeException("Sorry, that slot is no longer available.");
        }

        // Use the fixed query that excludes both CANCELLED and REJECTED
        if (bookingRepository.existsActiveBookingForTrip(tripId)) {
            throw new RuntimeException("This slot has just been taken. Please choose another.");
        }

        Trip trip = tripService.getTripById(tripId);

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setTrip(trip);
        booking.setPickupAddress(pickupAddress);
        booking.setDropoffAddress(dropoffAddress);
        booking.setStatus("PENDING_APPROVAL");
        booking.setPaymentStatus("UNPAID");
        booking.setCreatedAt(LocalDateTime.now());

        // Lock slot — shows as amber on calendar, not bookable by anyone else
        tripService.updateTripStatus(tripId, "PENDING");

        return bookingRepository.save(booking);
    }

    /**
     * User books an open business hours slot (teal ghost slot).
     * Creates a trip on the fly then immediately creates the booking.
     */
    @Transactional
    public Booking createBookingForOpenSlot(User user, LocalDate date, LocalTime startTime,
                                             String pickupAddress, String dropoffAddress) {
        Trip trip = tripService.createOnTheFlyTrip(date, startTime, pickupAddress, dropoffAddress);

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setTrip(trip);
        booking.setPickupAddress(pickupAddress);
        booking.setDropoffAddress(dropoffAddress);
        booking.setStatus("PENDING_APPROVAL");
        booking.setPaymentStatus("UNPAID");
        booking.setCreatedAt(LocalDateTime.now());

        return bookingRepository.save(booking);
    }

    /**
     * Admin accepts a booking.
     * Trip moves to BOOKED. Phase 9: Ozow payment triggered here.
     */
    @Transactional
    public void acceptBooking(UUID bookingId) {
        Booking booking = getBookingById(bookingId);
        booking.setStatus("CONFIRMED");
        booking.setPaymentStatus("AWAITING_PAYMENT");
        bookingRepository.save(booking);
        tripService.updateTripStatus(booking.getTrip().getId(), "BOOKED");
        // Phase 9: trigger Ozow payment here
    }

    /**
     * Admin rejects a booking.
     * - If the trip was created on the fly (label = "User Request"), delete it entirely
     *   so it doesn't pollute the calendar or block future bookings.
     * - If it was an admin-created trip, just set it back to AVAILABLE.
     */
    @Transactional
    public void rejectBooking(UUID bookingId) {
        Booking booking = getBookingById(bookingId);
        Trip trip = booking.getTrip();

        booking.setStatus("REJECTED");
        bookingRepository.save(booking);

        if ("User Request".equals(trip.getLabel())) {
            // On-the-fly trip — delete it so the open slot is clean again
            tripService.deleteTrip(trip.getId());
        } else {
            // Admin-created trip — just free the slot back to available
            tripService.updateTripStatus(trip.getId(), "AVAILABLE");
        }
    }

    /**
     * User cancels before admin responds.
     * Same logic as reject — delete on-the-fly trips, free admin trips.
     */
    @Transactional
    public void cancelBooking(UUID bookingId) {
        Booking booking = getBookingById(bookingId);
        Trip trip = booking.getTrip();

        booking.setStatus("CANCELLED");
        bookingRepository.save(booking);

        if ("User Request".equals(trip.getLabel())) {
            tripService.deleteTrip(trip.getId());
        } else {
            tripService.updateTripStatus(trip.getId(), "AVAILABLE");
        }
    }

    // Admin cancels a booking by trip ID from admin dashboard
    @Transactional
    public void cancelBookingByTripId(UUID tripId) {
        bookingRepository.findByTripIdAndStatusNot(tripId, "CANCELLED")
            .ifPresent(booking -> {
                booking.setStatus("CANCELLED");
                bookingRepository.save(booking);
                tripService.updateTripStatus(tripId, "AVAILABLE");
            });
    }

    // All bookings awaiting admin approval
    public List<Booking> getPendingBookings() {
        return bookingRepository.findByStatusOrderByCreatedAtAsc("PENDING_APPROVAL");
    }

    public Booking getBookingById(UUID id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + id));
    }

    public List<Booking> getUserBookings(User user) {
        return bookingRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public long countActiveBookings(User user) {
        return bookingRepository.findByUser(user)
                .stream()
                .filter(b -> "PENDING_APPROVAL".equals(b.getStatus()) || "CONFIRMED".equals(b.getStatus()))
                .count();
    }

    public void confirmBooking(UUID bookingId) {
        Booking booking = getBookingById(bookingId);
        booking.setStatus("CONFIRMED");
        booking.setPaymentStatus("PAID");
        bookingRepository.save(booking);
    }
}