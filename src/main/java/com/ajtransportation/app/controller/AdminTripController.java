package com.ajtransportation.app.controller;

import com.ajtransportation.app.model.PricingConfig;
import com.ajtransportation.app.model.Trip;
import com.ajtransportation.app.service.TripService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
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
        List<Trip> trips = tripService.getTripsForWeek(LocalDate.now());
        model.addAttribute("trips", trips);
        model.addAttribute("today", LocalDate.now());
        return "admin/trips-list";
    }

    // GET /admin/trips/new — show blank trip creation form
    @GetMapping("/new")
    public String newTripForm(Model model) {
        model.addAttribute("trip", new Trip());
        PricingConfig config = tripService.getPricingConfig();
        model.addAttribute("currentRate", config.getRatePerKm());
        model.addAttribute("currentMinFare", config.getMinimumFare());
        return "admin/trips-new";
    }

    // POST /admin/trips/new — save the new trip to DB
    @PostMapping("/new")
    public String createTrip(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam("startTime") String startTime,
            @RequestParam(value = "endTime", required = false) String endTime,
            @RequestParam("label") String label,
            @RequestParam(value = "fee", required = false) Double fee,
            @RequestParam(value = "pickupAddress", required = false) String pickupAddress,
            @RequestParam(value = "dropoffAddress", required = false) String dropoffAddress,
            RedirectAttributes redirectAttributes) {

        Trip trip = new Trip();
        trip.setDate(date);
        trip.setStartTime(LocalTime.parse(startTime));
        trip.setLabel(label);
        trip.setPickupAddress(pickupAddress);
        trip.setDropoffAddress(dropoffAddress);
        trip.setStatus("AVAILABLE");

        // Only set manual endTime/fee if addresses aren't provided
        // (Google Maps will override these when addresses are present)
        boolean hasAddresses = pickupAddress != null && !pickupAddress.isBlank()
                && dropoffAddress != null && !dropoffAddress.isBlank();

        if (!hasAddresses) {
            // Manual mode — use admin-entered values
            if (endTime != null && !endTime.isBlank()) {
                trip.setEndTime(LocalTime.parse(endTime));
            }
            if (fee != null) {
                trip.setFee(java.math.BigDecimal.valueOf(fee));
            }
        }

        // createTrip() auto-calls Google Maps if addresses are set
        Trip saved = tripService.createTrip(trip);

        String message = hasAddresses && saved.getDistanceKm() != null
                ? String.format("Trip created ✓ — %.1f km, %d min ETA + 15 min buffer = %d min total. Fee: R%.2f",
                    saved.getDistanceKm().doubleValue(),
                    saved.getGoogleEtaMinutes(),
                    saved.getBufferedDurationMinutes(),
                    saved.getFee() != null ? saved.getFee().doubleValue() : 0.0)
                : "Trip slot created for " + date + " at " + startTime +
                  (hasAddresses ? " (Google Maps unavailable — values set manually)" : "");

        redirectAttributes.addFlashAttribute("successMessage", message);
        return "redirect:/admin/trips/new";
    }

    // GET /admin/trips/pricing — show pricing config form
    @GetMapping("/pricing")
    public String pricingForm(Model model) {
        PricingConfig config = tripService.getPricingConfig();
        model.addAttribute("config", config);
        return "admin/pricing";
    }

    // POST /admin/trips/pricing — save new rates
    @PostMapping("/pricing")
    public String savePricing(
            @RequestParam("ratePerKm") BigDecimal ratePerKm,
            @RequestParam("minimumFare") BigDecimal minimumFare,
            RedirectAttributes redirectAttributes) {
        tripService.savePricingConfig(ratePerKm, minimumFare);
        redirectAttributes.addFlashAttribute("successMessage",
                String.format("Pricing updated: R%.2f/km, minimum R%.2f", ratePerKm, minimumFare));
        return "redirect:/admin/trips/pricing";
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

    // POST /admin/trips/delete/{id} — delete a trip
    @PostMapping("/delete/{id}")
    public String deleteTrip(
            @PathVariable UUID id,
            RedirectAttributes redirectAttributes) {
        tripService.deleteTrip(id);
        redirectAttributes.addFlashAttribute("successMessage", "Trip deleted.");
        return "redirect:/admin/trips";
    }
}
