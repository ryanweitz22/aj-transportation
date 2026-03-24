package com.ajtransportation.app.service;

import com.ajtransportation.app.model.Booking;
import com.ajtransportation.app.model.Trip;
import com.ajtransportation.app.model.User;
import com.ajtransportation.app.repository.BookingRepository;
import com.ajtransportation.app.repository.TripRepository;
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
    private final TripRepository    tripRepository;
    private final TripService       tripService;

    private static final List<String> INACTIVE_STATUSES = List.of("CANCELLED", "REJECTED");
    private static final int          EXPIRY_SECONDS    = 120;

    public BookingService(BookingRepository bookingRepository,
                          TripRepository tripRepository,
                          TripService tripService) {
        this.bookingRepository = bookingRepository;
        this.tripRepository    = tripRepository;
        this.tripService       = tripService;
    }

    @Transactional
    public Booking createBooking(User user, UUID tripId,
                                 String pickupAddress, String dropoffAddress) {
        Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new RuntimeException("That slot no longer exists."));

        if (!"AVAILABLE".equals(trip.getStatus())) {
            throw new RuntimeException("Sorry, that slot is no longer available.");
        }
        if (bookingRepository.existsByTripIdAndStatusNotInOrderByCreatedAtAsc(tripId, INACTIVE_STATUSES)) {
            throw new RuntimeException("This slot has just been taken. Please choose another.");
        }

        trip.setStatus("PENDING");
        tripRepository.save(trip);

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
     * Admin accepts a TODAY booking.
     * Sets status to AWAITING_PAYMENT — this is what the user's waiting screen
     * polls for. Once detected, the JS redirects the user to /payment/initiate/{id}
     * which sends them to Ozow to complete payment.
     * Trip is marked BOOKED immediately to hold the slot.
     */
    @Transactional
    public void acceptBooking(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

        Trip trip = booking.getTrip();

        booking.setStatus("AWAITING_PAYMENT");
        booking.setPaymentStatus("AWAITING_PAYMENT");
        bookingRepository.save(booking);

        trip.setStatus("BOOKED");
        tripRepository.save(trip);
    }

    /**
     * Admin rejects a booking.
     * On-the-fly trips are deleted entirely.
     * Admin-created trips are marked REJECTED and slot restored to AVAILABLE.
     */
    @Transactional
    public void rejectBooking(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

        Trip trip = booking.getTrip();

        if ("User Request".equals(trip.getLabel())) {
            bookingRepository.delete(booking);
            bookingRepository.flush();
            tripRepository.deleteById(trip.getId());
        } else {
            booking.setStatus("REJECTED");
            bookingRepository.save(booking);
            trip.setStatus("AVAILABLE");
            tripRepository.save(trip);
        }
    }

    @Transactional
    public void cancelBooking(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

        Trip trip = booking.getTrip();

        if ("User Request".equals(trip.getLabel())) {
            bookingRepository.delete(booking);
            bookingRepository.flush();
            tripRepository.deleteById(trip.getId());
        } else {
            booking.setStatus("CANCELLED");
            bookingRepository.save(booking);
            trip.setStatus("AVAILABLE");
            tripRepository.save(trip);
        }
    }

    @Transactional
    public void cancelBookingByTripId(UUID tripId) {
        bookingRepository.findByTripIdAndStatusNot(tripId, "CANCELLED")
            .ifPresent(booking -> {
                booking.setStatus("CANCELLED");
                bookingRepository.save(booking);
                tripRepository.findById(tripId).ifPresent(trip -> {
                    trip.setStatus("AVAILABLE");
                    tripRepository.save(trip);
                });
            });
    }

    /**
     * Polling endpoint used by booking-waiting.html every 3 seconds.
     *
     * Status flow the waiting screen cares about:
     *   PENDING_APPROVAL → still waiting, check expiry
     *   AWAITING_PAYMENT → admin accepted, JS will redirect user to /payment/initiate/{id}
     *   REJECTED         → admin rejected, show rejected message
     *   EXPIRED          → timed out, show expired message
     *   anything else    → show cancelled message
     */
    @Transactional
    public String getBookingStatusForPolling(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) return "REJECTED";

        if ("PENDING_APPROVAL".equals(booking.getStatus())) {
            long secondsElapsed = java.time.Duration.between(
                booking.getCreatedAt(), LocalDateTime.now()).getSeconds();

            if (secondsElapsed >= EXPIRY_SECONDS) {
                Trip trip = booking.getTrip();
                if ("User Request".equals(trip.getLabel())) {
                    bookingRepository.delete(booking);
                    bookingRepository.flush();
                    tripRepository.deleteById(trip.getId());
                } else {
                    booking.setStatus("CANCELLED");
                    bookingRepository.save(booking);
                    trip.setStatus("AVAILABLE");
                    tripRepository.save(trip);
                }
                return "EXPIRED";
            }
        }

        return booking.getStatus();
    }

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
            .filter(b -> "PENDING_APPROVAL".equals(b.getStatus())
                      || "AWAITING_PAYMENT".equals(b.getStatus())
                      || "CONFIRMED".equals(b.getStatus()))
            .count();
    }

    @Transactional
    public void confirmBooking(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));
        booking.setStatus("CONFIRMED");
        booking.setPaymentStatus("PAID");
        bookingRepository.save(booking);
    }
}