package com.ajtransportation.app.service;

import org.springframework.stereotype.Service;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

@Service
public class BusinessHoursService {

    public boolean isOpenDay(LocalDate date) {
        return date.getDayOfWeek() != DayOfWeek.SUNDAY;
    }

    public LocalTime openTime(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        if (day == DayOfWeek.SUNDAY)   return null;
        if (day == DayOfWeek.SATURDAY) return LocalTime.of(6, 0);
        return LocalTime.of(4, 0);
    }

    public LocalTime closeTime(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        if (day == DayOfWeek.SUNDAY)   return null;
        if (day == DayOfWeek.SATURDAY) return LocalTime.of(10, 0);
        if (day == DayOfWeek.FRIDAY)   return LocalTime.of(11, 30);
        return LocalTime.of(12, 0);
    }

    public boolean isWithinBusinessHours(LocalDate date, LocalTime time) {
        LocalTime open  = openTime(date);
        LocalTime close = closeTime(date);
        if (open == null) return false;
        return !time.isBefore(open) && time.isBefore(close);
    }

    public LocalDate maxBookingDate() {
        return LocalDate.now().plusYears(1);
    }

    public LocalDate minBookingDate() {
        return LocalDate.now().plusDays(1);
    }
}