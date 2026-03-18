package com.ajtransportation.app.controller;

import com.ajtransportation.app.model.RegisterRequest;
import com.ajtransportation.app.model.User;
import com.ajtransportation.app.service.BookingService;
import com.ajtransportation.app.service.TripRequestService;
import com.ajtransportation.app.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    private final UserService userService;
    private final BookingService bookingService;
    private final TripRequestService tripRequestService;

    public AuthController(UserService userService,
                          BookingService bookingService,
                          TripRequestService tripRequestService) {
        this.userService = userService;
        this.bookingService = bookingService;
        this.tripRequestService = tripRequestService;
    }

    @GetMapping("/login")
    public String loginPage(
            @RequestParam(value = "error",      required = false) String error,
            @RequestParam(value = "logout",     required = false) String logout,
            @RequestParam(value = "registered", required = false) String registered,
            Model model) {
        if (error != null)
            model.addAttribute("errorMessage", "Incorrect email or password. Please try again.");
        if (logout != null)
            model.addAttribute("successMessage", "You have been logged out.");
        if (registered != null)
            model.addAttribute("successMessage",
                "Account created! Please check your email and click the verification link before logging in.");
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerSubmit(
            @Valid @ModelAttribute("registerRequest") RegisterRequest request,
            BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) return "auth/register";
        String err = userService.register(request);
        if (err != null) { model.addAttribute("errorMessage", err); return "auth/register"; }
        return "redirect:/login?registered=true";
    }

    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam("token") String token, Model model) {
        boolean ok = userService.verifyEmail(token);
        if (ok)
            model.addAttribute("successMessage", "Email verified! You can now log in.");
        else
            model.addAttribute("errorMessage", "Invalid or already used verification link.");
        return "auth/login";
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByEmail(userDetails.getUsername());
        var bookings     = bookingService.getUserBookings(user);
        var tripRequests = tripRequestService.getUserRequests(user);
        long activeBookingCount  = bookingService.countActiveBookings(user);
        long pendingRequestCount = tripRequests.stream()
            .filter(r -> "PENDING".equals(r.getStatus())).count();

        model.addAttribute("user",                user);
        model.addAttribute("bookings",            bookings);
        model.addAttribute("tripRequests",        tripRequests);
        model.addAttribute("activeBookingCount",  activeBookingCount);
        model.addAttribute("totalBookingCount",   bookings.size());
        model.addAttribute("pendingRequestCount", pendingRequestCount);
        return "user/dashboard";
    }
}