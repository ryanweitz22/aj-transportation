package com.ajtransportation.app.service;

import com.ajtransportation.app.model.Booking;
import com.ajtransportation.app.model.Payment;
import com.ajtransportation.app.repository.BookingRepository;
import com.ajtransportation.app.repository.PaymentRepository;
import com.ajtransportation.app.repository.TripRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * OzowService — Phase 9
 *
 * Handles all Ozow payment logic:
 *   1. Building the redirect form fields for Ozow's hosted payment page
 *   2. Verifying the ITN (Instant Transaction Notification) hash from Ozow
 *   3. Updating booking + payment status on ITN receipt
 *
 * TODAY booking flow:
 *   Admin accepts → booking = AWAITING_PAYMENT
 *   → user waiting screen detects this → JS redirects to /payment/initiate/{id}
 *   → PaymentController builds Ozow form → browser POSTs to Ozow
 *   → Ozow processes → POSTs ITN to /payment/notify
 *   → handleItn() updates booking to CONFIRMED or CANCELLED
 *
 * FUTURE DAY booking flow:
 *   Booking submitted → /payment/initiate/{id} called immediately
 *   → same Ozow form → same ITN flow
 *
 * Credentials live in application-local.properties — never commit them.
 */
@Service
public class OzowService {

    @Value("${ozow.site-code:PLACEHOLDER_SITE_CODE}")
    private String siteCode;

    @Value("${ozow.private-key:PLACEHOLDER_PRIVATE_KEY}")
    private String privateKey;

    @Value("${ozow.sandbox:true}")
    private boolean sandbox;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final TripRepository    tripRepository;

    public OzowService(PaymentRepository paymentRepository,
                       BookingRepository bookingRepository,
                       TripRepository tripRepository) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.tripRepository    = tripRepository;
    }

    // ── Ozow hosted payment page URL ─────────────────────────────────────────
    // Ozow uses the same URL for sandbox and live — IsTest field controls the mode
    public String getOzowUrl() {
        return "https://pay.ozow.com/";
    }

    // ── Build form fields for redirect to Ozow ───────────────────────────────

    /**
     * Returns a LinkedHashMap of field name → value to POST to Ozow.
     * PaymentController renders these as hidden inputs in an auto-submitting form.
     */
    public Map<String, String> buildOzowFormFields(Booking booking) {
        String reference = booking.getId().toString();
        String amount    = formatAmount(booking.getTrip().getFee());
        String bankRef   = "AJT-" + reference.substring(0, 8).toUpperCase();

        String successUrl = baseUrl + "/payment/success";
        String cancelUrl  = baseUrl + "/payment/cancel";
        String errorUrl   = baseUrl + "/payment/error";
        String notifyUrl  = baseUrl + "/payment/notify";

        String isTest = sandbox ? "true" : "false";

        // Ozow hash input (SHA-512):
        // SiteCode + CountryCode + CurrencyCode + Amount + TransactionReference
        // + BankRef + CancelUrl + ErrorUrl + SuccessUrl + IsTest + PrivateKey
        // — all concatenated lowercase
        String hashInput = (siteCode + "ZA" + "ZAR" + amount + reference + bankRef
                + cancelUrl + errorUrl + successUrl + isTest + privateKey).toLowerCase();

        String hashCheck = sha512(hashInput);

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("SiteCode",             siteCode);
        fields.put("CountryCode",          "ZA");
        fields.put("CurrencyCode",         "ZAR");
        fields.put("Amount",               amount);
        fields.put("TransactionReference", reference);
        fields.put("BankRef",              bankRef);
        fields.put("CancelUrl",            cancelUrl);
        fields.put("ErrorUrl",             errorUrl);
        fields.put("SuccessUrl",           successUrl);
        fields.put("NotifyUrl",            notifyUrl);
        fields.put("IsTest",               isTest);
        fields.put("HashCheck",            hashCheck);

        // Create a PENDING Payment record so we can track it in the DB
        createPendingPaymentIfAbsent(booking, reference, amount);

        return fields;
    }

    // ── Handle ITN callback from Ozow ────────────────────────────────────────

    /**
     * Called by PaymentController POST /payment/notify.
     * Verifies the hash then updates booking + trip + payment status.
     *
     * Ozow Status values: "Complete", "Cancelled", "Error", "Abandoned", "PendingInvestigation"
     */
    @Transactional
    public void handleItn(Map<String, String> params) {
        String reference = params.get("TransactionReference");
        String status    = params.get("Status");
        String hashIn    = params.get("Hash");

        if (reference == null || status == null) return;

        if (!verifyItnHash(params, hashIn)) {
            // Hash mismatch — ignore this notification
            return;
        }

        UUID bookingId;
        try {
            bookingId = UUID.fromString(reference);
        } catch (IllegalArgumentException e) {
            return;
        }

        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) return;

        Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);

        if ("Complete".equalsIgnoreCase(status)) {
            // ── Payment successful ───────────────────────────────────────────
            booking.setStatus("CONFIRMED");
            booking.setPaymentStatus("PAID");
            bookingRepository.save(booking);

            booking.getTrip().setStatus("BOOKED");
            tripRepository.save(booking.getTrip());

            if (payment != null) {
                payment.setStatus("PAID");
                payment.setOzowTransactionId(params.getOrDefault("OzowTransactionId", ""));
                paymentRepository.save(payment);
            }

        } else {
            // ── Payment cancelled / failed / abandoned ───────────────────────
            String tripLabel = booking.getTrip().getLabel();

            if ("User Request".equals(tripLabel)) {
                // On-the-fly trip — delete booking then trip
                bookingRepository.delete(booking);
                bookingRepository.flush();
                tripRepository.deleteById(booking.getTrip().getId());
            } else {
                booking.setStatus("CANCELLED");
                booking.setPaymentStatus("FAILED");
                bookingRepository.save(booking);

                booking.getTrip().setStatus("AVAILABLE");
                tripRepository.save(booking.getTrip());
            }

            if (payment != null) {
                payment.setStatus("FAILED");
                paymentRepository.save(payment);
            }
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private void createPendingPaymentIfAbsent(Booking booking, String reference, String amount) {
        if (paymentRepository.findByBookingId(booking.getId()).isPresent()) return;

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(new BigDecimal(amount));
        payment.setOzowReference(reference);
        payment.setPaymentType("OZOW");
        payment.setStatus("PENDING");
        paymentRepository.save(payment);
    }

    /**
     * Verifies Ozow ITN hash.
     * Input: all ITN params except Hash itself, values concatenated in received order,
     * then + privateKey, all lowercased, SHA-512 hashed.
     */
    private boolean verifyItnHash(Map<String, String> params, String receivedHash) {
        if (receivedHash == null) return false;

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!"Hash".equalsIgnoreCase(entry.getKey())) {
                sb.append(entry.getValue());
            }
        }
        sb.append(privateKey);

        String expected = sha512(sb.toString().toLowerCase());
        return expected.equalsIgnoreCase(receivedHash);
    }

    private String sha512(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-512 hashing failed", e);
        }
    }

    private String formatAmount(BigDecimal fee) {
        if (fee == null) return "50.00";
        return String.format("%.2f", fee);
    }
}