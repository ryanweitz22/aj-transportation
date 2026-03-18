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
    private final TripRequestService tripRequestService;
    private final BusinessHoursService businessHoursService;

    public BookingsController(TripService tripService, BookingService bookingService,
                               UserService userService, TripRequestService tripRequestService,
                               BusinessHoursService businessHoursService) {
        this.tripService = tripService;
        this.bookingService = bookingService;
        this.userService = userService;
        this.tripRequestService = tripRequestService;
        this.businessHoursService = businessHoursService;
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

    @PostMapping("/bookings/book")
    public String bookTrip(@RequestParam("tripId") UUID tripId,
                           @AuthenticationPrincipal UserDetails userDetails,
                           RedirectAttributes ra) {
        User user = userService.findByEmail(userDetails.getUsername());
        try {
            Booking booking = bookingService.createBooking(user, tripId);
            ra.addFlashAttribute("successMessage",
                "Booking confirmed! Ref: " + booking.getId().toString().substring(0,8).toUpperCase());
            return "redirect:/dashboard";
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/bookings";
        }
    }

    @PostMapping("/bookings/request")
    public String requestTrip(
            @RequestParam("requestedDate")      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate requestedDate,
            @RequestParam("requestedStartTime") String requestedStartTime,
            @RequestParam("pickupAddress")      String pickupAddress,
            @RequestParam("dropoffAddress")     String dropoffAddress,
            @RequestParam(value = "additionalNotes", required = false) String additionalNotes,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes ra) {

        User user = userService.findByEmail(userDetails.getUsername());

        if (requestedDate.isBefore(businessHoursService.minBookingDate())) {
            ra.addFlashAttribute("errorMessage", "Cannot request a trip in the past.");
            return "redirect:/bookings";
        }
        if (requestedDate.isAfter(businessHoursService.maxBookingDate())) {
            ra.addFlashAttribute("errorMessage", "Cannot book more than 1 year in advance.");
            return "redirect:/bookings";
        }

        LocalTime startTime;
        try { startTime = LocalTime.parse(requestedStartTime); }
        catch (Exception e) { ra.addFlashAttribute("errorMessage", "Invalid time."); return "redirect:/bookings"; }

        if (!businessHoursService.isWithinBusinessHours(requestedDate, startTime)) {
            ra.addFlashAttribute("errorMessage", "That time is outside our business hours.");
            return "redirect:/bookings";
        }

        try {
            tripRequestService.createRequest(user, requestedDate, startTime, pickupAddress, dropoffAddress, additionalNotes);
            ra.addFlashAttribute("successMessage",
                "Trip request submitted! We will confirm via email.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/dashboard";
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