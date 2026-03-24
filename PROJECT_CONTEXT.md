# AJ Transportation — Project Context & Checklist
### For use when starting a new Claude chat session
### Last updated: March 24 2026 — Phase 9 Batch 0 complete, Batch 1 next

---

## 🧠 Project Summary

**App name:** AJ Transportation
**Purpose:** Web-based transportation booking system
**Owner:** Uncle Ajmal (South Africa)
**Team size:** 2 people (both complete beginners)
**IDE:** VS Code
**Version control:** GitHub Desktop only (no CLI git)

**What the app does (current live behaviour):**
- Customers register via email / phone number / username / password
- Email verification exists but is currently bypassed for development — all users have `email_verified = true` set manually in Supabase
- User booking calendar starts from today — past days hidden, past time slots on today greyed out and unclickable
- Calendar shows: green slots (admin-created AVAILABLE trips — clickable, book now), white slots (open-hours with no admin trip — clickable, book now), red slots (BOOKED — unavailable), grey slots (BLOCKED by admin or past time — unavailable)
- Clicking a green OR open-hours slot → 2-step modal → Step 1: enter pickup + dropoff (Google Places Autocomplete), Step 2: shows calculated fare (R8/km, min R50) + "Proceed to Payment" → booking submitted → redirected to waiting screen
- **The ONLY difference between a today booking and a future booking is how payment is processed — the modal and user experience are identical:**
  - **TODAY bookings:** booking submitted → waiting screen (120s) → admin accepts → user redirected to PayFast to pay → payment success = CONFIRMED. Admin rejects = no payment ever initiated.
  - **FUTURE DAY bookings:** booking submitted → user redirected directly to PayFast → payment success = auto-CONFIRMED + emails sent. No admin approval needed.
- **ABANDONED payments (future day):** If user reaches PayFast but does not complete payment within 5 minutes, a scheduled task auto-cancels the booking and frees the slot.
- **Failed/cancelled payments:** Booking immediately CANCELLED, trip immediately back to AVAILABLE. User must start a fresh booking — no retry on same booking.
- Admin receives a hard modal popup lock on ALL admin pages whenever a pending booking exists — cannot interact with page until they Accept or Reject. Post-action pause is 3500ms before polling resumes
- Trip pricing: R8.00/km, minimum fare R50.00 — calculated via Google Maps Distance Matrix API — fare is fixed at booking time and cannot be altered by admin
- Open-hours on-the-fly trips (label = "User Request") are DELETED not status-changed when rejected/cancelled/payment failed — delete booking first (FK), then trip
- Admin navbar: Bookings | Manage Slots | Block Time | Book for Client | Users | Logs
- Admin Bookings tab: read-only view of all upcoming trips with user details and payment status. No action buttons — all actions on Manage Slots
- Admin Manage Slots: full CRUD — block, unblock, cancel (1-hour rule), delete. Shows payment status per booking. Admin can "Mark as Paid (Cash)" for any CONFIRMED booking
- Admin Block Time: dedicated page to block specific time ranges or whole days. Reason is required and shows in Bookings/Manage Slots under Booked By column
- Admin Create Trip Slot: Google Maps autocomplete for pickup/dropoff, auto-generates route label, live fare preview, date/time validation prevents past slots, client name and notes fields for cash bookings
- **Admin "Book for Client" calendar:** Admin sees same user-facing calendar UI but with an exclusive "Cash Payment" option in the booking modal. Cash bookings are immediately CONFIRMED + paymentStatus = CASH, no PayFast redirect, confirmation email sent to client.
- Admin Logs: full history with date range filter + search by username/email/phone. Payment status shown per entry.
- Payments via PayFast (EFT + card pre-auth) — Phase 9 IN PROGRESS
- Email notifications (booking confirmation to user, notification to admin) — implemented as part of Phase 9
- SMS Notifications — Phase 10 (not started)

---

## 🛠️ Tech Stack (Fixed — do NOT change under any circumstances)

| Layer | Technology |
|---|---|
| Language | Java |
| Backend | Spring Boot 4.0.3 |
| Frontend | Thymeleaf + HTML + CSS + JavaScript |
| Database | Supabase (PostgreSQL 17.6) |
| Payments | PayFast (EFT + card pre-auth, South Africa) |
| Maps/Distance | Google Maps Distance Matrix API + Places API |
| Build tool | Apache Maven 3.9.14 |
| Security | Spring Security |
| Version control | GitHub Desktop |
| IDE | VS Code |

---

## 🚨 Critical Notes for Claude — Read Before Writing Any Code

1. Spring Boot **4.0.3** — always use `jakarta.*` not `javax.*` imports
2. Java on machine is 25, project targets 21 — do not change pom.xml
3. `application-local.properties` is gitignored — never commit it, share via WhatsApp only
4. Supabase uses **port 6543** (Transaction mode pooler) — NOT 5432
5. **`?prepareThreshold=0` in the JDBC URL is the single most critical setting** — without it the app crashes constantly on Supabase pooler with `prepared statement "S_X" already exists`. Must be in `application-local.properties` URL exactly like: `jdbc:postgresql://HOST:6543/postgres?prepareThreshold=0`
6. HikariCP pool: `maximum-pool-size=2`, `minimum-idle=1` — in `application-local.properties`
7. **NEVER add `connection-test-query` to HikariCP settings** — it conflicts with `prepareThreshold=0` and causes `prepared statement "S_X" already exists` crashes
8. Supabase username format: `postgres.PROJECT_ID`
9. GitHub Desktop only — never give CLI git commands to the team
10. Both teammates are complete beginners — explain every step clearly
11. **Payment gateway: PayFast only** — supports both EFT and card pre-authorisation. No Ozow. No Stripe. Do not reference Ozow anywhere in new code.
12. Fonts: **Syne** (headings) + **DM Sans** (body) — never change these
13. Colors: primary `#0a7c6e` (teal), accent `#f0a500` (gold), dark bg `#0d1117`
14. Rate per km: **R8.00/km**, minimum fare: **R50.00** — fare is calculated at booking time and is FIXED — admin cannot change it after booking
15. Google Maps API key in `application-local.properties` as `google.maps.api-key`
16. Google Maps Places API is used for frontend autocomplete — loaded in `bookings.html` AND `trips-new.html` AND `admin/calendar-client.html` via dynamic script tag using the same key
17. `app.base-url=http://localhost:8080` in `application.properties` — change for production. PayFast notify/success/cancel URLs are built from this base URL.
18. `RestTemplate` bean declared in `WebConfig.java` — Spring Boot 4 does NOT auto-create it. Used by PayFastService for server-to-server PayFast API calls.
19. Admin account must have `email_verified = true` set manually in Supabase SQL
20. `style.css` was fully rewritten to fix a broken unclosed CSS block — do not revert to old version
21. `contact.html` form uses `POST /contact` — handled by `PageController.java`
22. **Do NOT use `@Query` annotations in repositories** — they cause prepared statement crashes on Supabase transaction pooler. Use derived method names only
23. **Always give full file contents** when providing code — never partial edits or "find and replace" instructions
24. **Max 4 files per batch** — wait for user confirmation before next batch
25. **Admin login** — `/dashboard` checks role and redirects admin to `/admin/dashboard` automatically. Admin will NEVER land on the user dashboard under any circumstance
26. **Booking statuses**: `PENDING_APPROVAL` → `AWAITING_PAYMENT` (today flow: admin accepted, user must now pay) or `CONFIRMED` (future flow: payment succeeded, or admin cash booking) or `REJECTED` (admin rejected) or `CANCELLED` (user cancelled / timed out / payment failed) or `EXPIRED` (auto-cancelled after timeout)
27. **Payment statuses on Booking**: `UNPAID` → `AWAITING_PAYMENT` (today flow: admin accepted, awaiting PayFast) → `PAID` (PayFast success) or `FAILED` (PayFast failure). Cash bookings: immediately `CASH`. Never changes from PAID once set.
28. **Trip statuses**: `AVAILABLE` → `PENDING` (user booking submitted) → `BOOKED` (confirmed/paid) or back to `AVAILABLE` (rejected/cancelled/payment failed). **Admin cash bookings create trips with status BOOKED immediately** (not AVAILABLE — the time slot is taken).
29. **On-the-fly trips** (label = "User Request") — when a user clicks an open-hours slot (no admin trip exists), a trip is created on the fly. These MUST be DELETED not status-changed when rejected/cancelled/payment failed. Delete booking record FIRST (removes FK), then delete trip. This order is critical.
30. **`admin-notifications.js`** polls `/bookings/pending-count` every 3 seconds and shows a hard modal lock on all admin pages when pending bookings exist — cannot dismiss without responding. Post-action pause is **3500ms** (not 2000ms). For TODAY bookings, after admin accepts, the waiting screen redirects user to PayFast — admin popup disappears normally.
31. **`booking-waiting.html`** is only used for TODAY bookings. It polls `/bookings/status/{id}` and on `AWAITING_PAYMENT` status (admin accepted) redirects the user to `/payment/initiate/{id}`. For FUTURE DAY bookings the user goes directly to `/payment/initiate/{id}` — the waiting screen is never shown.
32. **`BookingService.java` injects `TripRepository` directly** — do not remove this. It updates trip status within the same `@Transactional` as the booking update to avoid nested transaction conflicts on Supabase pooler
33. **Never use nested `@Transactional` calls between services for booking/trip status updates** — always do both the booking save and trip status update in the same transaction in `BookingService`
34. **`Booking.java`** uses `FetchType.EAGER` for both `user` and `trip` relationships — do NOT change to LAZY or LazyInitializationException will crash the user dashboard
35. `show-sql=false` in `application.properties` — do not re-enable, SQL logging adds extra round-trips
36. **TODAY booking expiry timeout remains 120 seconds** (EXPIRY_SECONDS = 120 in BookingService) — this is the admin response window. FUTURE DAY payment abandonment timeout is **5 minutes** (300 seconds), handled separately by a `@Scheduled` task in `PaymentService`.
37. **`ObjectMapper` in `BookingsController` and `AdminDashboardController`** must have `mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)` — without this, `LocalDate` and `LocalTime` serialize as arrays `[2026,3,20]` instead of strings `"2026-03-20"` which breaks the JS calendar
38. **`TripService.getVisibleTripsForWeek()`** returns ALL statuses including BLOCKED — do not revert to filtering out BLOCKED trips. The JS calendar renders BLOCKED as grey
39. **Admin Bookings tab and Manage Slots** both use `BookingRepository.findByTripIdAndStatusNot()` to build a `bookingByTripId` map — this is how user name/phone shows in the table for BOOKED trips
40. **Admin Manage Slots 1-hour cancel rule** — `AdminTripController.cancelBooking()` checks `minutesUntilTrip >= 60` server-side before cancelling. Template shows greyed "🔒 Too close" button when < 60 minutes away
41. **BLOCKED trips and cash bookings show their reason/client info** in the Booked By column on both Bookings and Manage Slots pages. For cash bookings: "Client: [name] | Notes: [notes]". For blocked trips: reason is required when blocking via Block Time page.
42. **Past trips are hidden** from Bookings tab and Manage Slots — only today's future times and future dates show. Logs shows full history including past
43. **`/admin/requests`** redirects to `/admin/dashboard` — the requests flow is legacy and unused
44. **Admin calendar default** — week view starts from today not Monday. Day/month views work normally
45. **PayFast integration uses NO external SDK** — plain HTTP POST with MD5 signature and ITN (Instant Transaction Notification) webhook. All PayFast logic lives in `PayFastService.java`. pom.xml does NOT change for Phase 9.
46. **PayFast notify URL (`/payment/notify`) MUST be publicly accessible** — it is called server-to-server by PayFast with no session/cookie. It must be whitelisted in `SecurityConfig.java` as a public POST endpoint. During local dev, use ngrok to expose localhost.
47. **PayFast pre-auth (today bookings):** Uses `payment_type=authorize` in the PayFast request. On admin accept, a capture call is made to PayFast API using the `pf_payment_id` stored in the `Payment` record. On admin reject, a void/cancel call is made. If PayFast does not support pre-auth on the merchant's plan, fallback is Option 3: admin accepts first, THEN user pays.
48. **PayFast ITN (webhook) verification:** Must verify the MD5 signature AND call back PayFast's `validate` endpoint to confirm the ITN is genuine before updating any booking/payment status.
49. **`Payment.java`** stores `payfastPaymentId` (PayFast's internal ID for capture/void), `payfastToken` (for pre-auth capture), and `paymentType` (`PAYFAST` or `CASH`). Column `ozow_reference` was renamed to `payfast_payment_id` in the DB via SQL migration before Phase 9 began.
50. **Admin cash booking** — created via `/admin/trips/new` (Create Trip Slot form). Admin enters client name + notes. Trip saves with status BOOKED immediately (slot is taken, shows red on user calendar). Booking record created with paymentStatus=CASH. NOT accessible to regular users. `SecurityConfig` must restrict `/admin/trips/new` to ADMIN role only.
51. **Email is sent by `EmailService.java`** (new in Phase 9) — uses existing Spring Mail config. Sends: (a) confirmation to user on CONFIRMED+PAID, (b) notification to admin on auto-confirmed future-day bookings, (c) confirmation to client on admin cash booking.
52. **`/payment/notify` must use `@Transactional`** — it updates both Booking and Trip status. Same rules apply as all other booking/trip updates: do both in same transaction using `TripRepository` directly, no nested service calls.
53. **pom.xml — NO changes needed for Phase 9.** PayFast uses plain HTTP via `RestTemplate` (already declared in `WebConfig.java`) + MD5 hashing via `java.security.MessageDigest` (built into Java). No new dependencies.
54. **`calendar.js` slot types:** `slot-available` (green, admin-created AVAILABLE trips — clickable), `slot-open-hours` (open business hours with no admin trip — clickable, triggers on-the-fly booking), `slot-pending` (PENDING trip — not clickable), `slot-booked` (red, BOOKED — not clickable), `slot-blocked` (grey, BLOCKED — not clickable), `slot-past-today` (past time on today — not clickable).
55. **`bookings.html` has two modals** — Modal 1 (`booking-modal`) for green/admin-created slots, Modal 2 (`request-modal`) for open-hours slots. Both are live and functional. Do not remove either.

---

## 💳 PayFast Integration — Key Details

### How PayFast Works (Plain HTTP, No SDK)
1. Your server builds a POST form with payment fields + MD5 signature
2. User's browser is redirected to PayFast's hosted payment page
3. User pays via EFT or card on PayFast's servers (you never see card data)
4. PayFast POSTs an ITN (Instant Transaction Notification) to your notify URL
5. Your server verifies the ITN signature + validates with PayFast, then updates booking

### PayFast Credentials (in application-local.properties — NEVER commit)
```properties
payfast.merchant-id=YOUR_MERCHANT_ID
payfast.merchant-key=YOUR_MERCHANT_KEY
payfast.passphrase=YOUR_PASSPHRASE
payfast.sandbox=true
payfast.preauth-enabled=false