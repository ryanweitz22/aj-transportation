# AJ Transportation — Project Context & Checklist
### For use when starting a new Claude chat session
### Last updated: March 24 2026 — Phase 9 Ozow integration in progress, Batch 3 complete

---

## 🧠 Project Summary

**App name:** AJ Transportation
**Purpose:** Web-based transportation booking system
**Owner:** Uncle Ajmal (South Africa)
**Team size:** 2 people (both complete beginners)
**IDE:** VS Code
**Version control:** GitHub Desktop only (no CLI git)

**What the app does (current live behaviour):**
- Customers register via email / phone number / username / password. Login accepts either email OR username (case-insensitive).
- Email verification exists but is currently bypassed for development — all users have `email_verified = true` set manually in Supabase
- User booking calendar starts from today — past days hidden, past time slots on today greyed out and unclickable
- Calendar shows: green slots (admin-created AVAILABLE trips — clickable, book now), white slots (open-hours with no admin trip — clickable, book now), red slots (BOOKED — unavailable), grey slots (BLOCKED by admin or past time — unavailable)
- Clicking a green OR open-hours slot → 2-step modal → Step 1: enter pickup + dropoff (Google Places Autocomplete), Step 2: shows calculated fare (R8/km, min R50) + "Proceed to Payment" → booking submitted → redirected based on date:
- **The ONLY difference between a today booking and a future booking is how payment is processed — the modal and user experience are identical:**
  - **TODAY bookings:** booking submitted → waiting screen (120s) → admin accepts → booking status = `AWAITING_PAYMENT` → user's waiting screen detects this → redirects to `/payment/initiate/{id}` → `payment-redirect.html` auto-POSTs to Ozow hosted payment page → Ozow processes → ITN posted to `/payment/notify` → booking = CONFIRMED. Admin rejects = no payment ever initiated.
  - **FUTURE DAY bookings:** booking submitted → user redirected directly to `/payment/initiate/{id}` → same Ozow redirect flow. No admin approval needed.
- **Failed/cancelled payments:** Booking immediately CANCELLED, trip immediately back to AVAILABLE. User must start a fresh booking — no retry on same booking.
- Admin receives a hard modal popup lock on ALL admin pages whenever a pending booking exists — cannot interact with page until they Accept or Reject. Post-action pause is 3500ms before polling resumes.
- Trip pricing: R8.00/km, minimum fare R50.00 — calculated via Google Maps Distance Matrix API — fare is fixed at booking time and cannot be altered by admin
- Open-hours on-the-fly trips (label = "User Request") are DELETED not status-changed when rejected/cancelled/payment failed — delete booking first (FK), then trip
- Admin navbar: Bookings | Manage Slots | Block Time | Book for Client | Users | Logs
- Admin Bookings tab: read-only view of all upcoming trips with user details and payment status. No action buttons — all actions on Manage Slots
- Admin Manage Slots: full CRUD — block, unblock, cancel (1-hour rule), delete. Shows payment status per booking. Admin can "Mark as Paid (Cash)" for any CONFIRMED booking
- Admin Block Time: dedicated page to block specific time ranges or whole days. Reason is required and shows in Bookings/Manage Slots under Booked By column
- Admin Create Trip Slot: Google Maps autocomplete for pickup/dropoff, auto-generates route label, live fare preview, date/time validation prevents past slots, client name and notes fields for cash bookings
- **Admin "Book for Client" calendar:** Admin sees same user-facing calendar UI but with an exclusive "Cash Payment" option in the booking modal. Cash bookings are immediately CONFIRMED + paymentStatus = CASH, no Ozow redirect, confirmation email sent to client.
- Admin Logs: full history with date range filter + search by username/email/phone. Payment status shown per entry.
- Payments via Ozow (South African EFT) — Phase 9 IN PROGRESS
- Email notifications (booking confirmation to user, notification to admin) — implemented as part of Phase 9
- SMS Notifications — Phase 10 (not started)

---

## 🛠️ Tech Stack (Fixed — do NOT change under any circumstances)

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Backend | Spring Boot 4.0.3 |
| Frontend | Thymeleaf + HTML + CSS + JavaScript |
| Database | Supabase (PostgreSQL 17.6) |
| Payments | Ozow (South African EFT — hosted payment page redirect) |
| Maps/Distance | Google Maps Distance Matrix API + Places API |
| Build tool | Apache Maven 3.9.14 |
| Security | Spring Security 6 |
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
7. **NEVER add `connection-test-query` to HikariCP settings** — it conflicts with `prepareThreshold=0` and causes crashes
8. Supabase username format: `postgres.PROJECT_ID`
9. GitHub Desktop only — never give CLI git commands to the team
10. Both teammates are complete beginners — explain every step clearly
11. **Payment gateway: Ozow only** — South African EFT via hosted payment page redirect. No PayFast. No Stripe. Do not reference PayFast anywhere in new code. All payment logic lives in `OzowService.java`.
12. Fonts: **Syne** (headings) + **DM Sans** (body) — never change these
13. Colors: primary `#0a7c6e` (teal), accent `#f0a500` (gold), dark bg `#0d1117`
14. Rate per km: **R8.00/km**, minimum fare: **R50.00** — fare is calculated at booking time and is FIXED — admin cannot change it after booking
15. Google Maps API key in `application-local.properties` as `google.maps.api-key`
16. Google Maps Places API is used for frontend autocomplete — loaded in `bookings.html` AND `trips-new.html` AND `admin/calendar-client.html` via dynamic script tag using the same key
17. `app.base-url=http://localhost:8080` in `application.properties` — change for production. Ozow notify/success/cancel URLs are built from this base URL.
18. `RestTemplate` bean declared in `WebConfig.java` — Spring Boot 4 does NOT auto-create it.
19. Admin account must have `email_verified = true` set manually in Supabase SQL
20. `style.css` was fully rewritten to fix a broken unclosed CSS block — do not revert to old version
21. `contact.html` form uses `POST /contact` — handled by `PageController.java`
22. **Do NOT use `@Query` annotations in repositories** — they cause prepared statement crashes on Supabase transaction pooler. Use derived method names only
23. **Always give full file contents** when providing code — never partial edits or "find and replace" instructions
24. **Max 4 files per batch** — wait for user confirmation before next batch
25. **Admin login** — `/dashboard` checks role and redirects admin to `/admin/dashboard` automatically. Admin will NEVER land on the user dashboard under any circumstance
26. **Booking statuses**: `PENDING_APPROVAL` → `AWAITING_PAYMENT` (today flow: admin accepted, user must now pay via Ozow) or `CONFIRMED` (payment succeeded via Ozow ITN, or admin cash booking) or `REJECTED` (admin rejected) or `CANCELLED` (user cancelled / timed out / payment failed) or `EXPIRED` (auto-cancelled after 120s timeout)
27. **Payment statuses on Booking**: `UNPAID` → `AWAITING_PAYMENT` (today flow: admin accepted, awaiting Ozow) → `PAID` (Ozow ITN Complete) or `FAILED` (Ozow ITN non-Complete). Cash bookings: immediately `CASH`. Never changes from PAID once set.
28. **Trip statuses**: `AVAILABLE` → `PENDING` (user booking submitted) → `BOOKED` (confirmed/paid) or back to `AVAILABLE` (rejected/cancelled/payment failed). **Admin cash bookings create trips with status BOOKED immediately.**
29. **On-the-fly trips** (label = "User Request") — when a user clicks an open-hours slot (no admin trip exists), a trip is created on the fly. These MUST be DELETED not status-changed when rejected/cancelled/payment failed. Delete booking record FIRST (removes FK), then delete trip. This order is critical.
30. **`admin-notifications.js`** polls `/bookings/pending-count` every 3 seconds and shows a hard modal lock on all admin pages when pending bookings exist — cannot dismiss without responding. Post-action pause is **3500ms**. After admin accepts a TODAY booking, the user's waiting screen detects `AWAITING_PAYMENT` and redirects to Ozow — admin popup disappears normally.
31. **`booking-waiting.html`** is only used for TODAY bookings. It polls `/bookings/status/{id}` every 3 seconds. On `AWAITING_PAYMENT` (admin accepted) it shows "Booking accepted!" for 2 seconds then redirects to `/payment/initiate/{id}`. For FUTURE DAY bookings the user goes directly to `/payment/initiate/{id}` — the waiting screen is never shown.
32. **`BookingService.java` injects `TripRepository` directly** — do not remove this. It updates trip status within the same `@Transactional` as the booking update to avoid nested transaction conflicts on Supabase pooler
33. **Never use nested `@Transactional` calls between services for booking/trip status updates** — always do both the booking save and trip status update in the same transaction in `BookingService`
34. **`Booking.java`** uses `FetchType.EAGER` for both `user` and `trip` relationships — do NOT change to LAZY or LazyInitializationException will crash the user dashboard
35. `show-sql=false` in `application.properties` — do not re-enable, SQL logging adds extra round-trips
36. **TODAY booking expiry timeout is 120 seconds** (EXPIRY_SECONDS = 120 in BookingService) — this is the admin response window. After expiry the booking is CANCELLED/EXPIRED and the slot is freed.
37. **`ObjectMapper` in `BookingsController` and `AdminDashboardController`** must have `mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)` — without this, `LocalDate` and `LocalTime` serialize as arrays `[2026,3,20]` instead of strings `"2026-03-20"` which breaks the JS calendar
38. **`TripService.getVisibleTripsForWeek()`** returns ALL statuses including BLOCKED — do not revert to filtering out BLOCKED trips. The JS calendar renders BLOCKED as grey
39. **Admin Bookings tab and Manage Slots** both use `BookingRepository.findByTripIdAndStatusNot()` to build a `bookingByTripId` map — this is how user name/phone shows in the table for BOOKED trips
40. **Admin Manage Slots 1-hour cancel rule** — `AdminTripController.cancelBooking()` checks `minutesUntilTrip >= 60` server-side before cancelling. Template shows greyed "🔒 Too close" button when < 60 minutes away
41. **BLOCKED trips and cash bookings show their reason/client info** in the Booked By column on both Bookings and Manage Slots pages. For cash bookings: "Client: [name] | Notes: [notes]". For blocked trips: reason is required when blocking via Block Time page.
42. **Past trips are hidden** from Bookings tab and Manage Slots — only today's future times and future dates show. Logs shows full history including past
43. **`/admin/requests`** redirects to `/admin/dashboard` — the requests flow is legacy and unused
44. **Admin calendar default** — week view starts from today not Monday. Day/month views work normally
45. **Ozow integration uses NO external SDK** — plain HTTP POST form redirect with SHA-512 hash. All Ozow logic lives in `OzowService.java`. pom.xml does NOT change for Phase 9.
46. **Ozow notify URL (`/payment/notify`) MUST be publicly accessible** — it is called server-to-server by Ozow with no session/cookie. It is whitelisted in `SecurityConfig.java` as a public POST endpoint with CSRF disabled for that path only. During local dev, use ngrok to expose localhost.
47. **Ozow ITN verification:** `OzowService.handleItn()` verifies the SHA-512 hash before updating any booking/payment status. Hash mismatch = silent ignore, no update.
48. **`Payment.java`** stores `ozowTransactionId` (Ozow's transaction ID from ITN), `ozowReference` (your internal booking ID sent to Ozow), and `paymentType` (`OZOW` or `CASH`). DB columns are `ozow_transaction_id` and `ozow_reference`.
49. **Admin cash booking** — created via `/admin/trips/new`. Trip saves with status BOOKED immediately. Booking record created with paymentStatus=CASH, status=CONFIRMED. No Ozow redirect.
50. **Email is sent by `EmailService.java`** — uses existing Spring Mail config. Sends: (a) confirmation to user on CONFIRMED+PAID, (b) notification to admin on auto-confirmed future-day bookings, (c) confirmation to client on admin cash booking.
51. **`/payment/notify` must use `@Transactional`** in `OzowService.handleItn()` — it updates both Booking and Trip status in the same transaction using `TripRepository` directly. No nested service calls.
52. **pom.xml — NO changes needed for Phase 9.** Ozow uses plain HTTP form POST + SHA-512 via `java.security.MessageDigest` (built into Java). No new dependencies.
53. **`calendar.js` slot types:** `slot-available` (green), `slot-open-hours` (white, open hours no admin trip), `slot-pending` (not clickable), `slot-booked` (red), `slot-blocked` (grey), `slot-past-today` (past time today — not clickable).
54. **`bookings.html` has two modals** — Modal 1 (`booking-modal`) for green/admin-created slots, Modal 2 (`request-modal`) for open-hours slots. Both are live and functional. Do not remove either.
55. **Login accepts email OR username** — `CustomUserDetailsService.loadUserByUsername()` tries `findByEmailIgnoreCase` first then `findByUsernameIgnoreCase`. `SecurityConfig` uses `.usernameParameter("identifier")` matching the login form field `name="identifier"`.
56. **`OzowService.formatAmount()`** must use `String.format("%.2f", fee)` — `new BigDecimal(amountString)` crashes if the string contains a comma (e.g. `"50,00"`). Always format the amount as a plain decimal string like `"50.00"` before passing to `new BigDecimal()`.
57. **Ozow credentials go in `application-local.properties`** (gitignored). Placeholder values in `application.properties` allow the app to start without errors during development. When real keys arrive, add only these three lines to the local file — no code changes needed:
```properties
    ozow.site-code=YOUR_REAL_SITE_CODE
    ozow.private-key=YOUR_REAL_PRIVATE_KEY
    ozow.api-key=YOUR_REAL_API_KEY
    ozow.sandbox=false
```

---

## 💳 Ozow Integration — Key Details

### How Ozow Works (Plain HTTP form redirect, No SDK)
1. Your server builds a map of form fields including a SHA-512 hash
2. `payment-redirect.html` renders these as hidden inputs and auto-submits to `https://pay.ozow.com/`
3. User pays via EFT on Ozow's hosted page — you never see banking details
4. Ozow POSTs an ITN (Instant Transaction Notification) to your `/payment/notify` endpoint
5. `OzowService.handleItn()` verifies the hash and updates booking/trip/payment status
6. Ozow redirects the user to your `/payment/success` or `/payment/cancel` page

### Ozow Credentials (in application-local.properties — NEVER commit)
```properties
ozow.site-code=YOUR_SITE_CODE
ozow.private-key=YOUR_PRIVATE_KEY
ozow.api-key=YOUR_API_KEY
ozow.sandbox=true
```

### Ozow Hash (SHA-512)
Input string (all lowercase, concatenated):
`SiteCode + CountryCode + CurrencyCode + Amount + TransactionReference + BankRef + CancelUrl + ErrorUrl + SuccessUrl + IsTest + PrivateKey`

### Ozow ITN Status Values
- `Complete` → payment succeeded → booking CONFIRMED, paymentStatus PAID
- `Cancelled` / `Error` / `Abandoned` / `PendingInvestigation` → booking CANCELLED, paymentStatus FAILED, slot freed

### Testing Without Real Ozow Keys
The app runs fully with placeholder keys. Everything works end-to-end EXCEPT the actual redirect to Ozow's payment page (it will show the payment-redirect screen but the form POST to Ozow will fail). To test the full flow locally once you have keys, use ngrok to expose `/payment/notify` publicly so Ozow can reach it.

---

## 🗂️ New Files Added in Phase 9 (Ozow)

| File | Path | Purpose |
|---|---|---|
| `OzowService.java` | `service/` | All Ozow payment logic — hash, form fields, ITN handler |
| `PaymentController.java` | `controller/` | `/payment/initiate`, `/payment/notify`, `/payment/success`, `/payment/cancel`, `/payment/error` |
| `payment-redirect.html` | `templates/user/` | Auto-submitting form that sends user to Ozow |
| `payment-success.html` | `templates/user/` | Success page after Ozow payment completes |
| `payment-cancel.html` | `templates/user/` | Cancel/error page if payment fails or is abandoned |

---

## 🔄 Payment Flow Diagrams

### TODAY Booking
```
User clicks slot
→ 2-step modal (pickup/dropoff/fare)
→ "Proceed to Payment" clicked
→ POST /bookings/book
→ Booking created (PENDING_APPROVAL), trip = PENDING
→ Redirect to /bookings/waiting/{id}
→ booking-waiting.html polls /bookings/status/{id} every 3s

  [Admin sees popup]
  → Accept → BookingService.acceptBooking()
             → booking = AWAITING_PAYMENT, trip = BOOKED
  → Reject → booking = REJECTED, trip = AVAILABLE (or deleted if on-the-fly)

→ Waiting screen detects AWAITING_PAYMENT
→ Shows "Booking accepted!" for 2s
→ Redirects to /payment/initiate/{id}
→ payment-redirect.html auto-POSTs to Ozow (1.5s delay)
→ User pays on Ozow

  [Ozow posts ITN to /payment/notify]
  → Complete  → booking = CONFIRMED, paymentStatus = PAID, trip = BOOKED
  → Other     → booking = CANCELLED, paymentStatus = FAILED, trip = AVAILABLE

→ Ozow redirects user to /payment/success or /payment/cancel
```

### FUTURE DAY Booking
```
User clicks slot
→ 2-step modal (pickup/dropoff/fare)
→ "Proceed to Payment" clicked
→ POST /bookings/book
→ Booking created (PENDING_APPROVAL), trip = PENDING
→ tripDate is NOT today
→ Redirect straight to /payment/initiate/{id}
→ payment-redirect.html auto-POSTs to Ozow
→ Same ITN flow as above
```

---

## 🗃️ Database — Key Tables

**`bookings`** — booking_status, payment_status, pickup_address, dropoff_address, created_at, user_id (FK), trip_id (FK)

**`trips`** — date, start_time, end_time, status, label, fee, distance_km, pickup_address, dropoff_address, blocked_reason

**`payments`** — booking_id (FK), amount, ozow_transaction_id, ozow_reference, payment_type (OZOW/CASH), status (PENDING/PAID/FAILED)

**`users`** — email, username, password (bcrypt), role (USER/ADMIN), email_verified, is_blocked

**`pricing_config`** — rate_per_km, minimum_fare (single row, id=1 always)

---

## 🔗 Route Map

| Route | Status | Access |
|---|---|---|
| `GET /` | ✅ | Public |
| `GET /login` | ✅ | Public |
| `GET /register` | ✅ | Public |
| `GET /bookings` | ✅ | Public |
| `GET /bookings/status/{id}` | ✅ | Authenticated |
| `GET /bookings/pending-count` | ✅ | Public (polled by admin JS) |
| `GET /bookings/waiting/{id}` | ✅ | Authenticated |
| `POST /bookings/book` | ✅ | Authenticated |
| `POST /bookings/cancel/{id}` | ✅ | Authenticated |
| `GET /calculate-fare` | ✅ | Public |
| `GET /payment/initiate/{id}` | ✅ Phase 9 | Authenticated |
| `POST /payment/notify` | ✅ Phase 9 | Public (Ozow server-to-server) |
| `GET /payment/success` | ✅ Phase 9 | Authenticated |
| `GET /payment/cancel` | ✅ Phase 9 | Authenticated |
| `GET /payment/error` | ✅ Phase 9 | Authenticated |
| `GET /dashboard` | ✅ | Authenticated |
| `GET /admin/dashboard` | ✅ | ADMIN |
| `POST /admin/bookings/accept/{id}` | ✅ | ADMIN |
| `POST /admin/bookings/reject/{id}` | ✅ | ADMIN |
| `GET /admin/trips` | ✅ | ADMIN |
| `GET /admin/trips/new` | ✅ | ADMIN |
| `POST /admin/trips/new` | ✅ | ADMIN |
| `POST /admin/trips/cancel/{id}` | ✅ | ADMIN (1-hour rule) |
| `POST /admin/trips/block/{id}` | ✅ | ADMIN |
| `POST /admin/trips/unblock/{id}` | ✅ | ADMIN |
| `POST /admin/trips/delete/{id}` | ✅ | ADMIN |
| `GET /admin/block` | ✅ | ADMIN |
| `POST /admin/block/time-range` | ✅ | ADMIN |
| `POST /admin/block/whole-day` | ✅ | ADMIN |
| `POST /admin/block/unblock/{id}` | ✅ | ADMIN |
| `GET /admin/users` | ✅ | ADMIN |
| `GET /admin/logs` | ✅ | ADMIN |
| `GET /admin/requests` | ✅ | ADMIN (redirects to dashboard — legacy) |

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
Ozow uses plain HTTP form POST + SHA-512 via `java.security.MessageDigest` (built into Java). No new dependencies.

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

For Ozow ITN testing (local dev):
5. Download ngrok from ngrok.com
6. In a second terminal: ngrok http 8080
7. Copy the https://xxx.ngrok.io URL
8. Set app.base-url=https://xxx.ngrok.io in application-local.properties
9. Ozow can now reach your local /payment/notify endpoint
```

---

## 🔄 Team Workflow
1. GitHub Desktop → Fetch origin → Pull origin before starting
2. Make changes in VS Code
3. GitHub Desktop → write commit message → Commit to main → Push origin
4. Share `application-local.properties` via WhatsApp only — never via GitHub

---

## 💡 Claude Behaviour Rules for This Project

- **Never produce downloadable files** — always give the full file contents as copyable code in chat so the user can paste directly into VS Code
- **Always give full file contents** — never partial edits, never "find this line and replace"
- **Max 4 files per batch** — wait for user confirmation before next batch
- **Always pause at natural test points** — compile check, then functional test, then next batch
- **Never use `@Query` annotations** in Spring Data repositories — derived method names only
- **Never change pom.xml** dependencies
- **Never use `javax.*` imports** — always `jakarta.*`
- **Always read the gitingest carefully** before writing any code — never assume the codebase matches what was previously discussed
- **Never add `connection-test-query`** to HikariCP settings
- **Never change `Booking.java` FetchType** to LAZY
- **Never use nested `@Transactional` service-to-service calls** for booking/trip status updates — always update both in `BookingService` using `TripRepository` directly
- **Never reference PayFast** anywhere in new code — the payment gateway is Ozow
- **Always format Ozow amount as `String.format("%.2f", fee)`** before passing to `new BigDecimal()` — commas in number strings will crash the app
- **Always confirm understanding and ask clarifying questions** before writing code for complex features
- **Always read the full gitingest before writing any code** — never assume the codebase matches what was previously discussed
- **Ozow notify endpoint must always be handled in `@Transactional`** and must update both Booking and Trip in the same transaction
- **Never expose Ozow credentials in templates or JS** — site code, private key and API key are server-side only
