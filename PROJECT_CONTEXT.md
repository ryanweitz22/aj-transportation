# AJ Transportation — Project Context & Checklist
### For use when starting a new Claude chat session
### Last updated: March 23 2026 — Admin interface complete, ready for Phase 9

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
- Calendar shows: green slots (admin-created trips — book immediately), teal ghost slots (open business hours — any user can book), amber/pending slots (slot just booked, awaiting admin approval), red slots (confirmed booked), grey (blocked/past/closed)
- Clicking a green OR teal slot → 2-step Uber-style modal → Step 1: enter pickup + dropoff (Google Places Autocomplete), Step 2: shows calculated fare (R8/km, min R50) + confirms → user redirected to waiting screen
- Waiting screen polls backend every 3 seconds for 120 seconds — if admin accepts → auto-redirects to dashboard, if admin rejects → rejected screen, if no response in 120 seconds → auto-cancelled and slot freed
- Admin receives a hard modal popup lock on ALL admin pages whenever a pending booking exists — cannot interact with page until they Accept or Reject. Post-action pause is 3500ms before polling resumes
- Trip pricing: R8.00/km, minimum fare R50.00 — calculated via Google Maps Distance Matrix API — fare is fixed at booking time and cannot be altered by admin
- Open business hours slots: if no admin-created trip exists, a trip is created on the fly when user books. These on-the-fly trips (label = "User Request") are DELETED (not status-changed) if rejected/cancelled — not kept in DB
- Admin navbar: Bookings | Manage Slots | Block Time | Users | Logs
- Admin Bookings tab: read-only view of all upcoming trips with user details. No action buttons — all actions on Manage Slots
- Admin Manage Slots: full CRUD — block, unblock, cancel (1-hour rule), delete. Shows BLOCKED, BOOKED, AVAILABLE, PENDING trips. Past trips hidden
- Admin Block Time: dedicated page to block specific time ranges or whole days. Reason is required and shows in Bookings/Manage Slots under Booked By column
- Admin Create Trip Slot: Google Maps autocomplete for pickup/dropoff, auto-generates route label, live fare preview, date/time validation prevents past slots
- Admin Logs: full history with date range filter + search by username/email/phone
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
16. Google Maps Places API is used for frontend autocomplete — loaded in `bookings.html` AND `trips-new.html` via dynamic script tag using the same key
17. `app.base-url=http://localhost:8080` in `application.properties` — change for production
18. `RestTemplate` bean declared in `WebConfig.java` — Spring Boot 4 does NOT auto-create it
19. Admin account must have `email_verified = true` set manually in Supabase SQL
20. `style.css` was fully rewritten to fix a broken unclosed CSS block — do not revert to old version
21. `contact.html` form uses `POST /contact` — handled by `PageController.java`
22. **Do NOT use `@Query` annotations in repositories** — they cause prepared statement crashes on Supabase transaction pooler. Use derived method names only
23. **Always give full file contents** when providing code — never partial edits or "find and replace" instructions
24. **Max 4 files per batch** — wait for user confirmation before next batch
25. **Admin login** — `/dashboard` checks role and redirects admin to `/admin/dashboard` automatically. Admin will NEVER land on the user dashboard under any circumstance
26. **Booking statuses**: `PENDING_APPROVAL` → `CONFIRMED` (admin accepted) or `REJECTED` (admin rejected) or `CANCELLED` (user cancelled / timed out) or `EXPIRED` (auto-cancelled after 120s)
27. **Trip statuses**: `AVAILABLE` → `PENDING` (booking submitted) → `BOOKED` (admin confirmed) or back to `AVAILABLE` (rejected/cancelled)
28. **On-the-fly trips** (label = "User Request") MUST be DELETED not status-changed when rejected/cancelled. Delete booking record FIRST (removes FK), then delete trip. This order is critical
29. **`admin-notifications.js`** polls `/bookings/pending-count` every 3 seconds and shows a hard modal lock on all admin pages when pending bookings exist — admin cannot dismiss it without responding. Post-action pause is **3500ms** (not 2000ms) to allow Supabase to commit before next poll
30. **`booking-waiting.html`** is saved as `booking-waiting.html` (with hyphen) and the controller returns `"user/booking-waiting"` — these MATCH correctly. After CONFIRMED status, page auto-redirects to `/dashboard` after 2.5 seconds
31. **`BookingService.java` injects `TripRepository` directly** — do not remove this. It updates trip status within the same `@Transactional` as the booking update to avoid nested transaction conflicts on Supabase pooler
32. **Never use nested `@Transactional` calls between services for booking/trip status updates** — always do both the booking save and trip status update in the same transaction in `BookingService`
33. **`Booking.java`** uses `FetchType.EAGER` for both `user` and `trip` relationships — do NOT change to LAZY or LazyInitializationException will crash the user dashboard
34. `show-sql=false` in `application.properties` — do not re-enable, SQL logging adds extra round-trips
35. Auto-expire timeout is **120 seconds** (not 60) — the `EXPIRY_SECONDS = 120` constant in `BookingService`
36. **`ObjectMapper` in `BookingsController` and `AdminDashboardController`** must have `mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)` — without this, `LocalDate` and `LocalTime` serialize as arrays `[2026,3,20]` instead of strings `"2026-03-20"` which breaks the JS calendar
37. **`TripService.getVisibleTripsForWeek()`** returns ALL statuses including BLOCKED — do not revert to filtering out BLOCKED trips. The JS calendar renders BLOCKED as grey and suppresses ghost slots for that time window
38. **Admin Bookings tab and Manage Slots** both use `BookingRepository.findByTripIdAndStatusNot()` to build a `bookingByTripId` map — this is how user name/phone shows in the table for BOOKED trips
39. **Admin Manage Slots 1-hour cancel rule** — `AdminTripController.cancelBooking()` checks `minutesUntilTrip >= 60` server-side before cancelling. Template shows greyed "🔒 Too close" button when < 60 minutes away
40. **BLOCKED trips show their reason** in the Booked By column on both Bookings and Manage Slots pages. Reason is required when blocking via Block Time page
41. **Past trips are hidden** from Bookings tab and Manage Slots — only today's future times and future dates show. Logs shows full history including past
42. **`/admin/requests`** redirects to `/admin/dashboard` — the requests flow is legacy and unused
43. **Admin calendar default** — week view starts from today not Monday. Day/month views work normally

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
| User not redirected after booking confirmed | `booking-waiting.html` auto-redirects to `/dashboard` after 2.5 seconds on CONFIRMED |
| Past trips showing on admin pages | Filter applied in `AdminDashboardController` and `AdminTripController` — past trips hidden except in Logs |
| User calendar starting from past Monday | `BookingsController` default week start changed to `LocalDate.now()` |

---

## ⚠️ Known Issues / Watch Points

1. **`?prepareThreshold=0`** — verify this is still in `application-local.properties` after any restart. Every prepared statement crash traces back to this being missing
2. **`bookings-pending.html`** at `/admin/bookings/pending` — still exists but is no longer linked from any navbar. Can be deleted in a future cleanup
3. **Duplicate bookings for same open slot** — two users clicking the same ghost slot simultaneously could both succeed. `createBooking()` does trip status check + mark PENDING in one transaction but concurrent load hasn't been tested under high traffic

---

## 📁 Current File State
```
src/main/java/com/ajtransportation/app/
├── config/
│   ├── SecurityConfig.java              ✅ public routes updated — /bookings/waiting/** removed (requires login)
│   └── WebConfig.java                   ✅ RestTemplate bean
├── controller/
│   ├── PageController.java              ✅ GET + POST /contact
│   ├── AuthController.java              ✅ role-aware /dashboard redirect — admin always goes to /admin/dashboard
│   ├── BookingsController.java          ✅ week starts from today not Monday. ObjectMapper disables WRITE_DATES_AS_TIMESTAMPS
│   ├── AdminBookingController.java      ✅ /admin/bookings/pending /admin/bookings/accept/{id} /admin/bookings/reject/{id}
│   ├── AdminTripController.java         ✅ includes BLOCKED in listTrips. 1-hour cancel rule. cancel-block/{id} endpoint
│   ├── AdminDashboardController.java    ✅ week starts from today. Past trips filtered. Stats from rawTrips. bookingByTripId map
│   ├── AdminBlockController.java        ✅ NEW — /admin/block GET/POST time-range/whole-day/unblock
│   ├── AdminRequestController.java      ✅ redirects to /admin/dashboard (legacy — unused)
│   ├── AdminUserController.java         ✅ /admin/users view/block/verify/delete
│   └── AdminLogsController.java         ✅ date filter + search by username/email/phone. bookingByTripId map
├── model/
│   ├── User.java                        ✅
│   ├── Trip.java                        ✅ end_time nullable, label, distanceKm, fee, googleEtaMinutes, bufferedDurationMinutes, blockedReason
│   ├── Booking.java                     ✅ FetchType.EAGER for user + trip
│   ├── TripRequest.java                 ✅ (legacy — unused)
│   ├── Payment.java                     ✅
│   ├── PricingConfig.java               ✅
│   └── RegisterRequest.java             ✅
├── repository/
│   ├── UserRepository.java              ✅
│   ├── TripRepository.java              ✅
│   ├── BookingRepository.java           ✅
│   ├── TripRequestRepository.java       ✅ (legacy)
│   ├── PaymentRepository.java           ✅
│   └── PricingConfigRepository.java     ✅
├── service/
│   ├── BookingService.java              ✅ EXPIRY_SECONDS=120. All booking/trip updates in same transaction
│   ├── TripService.java                 ✅ getVisibleTripsForWeek returns ALL statuses including BLOCKED
│   ├── BusinessHoursService.java        ✅ OPEN EVERY DAY 04:00–23:00 + isPastSlot()
│   ├── GoogleMapsService.java           ✅ getDistanceAndEta() calculateFee()
│   ├── UserService.java                 ✅
│   ├── CustomUserDetailsService.java    ✅
│   └── TripRequestService.java          ✅ (legacy — unused)

src/main/resources/
├── application.properties               ✅
├── static/
│   ├── css/
│   │   ├── style.css                    ✅
│   │   └── bookings.css                 ✅
│   └── js/
│       ├── main.js                      ✅
│       ├── calendar.js                  ✅ BLOCKED rendered grey. Ghost slots suppressed for ALL statuses. Calendar starts from today
│       ├── bookings.js                  ✅ 2-step modal + Google Places + fare calculation
│       └── admin-notifications.js       ✅ 3500ms post-action pause. "Accepting..." processing message
└── templates/
    ├── index.html                       ✅
    ├── about.html                       ✅
    ├── contact.html                     ✅
    ├── admin/
    │   ├── dashboard.html               ✅ Read-only bookings view. No action buttons. Stats from selected period
    │   ├── block.html                   ✅ NEW — Block Time page. Reason required
    │   ├── bookings-pending.html        ✅ (legacy — no longer linked from navbar)
    │   ├── trips-list.html              ✅ BLOCKED trips shown. Reason in Booked By. 1-hour cancel rule
    │   ├── trips-new.html               ✅ Google Maps autocomplete. Auto route label. Live fare preview. Date/time validation
    │   ├── pricing.html                 ✅
    │   ├── requests.html                ✅ (legacy — unused)
    │   ├── users.html                   ✅
    │   └── logs.html                    ✅ Search by user. Reason shown for blocked. User details for booked
    ├── auth/
    │   ├── login.html                   ✅
    │   └── register.html                ✅
    └── user/
        ├── bookings.html                ✅ Calendar starts from today. Past slots greyed out
        ├── booking-waiting.html         ✅ Auto-redirects to dashboard 2.5s after CONFIRMED
        └── dashboard.html               ✅
```

---

## ⚙️ application.properties (current committed state)
```properties
spring.application.name=aj-transportation
spring.config.import=optional:classpath:application-local.properties
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.open-in-view=false
spring.thymeleaf.cache=false
spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.suffix=.html
server.port=8080
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
app.base-url=http://localhost:8080
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
| `/admin/bookings/pending` | ✅ | ADMIN (legacy — not linked) |
| `/admin/bookings/accept/{id}` POST | ✅ | ADMIN |
| `/admin/bookings/reject/{id}` POST | ✅ | ADMIN |
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