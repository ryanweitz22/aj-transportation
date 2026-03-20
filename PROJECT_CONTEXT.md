# AJ Transportation — Project Context & Checklist
### For use when starting a new Claude chat session
### Last updated: After major booking flow overhaul — waiting screen + admin notification popup in progress

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
- Weekly calendar shows: green slots (admin-created trips — book immediately), teal ghost slots (open business hours — any user can book), amber/pending slots (slot just booked, awaiting admin approval), red slots (confirmed booked), grey (blocked/past/closed)
- Clicking a green OR teal slot → 2-step Uber-style modal → Step 1: enter pickup + dropoff (Google Places Autocomplete), Step 2: shows calculated fare (R8/km, min R50) + confirms → user redirected to waiting screen
- Waiting screen polls backend every 3 seconds for 60 seconds — if admin accepts → confirmed, if admin rejects → rejected, if no response in 60 seconds → auto-cancelled and slot freed
- Admin receives a hard modal popup lock on ALL admin pages whenever a pending booking exists — cannot interact with page until they Accept or Reject
- Trip pricing: R8.00/km, minimum fare R50.00 — calculated via Google Maps Distance Matrix API — fare is fixed at booking time and cannot be altered by admin
- Open business hours slots: if no admin-created trip exists, a trip is created on the fly when user books. These on-the-fly trips (label = "User Request") are deleted if rejected/cancelled — not kept in DB
- Admin can: create trips, block slots, cancel bookings, view pending bookings, view all users, view trip logs
- Payments via Ozow (South African EFT) — Phase 9 (not started)
- Email notifications — Phase 10 (not started)

---

## 🛠️ Tech Stack (Fixed — do NOT change under any circumstances)

| Layer | Technology |
|---|---|
| Language | Java |
| Backend | Spring Boot 4.0.3 |
| Frontend | Thymeleaf + HTML + CSS + JavaScript |
| Database | Supabase (PostgreSQL 17.6) |
| Payments | Ozow (South African EFT) |
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
4. Supabase uses **port 6543** (Transaction mode pooler) — NOT 5432. Using 5432 causes `MaxClientsInSessionMode` crash
5. Supabase connection URL must include `?prepareThreshold=0` to avoid `prepared statement "S_1" already exists` crash — e.g. `jdbc:postgresql://HOST:6543/postgres?prepareThreshold=0`
6. HikariCP pool: `maximum-pool-size=2`, `minimum-idle=1` — required for Supabase free tier
7. Supabase username format: `postgres.PROJECT_ID`
8. GitHub Desktop only — never give CLI git commands to the team
9. Both teammates are complete beginners — explain every step clearly
10. Payment gateway: Ozow only — South African EFT, no cards
11. Fonts: **Syne** (headings) + **DM Sans** (body) — never change these
12. Colors: primary `#0a7c6e` (teal), accent `#f0a500` (gold), dark bg `#0d1117`
13. Rate per km: **R8.00/km**, minimum fare: **R50.00** — fare is calculated at booking time and is FIXED — admin cannot change it after booking
14. Google Maps API key in `application-local.properties` as `google.maps.api-key`
15. Google Maps Places API is also used for frontend autocomplete — loaded in `bookings.html` via dynamic script tag using the same key
16. `app.base-url=http://localhost:8080` in `application.properties` — change for production
17. `RestTemplate` bean declared in `WebConfig.java` — Spring Boot 4 does NOT auto-create it
18. Admin account must have `email_verified = true` set manually in Supabase SQL
19. `style.css` was fully rewritten to fix a broken unclosed CSS block — do not revert to old version
20. `contact.html` form uses `POST /contact` — handled by `PageController.java`
21. **Do NOT use `@Query` annotations in repositories** — they cause `prepared statement "S_1" already exists` crash on Supabase transaction pooler. Use derived method names only
22. **Always give full file contents** when providing code — never partial edits or "find and replace" instructions
23. **Break work into batches of max 4 files** — wait for user confirmation before next batch
24. **Admin login** — `/dashboard` checks role and redirects admin to `/admin/dashboard` automatically
25. **Booking statuses**: `PENDING_APPROVAL` → `CONFIRMED` (admin accepted) or `REJECTED` (admin rejected) or `CANCELLED` (user cancelled / timed out) or `EXPIRED` (auto-cancelled after 60s)
26. **Trip statuses**: `AVAILABLE` → `PENDING` (booking submitted, awaiting admin) → `BOOKED` (admin confirmed) or back to `AVAILABLE` (rejected/cancelled)
27. **On-the-fly trips** (label = "User Request") are created when users book open time slots. They must be DELETED (not just status-changed) when rejected or cancelled to keep DB clean
28. **`admin-notifications.js`** polls `/bookings/pending-count` every 3 seconds and shows a hard modal lock on all admin pages when pending bookings exist — admin cannot dismiss it without responding
29. **`booking-waiting.html`** — note the file is saved as `bookingwaiting.html` in the repo (no hyphen) but the controller returns `"user/booking-waiting"` — **this is a mismatch that needs fixing** in a new session
30. The admin dashboard navbar currently only has Schedule, Trips, + New Slot links — the full navbar with Pending Bookings, Users, Logs still needs to be applied to all admin pages

---

## ✅ Phase Completion Status

### Phase 1 — Project Setup: ✅ COMPLETE
### Phase 2 — Spring Boot Project: ✅ COMPLETE
### Phase 3 — Frontend Pages: ✅ COMPLETE
### Phase 4 — Database Tables + Java Models: ✅ COMPLETE
### Phase 5 — User Registration & Login: ✅ COMPLETE

**Phase 5 updates applied:**
- `User.java` — added `phone_number`, `email_verified`, `verification_token`, `is_blocked`
- `RegisterRequest.java` — added `phoneNumber` with SA phone number validation
- `UserService.java` — sends verification email on register; `verifyEmail(token)` method
- `UserRepository.java` — added `findByVerificationToken()`, `findAllByOrderByCreatedAtDesc()`
- `CustomUserDetailsService.java` — blocks login if `emailVerified=false` OR `isBlocked=true`
- `AuthController.java` — role-aware dashboard redirect: admin → `/admin/dashboard`, user → user dashboard
- `register.html` — phone number field added
- `application.properties` — `app.base-url=http://localhost:8080` added

**⚠️ Required Supabase SQL (run once):**
```sql
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_number VARCHAR;
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE users ADD COLUMN IF NOT EXISTS verification_token VARCHAR;
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_blocked BOOLEAN NOT NULL DEFAULT false;
UPDATE users SET email_verified = true WHERE email_verified = false;
```

### Phase 6 — Booking Calendar: ✅ COMPLETE (significantly evolved from original spec)

**Current calendar behaviour:**
- Green slots = admin-created AVAILABLE trips → click to book
- Teal ghost slots = open business hours with no trip → click to book (trip created on the fly)
- Amber/pending = PENDING trip (awaiting admin approval) → not clickable
- Red = BOOKED → not clickable
- Grey = BLOCKED or past → not clickable
- `TripRequestService.java` and `TripRequest.java` still exist in codebase but are no longer used for the booking flow — custom trip requests were removed in favour of the on-the-fly trip system

**⚠️ Required Supabase SQL:**
```sql
CREATE TABLE IF NOT EXISTS trip_requests (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id),
  requested_date DATE NOT NULL,
  requested_start_time TIME NOT NULL,
  pickup_address VARCHAR NOT NULL,
  dropoff_address VARCHAR NOT NULL,
  additional_notes VARCHAR(500),
  status VARCHAR NOT NULL DEFAULT 'PENDING',
  trip_id UUID REFERENCES trips(id),
  admin_notes VARCHAR(500),
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
ALTER TABLE trips ALTER COLUMN end_time DROP NOT NULL;
```

### Phase 7 — Admin Dashboard: 🔄 PARTIALLY COMPLETE

**Completed:**
- `AdminDashboardController.java` — Day/Week/Month schedule view, block/unblock slots, cancel bookings
- `AdminTripController.java` — create trips, pricing config
- `AdminBookingController.java` — `/admin/bookings/pending`, `/admin/bookings/accept/{id}`, `/admin/bookings/reject/{id}`
- `AdminUserController.java` — view/block/unblock/verify/delete users
- `AdminLogsController.java` — trip log with date range filter
- `admin/bookings-pending.html` — pending bookings table with Accept/Reject buttons
- `admin/dashboard.html` — schedule with Day/Week/Month toggle + `admin-notifications.js` included
- `admin/users.html`, `admin/logs.html` — ✅ complete
- `user/dashboard.html` — bookings table with PENDING_APPROVAL / CONFIRMED / REJECTED / CANCELLED status badges
- `user/bookings.html` — 2-step Uber-style booking modal with Google Places Autocomplete + live fare calculation

**Phase 7 items still TODO:**
1. Admin private booking — book a trip on behalf of a specific user
2. Block whole day — block all slots on a date at once
3. Revenue dashboard at `/admin/revenue`

### Phase 8 — Google Maps + Price-Per-Km: ✅ COMPLETE
- `GoogleMapsService.java` — `getDistanceAndEta()` + `calculateFee()` methods
- `TripService.java` — `createTrip()` auto-enriches, `createOnTheFlyTrip()` for open slots
- `BookingsController.java` — `/bookings/calculate-fare` endpoint called by frontend JS
- Fare displayed on step 2 of booking modal — fixed at R8/km min R50

### Phase 8.5 — Booking Flow Overhaul: 🔄 IN PROGRESS

**Completed:**
- Uber-style 2-step booking modal (pickup → dropoff → live fare → confirm)
- `BookingService.java` — `createBooking()`, `createBookingForOpenSlot()`, `acceptBooking()`, `rejectBooking()`, `cancelBooking()`, `getBookingStatusForPolling()`
- `BookingRepository.java` — `existsByTripIdAndStatusNotInOrderByCreatedAtAsc()` (no @Query — avoids Supabase pooler crash)
- `BookingsController.java` — `/bookings/book`, `/bookings/waiting/{id}`, `/bookings/status/{id}`, `/bookings/pending-count`, `/bookings/calculate-fare`
- `SecurityConfig.java` — `/bookings/waiting/**`, `/bookings/status/**`, `/bookings/pending-count` added as public routes
- `user/booking-waiting.html` (saved as `bookingwaiting.html` in repo — **hyphen mismatch bug**)
- `admin-notifications.js` — polling + hard modal lock on all admin pages
- `BusinessHoursService.java` — added `isPastSlot()` to block past time bookings

**Still TODO in this phase:**
- Fix `booking-waiting.html` vs `bookingwaiting.html` filename mismatch
- Add full admin navbar (Pending Bookings, Users, Logs) to ALL admin pages
- Remove "Estimated Fare" duplicate display from booking modal step 2 — fare should only show in the Fare row, not as a separate card
- Confirm the end-to-end flow works: book → waiting screen → admin popup → accept/reject → user screen updates

### Phase 9 — Ozow Payment Integration: ⬜ NOT STARTED
### Phase 10 — Email + SMS Notifications: ⬜ NOT STARTED
### Phase 11 — Security Hardening: ⬜ NOT STARTED
### Phase 12 — Mobile Responsiveness: ⬜ NOT STARTED
### Phase 13 — Deployment: ⬜ NOT STARTED

---

## 📁 Complete File Structure (True Current State)

```
src/main/java/com/ajtransportation/app/
├── config/
│   ├── SecurityConfig.java              ✅ public routes include /bookings/waiting/**, /bookings/status/**, /bookings/pending-count
│   └── WebConfig.java                   ✅ RestTemplate bean
├── controller/
│   ├── PageController.java              ✅ GET + POST /contact
│   ├── AuthController.java              ✅ role-aware /dashboard redirect
│   ├── BookingsController.java          ✅ /bookings /bookings/book /bookings/waiting/{id} /bookings/status/{id} /bookings/pending-count /bookings/calculate-fare /bookings/cancel/{id}
│   ├── AdminBookingController.java      ✅ /admin/bookings/pending /admin/bookings/accept/{id} /admin/bookings/reject/{id}
│   ├── AdminTripController.java         ✅ /admin/trips/** /admin/trips/pricing
│   ├── AdminDashboardController.java    ✅ /admin/dashboard Day/Week/Month
│   ├── AdminRequestController.java      ✅ /admin/requests (legacy — may be unused)
│   ├── AdminUserController.java         ✅ /admin/users view/block/verify/delete
│   └── AdminLogsController.java         ✅ /admin/logs date filter
├── model/
│   ├── User.java                        ✅ phone email_verified verification_token is_blocked
│   ├── Trip.java                        ✅ end_time nullable, label, distanceKm, fee, googleEtaMinutes, bufferedDurationMinutes
│   ├── Booking.java                     ✅ pickupAddress dropoffAddress status paymentStatus createdAt
│   ├── TripRequest.java                 ✅ (legacy — exists but unused in new flow)
│   ├── Payment.java                     ✅
│   ├── PricingConfig.java               ✅
│   └── RegisterRequest.java             ✅ phoneNumber + SA validation
├── repository/
│   ├── UserRepository.java              ✅
│   ├── TripRepository.java              ✅
│   ├── BookingRepository.java           ✅ existsByTripIdAndStatusNotInOrderByCreatedAtAsc (NO @Query)
│   ├── TripRequestRepository.java       ✅ (legacy)
│   ├── PaymentRepository.java           ✅
│   └── PricingConfigRepository.java     ✅
├── service/
│   ├── BookingService.java              ✅ createBooking createBookingForOpenSlot acceptBooking rejectBooking cancelBooking getBookingStatusForPolling getPendingBookings
│   ├── TripService.java                 ✅ createTrip createOnTheFlyTrip updateTripStatus blockTrip unblockTrip deleteTrip enrichTripWithGoogleMaps
│   ├── BusinessHoursService.java        ✅ Mon–Thu 04:00–12:00, Fri 04:00–11:30, Sat 06:00–10:00, Sun closed + isPastSlot()
│   ├── GoogleMapsService.java           ✅ getDistanceAndEta() calculateFee()
│   ├── UserService.java                 ✅ register verifyEmail findByEmail
│   ├── CustomUserDetailsService.java    ✅ blocks login if emailVerified=false OR isBlocked=true
│   └── TripRequestService.java          ✅ (legacy — exists but unused in new flow)

src/main/resources/
├── application.properties               ✅ (see below)
├── static/
│   ├── css/
│   │   ├── style.css                    ✅ fully rewritten
│   │   └── bookings.css                 ✅ includes slot-open-hours + slot-pending styles
│   └── js/
│       ├── main.js                      ✅ mobile menu + scroll
│       ├── calendar.js                  ✅ green/teal/amber/red/grey slots + click handlers
│       ├── bookings.js                  ✅ 2-step modal + Google Places + fare calculation polling
│       └── admin-notifications.js       ✅ polls /bookings/pending-count + hard modal lock
└── templates/
    ├── index.html                       ✅
    ├── about.html                       ✅
    ├── contact.html                     ✅
    ├── admin/
    │   ├── dashboard.html               ✅ has admin-notifications.js — navbar still needs full links
    │   ├── bookings-pending.html        ✅ Accept/Reject table
    │   ├── trips-list.html              ✅
    │   ├── trips-new.html               ✅
    │   ├── pricing.html                 ✅
    │   ├── requests.html                ✅ (legacy)
    │   ├── users.html                   ✅
    │   └── logs.html                    ✅
    ├── auth/
    │   ├── login.html                   ✅
    │   └── register.html                ✅
    └── user/
        ├── bookings.html                ✅ 2-step modal + Google Places + fare
        ├── bookingwaiting.html          ⚠️ FILENAME MISMATCH — controller returns "user/booking-waiting" but file is "bookingwaiting.html" (no hyphen) — needs rename or controller fix
        └── dashboard.html               ✅ PENDING_APPROVAL / CONFIRMED / REJECTED / CANCELLED badges
```

---

## 🗺️ URL Routing (Complete Current State)

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
| `/admin/dashboard` | ✅ | ADMIN |
| `/admin/dashboard/block/{id}` | ✅ | ADMIN |
| `/admin/dashboard/unblock/{id}` | ✅ | ADMIN |
| `/admin/dashboard/cancel-by-trip/{id}` | ✅ | ADMIN |
| `/admin/bookings/pending` | ✅ | ADMIN |
| `/admin/bookings/accept/{id}` POST | ✅ | ADMIN |
| `/admin/bookings/reject/{id}` POST | ✅ | ADMIN |
| `/admin/trips` | ✅ | ADMIN |
| `/admin/trips/new` | ✅ | ADMIN |
| `/admin/trips/pricing` | ✅ | ADMIN |
| `/admin/trips/block/{id}` | ✅ | ADMIN |
| `/admin/trips/unblock/{id}` | ✅ | ADMIN |
| `/admin/trips/delete/{id}` | ✅ | ADMIN |
| `/admin/users` | ✅ | ADMIN |
| `/admin/users/block/{id}` | ✅ | ADMIN |
| `/admin/users/unblock/{id}` | ✅ | ADMIN |
| `/admin/users/verify/{id}` | ✅ | ADMIN |
| `/admin/users/delete/{id}` | ✅ | ADMIN |
| `/admin/logs` | ✅ | ADMIN |
| `/admin/revenue` | ⬜ Phase 7 remaining | ADMIN |

---

## 🐛 Bugs Fixed (Do Not Reintroduce)

| Bug | Fix |
|---|---|
| Booking modal not opening | Rebuilt slots with `data-trip` JSON + `onclick="handleSlotClick(this)"` |
| Modal class mismatch | Uses `.open` class toggle, not `.hidden` |
| Supabase MaxClientsInSessionMode | Port changed to 6543 + HikariCP pool limited |
| Trip.java end_time crash | `@Column(name="end_time")` — removed `nullable=false` |
| style.css broken CSS block | Unclosed rule corrupted all styles below it — fully rewritten |
| Contact form 405 | Added `POST /contact` to `PageController.java` |
| RestTemplate missing bean | Added `WebConfig.java` with `@Bean RestTemplate` |
| `prepared statement "S_1" already exists` | Added `?prepareThreshold=0` to JDBC URL + removed all `@Query` annotations from repositories |
| Rejected bookings blocking future bookings | `existsByTripIdAndStatusNotInOrderByCreatedAtAsc` excludes both CANCELLED and REJECTED |
| On-the-fly trips orphaned after rejection | On-the-fly trips (label="User Request") are deleted on rejection/cancellation |
| Past time slot booking crash | `isPastSlot()` in `BusinessHoursService` blocks past times on today's date |
| Admin landing on user dashboard | `AuthController.dashboard()` checks role and redirects admin to `/admin/dashboard` |
| Calendar empty (no open slots) | `calendar.js` reinstated open business hours teal ghost slots alongside existing trips |
| `getBookingStatus` null id crash | Null tripId check added in `BookingsController.bookTrip()` before UUID parsing |

---

## ⚠️ Known Issues / Immediate TODOs for Next Session

**Fix first (blocking):**
1. **`bookingwaiting.html` filename mismatch** — controller returns `"user/booking-waiting"` but file is saved as `bookingwaiting.html` (no hyphen). Either rename the file to `booking-waiting.html` OR change the controller return to `"user/bookingwaiting"`. The file must be renamed in the repo.
2. **Admin navbar incomplete** — `admin/dashboard.html`, `admin/logs.html`, `admin/pricing.html`, `admin/trips-list.html`, `admin/trips-new.html`, `admin/users.html`, `admin/bookings-pending.html`, `admin/requests.html` all need the full navbar with: Schedule, Pending Bookings, Trips, + New Slot, Users, Logs + `admin-notifications.js` script tag + CSRF meta tag in head
3. **Test the full booking flow end to end** — book a slot → waiting screen → admin popup → accept → user confirmed, and reject → user rejected, and 60s timeout → user expired

**Then continue with:**
4. Remove the duplicate "Estimated Fare" card from booking modal step 2 — fare should only show in the trip details Fare row
5. Admin private booking (Phase 7 Block A)
6. Block whole day (Phase 7 Block B)
7. Revenue dashboard (Phase 7 Block C)
8. Phase 9 — Ozow payment integration

---

## ⚙️ application.properties (committed to GitHub)

```properties
spring.application.name=aj-transportation
spring.config.import=optional:classpath:application-local.properties
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.format_sql=true
spring.thymeleaf.cache=false
spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.suffix=.html
server.port=8080
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.jpa.open-in-view=false
app.base-url=http://localhost:8080
```

## ⚙️ application-local.properties template (GITIGNORED — share via WhatsApp only)

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
```

**⚠️ The `?prepareThreshold=0` on the datasource URL is critical — without it the app crashes on Supabase transaction pooler with a prepared statement conflict.**

---

## 📦 pom.xml Dependencies (No Changes Needed Until Phase 9)

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

No new Maven dependencies needed until Phase 9 (Ozow) or Phase 10 (Twilio SMS).

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

Get bcrypt hash from [bcrypt-generator.com](https://bcrypt-generator.com).

---

## 🚀 How to Run

```
1. VS Code → open project folder
2. Terminal (Ctrl + `): mvn spring-boot:run
3. Browser: http://localhost:8080
4. Stop: Ctrl+C
```

---

## 🔄 Team Workflow

1. GitHub Desktop → **Fetch origin** → **Pull origin** before starting
2. Make changes in VS Code
3. GitHub Desktop → write commit message → **Commit to main** → **Push origin**
4. Share `application-local.properties` via WhatsApp only — never via GitHub

---

## 💡 Claude Behaviour Rules for This Project

- Always give **full file contents** — never partial edits, never "find this line and replace"
- Max **4 files per batch** — wait for user confirmation before next batch
- Always **pause at natural test points** — compile check, then functional test, then next batch
- Never use `@Query` annotations in Spring Data repositories
- Never change pom.xml dependencies
- Never use `javax.*` imports — always `jakarta.*`
- Always check the gitingest before writing code if provided
- Booking flow output format: code blocks only, file path stated clearly above each block
