package com.ajtransportation.app.repository;

import com.ajtransportation.app.model.Booking;
import com.ajtransportation.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findByUserOrderByCreatedAtDesc(User user);

    List<Booking> findByUser(User user);

    // Fixed: excludes both CANCELLED and REJECTED so rejected bookings
    // don't block future bookings on the same trip
    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.trip.id = :tripId " +
           "AND b.status NOT IN ('CANCELLED', 'REJECTED')")
    boolean existsActiveBookingForTrip(@Param("tripId") UUID tripId);

    Optional<Booking> findByTripIdAndStatusNot(UUID tripId, String status);

    // Used by admin pending bookings page
    List<Booking> findByStatusOrderByCreatedAtAsc(String status);
}