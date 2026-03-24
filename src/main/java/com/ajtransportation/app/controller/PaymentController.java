package com.ajtransportation.app.controller;

import com.ajtransportation.app.model.Booking;
import com.ajtransportation.app.service.BookingService;
import com.ajtransportation.app.service.OzowService;
import com.ajtransportation.app.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * PaymentController — Phase 9
 *
 * TODAY booking flow:
 *   User waits on booking-waiting.html → admin accepts → booking status = AWAITING_PAYMENT
 *   → waiting screen JS detects AWAITING_PAYMENT → redirects to GET /payment/initiate/{id}
 *   → this controller builds Ozow form → renders payment-redirect.html
 *   → page auto-submits POST form to Ozow → user lands on Ozow hosted payment page
 *   → Ozow POSTs ITN to POST /payment/notify → OzowService updates booking
 *   → Ozow sends user back to /payment/success or /payment/cancel
 *
 * FUTURE DAY booking flow:
 *   Booking submitted → BookingsController redirects straight to GET /payment/initiate/{id}
 *   → same Ozow form → same ITN flow, no waiting screen involved
 */
@Controller
@RequestMapping("/payment")
public class PaymentController {

    private final OzowService    ozowService;
    private final BookingService bookingService;
    private final UserService    userService;

    public PaymentController(OzowService ozowService,
                             BookingService bookingService,
                             UserService userService) {
        this.ozowService    = ozowService;
        this.bookingService = bookingService;
        this.userService    = userService;
    }

    // ── Initiate payment — builds Ozow form and auto-redirects ───────────────

    @GetMapping("/initiate/{id}")
    public String initiatePayment(@PathVariable UUID id,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  Model model) {
        Booking booking = bookingService.getBookingById(id);

        // Security — only the booking owner can initiate payment
        if (!booking.getUser().getEmail().equals(userDetails.getUsername())) {
            return "redirect:/dashboard";
        }

        // Only allow if booking is in a valid payment state
        String status = booking.getStatus();
        if (!"AWAITING_PAYMENT".equals(status) && !"PENDING_APPROVAL".equals(status)) {
            return "redirect:/dashboard";
        }

        Map<String, String> formFields = ozowService.buildOzowFormFields(booking);

        model.addAttribute("ozowUrl",    ozowService.getOzowUrl());
        model.addAttribute("formFields", formFields);
        return "user/payment-redirect";
    }

    // ── Ozow ITN — server-to-server callback (public, no CSRF, no session) ───

    @PostMapping("/notify")
    @ResponseBody
    public String handleNotify(@RequestParam Map<String, String> params) {
        try {
            ozowService.handleItn(params);
        } catch (Exception e) {
            // Always return 200 to Ozow — never let exceptions cause retries
        }
        return "OK";
    }

    // ── Return pages — Ozow sends user back here after payment ───────────────

    @GetMapping("/success")
    public String paymentSuccess(
            @RequestParam(value = "TransactionReference", required = false) String ref,
            Model model) {
        if (ref != null) {
            try {
                Booking booking = bookingService.getBookingById(UUID.fromString(ref));
                model.addAttribute("fare",    booking.getTrip().getFee() != null
                                                ? "R" + booking.getTrip().getFee() : "TBC");
                model.addAttribute("pickup",  booking.getPickupAddress());
                model.addAttribute("dropoff", booking.getDropoffAddress());
                model.addAttribute("date",    booking.getTrip().getDate());
                model.addAttribute("time",    booking.getTrip().getStartTime());
            } catch (Exception ignored) {}
        }
        return "user/payment-success";
    }

    @GetMapping("/cancel")
    public String paymentCancel(Model model) {
        model.addAttribute("message",
            "Your payment was cancelled. Your booking slot has been released. Please try again.");
        return "user/payment-cancel";
    }

    @GetMapping("/error")
    public String paymentError(Model model) {
        model.addAttribute("message",
            "Something went wrong with your payment. Please try booking again.");
        return "user/payment-cancel";
    }
}