package com.ajtransportation.app.controller;

import com.ajtransportation.app.model.Booking;
import com.ajtransportation.app.model.Trip;
import com.ajtransportation.app.repository.BookingRepository;
import com.ajtransportation.app.repository.TripRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/logs")
@PreAuthorize("hasRole('ADMIN')")
public class AdminLogsController {

    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;

    public AdminLogsController(TripRepository tripRepository,
                                BookingRepository bookingRepository) {
        this.tripRepository = tripRepository;
        this.bookingRepository = bookingRepository;
    }

    @GetMapping
    public String logs(
            @RequestParam(value = "from",   required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to",     required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "search", required = false) String search,
            Model model) {

        if (from == null) from = LocalDate.now().withDayOfMonth(1);
        if (to   == null) to   = LocalDate.now()
                                    .withDayOfMonth(LocalDate.now().lengthOfMonth());

        List<Trip> allTrips = tripRepository.findByDateBetween(from, to);

        // Build bookingByTripId map for all BOOKED trips in the range
        Map<UUID, Booking> bookingByTripId = new HashMap<>();
        for (Trip trip : allTrips) {
            if ("BOOKED".equals(trip.getStatus())) {
                bookingRepository
                    .findByTripIdAndStatusNot(trip.getId(), "CANCELLED")
                    .ifPresent(b -> bookingByTripId.put(trip.getId(), b));
            }
        }

        // Apply search filter — match against username, email or phone
        List<Trip> trips;
        if (search != null && !search.isBlank()) {
            final String term = search.trim().toLowerCase();
            trips = allTrips.stream()
                .filter(t -> {
                    // Always show blocked trips — they have no user
                    if ("BLOCKED".equals(t.getStatus())) {
                        // Match against blocked reason if search term matches
                        String reason = t.getBlockedReason();
                        return reason != null
                            && reason.toLowerCase().contains(term);
                    }
                    Booking b = bookingByTripId.get(t.getId());
                    if (b == null) return false;
                    String username = b.getUser().getUsername();
                    String email    = b.getUser().getEmail();
                    String phone    = b.getUser().getPhoneNumber();
                    return (username != null && username.toLowerCase().contains(term))
                        || (email    != null && email.toLowerCase().contains(term))
                        || (phone    != null && phone.toLowerCase().contains(term));
                })
                .collect(Collectors.toList());
        } else {
            trips = allTrips;
        }

        long available = trips.stream()
            .filter(t -> "AVAILABLE".equals(t.getStatus())).count();
        long booked    = trips.stream()
            .filter(t -> "BOOKED".equals(t.getStatus())).count();
        long blocked   = trips.stream()
            .filter(t -> "BLOCKED".equals(t.getStatus())).count();

        BigDecimal revenue = trips.stream()
            .filter(t -> "BOOKED".equals(t.getStatus()) && t.getFee() != null)
            .map(Trip::getFee)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("trips",            trips);
        model.addAttribute("bookingByTripId",  bookingByTripId);
        model.addAttribute("from",             from);
        model.addAttribute("to",               to);
        model.addAttribute("search",           search != null ? search : "");
        model.addAttribute("available",        available);
        model.addAttribute("booked",           booked);
        model.addAttribute("blocked",          blocked);
        model.addAttribute("revenue",          revenue);
        return "admin/logs";
    }
}