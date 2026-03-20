package com.ajtransportation.app.controller;

import com.ajtransportation.app.model.Booking;
import com.ajtransportation.app.model.Trip;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
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

    // ── Shared mapper ─────────────────────────────────────────────────────────
    private ObjectMapper buildMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    // GET /admin/dashboard
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
                trips = tripService.getTripsForRange(monthStart, monthEnd);
                viewLabel = date.getMonth().toString() + " " + date.getYear();
            }
            default -> {
                LocalDate weekStart = date.with(DayOfWeek.MONDAY);
                trips = tripService.getTripsForWeek(weekStart);
                date  = weekStart;
                viewLabel = weekStart + " – " + weekStart.plusDays(6);
            }
        }

        String tripsJson = buildMapper().writeValueAsString(trips);

        model.addAttribute("trips",       trips);
        model.addAttribute("tripsJson",   tripsJson);
        model.addAttribute("currentView", view);
        model.addAttribute("currentDate", date);
        model.addAttribute("viewLabel",   viewLabel);
        model.addAttribute("prevDate",    getPrevDate(view, date));
        model.addAttribute("nextDate",    getNextDate(view, date));

        return "admin/dashboard";
    }

    // POST /admin/dashboard/cancel/{bookingId}
    @PostMapping("/cancel/{bookingId}")
    public String cancelBooking(@PathVariable UUID bookingId,
                                RedirectAttributes ra) {
        bookingService.cancelBooking(bookingId);
        ra.addFlashAttribute("successMessage", "Booking cancelled successfully.");
        return "redirect:/admin/dashboard";
    }

    // POST /admin/dashboard/block/{tripId} — block a single existing trip
    @PostMapping("/block/{tripId}")
    public String blockSlot(@PathVariable UUID tripId,
                            @RequestParam(value = "reason", defaultValue = "Blocked by admin") String reason,
                            RedirectAttributes ra) {
        tripService.blockTrip(tripId, reason);
        ra.addFlashAttribute("successMessage", "Slot blocked.");
        return "redirect:/admin/dashboard";
    }

    // POST /admin/dashboard/unblock/{tripId}
    @PostMapping("/unblock/{tripId}")
    public String unblockSlot(@PathVariable UUID tripId,
                              RedirectAttributes ra) {
        tripService.unblockTrip(tripId);
        ra.addFlashAttribute("successMessage", "Slot unblocked.");
        return "redirect:/admin/dashboard";
    }

    // POST /admin/dashboard/cancel-by-trip/{tripId}
    @PostMapping("/cancel-by-trip/{tripId}")
    public String cancelByTrip(@PathVariable UUID tripId,
                               RedirectAttributes ra) {
        bookingService.cancelBookingByTripId(tripId);
        ra.addFlashAttribute("successMessage", "Booking cancelled. Slot is now available again.");
        return "redirect:/admin/dashboard";
    }

    /**
     * POST /admin/dashboard/block-day
     *
     * Blocks ALL AVAILABLE trips on the chosen date in one click.
     * PENDING and BOOKED trips are left untouched.
     */
    @PostMapping("/block-day")
    public String blockDay(
            @RequestParam("date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "reason", defaultValue = "Day blocked by admin") String reason,
            RedirectAttributes ra) {

        List<Trip> trips = tripService.getTripsForDay(date);
        long count = 0;
        for (Trip trip : trips) {
            if ("AVAILABLE".equals(trip.getStatus())) {
                tripService.blockTrip(trip.getId(), reason);
                count++;
            }
        }

        ra.addFlashAttribute("successMessage", count > 0
            ? count + " slot(s) blocked on " + date + "."
            : "No available slots found on " + date + " to block.");

        return "redirect:/admin/dashboard?view=day&date=" + date;
    }

    /**
     * POST /admin/dashboard/block-time-range
     *
     * Creates a new BLOCKED trip record for a specific date and time range
     * chosen by the admin — even if no trip exists for that slot yet.
     * This is how the admin blocks a custom time window that users haven't
     * booked yet (e.g. block 10:00–14:00 on a specific day).
     */
    @PostMapping("/block-time-range")
    public String blockTimeRange(
            @RequestParam("date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam("startTime") String startTime,
            @RequestParam("endTime")   String endTime,
            @RequestParam(value = "reason", defaultValue = "Blocked by admin") String reason,
            RedirectAttributes ra) {

        LocalTime start = LocalTime.parse(startTime);
        LocalTime end   = LocalTime.parse(endTime);

        if (!end.isAfter(start)) {
            ra.addFlashAttribute("errorMessage", "End time must be after start time.");
            return "redirect:/admin/dashboard?view=day&date=" + date;
        }

        Trip blocked = new Trip();
        blocked.setDate(date);
        blocked.setStartTime(start);
        blocked.setEndTime(end);
        blocked.setLabel("Blocked by admin");
        blocked.setStatus("BLOCKED");
        blocked.setBlockedReason(reason);
        tripService.createTrip(blocked);

        ra.addFlashAttribute("successMessage",
            "Time blocked: " + startTime + " – " + endTime + " on " + date + ".");

        return "redirect:/admin/dashboard?view=day&date=" + date;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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