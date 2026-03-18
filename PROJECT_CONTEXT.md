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
- Trip pricing is calculated automatically via a **price-per-km algorithm** based on owner's rates
- **Google Maps API** used for distance/timing calculations on each trip
- Each trip slot gets a **15-minute automatic buffer** added to Google Maps ETA to cater for unforeseen stops or delays
- Admin/owner sets trip availability and can **block time slots** (breaks, Friday prayers, sick days, personal reasons)
- Admin can toggle any slot between **Busy** and **Available** from a separate owner-only interface
- Payments processed via Ozow (South African EFT) — linked to Uncle Ajmal's account once security is confirmed
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
| Maps/Distance | Google Maps API (Distance Matrix + Directions) |
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
14. Ozow payment account linked **only after** security is confirmed and app is secure

---

## 🆕 Key Features to Build

### 1. 💰 Price-Per-Km Algorithm
- Admin sets rate/km via dashboard input field
- Price calculated automatically from Google Maps distance
- Price shown to user before confirming booking
- Minimum fare applies regardless of distance

### 2. 🗺️ Google Maps Integration
- Google Maps Distance Matrix API for distance + ETA
- Estimated time + **15-minute buffer** = slot duration
- API key stored in `application-local.properties` (never committed)

### 3. ⏱️ 15-Minute Buffer System
- Every trip: Google Maps ETA + 15 minutes auto-added
- Prevents double-bookings from traffic/delays
- Admin can override per-trip if needed (future feature)

### 4. 🚫 Admin Slot Blocking
- Admin blocks/unblocks slots with a simple toggle
- Block entire days (Fridays, sick days, public holidays)
- Blocked slots show as unavailable on customer calendar
- Customers cannot see the reason for a block

### 5. 📸 Branding Assets (Pending from Owner)
- [ ] Company logo
- [ ] Photos of Uncle Ajmal
- [ ] Photos of clients / trips
- [ ] Exact rate per km (e.g. R8.50/km)
- [ ] Minimum fare amount

### 6. 💳 Ozow Payment Account Linking
- Only linked after Phase 11 security hardening is complete

---

## ✅ Phase Completion Status

### Phase 1 — Setup: ✅ COMPLETE
- Java 25, Maven 3.9.14, VS Code, GitHub Desktop all installed
- Supabase project `aj-transportation` created
- GitHub repo created (private), both teammates added

### Phase 2 — Spring Boot Project: ✅ COMPLETE
- Project generated (Spring Boot 4.0.3, Java 21, Maven)
- All dependencies added
- Full folder structure created
- Supabase database connected (`HikariPool-1 - Start completed`)
- README.md and PROJECT_CONTEXT.md created

### Phase 3 — Frontend Pages: ✅ COMPLETE

| File | Location |
|---|---|
| `index.html` | `src/main/resources/templates/` |
| `about.html` | `src/main/resources/templates/` |
| `contact.html` | `src/main/resources/templates/` |
| `bookings.html` | `src/main/resources/templates/user/` |
| `style.css` | `src/main/resources/static/css/` |
| `bookings.css` | `src/main/resources/static/css/` |
| `main.js` | `src/main/resources/static/js/` |
| `calendar.js` | `src/main/resources/static/js/` |

### Phase 4 — Database Tables + Java Models: ✅ COMPLETE

**Supabase tables created:**
- `users` — email, username, bcrypt password, role (USER/ADMIN)
- `trips` — date, times, addresses, Google Maps fields, fee, status, label
- `bookings` — links user → trip, tracks status + payment_status
- `payments` — links booking → Ozow reference, amount, status
- `pricing_config` — single-row table: rate_per_km + minimum_fare

**Java model classes created (`model/`):**
- `User.java`
- `Trip.java`
- `Booking.java`
- `Payment.java`
- `PricingConfig.java`
- `RegisterRequest.java` — form validation DTO (added Phase 5)

**Repository interfaces created (`repository/`):**
- `UserRepository.java` — findByEmail, findByUsername, existsByEmail, existsByUsername
- `TripRepository.java` — findByDate, findByDateBetween, findByDateBetweenAndStatus, findByStatus
- `BookingRepository.java` — findByUser, findByUserOrderByCreatedAtDesc, existsByTripIdAndStatusNot
- `PaymentRepository.java` — findByOzowReference, findByBookingId
- `PricingConfigRepository.java` — use findById(1) for single config row

### Phase 5 — User Registration & Login: ✅ COMPLETE

**Files created:**

| File | Location |
|---|---|
| `AuthController.java` | `src/main/java/com/ajtransportation/app/controller/` |
| `PageController.java` | `src/main/java/com/ajtransportation/app/controller/` (updated) |
| `UserService.java` | `src/main/java/com/ajtransportation/app/service/` |
| `CustomUserDetailsService.java` | `src/main/java/com/ajtransportation/app/service/` |
| `RegisterRequest.java` | `src/main/java/com/ajtransportation/app/model/` |
| `SecurityConfig.java` | `src/main/java/com/ajtransportation/app/config/` (updated) |
| `login.html` | `src/main/resources/templates/auth/` |
| `register.html` | `src/main/resources/templates/auth/` |
| `dashboard.html` | `src/main/resources/templates/user/` |

**What works after Phase 5:**
- `/register` — form with email, username, password, confirm password. Validates duplicates.
- `/login` — Spring Security form login using email + password
- `/dashboard` — logged-in users see their account info (bookings section shows 0 — Phase 6)
- `/logout` — clears session, redirects to homepage
- Passwords stored as BCrypt hashes — never plain text
- Duplicate email/username validation on registration
- Role-based access: USER vs ADMIN routes protected
- Navbar shows correct buttons based on login state (via `sec:authorize`)
- Admin user created directly in Supabase via SQL + bcrypt-generator.com

### Phase 6 — Admin Trip Creation + Calendar Backend: ⬜ NEXT
### Phase 7 — Admin Dashboard + Slot Blocking: ⬜ TODO
### Phase 8 — Google Maps + Price-Per-Km Algorithm: ⬜ TODO
### Phase 9 — Ozow Payment Integration: ⬜ TODO
### Phase 10 — Email Notifications: ⬜ TODO
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
│   │   └── SecurityConfig.java             ✅ Phase 5 — login/register/admin routes, BCrypt bean
│   ├── controller/
│   │   ├── PageController.java              ✅ Maps / /about /contact /bookings (handles logout param)
│   │   └── AuthController.java             ✅ /login GET, /register GET+POST, /dashboard GET
│   ├── model/
│   │   ├── User.java                        ✅ Phase 4
│   │   ├── Trip.java                        ✅ Phase 4
│   │   ├── Booking.java                     ✅ Phase 4
│   │   ├── Payment.java                     ✅ Phase 4
│   │   ├── PricingConfig.java               ✅ Phase 4
│   │   └── RegisterRequest.java             ✅ Phase 5 — form validation DTO
│   ├── repository/
│   │   ├── UserRepository.java              ✅ Phase 4
│   │   ├── TripRepository.java              ✅ Phase 4
│   │   ├── BookingRepository.java           ✅ Phase 4
│   │   ├── PaymentRepository.java           ✅ Phase 4
│   │   └── PricingConfigRepository.java     ✅ Phase 4
│   ├── service/
│   │   ├── UserService.java                 ✅ Phase 5 — register logic, BCrypt encoding
│   │   └── CustomUserDetailsService.java    ✅ Phase 5 — Spring Security login by email
│   └── AppApplication.java                  ✅ Working
│
├── src/main/resources/
│   ├── templates/
│   │   ├── index.html                       ✅ Homepage
│   │   ├── about.html                       ✅ About page
│   │   ├── contact.html                     ✅ Contact page (form present, POST not wired yet)
│   │   ├── auth/
│   │   │   ├── login.html                   ✅ Phase 5
│   │   │   └── register.html                ✅ Phase 5
│   │   ├── user/
│   │   │   ├── bookings.html                ✅ Calendar UI (uses SAMPLE_TRIPS dummy data — Phase 6 connects DB)
│   │   │   └── dashboard.html               ✅ Phase 5 — shows username, email, role badge
│   │   ├── admin/                           ⬜ Phase 6/7 — empty folder
│   │   └── layout/                          ⬜ base.html not yet used
│   ├── static/
│   │   ├── css/
│   │   │   ├── style.css                    ✅ Main styles (includes .form-input-error)
│   │   │   └── bookings.css                 ✅ Calendar-specific styles
│   │   ├── js/
│   │   │   ├── main.js                      ✅ Navbar toggle, scroll shadow, scroll animations, alert dismiss
│   │   │   └── calendar.js                  ✅ Weekly calendar logic — SAMPLE_TRIPS replaced in Phase 6
│   │   └── images/                          ⬜ Awaiting assets from Uncle Ajmal
│   ├── application.properties               ✅ Committed to GitHub
│   └── application-local.properties         ✅ Local only — GITIGNORED
│
├── pom.xml                                  ✅ All dependencies
├── .gitignore                               ✅ Protects secrets
└── README.md                                ✅ Professional readme
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
# Google Maps API key (add in Phase 8)
google.maps.api-key=YOUR_GOOGLE_MAPS_API_KEY
```

---

## 📦 pom.xml Dependencies (Current)

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
```

> 📌 Google Maps Java client library will be added to pom.xml in Phase 8

---

## 🗄️ Database Tables (Created in Supabase — Phase 4)

### `users`
| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key, auto-generated |
| email | VARCHAR | Unique, used for login |
| username | VARCHAR | Display name |
| password | VARCHAR | BCrypt hashed — NEVER plain text |
| role | VARCHAR | USER or ADMIN |
| created_at | TIMESTAMP | Auto-set |

### `trips`
| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| date | DATE | Trip date |
| start_time | TIME | Start of slot |
| end_time | TIME | End of slot — Google ETA + 15 min buffer |
| pickup_address | VARCHAR | For Google Maps calculation |
| dropoff_address | VARCHAR | For Google Maps calculation |
| distance_km | DECIMAL | Fetched from Google Maps |
| google_eta_minutes | INT | Raw ETA from Google |
| buffered_duration_minutes | INT | google_eta + 15 |
| fee | DECIMAL | Calculated by price-per-km algorithm |
| status | VARCHAR | AVAILABLE / BOOKED / BLOCKED |
| blocked_reason | VARCHAR | Internal only — not shown to customers |
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
| status | VARCHAR | PENDING / SUCCESS / FAILED |
| created_at | TIMESTAMP | Auto-set |

### `pricing_config`
| Column | Type | Notes |
|---|---|---|
| id | INT | Primary key (always = 1, single row) |
| rate_per_km | DECIMAL | Set by admin e.g. R8.50/km |
| minimum_fare | DECIMAL | Minimum charge regardless of distance |
| updated_at | TIMESTAMP | Last updated |

---

## 🗺️ URL Routing (Current + Planned)

| URL | Status | Auth |
|---|---|---|
| `/` | ✅ Built | ❌ Public |
| `/about` | ✅ Built | ❌ Public |
| `/contact` | ✅ Built (form not wired) | ❌ Public |
| `/bookings` | ✅ Built (dummy data) | ❌ Public |
| `/login` | ✅ Phase 5 | ❌ Public |
| `/register` | ✅ Phase 5 | ❌ Public |
| `/dashboard` | ✅ Phase 5 | ✅ Logged in |
| `/logout` | ✅ Phase 5 | ✅ Logged in |
| `/admin/dashboard` | ⬜ Phase 6/7 | ✅ ADMIN only |
| `/admin/trips/new` | ⬜ Phase 6 | ✅ ADMIN only |
| `/admin/trips/block` | ⬜ Phase 7 | ✅ ADMIN only |
| `/admin/pricing` | ⬜ Phase 8 | ✅ ADMIN only |
| `/api/price-estimate` | ⬜ Phase 8 | ❌ Public API |

---

## ⚠️ Known Gaps / Notes for Next Session

- `contact.html` has a form with a POST to `/contact` but no controller handles it yet — submitting will error. Wire up in a later phase or ignore until Phase 10.
- `calendar.js` still uses `SAMPLE_TRIPS` dummy data — Phase 6 replaces this with real DB data.
- `dashboard.html` stat cards show hardcoded `0` — Phase 6 wires up real booking counts.
- `admin/` templates folder is empty — Phase 6/7 builds this out.
- `layout/base.html` exists in folder structure but is not currently used by any template (each page is self-contained with its own navbar/footer).

---

## 📋 Assets Still Needed From Uncle Ajmal

- [ ] Company logo (PNG or SVG preferred)
- [ ] Photos of Uncle Ajmal (for About page)
- [ ] Photos of clients / trips (for homepage)
- [ ] Exact rate per km he charges (e.g. R8.50/km)
- [ ] Minimum fare amount (if any)
- [ ] Which days/times to block by default (e.g. every Friday 12:00–14:00)
- [ ] Any additional features he wants

---

## 👥 Team Workflow

- Both teammates use **GitHub Desktop** only (no CLI git)
- Always **Fetch + Pull** before starting work each day
- Always **Commit + Push** when done for the day
- `application-local.properties` shared privately via WhatsApp — never via GitHub

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

## 📌 What To Do Next — Phase 6

**Goal:** Build admin trip creation and connect the booking calendar to real database data.

**Step 1 — TripService.java** (new file in `service/`):
- `createTrip(...)` — save a new trip to the DB
- `getTripsForWeek(LocalDate weekStart)` — fetch trips for a 7-day range
- `getTripById(UUID id)` — fetch a single trip

**Step 2 — AdminController.java** (new file in `controller/`):
- GET `/admin/dashboard` — admin home page showing all trips
- GET `/admin/trips/new` — show trip creation form
- POST `/admin/trips/new` — save new trip, redirect back to dashboard

**Step 3 — Admin templates** (new files in `templates/admin/`):
- `dashboard.html` — admin overview with trip list and stats
- `new-trip.html` — form: date, start time, label, fee, pickup address, dropoff address

**Step 4 — Update PageController:**
- GET `/bookings` — fetch real trips from DB for current week, pass to Thymeleaf as a JSON string embedded in a `<script>` tag

**Step 5 — Update `calendar.js`:**
- Replace `SAMPLE_TRIPS` constant with trips read from the embedded JSON script tag

**Step 6 — Update `dashboard.html` (user):**
- Show real booking counts using `BookingRepository`
