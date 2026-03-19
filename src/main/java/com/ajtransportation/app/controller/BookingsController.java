package com.ajtransportation.app.controller;

import com.ajtransportation.app.model.Booking;
import com.ajtransportation.app.model.Trip;
import com.ajtransportation.app.model.User;
import com.ajtransportation.app.service.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class BookingsController {

    private final TripService tripService;
    private final BookingService bookingService;
    private final UserService userService;
    private final BusinessHoursService businessHoursService;
    private final GoogleMapsService googleMapsService;

    public BookingsController(TripService tripService, BookingService bookingService,
                               UserService userService,
                               BusinessHoursService businessHoursService,
                               GoogleMapsService googleMapsService) {
        this.tripService = tripService;
        this.bookingService = bookingService;
        this.userService = userService;
        this.businessHoursService = businessHoursService;
        this.googleMapsService = googleMapsService;
    }

    @GetMapping("/bookings")
    public String bookingsPage(
            @RequestParam(value = "week", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week,
            Model model) throws JsonProcessingException {

        if (week == null) week = LocalDate.now().with(DayOfWeek.MONDAY);

        LocalDate minWeek = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate maxWeek = businessHoursService.maxBookingDate().with(DayOfWeek.MONDAY);
        if (week.isBefore(minWeek)) week = minWeek;
        if (week.isAfter(maxWeek))  week = maxWeek;

        List<Trip> trips = tripService.getVisibleTripsForWeek(week);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String tripsJson = mapper.writeValueAsString(trips);
        String businessHoursJson = buildBusinessHoursJson(week, mapper);

        model.addAttribute("tripsJson",         tripsJson);
        model.addAttribute("businessHoursJson", businessHoursJson);
        model.addAttribute("weekStart",  week);
        model.addAttribute("weekEnd",    week.plusDays(6));
        model.addAttribute("prevWeek",   week.minusWeeks(1));
        model.addAttribute("nextWeek",   week.plusWeeks(1));
        model.addAttribute("minWeek",    minWeek);
        model.addAttribute("maxWeek",    maxWeek);
        return "user/bookings";
    }

    @GetMapping("/bookings/calculate-fare")
    @ResponseBody
    public Map<String, Object> calculateFare(
            @RequestParam("pickup")  String pickup,
            @RequestParam("dropoff") String dropoff) {

        Map<String, Object> result = new LinkedHashMap<>();
        try {
            GoogleMapsService.DistanceResult dr = googleMapsService.getDistanceAndEta(pickup, dropoff);
            if (dr == null) {
                result.put("success", false);
                result.put("error",   "Could not calculate fare. The driver will confirm your fare.");
                return result;
            }
            java.math.BigDecimal rate        = java.math.BigDecimal.valueOf(8.0);
            java.math.BigDecimal minimumFare = java.math.BigDecimal.valueOf(50.0);
            java.math.BigDecimal fare        = googleMapsService.calculateFee(dr.distanceKm, rate, minimumFare);
            result.put("success",    true);
            result.put("distanceKm", dr.distanceKm);
            result.put("fare",       fare);
        } catch (Exception e) {
            result.put("success", false);
            result.put("error",   "Could not calculate fare. The driver will confirm your fare.");
        }
        return result;
    }

    @PostMapping("/bookings/book")
    public String bookTrip(
            @RequestParam(value = "tripId",        required = false) String tripId,
            @RequestParam(value = "slotDate",      required = false) String slotDate,
            @RequestParam(value = "slotTime",      required = false) String slotTime,
            @RequestParam("pickupAddress")  String pickupAddress,
            @RequestParam("dropoffAddress") String dropoffAddress,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes ra) {

        if (pickupAddress == null || pickupAddress.isBlank()) {
            ra.addFlashAttribute("errorMessage", "Please enter a pickup location.");
            return "redirect:/bookings";
        }
        if (dropoffAddress == null || dropoffAddress.isBlank()) {
            ra.addFlashAttribute("errorMessage", "Please enter a dropoff location.");
            return "redirect:/bookings";
        }

        User user = userService.findByEmail(userDetails.getUsername());

        try {
            if (tripId != null && !tripId.isBlank()) {
                // Green slot — existing admin-created trip
                Trip trip = tripService.getTripById(UUID.fromString(tripId));

                // Block booking if this trip's time has already passed today
                if (businessHoursService.isPastSlot(trip.getDate(), trip.getStartTime())) {
                    ra.addFlashAttribute("errorMessage", "That time slot has already passed.");
                    return "redirect:/bookings";
                }

                bookingService.createBooking(user, UUID.fromString(tripId), pickupAddress, dropoffAddress);

            } else {
                // Open slot — create trip on the fly
                if (slotDate == null || slotTime == null) {
                    ra.addFlashAttribute("errorMessage", "Invalid slot selection. Please try again.");
                    return "redirect:/bookings";
                }

                LocalDate date      = LocalDate.parse(slotDate);
                LocalTime startTime = LocalTime.parse(slotTime);

                // Block booking if this open slot time has already passed today
                if (businessHoursService.isPastSlot(date, startTime)) {
                    ra.addFlashAttribute("errorMessage", "That time slot has already passed.");
                    return "redirect:/bookings";
                }

                bookingService.createBookingForOpenSlot(user, date, startTime, pickupAddress, dropoffAddress);
            }

            ra.addFlashAttribute("successMessage",
                "Booking submitted! Your driver will review and confirm shortly.");
            return "redirect:/dashboard";

        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/bookings";
        }
    }

    @PostMapping("/bookings/cancel/{id}")
    public String cancelBooking(@PathVariable UUID id,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes ra) {
        User user = userService.findByEmail(userDetails.getUsername());
        Booking booking = bookingService.getBookingById(id);
        if (!booking.getUser().getId().equals(user.getId())) {
            ra.addFlashAttribute("errorMessage", "Not authorised to cancel this booking.");
            return "redirect:/dashboard";
        }
        bookingService.cancelBooking(id);
        ra.addFlashAttribute("successMessage", "Booking cancelled.");
        return "redirect:/dashboard";
    }

    private String buildBusinessHoursJson(LocalDate weekStart, ObjectMapper mapper) throws JsonProcessingException {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) {
            LocalDate day = weekStart.plusDays(i);
            Map<String, String> hours = new LinkedHashMap<>();
            LocalTime open  = businessHoursService.openTime(day);
            LocalTime close = businessHoursService.closeTime(day);
            hours.put("open",  open  != null ? open.toString()  : null);
            hours.put("close", close != null ? close.toString() : null);
            map.put(day.toString(), hours);
        }
        return mapper.writeValueAsString(map);
    }
}