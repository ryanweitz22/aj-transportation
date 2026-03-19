package com.ajtransportation.app.repository;

import com.ajtransportation.app.model.Booking;
import com.ajtransportation.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findByUserOrderByCreatedAtDesc(User user);

    List<Booking> findByUser(User user);

    boolean existsByTripIdAndStatusNot(UUID tripId, String status);

    Optional<Booking> findByTripIdAndStatusNot(UUID tripId, String status);

    // Used by admin pending bookings page
    List<Booking> findByStatusOrderByCreatedAtAsc(String status);
}