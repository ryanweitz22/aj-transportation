# AJ Transportation — Project Context & Checklist
### For use when starting a new Claude chat session
### Last updated: March 20 2026 — Booking flow largely working, Supabase connection stability issues ongoing

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
- Waiting screen polls backend every 3 seconds for 120 seconds — if admin accepts → confirmed, if admin rejects → rejected, if no response in 120 seconds → auto-cancelled and slot freed
- Admin receives a hard modal popup lock on ALL admin pages whenever a pending booking exists — cannot interact with page until they Accept or Reject
- Trip pricing: R8.00/km, minimum fare R50.00 — calculated via Google Maps Distance Matrix API — fare is fixed at booking time and cannot be altered by admin
- Open business hours slots: if no admin-created trip exists, a trip is created on the fly when user books. These on-the-fly trips (label = "User Request") are DELETED (not status-changed) if rejected/cancelled — not kept in DB
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
4. Supabase uses **port 6543** (Transaction mode pooler) — NOT 5432
5. **`?prepareThreshold=0` in the JDBC URL is the single most critical setting** — without it the app crashes constantly on Supabase pooler with `prepared statement "S_X" already exists`. Must be in `application-local.properties` URL exactly like: `jdbc:postgresql://HOST:6543/postgres?prepareThreshold=0`
6. HikariCP pool: `maximum-pool-size=2`, `minimum-idle=1` — in `application-local.properties`
7. **NEVER add `connection-test-query` to HikariCP settings** — it conflicts with `prepareThreshold=0` and causes `prepared statement "S_X" already exists` crashes
8. Supabase username format: `postgres.PROJECT_ID`
9. GitHub Desktop only — never give CLI git commands to the team
10. Both teammates are complete beginners — explain every step clearly
11. Payment gateway: Ozow only — South African EFT, no cards
12. Fonts: **Syne** (headings) + **DM Sans** (body) — never change these
13. Colors: primary `#0a7c6e` (teal), accent `#f0a500` (gold), dark bg `#0d1117`
14. Rate per km: **R8.00/km**, minimum fare: **R50.00** — fare is calculated at booking time and is FIXED — admin cannot change it after booking
15. Google Maps API key in `application-local.properties` as `google.maps.api-key`
16. Google Maps Places API is also used for frontend autocomplete — loaded in `bookings.html` via dynamic script tag using the same key
17. `app.base-url=http://localhost:8080` in `application.properties` — change for production
18. `RestTemplate` bean declared in `WebConfig.java` — Spring Boot 4 does NOT auto-create it
19. Admin account must have `email_verified = true` set manually in Supabase SQL
20. `style.css` was fully rewritten to fix a broken unclosed CSS block — do not revert to old version
21. `contact.html` form uses `POST /contact` — handled by `PageController.java`
22. **Do NOT use `@Query` annotations in repositories** — they cause prepared statement crashes on Supabase transaction pooler. Use derived method names only
23. **Always give full file contents** when providing code — never partial edits or "find and replace" instructions
24. **Break work into batches of max 4 files** — wait for user confirmation before next batch
25. **Admin login** — `/dashboard` checks role and redirects admin to `/admin/dashboard` automatically
26. **Booking statuses**: `PENDING_APPROVAL` → `CONFIRMED` (admin accepted) or `REJECTED` (admin rejected) or `CANCELLED` (user cancelled / timed out) or `EXPIRED` (auto-cancelled after 120s)
27. **Trip statuses**: `AVAILABLE` → `PENDING` (booking submitted) → `BOOKED` (admin confirmed) or back to `AVAILABLE` (rejected/cancelled)
28. **On-the-fly trips** (label = "User Request") MUST be DELETED not status-changed when rejected/cancelled. Delete booking record FIRST (removes FK), then delete trip. This order is critical.
29. **`admin-notifications.js`** polls `/bookings/pending-count` every 3 seconds and shows a hard modal lock on all admin pages when pending bookings exist — admin cannot dismiss it without responding
30. **`booking-waiting.html`** is saved as `booking-waiting.html` (with hyphen) and the controller returns `"user/booking-waiting"` — these now MATCH correctly
31. **`BookingService.java` injects `TripRepository` directly** — do not remove this. It updates trip status within the same `@Transactional` as the booking update to avoid nested transaction conflicts on Supabase pooler
32. **Never use nested `@Transactional` calls between services for booking/trip status updates** — always do both the booking save and trip status update in the same transaction in `BookingService`
33. **`Booking.java`** uses `FetchType.EAGER` for both `user` and `trip` relationships — do NOT change to LAZY or LazyInitializationException will crash the user dashboard
34. `show-sql=false` in `application.properties` — do not re-enable, SQL logging adds extra round-trips
35. Auto-expire timeout is **120 seconds** (not 60) — the `EXPIRY_SECONDS = 120` constant in `BookingService`

---

## ✅ Phase Completion Status

### Phase 1 — Project Setup: ✅ COMPLETE
### Phase 2 — Spring Boot Project: ✅ COMPLETE
### Phase 3 — Frontend Pages: ✅ COMPLETE
### Phase 4 — Database Tables + Java Models: ✅ COMPLETE
### Phase 5 — User Registration & Login: ✅ COMPLETE
### Phase 6 — Booking Calendar: ✅ COMPLETE
### Phase 7 — Admin Dashboard: 🔄 PARTIALLY COMPLETE
### Phase 8 — Google Maps + Price-Per-Km: ✅ COMPLETE
### Phase 8.5 — Booking Flow: ✅ LARGELY COMPLETE (see known issues below)
### Phase 9 — Ozow Payment Integration: ⬜ NOT STARTED
### Phase 10 — Email + SMS Notifications: ⬜ NOT STARTED
### Phase 11 — Security Hardening: ⬜ NOT STARTED
### Phase 12 — Mobile Responsiveness: ⬜ NOT STARTED
### Phase 13 — Deployment: ⬜ NOT STARTED

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
| Admin popup reappears after accept/reject | `admin-notifications.js` stops polling immediately on respond, waits 2s, then resumes |
| User waiting screen never received CONFIRMED/REJECTED | `getBookingStatusForPolling()` returns "REJECTED" if booking not found (deleted on-the-fly trip) |
| Ghost slots not suppressed for BOOKED/PENDING trips with no endTime | `calendar.js` now uses `bhClose` as fallback end time for non-AVAILABLE trips without endTime |
| `booking-waiting.html` filename mismatch | File renamed to `booking-waiting.html` — matches controller return `"user/booking-waiting"` |
| Past time slot booking | `isPastSlot()` in `BusinessHoursService` blocks past times |
| Admin landing on user dashboard | `AuthController` checks role, redirects admin to `/admin/dashboard` |
| RestTemplate missing bean | `WebConfig.java` with `@Bean RestTemplate` |
| `style.css` broken CSS block | Fully rewritten |

---

## ⚠️ Known Issues / Immediate TODOs for Next Session

**Still broken / not yet fixed:**
1. **Intermittent login errors** — `prepared statement "S_X" already exists` still occurs occasionally on user login. Root cause: `?prepareThreshold=0` may not be in `application-local.properties`. Verify this line exists exactly: `spring.datasource.url=jdbc:postgresql://HOST:6543/postgres?prepareThreshold=0`
2. **Slot still shows available after accept** — After admin accepts a booking, the slot may still appear bookable on user calendar. Could be: (a) browser cache — user needs to refresh, or (b) `updateTripStatus` still failing on Supabase. Need to test after latest `BookingService` fix.
3. **Admin pop-up accept loop** — After latest fix (BookingService using TripRepository directly) this should be resolved. Needs end-to-end test.
4. **Duplicate bookings for same slot** — Two users booking at same time can both succeed before either commits. `createBooking()` now does trip status check + mark PENDING in one transaction — needs testing under concurrent load.

**Phase 7 remaining (after booking flow is stable):**
5. Admin private booking — book a trip on behalf of a specific user
6. Block whole day — block all slots on a date at once (currently admin can only block individual trips)
7. Revenue dashboard at `/admin/revenue`
8. Full admin navbar on ALL admin pages — currently only some pages have the full navbar

**Then:**
9. Phase 9 — Ozow payment integration
10. Remove duplicate "Estimated Fare" card from booking modal step 2

---

## 📁 Current File State

```
src/main/java/com/ajtransportation/app/
├── config/
│   ├── SecurityConfig.java              ✅ public routes: /bookings/waiting/**, /bookings/status/**, /bookings/pending-count, /bookings/calculate-fare
│   └── WebConfig.java                   ✅ RestTemplate bean
├── controller/
│   ├── PageController.java              ✅ GET + POST /contact
│   ├── AuthController.java              ✅ role-aware /dashboard redirect
│   ├── BookingsController.java          ✅ /bookings /bookings/book /bookings/waiting/{id} /bookings/status/{id} /bookings/pending-count /bookings/calculate-fare /bookings/cancel/{id}
│   ├── AdminBookingController.java      ✅ /admin/bookings/pending /admin/bookings/accept/{id} /admin/bookings/reject/{id}
│   ├── AdminTripController.java         ✅ /admin/trips/** /admin/trips/pricing /admin/trips/block/{id} /admin/trips/unblock/{id} /admin/trips/delete/{id}
│   ├── AdminDashboardController.java    ✅ /admin/dashboard Day/Week/Month
│   ├── AdminRequestController.java      ✅ (legacy — unused)
│   ├── AdminUserController.java         ✅ /admin/users view/block/verify/delete
│   └── AdminLogsController.java         ✅ /admin/logs date filter
├── model/
│   ├── User.java                        ✅ phone email_verified verification_token is_blocked
│   ├── Trip.java                        ✅ end_time nullable, label, distanceKm, fee, googleEtaMinutes, bufferedDurationMinutes
│   ├── Booking.java                     ✅ FetchType.EAGER for user + trip, pickupAddress dropoffAddress status paymentStatus createdAt
│   ├── TripRequest.java                 ✅ (legacy — unused in new flow)
│   ├── Payment.java                     ✅
│   ├── PricingConfig.java               ✅
│   └── RegisterRequest.java             ✅ phoneNumber + SA validation
├── repository/
│   ├── UserRepository.java              ✅
│   ├── TripRepository.java              ✅ findByDate, findByDateBetween, findByDateBetweenAndStatusNot, findByStatus
│   ├── BookingRepository.java           ✅ existsByTripIdAndStatusNotInOrderByCreatedAtAsc (NO @Query), findByTripIdAndStatusNot, findByStatusOrderByCreatedAtAsc, findByUserOrderByCreatedAtDesc, findByUser
│   ├── TripRequestRepository.java       ✅ (legacy)
│   ├── PaymentRepository.java           ✅
│   └── PricingConfigRepository.java     ✅
├── service/
│   ├── BookingService.java              ✅ injects TripRepository directly — updates trip + booking in same transaction. EXPIRY_SECONDS=120. createBooking, createBookingForOpenSlot, acceptBooking, rejectBooking, cancelBooking, cancelBookingByTripId, getBookingStatusForPolling, getPendingBookings, getUserBookings, countActiveBookings, confirmBooking
│   ├── TripService.java                 ✅ createTrip, createOnTheFlyTrip, updateTripStatus, blockTrip, unblockTrip, deleteTrip, getVisibleTripsForWeek, getPricingConfig, savePricingConfig
│   ├── BusinessHoursService.java        ✅ OPEN EVERY DAY 04:00–23:00 + isPastSlot()
│   ├── GoogleMapsService.java           ✅ getDistanceAndEta() calculateFee()
│   ├── UserService.java                 ✅ register verifyEmail findByEmail
│   ├── CustomUserDetailsService.java    ✅ blocks login if emailVerified=false OR isBlocked=true
│   └── TripRequestService.java          ✅ (legacy — unused)

src/main/resources/
├── application.properties               ✅ (see below — show-sql=false, no connection-test-query)
├── static/
│   ├── css/
│   │   ├── style.css                    ✅ fully rewritten
│   │   └── bookings.css                 ✅
│   └── js/
│       ├── main.js                      ✅
│       ├── calendar.js                  ✅ ghost slot suppression uses bhClose for trips with no endTime
│       ├── bookings.js                  ✅ 2-step modal + Google Places + fare calculation
│       └── admin-notifications.js       ✅ stops polling on respond, 2s pause, then resumes
└── templates/
    ├── index.html                       ✅
    ├── about.html                       ✅
    ├── contact.html                     ✅
    ├── admin/
    │   ├── dashboard.html               ✅ has admin-notifications.js
    │   ├── bookings-pending.html        ✅
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
        ├── bookings.html                ✅
        ├── booking-waiting.html         ✅ RENAMED (was bookingwaiting.html — now correct with hyphen)
        └── dashboard.html               ✅
```

---

## ⚙️ application.properties (current committed state)

```properties
# ============================================
# AJ TRANSPORTATION - MAIN CONFIG
# Safe to commit to GitHub
# ============================================

spring.application.name=aj-transportation

# Load local secrets
spring.config.import=optional:classpath:application-local.properties

# Database settings (credentials come from local file)
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.open-in-view=false

# Thymeleaf
spring.thymeleaf.cache=false
spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.suffix=.html

# Server
server.port=8080

# Email settings (credentials come from local file)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
app.base-url=http://localhost:8080

# ── HikariCP — tuned for Supabase free tier transaction pooler ────────────────
# CRITICAL: Do NOT add connection-test-query — it conflicts with prepareThreshold=0
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
spring.datasource.hikari.keepalive-time=300000
spring.datasource.hikari.validation-timeout=5000
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
| `/admin/dashboard` | ✅ | ADMIN |
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
| `/admin/logs` | ✅ | ADMIN |
| `/admin/revenue` | ⬜ Phase 7 remaining | ADMIN |

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
```

---

## 🔄 Team Workflow

1. GitHub Desktop → Fetch origin → Pull origin before starting
2. Make changes in VS Code
3. GitHub Desktop → write commit message → Commit to main → Push origin
4. Share `application-local.properties` via WhatsApp only — never via GitHub

---

## 💡 Claude Behaviour Rules for This Project

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
