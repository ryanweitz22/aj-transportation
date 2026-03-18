package com.ajtransportation.app.controller;

import com.ajtransportation.app.model.Booking;
import com.ajtransportation.app.model.Trip;
import com.ajtransportation.app.model.User;
import com.ajtransportation.app.service.BookingService;
import com.ajtransportation.app.service.TripService;
import com.ajtransportation.app.service.UserService;
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
import java.util.List;
import java.util.UUID;

@Controller
public class BookingsController {

    private final TripService tripService;
    private final BookingService bookingService;
    private final UserService userService;

    public BookingsController(TripService tripService,
                               BookingService bookingService,
                               UserService userService) {
        this.tripService = tripService;
        this.bookingService = bookingService;
        this.userService = userService;
    }

    // GET /bookings?week=2026-03-16
    // Shows the booking calendar. Defaults to the current week.
    // Logged-in users see a Book button; guests see a "Login to book" prompt.
    @GetMapping("/bookings")
    public String bookingsPage(
            @RequestParam(value = "week", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week,
            Model model) throws JsonProcessingException {

        // Default to the current Monday if no week param provided
        if (week == null) {
            week = LocalDate.now().with(DayOfWeek.MONDAY);
        }

        // Users only see AVAILABLE and BOOKED slots — BLOCKED slots are hidden
        List<Trip> trips = tripService.getVisibleTripsForWeek(week);

        // Serialize trips to JSON so calendar.js can consume them directly
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String tripsJson = mapper.writeValueAsString(trips);

        model.addAttribute("tripsJson", tripsJson);
        model.addAttribute("weekStart", week);
        model.addAttribute("weekEnd", week.plusDays(6));

        // Navigation: previous and next week
        model.addAttribute("prevWeek", week.minusWeeks(1));
        model.addAttribute("nextWeek", week.plusWeeks(1));

        // Limit how far into the future users can browse (1 year)
        model.addAttribute("maxWeek", LocalDate.now().plusYears(1).with(DayOfWeek.MONDAY));

        return "user/bookings";
    }

    // POST /bookings/book
    // Authenticated users submit tripId to make a booking
    @PostMapping("/bookings/book")
    public String bookTrip(
            @RequestParam("tripId") UUID tripId,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        User user = userService.findByEmail(userDetails.getUsername());

        try {
            Booking booking = bookingService.createBooking(user, tripId);
            // Phase 9 will redirect to Ozow payment here instead
            redirectAttributes.addFlashAttribute("successMessage",
                    "Booking confirmed! Booking ID: " + booking.getId());
            return "redirect:/dashboard";

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/bookings";
        }
    }

    // POST /bookings/cancel/{id}
    // Logged-in users can cancel their own bookings
    @PostMapping("/bookings/cancel/{id}")
    public String cancelBooking(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        User user = userService.findByEmail(userDetails.getUsername());
        Booking booking = bookingService.getBookingById(id);

        // Security check — users can only cancel their own bookings
        if (!booking.getUser().getId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "You are not authorised to cancel this booking.");
            return "redirect:/dashboard";
        }

        bookingService.cancelBooking(id);
        redirectAttributes.addFlashAttribute("successMessage", "Booking cancelled successfully.");
        return "redirect:/dashboard";
    }
}
