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
import java.util.UUID;

@Controller
@RequestMapping("/admin/trips")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTripController {

    private final TripService tripService;

    public AdminTripController(TripService tripService) {
        this.tripService = tripService;
    }

    // GET /admin/trips — list all upcoming trips
    @GetMapping
    public String listTrips(Model model) {
        // Show trips from today onwards, sorted by date
        List<Trip> trips = tripService.getTripsForWeek(LocalDate.now());
        model.addAttribute("trips", trips);
        model.addAttribute("today", LocalDate.now());
        return "admin/trips-list";
    }

    // GET /admin/trips/new — show blank trip creation form
    @GetMapping("/new")
    public String newTripForm(Model model) {
        model.addAttribute("trip", new Trip());
        return "admin/trips-new";
    }

    // POST /admin/trips/new — save the new trip to DB
    @PostMapping("/new")
    public String createTrip(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam("startTime") String startTime,
            @RequestParam("endTime") String endTime,
            @RequestParam("label") String label,
            @RequestParam(value = "fee", required = false) Double fee,
            @RequestParam(value = "pickupAddress", required = false) String pickupAddress,
            @RequestParam(value = "dropoffAddress", required = false) String dropoffAddress,
            RedirectAttributes redirectAttributes) {

        Trip trip = new Trip();
        trip.setDate(date);
        trip.setStartTime(LocalTime.parse(startTime));
        trip.setEndTime(LocalTime.parse(endTime));
        trip.setLabel(label);
        trip.setFee(fee != null ? java.math.BigDecimal.valueOf(fee) : null);
        trip.setPickupAddress(pickupAddress);
        trip.setDropoffAddress(dropoffAddress);
        trip.setStatus("AVAILABLE");

        tripService.createTrip(trip);

        redirectAttributes.addFlashAttribute("successMessage",
                "Trip slot created for " + date + " at " + startTime);
        return "redirect:/admin/trips/new";
    }

    // POST /admin/trips/block/{id} — block a slot
    @PostMapping("/block/{id}")
    public String blockTrip(
            @PathVariable UUID id,
            @RequestParam(value = "reason", required = false) String reason,
            RedirectAttributes redirectAttributes) {

        tripService.blockTrip(id, reason);
        redirectAttributes.addFlashAttribute("successMessage", "Slot blocked successfully.");
        return "redirect:/admin/trips";
    }

    // POST /admin/trips/unblock/{id} — unblock a slot
    @PostMapping("/unblock/{id}")
    public String unblockTrip(
            @PathVariable UUID id,
            RedirectAttributes redirectAttributes) {

        tripService.unblockTrip(id);
        redirectAttributes.addFlashAttribute("successMessage", "Slot unblocked successfully.");
        return "redirect:/admin/trips";
    }

    // POST /admin/trips/delete/{id} — delete a trip (use carefully)
    @PostMapping("/delete/{id}")
    public String deleteTrip(
            @PathVariable UUID id,
            RedirectAttributes redirectAttributes) {

        tripService.deleteTrip(id);
        redirectAttributes.addFlashAttribute("successMessage", "Trip deleted.");
        return "redirect:/admin/trips";
    }
}
