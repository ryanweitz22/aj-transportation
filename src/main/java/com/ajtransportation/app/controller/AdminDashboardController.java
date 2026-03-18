package com.ajtransportation.app.controller;

import com.ajtransportation.app.model.Booking;
import com.ajtransportation.app.model.Trip;
import com.ajtransportation.app.service.BookingService;
import com.ajtransportation.app.service.TripService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final TripService tripService;
    private final BookingService bookingService;

    public AdminDashboardController(TripService tripService, BookingService bookingService) {
        this.tripService = tripService;
        this.bookingService = bookingService;
    }

    // GET /admin/dashboard — default to today's week view
    @GetMapping
    public String dashboard(
            @RequestParam(value = "view", defaultValue = "week") String view,
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model) throws JsonProcessingException {

        if (date == null) date = LocalDate.now();

        List<Trip> trips;
        String viewLabel;

        switch (view) {
            case "day" -> {
                trips = tripService.getTripsForDay(date);
                viewLabel = date.toString();
            }
            case "month" -> {
                LocalDate monthStart = date.withDayOfMonth(1);
                LocalDate monthEnd   = date.withDayOfMonth(date.lengthOfMonth());
                // Reuse week range for now — full month fetch via custom query
                trips = tripService.getTripsForRange(monthStart, monthEnd);
                viewLabel = date.getMonth().toString() + " " + date.getYear();
            }
            default -> {
                // week
                LocalDate weekStart = date.with(DayOfWeek.MONDAY);
                trips = tripService.getTripsForWeek(weekStart);
                date  = weekStart;
                viewLabel = weekStart + " – " + weekStart.plusDays(6);
            }
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String tripsJson = mapper.writeValueAsString(trips);

        model.addAttribute("trips", trips);
        model.addAttribute("tripsJson", tripsJson);
        model.addAttribute("currentView", view);
        model.addAttribute("currentDate", date);
        model.addAttribute("viewLabel", viewLabel);
        model.addAttribute("prevDate", getPrevDate(view, date));
        model.addAttribute("nextDate", getNextDate(view, date));

        return "admin/dashboard";
    }

    // POST /admin/dashboard/cancel/{bookingId}
    @PostMapping("/cancel/{bookingId}")
    public String cancelBooking(
            @PathVariable UUID bookingId,
            RedirectAttributes redirectAttributes) {

        bookingService.cancelBooking(bookingId);
        redirectAttributes.addFlashAttribute("successMessage", "Booking cancelled successfully.");
        return "redirect:/admin/dashboard";
    }

    // POST /admin/dashboard/block/{tripId}
    @PostMapping("/block/{tripId}")
    public String blockSlot(
            @PathVariable UUID tripId,
            @RequestParam(value = "reason", defaultValue = "Blocked by admin") String reason,
            RedirectAttributes redirectAttributes) {

        tripService.blockTrip(tripId, reason);
        redirectAttributes.addFlashAttribute("successMessage", "Slot blocked.");
        return "redirect:/admin/dashboard";
    }

    // POST /admin/dashboard/unblock/{tripId}
    @PostMapping("/unblock/{tripId}")
    public String unblockSlot(
            @PathVariable UUID tripId,
            RedirectAttributes redirectAttributes) {

        tripService.unblockTrip(tripId);
        redirectAttributes.addFlashAttribute("successMessage", "Slot unblocked.");
        return "redirect:/admin/dashboard";
    }

    // POST /admin/dashboard/cancel-by-trip/{tripId}
    @PostMapping("/cancel-by-trip/{tripId}")
    public String cancelByTrip(
            @PathVariable UUID tripId,
            RedirectAttributes redirectAttributes) {

        bookingService.cancelBookingByTripId(tripId);
        redirectAttributes.addFlashAttribute("successMessage", "Booking cancelled. Slot is now available again.");
        return "redirect:/admin/dashboard";
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

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
