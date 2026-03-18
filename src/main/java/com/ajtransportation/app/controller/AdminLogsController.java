package com.ajtransportation.app.controller;

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
import java.util.List;

@Controller
@RequestMapping("/admin/logs")
@PreAuthorize("hasRole('ADMIN')")
public class AdminLogsController {

    private final TripRepository tripRepository;

    public AdminLogsController(TripRepository tripRepository) {
        this.tripRepository = tripRepository;
    }

    @GetMapping
    public String logs(
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Model model) {

        if (from == null) from = LocalDate.now().withDayOfMonth(1);
        if (to   == null) to   = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());

        List<Trip> trips = tripRepository.findByDateBetween(from, to);

        long available = trips.stream().filter(t -> "AVAILABLE".equals(t.getStatus())).count();
        long booked    = trips.stream().filter(t -> "BOOKED".equals(t.getStatus())).count();
        long blocked   = trips.stream().filter(t -> "BLOCKED".equals(t.getStatus())).count();

        BigDecimal revenue = trips.stream()
            .filter(t -> "BOOKED".equals(t.getStatus()) && t.getFee() != null)
            .map(Trip::getFee)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("trips",     trips);
        model.addAttribute("from",      from);
        model.addAttribute("to",        to);
        model.addAttribute("available", available);
        model.addAttribute("booked",    booked);
        model.addAttribute("blocked",   blocked);
        model.addAttribute("revenue",   revenue);
        return "admin/logs";
    }
}