package com.ajtransportation.app.service;

import com.ajtransportation.app.model.Trip;
import com.ajtransportation.app.model.TripRequest;
import com.ajtransportation.app.model.User;
import com.ajtransportation.app.repository.TripRequestRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
public class TripRequestService {

    private final TripRequestRepository repo;

    public TripRequestService(TripRequestRepository repo) {
        this.repo = repo;
    }

    public TripRequest createRequest(User user, LocalDate date, LocalTime startTime,
                                     String pickupAddress, String dropoffAddress,
                                     String additionalNotes) {
        boolean alreadyRequested = repo.existsByUserAndRequestedDateAndRequestedStartTimeAndStatusNot(
            user, date, startTime, "REJECTED");
        if (alreadyRequested) {
            throw new RuntimeException(
                "You already have a pending request for this time slot. We will be in touch soon!");
        }
        TripRequest request = new TripRequest();
        request.setUser(user);
        request.setRequestedDate(date);
        request.setRequestedStartTime(startTime);
        request.setPickupAddress(pickupAddress.trim());
        request.setDropoffAddress(dropoffAddress.trim());
        request.setAdditionalNotes(additionalNotes != null ? additionalNotes.trim() : null);
        request.setStatus("PENDING");
        return repo.save(request);
    }

    public List<TripRequest> getUserRequests(User user) {
        return repo.findByUserOrderByCreatedAtDesc(user);
    }

    public List<TripRequest> getPendingRequests() {
        return repo.findByStatusOrderByCreatedAtDesc("PENDING");
    }

    public List<TripRequest> getAllRequests() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    public TripRequest getById(UUID id) {
        return repo.findById(id)
            .orElseThrow(() -> new RuntimeException("Trip request not found: " + id));
    }

    public void approveRequest(UUID requestId, Trip trip, String adminNotes) {
        TripRequest req = getById(requestId);
        req.setStatus("APPROVED");
        req.setTrip(trip);
        req.setAdminNotes(adminNotes);
        repo.save(req);
    }

    public void rejectRequest(UUID requestId, String adminNotes) {
        TripRequest req = getById(requestId);
        req.setStatus("REJECTED");
        req.setAdminNotes(adminNotes);
        repo.save(req);
    }
}