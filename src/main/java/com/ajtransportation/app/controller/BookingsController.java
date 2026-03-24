package com.ajtransportation.app.controller;

import com.ajtransportation.app.model.Booking;
import com.ajtransportation.app.model.Trip;
import com.ajtransportation.app.model.User;
import com.ajtransportation.app.service.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class BookingsController {

    private final TripService          tripService;
    private final BookingService       bookingService;
    private final UserService          userService;
    private final BusinessHoursService businessHoursService;
    private final GoogleMapsService    googleMapsService;

    public BookingsController(TripService tripService,
                              BookingService bookingService,
                              UserService userService,
                              BusinessHoursService businessHoursService,
                              GoogleMapsService googleMapsService) {
        this.tripService          = tripService;
        this.bookingService       = bookingService;
        this.userService          = userService;
        this.businessHoursService = businessHoursService;
        this.googleMapsService    = googleMapsService;
    }

    private ObjectMapper buildMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @GetMapping("/bookings")
    public String bookingsPage(
            @RequestParam(value = "week", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week,
            Model model) throws JsonProcessingException {

        LocalDate today = LocalDate.now();
        if (week == null) week = today;
        if (week.isBefore(today)) week = today;

        LocalDate maxStart = businessHoursService.maxBookingDate().minusDays(6);
        if (week.isAfter(maxStart)) week = maxStart;

        List<Trip> trips          = tripService.getVisibleTripsForWeek(week);
        ObjectMapper mapper       = buildMapper();
        String tripsJson          = mapper.writeValueAsString(trips);
        String businessHoursJson  = buildBusinessHoursJson(week, mapper);

        model.addAttribute("tripsJson",         tripsJson);
        model.addAttribute("businessHoursJson", businessHoursJson);
        model.addAttribute("weekStart",  week);
        model.addAttribute("weekEnd",    week.plusDays(6));
        model.addAttribute("prevWeek",   week.minusDays(7));
        model.addAttribute("nextWeek",   week.plusDays(7));
        model.addAttribute("today",      today);
        return "user/bookings";
    }

    @GetMapping("/calculate-fare")
    @ResponseBody
    public Map<String, Object> calculateFare(
            @RequestParam("pickup")  String pickup,
            @RequestParam("dropoff") String dropoff) {

        Map<String, Object> result = new LinkedHashMap<>();
        try {
            var dist = googleMapsService.getDistanceAndEta(pickup, dropoff);
            if (dist != null) {
                var config  = tripService.getPricingConfig();
                var rate    = config.getRatePerKm();
                var minFare = config.getMinimumFare();
                var calc    = dist.distanceKm.multiply(rate);
                var fare    = calc.compareTo(minFare) < 0 ? minFare : calc;
                fare = fare.setScale(2, java.math.RoundingMode.HALF_UP);
                result.put("success",    true);
                result.put("fare",       fare);
                result.put("distanceKm", dist.distanceKm);
                result.put("etaMinutes", dist.etaMinutes);
            } else {
                result.put("success", false);
                result.put("error",   "Could not calculate fare. The driver will confirm your fare.");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("error",   "Could not calculate fare. The driver will confirm your fare.");
        }
        return result;
    }

    @GetMapping("/bookings/status/{id}")
    @ResponseBody
    public Map<String, Object> bookingStatus(@PathVariable UUID id) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            String status = bookingService.getBookingStatusForPolling(id);
            result.put("status", status);
        } catch (Exception e) {
            result.put("status", "ERROR");
        }
        return result;
    }

    @GetMapping("/bookings/pending-count")
    @ResponseBody
    public Map<String, Object> pendingCount() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            List<Booking> pending = bookingService.getPendingBookings();
            result.put("count", pending.size());
            List<Map<String, Object>> bookings = pending.stream().map(b -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id",        b.getId().toString());
                m.put("userEmail", b.getUser().getEmail());
                m.put("pickup",    b.getPickupAddress() != null ? b.getPickupAddress() : "—");
                m.put("dropoff",   b.getDropoffAddress() != null ? b.getDropoffAddress() : "—");
                m.put("fare",      b.getTrip().getFee() != null ? "R" + b.getTrip().getFee() : "TBC");
                m.put("date",      b.getTrip().getDate().toString());
                m.put("time",      b.getTrip().getStartTime().toString());
                m.put("createdAt", b.getCreatedAt().toString());
                return m;
            }).toList();
            result.put("bookings", bookings);
        } catch (Exception e) {
            result.put("count",    0);
            result.put("bookings", List.of());
        }
        return result;
    }

    /**
     * POST /bookings/book
     *
     * TODAY bookings  → waiting screen (admin must accept first, then Ozow redirect)
     * FUTURE bookings → straight to /payment/initiate/{id} (direct Ozow redirect)
     */
    @PostMapping("/bookings/book")
    public String bookTrip(
            @RequestParam(value = "tripId",   required = false) String tripId,
            @RequestParam(value = "slotDate", required = false) String slotDate,
            @RequestParam(value = "slotTime", required = false) String slotTime,
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
            Booking booking;
            LocalDate tripDate;

            if (tripId != null && !tripId.isBlank()) {
                Trip trip = tripService.getTripById(UUID.fromString(tripId));
                if (businessHoursService.isPastSlot(trip.getDate(), trip.getStartTime())) {
                    ra.addFlashAttribute("errorMessage", "That time slot has already passed.");
                    return "redirect:/bookings";
                }
                tripDate = trip.getDate();
                booking  = bookingService.createBooking(user, UUID.fromString(tripId),
                               pickupAddress, dropoffAddress);
            } else {
                if (slotDate == null || slotTime == null) {
                    ra.addFlashAttribute("errorMessage", "Invalid slot selection. Please try again.");
                    return "redirect:/bookings";
                }
                tripDate            = LocalDate.parse(slotDate);
                LocalTime startTime = LocalTime.parse(slotTime);
                if (businessHoursService.isPastSlot(tripDate, startTime)) {
                    ra.addFlashAttribute("errorMessage", "That time slot has already passed.");
                    return "redirect:/bookings";
                }
                booking = bookingService.createBookingForOpenSlot(user, tripDate, startTime,
                              pickupAddress, dropoffAddress);
            }

            // TODAY → waiting screen (admin approves first, then user goes to Ozow)
            // FUTURE → straight to Ozow (no admin approval needed)
            if (tripDate.isEqual(LocalDate.now())) {
                return "redirect:/bookings/waiting/" + booking.getId();
            } else {
                return "redirect:/payment/initiate/" + booking.getId();
            }

        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/bookings";
        }
    }

    @GetMapping("/bookings/waiting/{id}")
    public String waitingScreen(@PathVariable UUID id,
                                @AuthenticationPrincipal UserDetails userDetails,
                                Model model) {
        Booking booking = bookingService.getBookingById(id);
        if (!booking.getUser().getEmail().equals(userDetails.getUsername())) {
            return "redirect:/dashboard";
        }
        model.addAttribute("bookingId", id);
        model.addAttribute("pickup",    booking.getPickupAddress());
        model.addAttribute("dropoff",   booking.getDropoffAddress());
        model.addAttribute("fare",      booking.getTrip().getFee() != null
                                            ? "R" + booking.getTrip().getFee() : "TBC");
        model.addAttribute("date",      booking.getTrip().getDate());
        model.addAttribute("time",      booking.getTrip().getStartTime());
        return "user/booking-waiting";
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

    private String buildBusinessHoursJson(LocalDate weekStart, ObjectMapper mapper)
            throws JsonProcessingException {
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