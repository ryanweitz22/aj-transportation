package com.ajtransportation.app.controller;

import com.ajtransportation.app.service.BookingService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.UUID;

@Controller
@RequestMapping("/admin/bookings")
public class AdminBookingController {

    private final BookingService bookingService;

    public AdminBookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    // Admin accepts a booking — triggers payment in Phase 9
    @PostMapping("/accept/{id}")
    public String acceptBooking(@PathVariable UUID id, RedirectAttributes ra) {
        try {
            bookingService.acceptBooking(id);
            ra.addFlashAttribute("successMessage", "Booking accepted. Payment will be triggered shortly.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/bookings/pending";
    }

    // Admin rejects a booking — slot freed back to AVAILABLE
    @PostMapping("/reject/{id}")
    public String rejectBooking(@PathVariable UUID id, RedirectAttributes ra) {
        try {
            bookingService.rejectBooking(id);
            ra.addFlashAttribute("successMessage", "Booking rejected. Slot is now available again.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/bookings/pending";
    }

    // Admin views all pending approval bookings
    @GetMapping("/pending")
    public String pendingBookings(org.springframework.ui.Model model) {
        model.addAttribute("bookings", bookingService.getPendingBookings());
        return "admin/bookings-pending";
    }
}