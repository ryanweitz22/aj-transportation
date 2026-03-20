package com.ajtransportation.app.controller;

import com.ajtransportation.app.model.Trip;
import com.ajtransportation.app.service.TripService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequestMapping("/admin/block")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBlockController {

    private final TripService tripService;

    public AdminBlockController(TripService tripService) {
        this.tripService = tripService;
    }

    /**
     * GET /admin/block
     * Shows the Block Time page. Defaults to today's date in the date picker.
     */
    @GetMapping
    public String blockPage(
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model) {

        if (date == null) date = LocalDate.now();

        // Load existing trips for the selected date so admin can
        // see what is already there before blocking
        List<Trip> existingTrips = tripService.getTripsForDay(date);

        model.addAttribute("selectedDate",  date);
        model.addAttribute("existingTrips", existingTrips);
        return "admin/block";
    }

    /**
     * POST /admin/block/time-range
     * Blocks a specific start–end time window on the chosen date.
     * Creates a new BLOCKED trip record covering that window.
     */
    @PostMapping("/time-range")
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
            ra.addFlashAttribute("errorMessage",
                "End time must be after start time. Please try again.");
            return "redirect:/admin/block?date=" + date;
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
            "✅ Blocked " + startTime + " – " + endTime + " on " + date + ".");

        return "redirect:/admin/block?date=" + date;
    }

    /**
     * POST /admin/block/whole-day
     * Blocks ALL AVAILABLE trips on the chosen date in one click.
     * PENDING and BOOKED trips are left untouched.
     */
    @PostMapping("/whole-day")
    public String blockWholeDay(
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
            ? "✅ " + count + " available slot(s) blocked on " + date + "."
            : "No available slots found on " + date + " to block. " +
              "Booked and already-blocked slots are not affected.");

        return "redirect:/admin/block?date=" + date;
    }

    /**
     * POST /admin/block/unblock/{tripId}
     * Unblocks a single trip directly from the Block Time page.
     */
    @PostMapping("/unblock/{tripId}")
    public String unblock(
            @PathVariable java.util.UUID tripId,
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            RedirectAttributes ra) {

        tripService.unblockTrip(tripId);
        ra.addFlashAttribute("successMessage", "Slot unblocked.");

        String redirect = date != null
            ? "redirect:/admin/block?date=" + date
            : "redirect:/admin/block";
        return redirect;
    }
}