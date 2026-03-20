package com.ajtransportation.app.controller;

import com.ajtransportation.app.model.Trip;
import com.ajtransportation.app.model.TripRequest;
import com.ajtransportation.app.service.TripRequestService;
import com.ajtransportation.app.service.TripService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin/requests")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRequestController {

    private final TripRequestService tripRequestService;
    private final TripService tripService;

    public AdminRequestController(TripRequestService tripRequestService, TripService tripService) {
        this.tripRequestService = tripRequestService;
        this.tripService = tripService;
    }

    @GetMapping
    public String listRequests() {
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/approve/{id}")
    public String approveRequest(
            @PathVariable UUID id,
            @RequestParam(value = "endTime",    required = false) String endTime,
            @RequestParam(value = "fee",        required = false) Double fee,
            @RequestParam(value = "adminNotes", required = false) String adminNotes,
            RedirectAttributes ra) {
        TripRequest req = tripRequestService.getById(id);
        Trip trip = new Trip();
        trip.setDate(req.getRequestedDate());
        trip.setStartTime(req.getRequestedStartTime());
        trip.setLabel(req.getPickupAddress() + " to " + req.getDropoffAddress());
        trip.setPickupAddress(req.getPickupAddress());
        trip.setDropoffAddress(req.getDropoffAddress());
        trip.setStatus("AVAILABLE");
        if (endTime != null && !endTime.isBlank()) trip.setEndTime(LocalTime.parse(endTime));
        if (fee != null) trip.setFee(BigDecimal.valueOf(fee));
        Trip saved = tripService.createTrip(trip);
        tripRequestService.approveRequest(id, saved, adminNotes);
        ra.addFlashAttribute("successMessage", "Request approved and trip created.");
        return "redirect:/admin/requests";
    }

    @PostMapping("/reject/{id}")
    public String rejectRequest(
            @PathVariable UUID id,
            @RequestParam(value = "adminNotes", required = false) String adminNotes,
            RedirectAttributes ra) {
        tripRequestService.rejectRequest(id, adminNotes);
        ra.addFlashAttribute("successMessage", "Request rejected.");
        return "redirect:/admin/requests";
    }
}