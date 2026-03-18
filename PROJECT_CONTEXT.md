# AJ Transportation — Project Context & Checklist
### For use when starting a new Claude chat session

---

## 🧠 Project Summary

**App name:** AJ Transportation
**Purpose:** Web-based transportation booking system
**Owner:** Uncle Ajmal
**Team size:** 2 people (both complete beginners)
**Demo target:** ~2 weeks from project start

**What the app does:**
- Customers register via email/username/password and book transportation trips
- They see a live weekly calendar showing available and booked time slots
- They cannot book already-taken slots
- Trip pricing is calculated automatically via a **price-per-km algorithm** — R8.00/km, minimum R50.00
- **Google Maps Distance Matrix API** used for real-world distance and ETA on each trip
- Each trip slot automatically has a **15-minute buffer** added to Google Maps ETA (endTime = startTime + ETA + 15 min)
- Admin/owner sets trip availability and can **block time slots** (breaks, Friday prayers, sick days, personal reasons)
- Admin can toggle any slot between **Blocked**, **Available**, and view Booked slots
- Payments will be processed via Ozow (South African EFT) — linked after security hardening
- Both customer and admin have completely separate interfaces
- Must be mobile responsive (phone + laptop)
- Must be highly secure before payment account is linked

---

## 🛠️ Tech Stack (Decided and Fixed)

| Layer | Technology |
|---|---|
| Language | Java |
| Backend | Spring Boot 4.0.3 |
| Frontend | Thymeleaf + HTML + CSS + JavaScript |
| Database | Supabase (PostgreSQL 17.6) |
| Payments | Ozow (South African EFT) |
| Maps/Distance | Google Maps Distance Matrix API |
| Build tool | Apache Maven 3.9.14 |
| Security | Spring Security |
| Version control | GitHub Desktop |
| IDE | VS Code |

> ⚠️ Do NOT suggest changing the tech stack under any circumstances.

---

## 🚨 Critical Notes for Claude

1. Spring Boot version is **4.0.3** — always use `jakarta.*` not `javax.*` imports
2. Java on machine is 25 but project targets 21 — this is fine, do not change
3. `application-local.properties` is gitignored — each teammate has their own local copy
4. Supabase connection uses **port 5432** with pooler host format
5. Supabase username format is `postgres.PROJECT_ID` (not just `postgres`)
6. GitHub Desktop is used — no command-line git commands needed
7. Both teammates are **complete beginners** — explain everything step by step
8. Payment gateway is **Ozow** — South African EFT only, no cards
9. Do NOT suggest changing the tech stack
10. Fonts used: **Syne** (headings) + **DM Sans** (body) — keep consistent
11. Color theme: primary `#0a7c6e` (teal), accent `#f0a500` (gold), dark bg `#0d1117`
12. Colors, wording, logo, and calendar structure are **NOT final** — placeholders only
13. Owner logo and photos of Uncle Ajmal + clients still need to be provided
14. Ozow payment account linked **only after** Phase 11 security hardening is complete
15. Admin account created directly in Supabase using bcrypt hash from bcrypt-generator.com
16. Rate per km is confirmed: **R8.00/km** with a minimum fare of **R50.00**
17. Google Maps API key stored in `application-local.properties` as `google.maps.api-key`
18. `RestTemplate` is used for Google Maps calls (no extra Maven dependency needed in Spring Boot)
19. `GoogleMapsService` gracefully handles missing API key — trip creation still works without it

---

## 💰 Price-Per-Km Algorithm (IMPLEMENTED — Phase 8)

- Rate: **R8.00/km** (configurable via `/admin/trips/pricing`)
- Minimum fare: **R50.00** (configurable)
- Formula: `fee = MAX(distanceKm × ratePerKm, minimumFare)`
- Google Maps Distance Matrix API provides accurate real-world distance
- Fee stored on the `trips` table and shown to user in booking modal
- `PricingConfig` table (id=1) stores current rate + minimum fare
- `TripService.savePricingConfig()` updates the config row
- `TripService.getPricingConfig()` loads it with safe defaults if row is absent

---

## 🗺️ Google Maps Integration (IMPLEMENTED — Phase 8)

**Service:** `GoogleMapsService.java`

**What it does:**
1. Calls `https://maps.googleapis.com/maps/api/distancematrix/json`
2. Returns `DistanceResult` with:
   - `distanceKm` — real-world km (2 decimal places)
   - `etaMinutes` — Google's travel time in minutes
   - `bufferedDurationMinutes` — etaMinutes + 15
3. `calculateFee(distanceKm, ratePerKm, minimumFare)` applies the price-per-km formula

**How trips get enriched automatically:**
- Admin fills in pickup + dropoff on `/admin/trips/new`
- On save, `TripService.createTrip()` calls `enrichTripWithGoogleMaps()`
- Sets: `distanceKm`, `googleEtaMinutes`, `bufferedDurationMinutes`, `endTime`, `fee`
- If Google Maps fails or API key is missing → trip saved with admin-entered values

**API key setup (each teammate):**
```properties
# application-local.properties
google.maps.api-key=YOUR_GOOGLE_MAPS_API_KEY
```

---

## ⏱️ 15-Minute Buffer System (IMPLEMENTED)

- `endTime = startTime + googleEtaMinutes + 15`
- Stored as `buffered_duration_minutes` in the `trips` table
- Prevents double-bookings caused by traffic delays or unforeseen stops
- Admin can still override `endTime` manually if addresses are not provided

---

## 📅 Booking Calendar — Fixed Issues (Phase 6 fix session)

**Problems fixed:**
1. **Trip slots not clickable** — `.slot-pill` had `pointer-events: none` in CSS. Fixed by rebuilding slot HTML in `calendar.js` without pill wrappers, using `onclick="handleSlotClick(this)"` directly on the `.trip-slot` div.
2. **Modal not opening** — The modal overlay used the wrong CSS class (`modal` + `modal-backdrop` approach) that conflicted with the Thymeleaf `sec:authorize` rendering. Rebuilt the modal as a single `.modal-overlay` div with class `open`/hidden toggled by JS.
3. **Booking form hidden for guests** — `sec:authorize` sections now correctly show: confirm form for logged-in users, login/register prompt for guests.
4. **Data attribute approach** — Trip data is now stored as JSON in `data-trip='...'` on each slot div. `handleSlotClick(this)` parses it. No more passing 6 args through `onclick`.

**Files changed in this fix session:**
- `src/main/resources/static/js/calendar.js` — full rewrite of slot rendering + modal logic
- `src/main/resources/templates/user/bookings.html` — modal rebuilt, sec:authorize fixed

---

## ✅ Phase Completion Status

### Phase 1 — Setup: ✅ COMPLETE
### Phase 2 — Spring Boot Project: ✅ COMPLETE
### Phase 3 — Frontend Pages: ✅ COMPLETE
### Phase 4 — Database Tables + Java Models: ✅ COMPLETE
### Phase 5 — User Registration & Login: ✅ COMPLETE
### Phase 6 — Booking Calendar Backend: ✅ COMPLETE
### Phase 7 — Admin Dashboard (partial): ✅ COMPLETE (schedule, slot blocking, booking cancel)
### Phase 8 — Google Maps + Price-Per-Km: ✅ COMPLETE

### Phase 7 remaining items: ⬜ TODO
- Admin User Management (`/admin/users`) — block, unblock, delete, edit users
- Revenue Dashboard with Ozow vs Cash split
- In-App Admin Notifications (`notifications` table)

### Phase 9 — Ozow Payment Integration: ⬜ TODO
### Phase 10 — Email + SMS Notifications: ⬜ TODO
### Phase 11 — Security Hardening + Ozow Account Link: ⬜ TODO
### Phase 12 — Mobile Responsiveness & Testing: ⬜ TODO
### Phase 13 — Deployment: ⬜ TODO

---

## 📁 Full Project Folder Structure (Current State)

```
aj-transportation/
│
├── src/main/java/com/ajtransportation/app/
│   ├── config/
│   │   └── SecurityConfig.java                  ✅ Phase 5 + @EnableMethodSecurity
│   ├── controller/
│   │   ├── PageController.java                  ✅ /, /about, /contact
│   │   ├── AuthController.java                  ✅ /register, /login, /dashboard
│   │   ├── BookingsController.java              ✅ /bookings, /bookings/book, /bookings/cancel
│   │   ├── AdminTripController.java             ✅ /admin/trips/**, /admin/trips/pricing
│   │   └── AdminDashboardController.java        ✅ /admin/dashboard (Day/Week/Month views)
│   ├── model/
│   │   ├── User.java                            ✅ Phase 4
│   │   ├── Trip.java                            ✅ Phase 4
│   │   ├── Booking.java                         ✅ Phase 4
│   │   ├── Payment.java                         ✅ Phase 4
│   │   ├── PricingConfig.java                   ✅ Phase 4
│   │   └── RegisterRequest.java                 ✅ Phase 5
│   ├── repository/
│   │   ├── UserRepository.java                  ✅ Phase 4
│   │   ├── TripRepository.java                  ✅ Phase 4
│   │   ├── BookingRepository.java               ✅ Phase 4
│   │   ├── PaymentRepository.java               ✅ Phase 4
│   │   └── PricingConfigRepository.java         ✅ Phase 4
│   ├── service/
│   │   ├── UserService.java                     ✅ Phase 5
│   │   ├── CustomUserDetailsService.java        ✅ Phase 5
│   │   ├── TripService.java                     ✅ Phase 6 + Phase 8 (Google Maps enrichment)
│   │   ├── BookingService.java                  ✅ Phase 6
│   │   └── GoogleMapsService.java               ✅ Phase 8 (NEW)
│   └── AppApplication.java                      ✅
│
├── src/main/resources/
│   ├── templates/
│   │   ├── index.html                           ✅ Homepage
│   │   ├── about.html                           ✅ About page
│   │   ├── contact.html                         ✅ Contact page
│   │   ├── auth/
│   │   │   ├── login.html                       ✅ Phase 5
│   │   │   └── register.html                    ✅ Phase 5
│   │   ├── user/
│   │   │   ├── bookings.html                    ✅ Phase 6 (FIXED — modal + click working)
│   │   │   └── dashboard.html                   ✅ Phase 6
│   │   ├── admin/
│   │   │   ├── dashboard.html                   ✅ Phase 7 (Day/Week/Month views)
│   │   │   ├── trips-list.html                  ✅ Phase 6
│   │   │   ├── trips-new.html                   ✅ Phase 8 (Google Maps mode indicator)
│   │   │   └── pricing.html                     ✅ Phase 8 (NEW — rate/km + minimum fare)
│   │   └── layout/
│   │       └── base.html                        ✅ Shared layout
│   ├── static/
│   │   ├── css/
│   │   │   ├── style.css                        ✅ Main styles
│   │   │   └── bookings.css                     ✅ Calendar styles
│   │   ├── js/
│   │   │   ├── main.js                          ✅ Navbar + animations
│   │   │   └── calendar.js                      ✅ Phase 6 FIXED — click-to-book working
│   │   └── images/                              ⬜ Awaiting assets from Uncle Ajmal
│   ├── application.properties                   ✅ Committed to GitHub
│   └── application-local.properties             ✅ Local only — GITIGNORED
│
├── pom.xml                                      ✅ All dependencies
├── .gitignore                                   ✅ Protects secrets
└── README.md                                    ✅
```

---

## ⚙️ Current application.properties

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
```

---

## ⚙️ application-local.properties template (GITIGNORED)

```properties
spring.datasource.url=jdbc:postgresql://[POOLER_HOST]:5432/postgres
spring.datasource.username=postgres.[PROJECT_ID]
spring.datasource.password=[DB_PASSWORD]
spring.datasource.driver-class-name=org.postgresql.Driver
supabase.url=https://[PROJECT_ID].supabase.co
supabase.anon-key=[ANON_KEY]
supabase.service-role-key=[SERVICE_ROLE_KEY]
spring.mail.username=YOUR_GMAIL@gmail.com
spring.mail.password=YOUR_GMAIL_APP_PASSWORD
# Google Maps API key — required for Phase 8 auto-calculation
google.maps.api-key=YOUR_GOOGLE_MAPS_API_KEY
```

---

## 📦 pom.xml Dependencies (Current — no new dependencies needed for Phase 8)

```
spring-boot-starter-data-jpa
spring-boot-starter-mail
spring-boot-starter-security
spring-boot-starter-thymeleaf
spring-boot-starter-validation
spring-boot-starter-webmvc          ← includes RestTemplate
thymeleaf-extras-springsecurity6
spring-boot-devtools (runtime)
postgresql (runtime)
jackson-datatype-jsr310             ← for LocalDate/LocalTime JSON serialisation
```

> 📌 `RestTemplate` is included with `spring-boot-starter-webmvc` — no extra dependency needed for Google Maps calls.

---

## 🗄️ Database Tables (Supabase — Phase 4 + Phase 7 additions needed)

### `users`
| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| email | VARCHAR | Unique, used for login |
| username | VARCHAR | Display name |
| password | VARCHAR | BCrypt hashed |
| role | VARCHAR | USER or ADMIN |
| is_blocked | BOOLEAN | Default false — add in Phase 7 user management |
| created_at | TIMESTAMP | Auto-set |

### `trips`
| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| date | DATE | Trip date |
| start_time | TIME | Start of slot |
| end_time | TIME | startTime + googleEtaMinutes + 15 min buffer |
| pickup_address | VARCHAR | For Google Maps |
| dropoff_address | VARCHAR | For Google Maps |
| distance_km | DECIMAL | From Google Maps Distance Matrix |
| google_eta_minutes | INT | Raw ETA from Google |
| buffered_duration_minutes | INT | google_eta + 15 |
| fee | DECIMAL | = MAX(distanceKm × R8.00, R50.00) |
| status | VARCHAR | AVAILABLE / BOOKED / BLOCKED |
| blocked_reason | VARCHAR | Internal only |
| label | VARCHAR | Route name shown on calendar |
| created_at | TIMESTAMP | Auto-set |

### `bookings`
| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| user_id | UUID | FK → users |
| trip_id | UUID | FK → trips |
| status | VARCHAR | PENDING / CONFIRMED / CANCELLED |
| payment_status | VARCHAR | UNPAID / PAID |
| created_at | TIMESTAMP | Auto-set |

### `payments`
| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| booking_id | UUID | FK → bookings |
| amount | DECIMAL | Amount paid |
| ozow_reference | VARCHAR | Ozow transaction ID |
| payment_method | VARCHAR | `OZOW` or `CASH` — add in Phase 7 |
| status | VARCHAR | PENDING / SUCCESS / FAILED |
| created_at | TIMESTAMP | Auto-set |

### `notifications` *(Phase 7 — not yet created)*
| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| message | VARCHAR | e.g. "John booked Trip on 2026-04-01 at 08:00" |
| is_read | BOOLEAN | Default false |
| created_at | TIMESTAMP | Auto-set |

### `pricing_config`
| Column | Type | Notes |
|---|---|---|
| id | INT | Always = 1 (single row) |
| rate_per_km | DECIMAL | R8.00/km (Uncle Ajmal's confirmed rate) |
| minimum_fare | DECIMAL | R50.00 |
| updated_at | TIMESTAMP | Last updated |

---

## 🗺️ URL Routing (Current State)

| URL | Status | Auth |
|---|---|---|
| `/` | ✅ Built | ❌ Public |
| `/about` | ✅ Built | ❌ Public |
| `/contact` | ✅ Built | ❌ Public |
| `/bookings` | ✅ Built + Fixed | ❌ Public |
| `/bookings/book` | ✅ Phase 6 | ✅ Logged in |
| `/bookings/cancel/{id}` | ✅ Phase 6 | ✅ Logged in |
| `/login` | ✅ Phase 5 | ❌ Public |
| `/register` | ✅ Phase 5 | ❌ Public |
| `/dashboard` | ✅ Phase 5 + 6 | ✅ Logged in |
| `/logout` | ✅ Phase 5 | ✅ Logged in |
| `/admin/trips` | ✅ Phase 6 | ✅ ADMIN only |
| `/admin/trips/new` | ✅ Phase 6 + 8 | ✅ ADMIN only |
| `/admin/trips/pricing` | ✅ Phase 8 | ✅ ADMIN only |
| `/admin/trips/block/{id}` | ✅ Phase 6 | ✅ ADMIN only |
| `/admin/trips/unblock/{id}` | ✅ Phase 6 | ✅ ADMIN only |
| `/admin/trips/delete/{id}` | ✅ Phase 6 | ✅ ADMIN only |
| `/admin/dashboard` | ✅ Phase 7 | ✅ ADMIN only |
| `/admin/dashboard/block/{id}` | ✅ Phase 7 | ✅ ADMIN only |
| `/admin/dashboard/unblock/{id}` | ✅ Phase 7 | ✅ ADMIN only |
| `/admin/dashboard/cancel-by-trip/{id}` | ✅ Phase 7 | ✅ ADMIN only |
| `/admin/users` | ⬜ Phase 7 remaining | ✅ ADMIN only |
| `/admin/users/edit/{id}` | ⬜ Phase 7 remaining | ✅ ADMIN only |
| `/admin/users/block/{id}` | ⬜ Phase 7 remaining | ✅ ADMIN only |
| `/admin/users/delete/{id}` | ⬜ Phase 7 remaining | ✅ ADMIN only |
| `/admin/revenue` | ⬜ Phase 7 remaining | ✅ ADMIN only |
| `/admin/notifications` | ⬜ Phase 7 remaining | ✅ ADMIN only |
| `/api/price-estimate` | ⬜ Future | ❌ Public API |

---

## 🔐 SecurityConfig.java (Current — Phase 5, unchanged)

- `/css/**`, `/js/**`, `/images/**`, `/fonts/**` — public
- `/`, `/about`, `/contact`, `/bookings` — public
- `/login`, `/register` — public
- `/admin/**` — ADMIN role only
- Everything else — authenticated users only
- `@EnableMethodSecurity` enabled for `@PreAuthorize` on controllers

---

## 📋 Assets Still Needed From Uncle Ajmal

- [ ] Company logo (PNG or SVG preferred)
- [ ] Photos of Uncle Ajmal (for About page)
- [ ] Photos of clients / trips (for homepage)
- [x] Rate per km — **confirmed R8.00/km**
- [ ] Minimum fare — **using R50.00 as placeholder**
- [ ] Days/times to block by default (e.g. every Friday 12:00–14:00)
- [ ] Ozow merchant credentials (for Phase 9)
- [ ] Any other features requested

---

## 👥 Team Workflow

- Both teammates use **GitHub Desktop** only (no CLI git)
- Always **Fetch + Pull** before starting work each day
- Always **Commit + Push** when done for the day
- `application-local.properties` shared privately via WhatsApp — never via GitHub
- Google Maps API key goes in `application-local.properties` — never commit it

---

## 🚀 How to Run the App

```
1. Open VS Code
2. Open Terminal (Ctrl + `)
3. Run: mvn spring-boot:run
4. Open browser: http://localhost:8080
5. Stop: Ctrl + C
```

---

## 🔑 Admin Login Details

- **URL:** `http://localhost:8080/login`
- **Email:** `admin@ajtransportation.co.za`
- **Password:** whatever was entered into bcrypt-generator.com when creating the hash
- Admin account was inserted directly into Supabase using SQL — role is `ADMIN`

---

## 🗺️ How to Get a Google Maps API Key (for Phase 8)

1. Go to [console.cloud.google.com](https://console.cloud.google.com)
2. Create a project (or use existing)
3. Enable the **Distance Matrix API**
4. Go to **APIs & Services → Credentials → Create API Key**
5. Copy the key
6. Add to your local `application-local.properties`:
   ```properties
   google.maps.api-key=AIzaSy...yourkey...
   ```
7. Restart the app (`Ctrl+C` then `mvn spring-boot:run`)
8. Create a trip with pickup + dropoff address — distance, ETA, and fee will auto-populate

> ⚠️ Google Maps requires a billing account but has a $200/month free tier — more than enough for this app.

---

## ⚠️ Known Items for Next Session

- **Phase 7 remaining:** Admin user management, revenue dashboard, in-app notifications
- **Phase 9:** Ozow payment integration (only after security hardening)
- **`is_blocked` column** needs to be added to `users` table in Supabase for user blocking feature
- **`payment_method` column** needs to be added to `payments` table for revenue split
- **`notifications` table** needs to be created in Supabase
