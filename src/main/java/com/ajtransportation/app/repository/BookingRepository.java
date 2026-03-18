package com.ajtransportation.app.repository;

import com.ajtransportation.app.model.Booking;
import com.ajtransportation.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    // All bookings for a specific user
    List<Booking> findByUser(User user);

    // All bookings for a specific user, newest first
    List<Booking> findByUserOrderByCreatedAtDesc(User user);

    // Check if a trip already has an active booking
    boolean existsByTripIdAndStatusNot(UUID tripId, String status);

    // Find the active booking for a specific trip (used by admin cancel)
    java.util.Optional<Booking> findByTripIdAndStatusNot(UUID tripId, String status);
}