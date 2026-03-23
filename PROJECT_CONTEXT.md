# AJ Transportation — Project Context & Checklist
### For use when starting a new Claude chat session
### Last updated: March 23 2026 — Phase 9 (PayFast Payment Integration) IN PROGRESS

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
- Calendar shows: green slots (admin-created trips — book immediately), teal ghost slots (open business hours — any user can book), amber/pending slots (slot just booked, awaiting payment/approval), red slots (confirmed booked), grey (blocked/past/closed)
- **FUTURE DAY bookings:** Clicking a green OR teal slot → 2-step modal → Step 1: enter pickup + dropoff (Google Places Autocomplete), Step 2: shows calculated fare + "Pay Now" → user redirected to PayFast hosted payment page → payment success = auto-CONFIRMED + emails sent. No admin approval needed for future days.
- **TODAY bookings:** Same modal flow → "Request Booking" → waiting screen (120s) → admin accepts → user redirected to PayFast to pay → payment success = CONFIRMED. Admin rejects = no payment ever initiated.
- **ABANDONED payments (future day):** If user reaches PayFast but does not complete payment within 5 minutes, a scheduled task auto-cancels the booking and frees the slot.
- **Failed/cancelled payments:** Booking immediately CANCELLED, trip immediately back to AVAILABLE. User must start a fresh booking — no retry on same booking.
- Admin receives a hard modal popup lock on ALL admin pages whenever a pending booking exists — cannot interact with page until they Accept or Reject. Post-action pause is 3500ms before polling resumes
- Trip pricing: R8.00/km, minimum fare R50.00 — calculated via Google Maps Distance Matrix API — fare is fixed at booking time and cannot be altered by admin
- Open business hours slots: if no admin-created trip exists, a trip is created on the fly when user books. These on-the-fly trips (label = "User Request") are DELETED (not status-changed) if rejected/cancelled — not kept in DB
- Admin navbar: Bookings | Manage Slots | Block Time | Book for Client | Users | Logs
- Admin Bookings tab: read-only view of all upcoming trips with user details and payment status. No action buttons — all actions on Manage Slots
- Admin Manage Slots: full CRUD — block, unblock, cancel (1-hour rule), delete. Shows payment status per booking. Admin can "Mark as Paid (Cash)" for any CONFIRMED booking
- Admin Block Time: dedicated page to block specific time ranges or whole days. Reason is required and shows in Bookings/Manage Slots under Booked By column
- Admin Create Trip Slot: Google Maps autocomplete for pickup/dropoff, auto-generates route label, live fare preview, date/time validation prevents past slots
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
28. **Trip statuses**: `AVAILABLE` → `PENDING` (booking submitted) → `BOOKED` (confirmed/paid) or back to `AVAILABLE` (rejected/cancelled/payment failed)
29. **On-the-fly trips** (label = "User Request") MUST be DELETED not status-changed when rejected/cancelled/payment failed. Delete booking record FIRST (removes FK), then delete trip. This order is critical
30. **`admin-notifications.js`** polls `/bookings/pending-count` every 3 seconds and shows a hard modal lock on all admin pages when pending bookings exist — cannot dismiss without responding. Post-action pause is **3500ms** (not 2000ms). For TODAY bookings, after admin accepts, the waiting screen redirects user to PayFast — admin popup disappears normally.
31. **`booking-waiting.html`** is saved as `booking-waiting.html` (with hyphen) and the controller returns `"user/booking-waiting"` — these MATCH correctly. After CONFIRMED status (future day auto-confirm), page does NOT redirect here — user goes straight to `payment-pending.html`. For TODAY bookings, CONFIRMED on the waiting screen means redirect to PayFast, NOT to dashboard.
32. **`BookingService.java` injects `TripRepository` directly** — do not remove this. It updates trip status within the same `@Transactional` as the booking update to avoid nested transaction conflicts on Supabase pooler
33. **Never use nested `@Transactional` calls between services for booking/trip status updates** — always do both the booking save and trip status update in the same transaction in `BookingService`
34. **`Booking.java`** uses `FetchType.EAGER` for both `user` and `trip` relationships — do NOT change to LAZY or LazyInitializationException will crash the user dashboard
35. `show-sql=false` in `application.properties` — do not re-enable, SQL logging adds extra round-trips
36. **TODAY booking expiry timeout remains 120 seconds** (EXPIRY_SECONDS = 120 in BookingService) — this is the admin response window. FUTURE DAY payment abandonment timeout is **5 minutes** (300 seconds), handled separately by a `@Scheduled` task in `PaymentService`.
37. **`ObjectMapper` in `BookingsController` and `AdminDashboardController`** must have `mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)` — without this, `LocalDate` and `LocalTime` serialize as arrays `[2026,3,20]` instead of strings `"2026-03-20"` which breaks the JS calendar
38. **`TripService.getVisibleTripsForWeek()`** returns ALL statuses including BLOCKED — do not revert to filtering out BLOCKED trips. The JS calendar renders BLOCKED as grey and suppresses ghost slots for that time window
39. **Admin Bookings tab and Manage Slots** both use `BookingRepository.findByTripIdAndStatusNot()` to build a `bookingByTripId` map — this is how user name/phone shows in the table for BOOKED trips
40. **Admin Manage Slots 1-hour cancel rule** — `AdminTripController.cancelBooking()` checks `minutesUntilTrip >= 60` server-side before cancelling. Template shows greyed "🔒 Too close" button when < 60 minutes away
41. **BLOCKED trips show their reason** in the Booked By column on both Bookings and Manage Slots pages. Reason is required when blocking via Block Time page
42. **Past trips are hidden** from Bookings tab and Manage Slots — only today's future times and future dates show. Logs shows full history including past
43. **`/admin/requests`** redirects to `/admin/dashboard` — the requests flow is legacy and unused
44. **Admin calendar default** — week view starts from today not Monday. Day/month views work normally
45. **PayFast integration uses NO external SDK** — plain HTTP POST with MD5 signature and ITN (Instant Transaction Notification) webhook. All PayFast logic lives in `PayFastService.java`. pom.xml does NOT change for Phase 9.
46. **PayFast notify URL (`/payment/notify`) MUST be publicly accessible** — it is called server-to-server by PayFast with no session/cookie. It must be whitelisted in `SecurityConfig.java` as a public POST endpoint. During local dev, use ngrok to expose localhost.
47. **PayFast pre-auth (today bookings):** Uses `payment_type=authorize` in the PayFast request. On admin accept, a capture call is made to PayFast API using the `pf_payment_id` stored in the `Payment` record. On admin reject, a void/cancel call is made. If PayFast does not support pre-auth on the merchant's plan, fallback is: user pays after admin accepts (Option 3 flow — admin accepts first, THEN user pays).
48. **PayFast ITN (webhook) verification:** Must verify the MD5 signature AND call back PayFast's `validate` endpoint to confirm the ITN is genuine before updating any booking/payment status.
49. **`Payment.java`** stores `payfast_payment_id` (PayFast's internal ID for capture/void), `payfast_token` (for pre-auth recurring/capture), and `payment_type` (OZOW legacy field — rename/reuse as needed or add new fields). Hibernate `ddl-auto=update` will add new columns automatically.
50. **Admin cash booking** — only accessible via `/admin/calendar-client` (the "Book for Client" page). The cash payment option is NOT available to regular users at any URL. `SecurityConfig` must restrict `/admin/calendar-client/**` to ADMIN role only.
51. **Email is sent by `EmailService.java`** (new in Phase 9) — uses existing Spring Mail config. Sends: (a) confirmation to user on CONFIRMED+PAID, (b) notification to admin on CONFIRMED+PAID for future-day bookings, (c) confirmation to client on admin cash booking.
52. **`/payment/notify` must use `@Transactional`** — it updates both Booking and Trip status. Same rules apply as all other booking/trip updates: do both in same transaction using `TripRepository` directly, no nested service calls.
53. **pom.xml — NO changes needed for Phase 9.** PayFast uses plain HTTP via `RestTemplate` (already declared in `WebConfig.java`) + MD5 hashing via `java.security.MessageDigest` (built into Java). No new dependencies.

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
```
When `payfast.sandbox=true`, all URLs point to `https://sandbox.payfast.co.za`. Set to `false` for production.

### PayFast Sandbox Setup (for testing)
1. Register free at https://sandbox.payfast.co.za
2. Get sandbox Merchant ID + Merchant Key
3. Set a passphrase in sandbox settings
4. Install ngrok (`winget install ngrok` or download from ngrok.com)
5. Run: `ngrok http 8080` — copy the https URL (e.g. `https://abc123.ngrok.io`)
6. Set `app.base-url=https://abc123.ngrok.io` in `application-local.properties` temporarily
7. PayFast will be able to call your notify URL during local testing

### PayFast URL Endpoints (added in Phase 9)
| URL | Auth | Purpose |
|---|---|---|
| `/payment/initiate/{bookingId}` | Logged in | Builds PayFast form + redirects to PayFast |
| `/payment/notify` | Public (POST) | PayFast ITN webhook — verifies + confirms booking |
| `/payment/success` | Public (GET) | Browser redirect after successful payment |
| `/payment/cancel` | Public (GET) | Browser redirect after cancelled/failed payment |
| `/payment/status/{bookingId}` | Logged in | Poll for payment status (today-flow waiting screen) |
| `/admin/calendar-client` | ADMIN | Admin "Book for Client" calendar with cash option |
| `/admin/bookings/mark-cash/{id}` | ADMIN | Mark a booking as paid by cash manually |

---

## ✅ Phase Completion Status

### Phase 1 — Project Setup: ✅ COMPLETE
### Phase 2 — Spring Boot Project: ✅ COMPLETE
### Phase 3 — Frontend Pages: ✅ COMPLETE
### Phase 4 — Database Tables + Java Models: ✅ COMPLETE
### Phase 5 — User Registration & Login: ✅ COMPLETE
### Phase 6 — Booking Calendar: ✅ COMPLETE
### Phase 7 — Admin Dashboard: ✅ COMPLETE
### Phase 8 — Google Maps + Price-Per-Km: ✅ COMPLETE
### Phase 8.5 — Booking Flow: ✅ COMPLETE
### Phase 9 — PayFast Payment Integration: 🔄 IN PROGRESS
### Phase 10 — SMS Notifications: ⬜ NOT STARTED
### Phase 11 — Security Hardening: ⬜ NOT STARTED
### Phase 12 — Mobile Responsiveness: ⬜ NOT STARTED
### Phase 13 — Deployment: ⬜ NOT STARTED

> ⚠️ NOTE: Email notifications (confirmation to user, notification to admin) are now part of Phase 9, not Phase 10. Phase 10 is SMS only.

---

## 🐛 Bugs Fixed (Do Not Reintroduce)

| Bug | Fix Applied |
|---|---|
| `prepared statement "S_X" already exists` | `?prepareThreshold=0` in JDBC URL + removed all `@Query` annotations + removed `connection-test-query` from HikariCP |
| Supabase MaxClientsInSessionMode | Port changed to 6543 + HikariCP pool size 2 |
| App crashes after ~3 minutes of use | Removed `connection-test-query`, increased `keepalive-time` to 300s, `max-lifetime` to 1800s |
| LazyInitializationException on user dashboard | `Booking.java` changed to `FetchType.EAGER` for Trip and User |
| On-the-fly trips orphaned after rejection | Booking deleted first (FK), then trip deleted — both in same `@Transactional` |
| Accept loops / user sees cancelled after accept | `acceptBooking()` now updates booking + trip in single transaction using `TripRepository` directly — no nested service call |
| Admin popup reappears after accept/reject | `admin-notifications.js` stops polling immediately on respond, waits **3500ms**, then resumes |
| User waiting screen never received CONFIRMED/REJECTED | `getBookingStatusForPolling()` returns "REJECTED" if booking not found (deleted on-the-fly trip) |
| Ghost slots not suppressed for BOOKED/PENDING/BLOCKED trips | `calendar.js` uses `bhClose` as fallback end time for non-AVAILABLE trips without endTime. BLOCKED trips now included in visible trips data |
| `booking-waiting.html` filename mismatch | File renamed to `booking-waiting.html` — matches controller return `"user/booking-waiting"` |
| Past time slot booking | `isPastSlot()` in `BusinessHoursService` blocks past times |
| Admin landing on user dashboard | `AuthController` checks role, redirects admin to `/admin/dashboard` |
| RestTemplate missing bean | `WebConfig.java` with `@Bean RestTemplate` |
| `style.css` broken CSS block | Fully rewritten |
| Date/time serialized as arrays in JS calendar | `ObjectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)` added to `BookingsController` and `AdminDashboardController` |
| BLOCKED slots showing as bookable teal ghost slots | `TripService.getVisibleTripsForWeek()` changed to return ALL statuses — JS renders BLOCKED as grey |
| User not redirected after booking confirmed | Updated in Phase 9 — CONFIRMED now redirects to PayFast or payment-success page, not dashboard directly |
| Past trips showing on admin pages | Filter applied in `AdminDashboardController` and `AdminTripController` — past trips hidden except in Logs |
| User calendar starting from past Monday | `BookingsController` default week start changed to `LocalDate.now()` |

---

## ⚠️ Known Issues / Watch Points

1. **`?prepareThreshold=0`** — verify this is still in `application-local.properties` after any restart. Every prepared statement crash traces back to this being missing
2. **`bookings-pending.html`** at `/admin/bookings/pending` — still exists but is no longer linked from any navbar. Can be deleted in a future cleanup
3. **Duplicate bookings for same open slot** — two users clicking the same ghost slot simultaneously could both succeed. `createBooking()` does trip status check + mark PENDING in one transaction but concurrent load hasn't been tested under high traffic
4. **PayFast notify URL requires public internet access** — during local development, ngrok must be running and `app.base-url` in `application-local.properties` must be set to the ngrok HTTPS URL. The app will still run without this but PayFast webhooks won't reach it (payments will appear stuck until manual resolution)
5. **PayFast pre-auth availability** — card pre-auth (hold then capture for today bookings) requires PayFast to enable it on Ajmal's merchant account. If not available, fallback is Option 3: admin accepts first, then user pays. This is handled by a `payfast.preauth-enabled=true/false` flag in `application-local.properties`

---

## 📁 Current File State

```
src/main/java/com/ajtransportation/app/
├── config/
│   ├── SecurityConfig.java              ✅ /payment/notify whitelisted as public POST. /admin/calendar-client restricted to ADMIN
│   └── WebConfig.java                   ✅ RestTemplate bean (used by PayFastService)
├── controller/
│   ├── PageController.java              ✅ GET + POST /contact
│   ├── AuthController.java              ✅ role-aware /dashboard redirect — admin always goes to /admin/dashboard
│   ├── BookingsController.java          ⚠️ NEEDS UPDATE — bookTrip() must redirect to /payment/initiate/{id} instead of waiting screen for future-day bookings
│   ├── AdminBookingController.java      ⚠️ NEEDS UPDATE — acceptBooking() must: (a) for today bookings set status AWAITING_PAYMENT so waiting screen redirects to PayFast; (b) trigger PayFast capture if pre-auth enabled
│   ├── AdminTripController.java         ✅ includes BLOCKED in listTrips. 1-hour cancel rule. cancel-block/{id} endpoint
│   ├── AdminDashboardController.java    ✅ week starts from today. Past trips filtered. Stats from rawTrips. bookingByTripId map
│   ├── AdminBlockController.java        ✅ /admin/block GET/POST time-range/whole-day/unblock
│   ├── AdminRequestController.java      ✅ redirects to /admin/dashboard (legacy — unused)
│   ├── AdminUserController.java         ✅ /admin/users view/block/verify/delete
│   ├── AdminLogsController.java         ⚠️ NEEDS UPDATE — show payment status in logs view
│   └── PayFastController.java           🆕 NEW — /payment/initiate, /payment/notify, /payment/success, /payment/cancel, /payment/status/{id}, /admin/calendar-client, /admin/bookings/mark-cash/{id}
├── model/
│   ├── User.java                        ✅
│   ├── Trip.java                        ✅
│   ├── Booking.java                     ✅ FetchType.EAGER for user + trip
│   ├── TripRequest.java                 ✅ (legacy — unused)
│   ├── Payment.java                     ⚠️ NEEDS UPDATE — add payfastPaymentId, payfastToken, paymentType fields. Hibernate ddl-auto=update will add columns automatically.
│   ├── PricingConfig.java               ✅
│   └── RegisterRequest.java             ✅
├── repository/
│   ├── UserRepository.java              ✅
│   ├── TripRepository.java              ✅
│   ├── BookingRepository.java           ✅
│   ├── TripRequestRepository.java       ✅ (legacy)
│   ├── PaymentRepository.java           ✅ findByOzowReference → rename to findByPayfastPaymentId in Phase 9
│   └── PricingConfigRepository.java     ✅
├── service/
│   ├── BookingService.java              ⚠️ NEEDS UPDATE — acceptBooking() status change to AWAITING_PAYMENT for today bookings; new confirmBookingAfterPayment(); new cancelBookingAfterFailedPayment(); EXPIRY_SECONDS stays 120 for today bookings
│   ├── TripService.java                 ✅
│   ├── BusinessHoursService.java        ✅ OPEN EVERY DAY 04:00–23:00 + isPastSlot()
│   ├── GoogleMapsService.java           ✅
│   ├── UserService.java                 ✅
│   ├── CustomUserDetailsService.java    ✅
│   ├── TripRequestService.java          ✅ (legacy — unused)
│   ├── PayFastService.java              🆕 NEW — builds PayFast request, verifies ITN signature, capture/void for pre-auth
│   ├── PaymentService.java              🆕 NEW — creates Payment records, handles expiry scheduled task (5 min for future-day), updates payment statuses
│   └── EmailService.java               🆕 NEW — sends confirmation email to user + notification to admin

src/main/resources/
├── application.properties               ⚠️ NEEDS UPDATE — add payfast.* config keys (without values)
├── static/
│   ├── css/
│   │   ├── style.css                    ✅
│   │   └── bookings.css                 ✅
│   └── js/
│       ├── main.js                      ✅
│       ├── calendar.js                  ✅
│       ├── bookings.js                  ⚠️ NEEDS UPDATE — Step 2 modal button changes to "Pay Now" for future-day, "Request Booking" for today. Form action changes accordingly.
│       └── admin-notifications.js       ⚠️ NEEDS UPDATE — for TODAY bookings, admin accept now polls /payment/status/{id} after the popup closes. Waiting screen behaviour updated.
└── templates/
    ├── index.html                       ✅
    ├── about.html                       ✅
    ├── contact.html                     ✅
    ├── admin/
    │   ├── dashboard.html               ⚠️ NEEDS UPDATE — add payment status column to bookings table
    │   ├── block.html                   ✅
    │   ├── bookings-pending.html        ✅ (legacy — no longer linked from navbar)
    │   ├── trips-list.html              ⚠️ NEEDS UPDATE — show payment status per booking. Add "Mark as Paid (Cash)" button for CONFIRMED+UNPAID bookings (admin only)
    │   ├── trips-new.html               ✅
    │   ├── pricing.html                 ✅
    │   ├── requests.html                ✅ (legacy — unused)
    │   ├── users.html                   ✅
    │   ├── logs.html                    ⚠️ NEEDS UPDATE — show payment status column
    │   └── calendar-client.html         🆕 NEW — admin "Book for Client" view. Same calendar UI as user but with Cash Payment toggle in modal. ADMIN only.
    ├── auth/
    │   ├── login.html                   ✅
    │   └── register.html                ✅
    └── user/
        ├── bookings.html                ✅ Calendar starts from today. Past slots greyed out
        ├── booking-waiting.html         ⚠️ NEEDS UPDATE — on CONFIRMED (today flow), redirect to PayFast instead of dashboard. New status AWAITING_PAYMENT triggers PayFast redirect.
        ├── dashboard.html               ⚠️ NEEDS UPDATE — show payment status badge per booking. Remove "Pay Now" button (no retry — fresh booking required on failure)
        ├── payment-pending.html         🆕 NEW — "Redirecting to payment..." intermediate page with 3-second auto-redirect to PayFast
        ├── payment-success.html         🆕 NEW — shown after PayFast success redirect. "You're all booked!" with booking summary
        └── payment-failed.html          🆕 NEW — shown after PayFast cancel/fail redirect. "Payment was not completed." with link to try again
```

---

## ⚙️ application-local.properties (GITIGNORED — share via WhatsApp only)
```properties
spring.datasource.url=jdbc:postgresql://[POOLER_HOST]:6543/postgres?prepareThreshold=0
spring.datasource.username=postgres.[PROJECT_ID]
spring.datasource.password=[DB_PASSWORD]
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.hikari.maximum-pool-size=2
spring.datasource.hikari.minimum-idle=1
supabase.url=https://[PROJECT_ID].supabase.co
supabase.anon-key=[ANON_KEY]
supabase.service-role-key=[SERVICE_ROLE_KEY]
spring.mail.username=YOUR_GMAIL@gmail.com
spring.mail.password=YOUR_GMAIL_APP_PASSWORD
google.maps.api-key=YOUR_GOOGLE_MAPS_API_KEY
payfast.merchant-id=YOUR_PAYFAST_MERCHANT_ID
payfast.merchant-key=YOUR_PAYFAST_MERCHANT_KEY
payfast.passphrase=YOUR_PAYFAST_PASSPHRASE
payfast.sandbox=true
payfast.preauth-enabled=false
```

**⚠️ The `?prepareThreshold=0` on the datasource URL is THE most critical setting in the entire project. Every prepared statement crash is caused by this being missing or wrong.**

---

## 🗺️ URL Routing

| URL | Status | Auth |
|---|---|---|
| `/` | ✅ | Public |
| `/about` | ✅ | Public |
| `/contact` GET + POST | ✅ | Public |
| `/bookings` | ✅ | Public |
| `/bookings/book` POST | ✅ | Logged in |
| `/bookings/waiting/{id}` GET | ✅ | Logged in |
| `/bookings/status/{id}` GET | ✅ | Public (polled by JS) |
| `/bookings/pending-count` GET | ✅ | Public (polled by admin JS) |
| `/bookings/calculate-fare` GET | ✅ | Public |
| `/bookings/cancel/{id}` POST | ✅ | Logged in |
| `/login` | ✅ | Public |
| `/register` | ✅ | Public |
| `/verify-email?token=` | ✅ | Public |
| `/dashboard` | ✅ | Logged in (redirects admin to /admin/dashboard) |
| `/logout` | ✅ | Logged in |
| `/payment/initiate/{bookingId}` | 🆕 Phase 9 | Logged in |
| `/payment/notify` | 🆕 Phase 9 | Public POST (PayFast webhook — no session) |
| `/payment/success` | 🆕 Phase 9 | Public GET |
| `/payment/cancel` | 🆕 Phase 9 | Public GET |
| `/payment/status/{bookingId}` | 🆕 Phase 9 | Logged in (polled by waiting screen) |
| `/admin/dashboard` | ✅ | ADMIN |
| `/admin/bookings/pending` | ✅ | ADMIN (legacy — not linked) |
| `/admin/bookings/accept/{id}` POST | ✅ | ADMIN |
| `/admin/bookings/reject/{id}` POST | ✅ | ADMIN |
| `/admin/bookings/mark-cash/{id}` POST | 🆕 Phase 9 | ADMIN |
| `/admin/calendar-client` | 🆕 Phase 9 | ADMIN |
| `/admin/calendar-client/book` POST | 🆕 Phase 9 | ADMIN |
| `/admin/trips` | ✅ | ADMIN |
| `/admin/trips/new` | ✅ | ADMIN |
| `/admin/trips/pricing` | ✅ | ADMIN |
| `/admin/trips/block/{id}` POST | ✅ | ADMIN |
| `/admin/trips/unblock/{id}` POST | ✅ | ADMIN |
| `/admin/trips/delete/{id}` POST | ✅ | ADMIN |
| `/admin/trips/cancel/{id}` POST | ✅ | ADMIN (1-hour rule enforced) |
| `/admin/trips/cancel-block/{id}` POST | ✅ | ADMIN |
| `/admin/block` GET | ✅ | ADMIN |
| `/admin/block/time-range` POST | ✅ | ADMIN |
| `/admin/block/whole-day` POST | ✅ | ADMIN |
| `/admin/block/unblock/{id}` POST | ✅ | ADMIN |
| `/admin/users` | ✅ | ADMIN |
| `/admin/logs` | ✅ | ADMIN |
| `/admin/requests` | ✅ | ADMIN (redirects to dashboard — legacy) |

---

## 📦 pom.xml Dependencies (NO CHANGES for Phase 9)
```
spring-boot-starter-data-jpa
spring-boot-starter-mail
spring-boot-starter-security
spring-boot-starter-thymeleaf
spring-boot-starter-validation
spring-boot-starter-webmvc
thymeleaf-extras-springsecurity6
spring-boot-devtools (runtime)
postgresql (runtime)
jackson-datatype-jsr310
```
PayFast uses plain HTTP via RestTemplate (already in WebConfig) + MD5 via java.security.MessageDigest (built-in Java). No new dependencies needed.

---

## 🔑 Admin Account Setup in Supabase
```sql
INSERT INTO users (id, email, username, password, role, email_verified, is_blocked, created_at)
VALUES (
  gen_random_uuid(),
  'admin@ajtransportation.co.za',
  'admin',
  '$2a$10$PASTE_YOUR_BCRYPT_HASH_HERE',
  'ADMIN',
  true,
  false,
  NOW()
);
```

---

## 🚀 How to Run
```
1. VS Code → open project folder
2. Terminal (Ctrl + `): mvn spring-boot:run
3. Browser: http://localhost:8080
4. Stop: Ctrl+C

For PayFast webhook testing (local dev):
5. Download ngrok from ngrok.com and install
6. In a second terminal: ngrok http 8080
7. Copy the https://xxx.ngrok.io URL
8. Set app.base-url=https://xxx.ngrok.io in application-local.properties
9. PayFast can now reach your local /payment/notify endpoint
```

---

## 🔄 Team Workflow

1. GitHub Desktop → Fetch origin → Pull origin before starting
2. Make changes in VS Code
3. GitHub Desktop → write commit message → Commit to main → Push origin
4. Share `application-local.properties` via WhatsApp only — never via GitHub

---

## 💡 Claude Behaviour Rules for This Project

- **Never produce downloadable files** — always give the full file contents as copyable code in the chat so the user can paste directly into VS Code
- Always give **full file contents** — never partial edits, never "find this line and replace"
- Max **4 files per batch** — wait for user confirmation before next batch
- Always **pause at natural test points** — compile check, then functional test, then next batch
- Never use `@Query` annotations in Spring Data repositories
- Never change pom.xml dependencies
- Never use `javax.*` imports — always `jakarta.*`
- Always read the gitingest carefully before writing any code
- Never add `connection-test-query` to HikariCP settings
- Never change `Booking.java` FetchType back to LAZY
- Never make nested `@Transactional` service-to-service calls for booking/trip status updates — always update both in `BookingService` using `TripRepository` directly
- Always provide the **full file** — the user pastes the entire file into VS Code, they cannot do partial edits
- Always confirm understanding and ask clarifying questions before writing code for complex features
- **Never reference Ozow anywhere in new code** — the payment gateway is PayFast. Any existing Ozow references (e.g. `findByOzowReference` in PaymentRepository) must be updated to PayFast equivalents
- **PayFast notify endpoint must always be `@Transactional`** and must update both Booking and Trip in the same transaction
- **Never expose PayFast credentials in templates or JS** — merchant ID and key are server-side only
