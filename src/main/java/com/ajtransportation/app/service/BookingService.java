package com.ajtransportation.app.service;

import com.ajtransportation.app.model.Booking;
import com.ajtransportation.app.model.Trip;
import com.ajtransportation.app.model.User;
import com.ajtransportation.app.repository.BookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
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

    private static final List<String> INACTIVE_STATUSES = List.of("CANCELLED", "REJECTED");

    public BookingService(BookingRepository bookingRepository, TripService tripService) {
        this.bookingRepository = bookingRepository;
        this.tripService = tripService;
    }

    /**
     * Creates a booking for an admin-created trip slot.
     * READ_COMMITTED prevents two users booking the same slot simultaneously.
     * rollbackFor = Exception.class ensures ANY failure rolls back cleanly —
     * this is the key fix to stop Supabase connections getting stuck in
     * an aborted transaction state (SQLState 25P02).
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public Booking createBooking(User user, UUID tripId, String pickupAddress, String dropoffAddress) {
        if (!tripService.isTripAvailable(tripId)) {
            throw new RuntimeException("Sorry, that slot is no longer available.");
        }
        if (bookingRepository.existsByTripIdAndStatusNotInOrderByCreatedAtAsc(tripId, INACTIVE_STATUSES)) {
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

        tripService.updateTripStatus(tripId, "PENDING");

        return bookingRepository.save(booking);
    }

    /**
     * Creates a booking for an open business hours slot (trip created on the fly).
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
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
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void acceptBooking(UUID bookingId) {
        Booking booking = getBookingById(bookingId);
        booking.setStatus("CONFIRMED");
        booking.setPaymentStatus("AWAITING_PAYMENT");
        bookingRepository.save(booking);
        tripService.updateTripStatus(booking.getTrip().getId(), "BOOKED");
        // Phase 9: trigger Ozow payment here
    }

    /**
     * Admin rejects a booking — slot freed back to AVAILABLE.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void rejectBooking(UUID bookingId) {
        Booking booking = getBookingById(bookingId);
        Trip trip = booking.getTrip();
        booking.setStatus("REJECTED");
        bookingRepository.save(booking);
        if ("User Request".equals(trip.getLabel())) {
            tripService.deleteTrip(trip.getId());
        } else {
            tripService.updateTripStatus(trip.getId(), "AVAILABLE");
        }
    }

    /**
     * User or system cancels a booking.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
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

    /**
     * Admin cancels a booking by trip ID.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void cancelBookingByTripId(UUID tripId) {
        bookingRepository.findByTripIdAndStatusNot(tripId, "CANCELLED")
            .ifPresent(booking -> {
                booking.setStatus("CANCELLED");
                bookingRepository.save(booking);
                tripService.updateTripStatus(tripId, "AVAILABLE");
            });
    }

    /**
     * Polled every 3 seconds by the user waiting screen.
     * Auto-cancels and frees the slot if admin hasn't responded within 60 seconds.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public String getBookingStatusForPolling(UUID bookingId) {
        Booking booking = getBookingById(bookingId);

        if ("PENDING_APPROVAL".equals(booking.getStatus())) {
            long secondsElapsed = java.time.Duration.between(
                booking.getCreatedAt(), LocalDateTime.now()).getSeconds();

            if (secondsElapsed >= 60) {
                Trip trip = booking.getTrip();
                booking.setStatus("CANCELLED");
                bookingRepository.save(booking);
                if ("User Request".equals(trip.getLabel())) {
                    tripService.deleteTrip(trip.getId());
                } else {
                    tripService.updateTripStatus(trip.getId(), "AVAILABLE");
                }
                return "EXPIRED";
            }
        }

        return booking.getStatus();
    }

    /**
     * Read-only queries — readOnly = true means Spring never opens a write
     * transaction, reducing load on Supabase's 2-connection free tier pool.
     */
    @Transactional(readOnly = true)
    public List<Booking> getPendingBookings() {
        return bookingRepository.findByStatusOrderByCreatedAtAsc("PENDING_APPROVAL");
    }

    @Transactional(readOnly = true)
    public Booking getBookingById(UUID id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Booking> getUserBookings(User user) {
        return bookingRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public long countActiveBookings(User user) {
        return bookingRepository.findByUser(user)
                .stream()
                .filter(b -> "PENDING_APPROVAL".equals(b.getStatus()) || "CONFIRMED".equals(b.getStatus()))
                .count();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void confirmBooking(UUID bookingId) {
        Booking booking = getBookingById(bookingId);
        booking.setStatus("CONFIRMED");
        booking.setPaymentStatus("PAID");
        bookingRepository.save(booking);
    }
}