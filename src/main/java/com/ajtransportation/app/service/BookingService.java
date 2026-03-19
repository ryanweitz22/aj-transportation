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
     * Creates a booking for an existing admin-created trip (green slot).
     * tripId is a known UUID — trip must exist and be AVAILABLE.
     */
    @Transactional
    public Booking createBooking(User user, UUID tripId, String pickupAddress, String dropoffAddress) {
        if (!tripService.isTripAvailable(tripId)) {
            throw new RuntimeException("Sorry, that slot is no longer available.");
        }

        Trip trip = tripService.getTripById(tripId);

        boolean alreadyBooked = bookingRepository.existsByTripIdAndStatusNot(tripId, "CANCELLED");
        if (alreadyBooked) {
            throw new RuntimeException("This slot has just been taken. Please choose another.");
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setTrip(trip);
        booking.setPickupAddress(pickupAddress);
        booking.setDropoffAddress(dropoffAddress);
        booking.setStatus("PENDING_APPROVAL");
        booking.setPaymentStatus("UNPAID");
        booking.setCreatedAt(LocalDateTime.now());

        // Lock slot to PENDING — shows as amber, not bookable by anyone else
        tripService.updateTripStatus(tripId, "PENDING");

        return bookingRepository.save(booking);
    }

    /**
     * Creates a booking for an open business hours slot (no existing trip).
     * A trip is created on the fly, then immediately booked.
     * date + startTime come from the slot the user clicked on the calendar.
     */
    @Transactional
    public Booking createBookingForOpenSlot(User user, LocalDate date, LocalTime startTime,
                                             String pickupAddress, String dropoffAddress) {
        // Create the trip record on the fly — status starts as PENDING
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
     * Admin accepts a booking — trip moves to BOOKED.
     * Phase 9: Ozow payment triggered here.
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
     * Admin rejects a booking — slot goes back to AVAILABLE.
     */
    @Transactional
    public void rejectBooking(UUID bookingId) {
        Booking booking = getBookingById(bookingId);
        booking.setStatus("REJECTED");
        bookingRepository.save(booking);
        tripService.updateTripStatus(booking.getTrip().getId(), "AVAILABLE");
    }

    /**
     * User cancels before admin responds — slot goes back to AVAILABLE.
     */
    @Transactional
    public void cancelBooking(UUID bookingId) {
        Booking booking = getBookingById(bookingId);
        booking.setStatus("CANCELLED");
        bookingRepository.save(booking);
        tripService.updateTripStatus(booking.getTrip().getId(), "AVAILABLE");
    }

    // Admin cancels by trip ID from admin dashboard
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