package com.ajtransportation.app.repository;

import com.ajtransportation.app.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TripRepository extends JpaRepository<Trip, UUID> {

    // All trips for a specific date
    List<Trip> findByDate(LocalDate date);

    // All trips between two dates (for weekly calendar view)
    List<Trip> findByDateBetween(LocalDate startDate, LocalDate endDate);

    // Only available trips between two dates
    List<Trip> findByDateBetweenAndStatusNot(LocalDate startDate, LocalDate endDate, String status);

    // All trips with a specific status
    List<Trip> findByStatus(String status);
}