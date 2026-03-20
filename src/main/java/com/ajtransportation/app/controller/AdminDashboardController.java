package com.ajtransportation.app.controller;

import com.ajtransportation.app.model.Booking;
import com.ajtransportation.app.model.Trip;
import com.ajtransportation.app.repository.BookingRepository;
import com.ajtransportation.app.service.BookingService;
import com.ajtransportation.app.service.TripService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final TripService tripService;
    private final BookingService bookingService;
    private final BookingRepository bookingRepository;

    public AdminDashboardController(TripService tripService,
                                     BookingService bookingService,
                                     BookingRepository bookingRepository) {
        this.tripService = tripService;
        this.bookingService = bookingService;
        this.bookingRepository = bookingRepository;
    }

    private ObjectMapper buildMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @GetMapping
    public String dashboard(
            @RequestParam(value = "view", defaultValue = "week") String view,
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model) throws JsonProcessingException {

        LocalDate today = LocalDate.now();
        LocalTime now   = LocalTime.now();

        if (date == null) date = today;

        List<Trip> rawTrips;
        String viewLabel;

        switch (view) {
            case "day" -> {
                rawTrips  = tripService.getTripsForDay(date);
                viewLabel = date.toString();
            }
            case "month" -> {
                LocalDate monthStart = date.withDayOfMonth(1);
                LocalDate monthEnd   = date.withDayOfMonth(date.lengthOfMonth());
                rawTrips  = tripService.getTripsForRange(monthStart, monthEnd);
                viewLabel = date.getMonth().toString() + " " + date.getYear();
            }
            default -> {
                // Week view starts from TODAY not Monday
                rawTrips  = tripService.getTripsForRange(today, today.plusDays(6));
                date      = today;
                viewLabel = today + " – " + today.plusDays(6);
            }
        }

        // Filter — identical logic to Manage Slots:
        // Past dates → always hidden
        // Today → only show trips whose start time is still in the future
        // Future dates → always show
        List<Trip> trips = rawTrips.stream()
            .filter(t -> {
                if (t.getDate().isBefore(today)) return false;
                if (t.getDate().isAfter(today))  return true;
                return t.getStartTime().isAfter(now);
            })
            .collect(Collectors.toList());

        // Build bookingByTripId map for BOOKED/PENDING trips
        Map<UUID, Booking> bookingByTripId = new HashMap<>();
        for (Trip trip : trips) {
            if ("BOOKED".equals(trip.getStatus())
                || "PENDING".equals(trip.getStatus())) {
                bookingRepository
                    .findByTripIdAndStatusNot(trip.getId(), "CANCELLED")
                    .ifPresent(b -> bookingByTripId.put(trip.getId(), b));
            }
        }

        long availableCount = trips.stream()
            .filter(t -> "AVAILABLE".equals(t.getStatus())).count();
        long bookedCount = trips.stream()
            .filter(t -> "BOOKED".equals(t.getStatus())).count();
        long blockedCount = trips.stream()
            .filter(t -> "BLOCKED".equals(t.getStatus())).count();

        String tripsJson = buildMapper().writeValueAsString(trips);

        model.addAttribute("trips",            trips);
        model.addAttribute("tripsJson",        tripsJson);
        model.addAttribute("bookingByTripId",  bookingByTripId);
        model.addAttribute("currentView",      view);
        model.addAttribute("currentDate",      date);
        model.addAttribute("viewLabel",        viewLabel);
        model.addAttribute("prevDate",         getPrevDate(view, date));
        model.addAttribute("nextDate",         getNextDate(view, date));
        model.addAttribute("today",            today);
        model.addAttribute("availableCount",   availableCount);
        model.addAttribute("bookedCount",      bookedCount);
        model.addAttribute("blockedCount",     blockedCount);

        return "admin/dashboard";
    }

    @PostMapping("/cancel/{bookingId}")
    public String cancelBooking(@PathVariable UUID bookingId,
                                RedirectAttributes ra) {
        bookingService.cancelBooking(bookingId);
        ra.addFlashAttribute("successMessage", "Booking cancelled.");
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/block/{tripId}")
    public String blockSlot(@PathVariable UUID tripId,
                            @RequestParam(value = "reason",
                                          defaultValue = "Blocked by admin") String reason,
                            RedirectAttributes ra) {
        tripService.blockTrip(tripId, reason);
        ra.addFlashAttribute("successMessage", "Slot blocked.");
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/unblock/{tripId}")
    public String unblockSlot(@PathVariable UUID tripId,
                              RedirectAttributes ra) {
        tripService.unblockTrip(tripId);
        ra.addFlashAttribute("successMessage", "Slot unblocked.");
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/cancel-by-trip/{tripId}")
    public String cancelByTrip(@PathVariable UUID tripId,
                               RedirectAttributes ra) {
        bookingService.cancelBookingByTripId(tripId);
        ra.addFlashAttribute("successMessage",
            "Booking cancelled. Slot is now available again.");
        return "redirect:/admin/dashboard";
    }

    private LocalDate getPrevDate(String view, LocalDate date) {
        return switch (view) {
            case "day"   -> date.minusDays(1);
            case "month" -> date.minusMonths(1);
            default      -> date.minusWeeks(1);
        };
    }

    private LocalDate getNextDate(String view, LocalDate date) {
        return switch (view) {
            case "day"   -> date.plusDays(1);
            case "month" -> date.plusMonths(1);
            default      -> date.plusWeeks(1);
        };
    }
}