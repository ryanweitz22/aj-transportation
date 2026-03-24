package com.ajtransportation.app.repository;

import com.ajtransportation.app.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    // Look up a payment by Ozow's transaction ID
    Optional<Payment> findByOzowTransactionId(String ozowTransactionId);

    // Find payment linked to a specific booking
    Optional<Payment> findByBookingId(UUID bookingId);
}