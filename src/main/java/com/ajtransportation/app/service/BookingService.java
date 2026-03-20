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

    private static final List<String> INACTIVE_STATUSES = List.of("CANCELLED", "REJECTED");

    // Increased from 60s to 120s to allow for Supabase latency under load
    private static final int EXPIRY_SECONDS = 120;

    public BookingService(BookingRepository bookingRepository, TripService tripService) {
        this.bookingRepository = bookingRepository;
        this.tripService = tripService;
    }

    @Transactional
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

    @Transactional
    public void acceptBooking(UUID bookingId) {
        Booking booking = getBookingById(bookingId);
        booking.setStatus("CONFIRMED");
        booking.setPaymentStatus("AWAITING_PAYMENT");
        bookingRepository.save(booking);
        tripService.updateTripStatus(booking.getTrip().getId(), "BOOKED");
    }

    @Transactional
    public void rejectBooking(UUID bookingId) {
        Booking booking = getBookingById(bookingId);
        Trip trip = booking.getTrip();

        if ("User Request".equals(trip.getLabel())) {
            // Delete booking first (removes FK reference), then delete trip
            bookingRepository.delete(booking);
            tripService.deleteTrip(trip.getId());
        } else {
            booking.setStatus("REJECTED");
            bookingRepository.save(booking);
            tripService.updateTripStatus(trip.getId(), "AVAILABLE");
        }
    }

    @Transactional
    public void cancelBooking(UUID bookingId) {
        Booking booking = getBookingById(bookingId);
        Trip trip = booking.getTrip();

        if ("User Request".equals(trip.getLabel())) {
            bookingRepository.delete(booking);
            tripService.deleteTrip(trip.getId());
        } else {
            booking.setStatus("CANCELLED");
            bookingRepository.save(booking);
            tripService.updateTripStatus(trip.getId(), "AVAILABLE");
        }
    }

    @Transactional
    public void cancelBookingByTripId(UUID tripId) {
        bookingRepository.findByTripIdAndStatusNot(tripId, "CANCELLED")
            .ifPresent(booking -> {
                booking.setStatus("CANCELLED");
                bookingRepository.save(booking);
                tripService.updateTripStatus(tripId, "AVAILABLE");
            });
    }

    @Transactional
    public String getBookingStatusForPolling(UUID bookingId) {
        // Booking deleted (rejected on-the-fly trip) — tell user it was rejected
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) return "REJECTED";

        if ("PENDING_APPROVAL".equals(booking.getStatus())) {
            long secondsElapsed = java.time.Duration.between(
                booking.getCreatedAt(), LocalDateTime.now()).getSeconds();

            if (secondsElapsed >= EXPIRY_SECONDS) {
                Trip trip = booking.getTrip();
                if ("User Request".equals(trip.getLabel())) {
                    bookingRepository.delete(booking);
                    tripService.deleteTrip(trip.getId());
                } else {
                    booking.setStatus("CANCELLED");
                    bookingRepository.save(booking);
                    tripService.updateTripStatus(trip.getId(), "AVAILABLE");
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
                .filter(b -> "PENDING_APPROVAL".equals(b.getStatus()) || "CONFIRMED".equals(b.getStatus()))
                .count();
    }

    @Transactional
    public void confirmBooking(UUID bookingId) {
        Booking booking = getBookingById(bookingId);
        booking.setStatus("CONFIRMED");
        booking.setPaymentStatus("PAID");
        bookingRepository.save(booking);
    }
}