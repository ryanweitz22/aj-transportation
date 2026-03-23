package com.ajtransportation.app.controller;

import com.ajtransportation.app.model.Booking;
import com.ajtransportation.app.model.PricingConfig;
import com.ajtransportation.app.model.Trip;
import com.ajtransportation.app.repository.BookingRepository;
import com.ajtransportation.app.service.BookingService;
import com.ajtransportation.app.service.TripService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/trips")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTripController {

    private final TripService tripService;
    private final BookingService bookingService;
    private final BookingRepository bookingRepository;

    public AdminTripController(TripService tripService,
                                BookingService bookingService,
                                BookingRepository bookingRepository) {
        this.tripService = tripService;
        this.bookingService = bookingService;
        this.bookingRepository = bookingRepository;
    }

    @GetMapping
    public String listTrips(Model model) {
        LocalDate today = LocalDate.now();
        LocalTime now   = LocalTime.now();

        // Fetch next 4 weeks
        List<Trip> allTrips = tripService.getTripsForRange(today, today.plusWeeks(4));

        // Include BLOCKED trips alongside others — filter out only past times
        List<Trip> trips = allTrips.stream()
            .filter(t -> {
                if (t.getDate().isAfter(today)) return true;
                if (t.getDate().isBefore(today)) return false;
                // Today — always show BLOCKED and BOOKED even if past
                if ("BLOCKED".equals(t.getStatus())
                    || "BOOKED".equals(t.getStatus())) return true;
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

        // 1-hour cancel rule for BOOKED trips
        Map<UUID, Boolean> canCancelMap = new HashMap<>();
        for (Trip trip : trips) {
            if ("BOOKED".equals(trip.getStatus())) {
                LocalDateTime tripDateTime = LocalDateTime.of(
                    trip.getDate(), trip.getStartTime());
                long minutesUntilTrip = java.time.Duration.between(
                    LocalDateTime.now(), tripDateTime).toMinutes();
                canCancelMap.put(trip.getId(), minutesUntilTrip >= 60);
            }
        }

        model.addAttribute("trips",           trips);
        model.addAttribute("bookingByTripId", bookingByTripId);
        model.addAttribute("canCancelMap",    canCancelMap);
        model.addAttribute("today",           today);
        return "admin/trips-list";
    }

    @GetMapping("/new")
    public String newTripForm(Model model) {
        model.addAttribute("trip", new Trip());
        PricingConfig config = tripService.getPricingConfig();
        model.addAttribute("currentRate",    config.getRatePerKm());
        model.addAttribute("currentMinFare", config.getMinimumFare());
        return "admin/trips-new";
    }

    @PostMapping("/new")
    public String createTrip(
            @RequestParam("date")      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam("startTime") String startTime,
            @RequestParam("label")     String label,
            @RequestParam(value = "pickupAddress",  required = false) String pickupAddress,
            @RequestParam(value = "dropoffAddress", required = false) String dropoffAddress,
            RedirectAttributes redirectAttributes) {

        Trip trip = new Trip();
        trip.setDate(date);
        trip.setStartTime(LocalTime.parse(startTime));
        trip.setLabel(label);
        trip.setPickupAddress(pickupAddress);
        trip.setDropoffAddress(dropoffAddress);
        trip.setStatus("AVAILABLE");

        Trip saved = tripService.createTrip(trip);

        String message = saved.getDistanceKm() != null
            ? String.format("Trip created ✓ — %.1f km, %d min ETA + 15 min buffer. Fee: R%.2f",
                saved.getDistanceKm().doubleValue(),
                saved.getGoogleEtaMinutes(),
                saved.getFee() != null ? saved.getFee().doubleValue() : 0.0)
            : "Trip slot created for " + date + " at " + startTime;

        redirectAttributes.addFlashAttribute("successMessage", message);
        return "redirect:/admin/trips/new";
    }

    @GetMapping("/pricing")
    public String pricingForm(Model model) {
        PricingConfig config = tripService.getPricingConfig();
        model.addAttribute("config", config);
        return "admin/pricing";
    }

    @PostMapping("/pricing")
    public String savePricing(
            @RequestParam("ratePerKm")   BigDecimal ratePerKm,
            @RequestParam("minimumFare") BigDecimal minimumFare,
            RedirectAttributes redirectAttributes) {
        tripService.savePricingConfig(ratePerKm, minimumFare);
        redirectAttributes.addFlashAttribute("successMessage",
            String.format("Pricing updated: R%.2f/km, minimum R%.2f",
                ratePerKm, minimumFare));
        return "redirect:/admin/trips/pricing";
    }

    @PostMapping("/block/{id}")
    public String blockTrip(@PathVariable UUID id,
                            @RequestParam(value = "reason", required = false) String reason,
                            RedirectAttributes redirectAttributes) {
        tripService.blockTrip(id, reason);
        redirectAttributes.addFlashAttribute("successMessage", "Slot blocked.");
        return "redirect:/admin/trips";
    }

    @PostMapping("/unblock/{id}")
    public String unblockTrip(@PathVariable UUID id,
                              RedirectAttributes redirectAttributes) {
        tripService.unblockTrip(id);
        redirectAttributes.addFlashAttribute("successMessage", "Slot unblocked.");
        return "redirect:/admin/trips";
    }

    @PostMapping("/delete/{id}")
    public String deleteTrip(@PathVariable UUID id,
                             RedirectAttributes redirectAttributes) {
        tripService.deleteTrip(id);
        redirectAttributes.addFlashAttribute("successMessage", "Trip deleted.");
        return "redirect:/admin/trips";
    }

    // Cancel booking — 1-hour rule enforced server-side
    @PostMapping("/cancel/{id}")
    public String cancelBooking(@PathVariable UUID id,
                                RedirectAttributes ra) {
        Trip trip = tripService.getTripById(id);
        LocalDateTime tripDateTime = LocalDateTime.of(
            trip.getDate(), trip.getStartTime());
        long minutesUntilTrip = java.time.Duration.between(
            LocalDateTime.now(), tripDateTime).toMinutes();

        if (minutesUntilTrip < 60) {
            ra.addFlashAttribute("errorMessage",
                "Cannot cancel — trip is less than 1 hour away.");
            return "redirect:/admin/trips";
        }

        bookingService.cancelBookingByTripId(id);
        ra.addFlashAttribute("successMessage",
            "Booking cancelled. Slot is now available.");
        return "redirect:/admin/trips";
    }

    // Cancel a BLOCKED slot — frees it back to AVAILABLE
    @PostMapping("/cancel-block/{id}")
    public String cancelBlock(@PathVariable UUID id,
                              RedirectAttributes ra) {
        tripService.unblockTrip(id);
        ra.addFlashAttribute("successMessage",
            "Blocked slot cancelled and is now available.");
        return "redirect:/admin/trips";
    }
}