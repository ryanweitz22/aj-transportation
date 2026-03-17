package com.ajtransportation.app.controller;

import com.ajtransportation.app.model.RegisterRequest;
import com.ajtransportation.app.model.User;
import com.ajtransportation.app.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // ---- LOGIN ----

    @GetMapping("/login")
    public String loginPage(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            Model model) {

        if (error != null) {
            model.addAttribute("errorMessage", "Incorrect email or password. Please try again.");
        }
        if (logout != null) {
            model.addAttribute("successMessage", "You have been logged out successfully.");
        }
        return "auth/login";
    }

    // ---- REGISTER ----

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerSubmit(
            @Valid @ModelAttribute("registerRequest") RegisterRequest request,
            BindingResult bindingResult,
            Model model) {

        // Step 1: Check for validation annotation errors (blank fields, bad email format, etc.)
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        // Step 2: Run service-level checks (duplicate email, passwords match, etc.)
        String errorMessage = userService.register(request);
        if (errorMessage != null) {
            model.addAttribute("errorMessage", errorMessage);
            return "auth/register";
        }

        // Step 3: Success — redirect to login with a success message
        return "redirect:/login?registered=true";
    }

    // ---- DASHBOARD ----

    @GetMapping("/dashboard")
    public String dashboard(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        // Look up full user object so we can show their username etc.
        User user = userService.findByEmail(userDetails.getUsername());
        model.addAttribute("user", user);

        // Bookings will be added in Phase 6
        return "user/dashboard";
    }
}
