package com.ajtransportation.app.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    // Ozow's unique transaction ID returned in the ITN callback
    @Column(name = "ozow_transaction_id")
    private String ozowTransactionId;

    // Your internal reference sent to Ozow (bookingId string)
    @Column(name = "ozow_reference")
    private String ozowReference;

    // OZOW or CASH
    @Column(name = "payment_type")
    private String paymentType;

    // PENDING → PAID / FAILED
    @Column(nullable = false)
    private String status = "PENDING";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // ---- Getters & Setters ----

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getOzowTransactionId() { return ozowTransactionId; }
    public void setOzowTransactionId(String ozowTransactionId) { this.ozowTransactionId = ozowTransactionId; }

    public String getOzowReference() { return ozowReference; }
    public void setOzowReference(String ozowReference) { this.ozowReference = ozowReference; }

    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}