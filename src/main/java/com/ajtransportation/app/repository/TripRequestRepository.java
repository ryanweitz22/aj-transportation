package com.ajtransportation.app.repository;

import com.ajtransportation.app.model.TripRequest;
import com.ajtransportation.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface TripRequestRepository extends JpaRepository<TripRequest, UUID> {

    List<TripRequest> findByUserOrderByCreatedAtDesc(User user);

    List<TripRequest> findByStatusOrderByCreatedAtDesc(String status);

    List<TripRequest> findAllByOrderByCreatedAtDesc();

    boolean existsByUserAndRequestedDateAndRequestedStartTimeAndStatusNot(
        User user, LocalDate date, LocalTime time, String status);
}