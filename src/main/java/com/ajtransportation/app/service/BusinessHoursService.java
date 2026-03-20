package com.ajtransportation.app.service;

import org.springframework.stereotype.Service;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

@Service
public class BusinessHoursService {

    public boolean isOpenDay(LocalDate date) {
        // Open every day — Sunday included
        return true;
    }

    public LocalTime openTime(LocalDate date) {
        // 04:00 every day
        return LocalTime.of(4, 0);
    }

    public LocalTime closeTime(LocalDate date) {
        // 23:00 every day
        return LocalTime.of(23, 0);
    }

    public boolean isWithinBusinessHours(LocalDate date, LocalTime time) {
        LocalTime open  = openTime(date);
        LocalTime close = closeTime(date);
        if (open == null) return false;
        return !time.isBefore(open) && time.isBefore(close);
    }

    /**
     * Returns true if the given date+time is in the past.
     * Used to block bookings on today's date for times that have already passed.
     */
    public boolean isPastSlot(LocalDate date, LocalTime time) {
        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) return true;
        if (date.isAfter(today))  return false;
        return time.isBefore(LocalTime.now());
    }

    public LocalDate maxBookingDate() {
        return LocalDate.now().plusYears(1);
    }

    public LocalDate minBookingDate() {
        return LocalDate.now();
    }
}