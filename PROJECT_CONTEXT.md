# AJ Transportation — Project Context & Checklist
### For use when starting a new Claude chat session
### Last updated: After Phase 6 complete + Phase 7 partial

---

## 🧠 Project Summary

**App name:** AJ Transportation
**Purpose:** Web-based transportation booking system
**Owner:** Uncle Ajmal (South Africa)
**Team size:** 2 people (both complete beginners)
**IDE:** VS Code
**Version control:** GitHub Desktop only (no CLI git)

**What the app does:**
- Customers register via email / phone number / username / password
- Email verification is required before a user can log in
- Weekly calendar shows: green slots (existing trips — book immediately), amber slots (open business hours — request a custom trip), red slots (already booked), grey (closed/blocked/past)
- Clicking a green slot → booking modal → user confirms → trip is booked immediately
- Clicking an amber slot → request modal → user fills in pickup + dropoff + notes → admin reviews and approves or rejects
- Trip pricing: R8.00/km, minimum fare R50.00 — calculated via Google Maps Distance Matrix API
- Each trip gets a 15-minute buffer added to Google Maps ETA (endTime = startTime + ETA + 15 min)
- Admin can: create trips, block slots, cancel bookings, approve/reject trip requests, view all users, view trip logs
- Payments via Ozow (South African EFT) — Phase 9
- Email notifications — Phase 10

---

## 🛠️ Tech Stack (Fixed — do NOT change under any circumstances)

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

---

## 🚨 Critical Notes for Claude — Read Before Writing Any Code

1. Spring Boot **4.0.3** — always use `jakarta.*` not `javax.*` imports
2. Java on machine is 25, project targets 21 — do not change pom.xml
3. `application-local.properties` is gitignored — never commit it, share via WhatsApp only
4. Supabase uses **port 6543** (Transaction mode pooler) — NOT 5432. Using 5432 causes `MaxClientsInSessionMode` crash
5. HikariCP pool: `maximum-pool-size=2`, `minimum-idle=1` — required for Supabase free tier
6. Supabase username format: `postgres.PROJECT_ID`
7. GitHub Desktop only — never give CLI git commands to the team
8. Both teammates are complete beginners — explain every step clearly
9. Payment gateway: Ozow only — South African EFT, no cards
10. Fonts: **Syne** (headings) + **DM Sans** (body) — never change these
11. Colors: primary `#0a7c6e` (teal), accent `#f0a500` (gold), dark bg `#0d1117`
12. Rate per km: **R8.00/km**, minimum fare: **R50.00** — configurable at `/admin/trips/pricing`
13. Google Maps API key in `application-local.properties` as `google.maps.api-key`
14. `app.base-url=http://localhost:8080` in `application.properties` — change for production
15. `RestTemplate` bean declared in `WebConfig.java` — Spring Boot 4 does NOT auto-create it
16. Admin account must have `email_verified = true` set manually in Supabase SQL
17. `style.css` was fully rewritten to fix a broken unclosed CSS block — do not revert to old version
18. `contact.html` form uses `POST /contact` — handled by `PageController.java`
19. When generating steps for the team, always split work between Person 1 and Person 2 simultaneously where possible
20. **Do NOT generate an entire phase at once** — break into small blocks and wait for user confirmation before proceeding to next block

---

## ✅ Phase Completion Status

### Phase 1 — Project Setup: ✅ COMPLETE
### Phase 2 — Spring Boot Project: ✅ COMPLETE
### Phase 3 — Frontend Pages: ✅ COMPLETE
### Phase 4 — Database Tables + Java Models: ✅ COMPLETE
### Phase 5 — User Registration & Login: ✅ COMPLETE (with updates)

**Phase 5 updates applied:**
- `User.java` — added `phone_number`, `email_verified`, `verification_token`, `is_blocked`
- `RegisterRequest.java` — added `phoneNumber` with SA phone number validation
- `UserService.java` — sends verification email on register; `verifyEmail(token)` method
- `UserRepository.java` — added `findByVerificationToken()`, `findAllByOrderByCreatedAtDesc()`
- `CustomUserDetailsService.java` — blocks login if `emailVerified=false` OR `isBlocked=true`
- `AuthController.java` — added `/verify-email?token=` endpoint; better error messages
- `register.html` — phone number field added, email verification notice shown
- `application.properties` — `app.base-url=http://localhost:8080` added

**⚠️ Required Supabase SQL (run once if not already done):**
```sql
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_number VARCHAR;
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE users ADD COLUMN IF NOT EXISTS verification_token VARCHAR;
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_blocked BOOLEAN NOT NULL DEFAULT false;

-- IMPORTANT: Must run this or admin cannot log in
UPDATE users SET email_verified = true WHERE role = 'ADMIN';
```

### Phase 6 — Booking Calendar: ✅ COMPLETE

**All Phase 6 items done:**
- `BookingsController.java` — `/bookings`, `/bookings/book`, `/bookings/cancel/{id}`, `/bookings/request`
- `TripService.java` — trip CRUD + Google Maps enrichment
- `BookingService.java` — create/cancel bookings with race condition protection
- `calendar.js` — full rewrite: green (available) + amber (open request) + red (booked) slots; both modals wired
- `bookings.html` — both modals (booking + request), sec:authorize correct
- `TripRequest.java` — model for user-submitted trip requests
- `TripRequestRepository.java` — queries for requests
- `TripRequestService.java` — create/approve/reject requests
- `BusinessHoursService.java` — Mon–Thu 04:00–12:00, Fri 04:00–11:30, Sat 06:00–10:00, Sun closed
- `WebConfig.java` — RestTemplate bean (new file required for Spring Boot 4)

**⚠️ Required Supabase SQL (run once if not already done):**
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

**Completed in Phase 7:**
- `AdminDashboardController.java` — Day/Week/Month schedule view, block/unblock slots, cancel bookings
- `AdminTripController.java` — create trips, pricing config
- `AdminRequestController.java` — ✅ `/admin/requests` approve/reject trip requests
- `AdminUserController.java` — ✅ `/admin/users` view/block/unblock/verify/delete
- `AdminLogsController.java` — ✅ `/admin/logs` trip log with date range filter
- `admin/dashboard.html` — schedule with Day/Week/Month toggle
- `admin/requests.html` — ✅ trip request inbox (shows user email + phone)
- `admin/users.html` — ✅ user management table
- `admin/logs.html` — ✅ trip log with revenue summary
- `user/dashboard.html` — updated: bookings table + trip requests log + 3 stat cards
- `user/bookings.html` — booking modal + request modal fully working

**⚠️ Phase 7 items still TODO (in priority order):**
1. Admin private booking — book a trip on behalf of a specific user
2. Block whole day — block all slots on a given date at once
3. Revenue dashboard at `/admin/revenue`
4. In-app admin notifications — `notifications` table + badge count

### Phase 8 — Google Maps + Price-Per-Km: ✅ COMPLETE
- `GoogleMapsService.java` — Distance Matrix API
- `TripService.java` — auto-enriches trips when addresses provided
- `admin/trips-new.html` — shows Google Maps mode vs manual mode
- `admin/pricing.html` — live fee preview

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
│   ├── SecurityConfig.java              ✅ /verify-email added as public route
│   └── WebConfig.java                   ✅ RestTemplate bean
├── controller/
│   ├── PageController.java              ✅ GET + POST /contact
│   ├── AuthController.java              ✅ /register /login /dashboard /verify-email
│   ├── BookingsController.java          ✅ /bookings /bookings/book /bookings/request /bookings/cancel
│   ├── AdminTripController.java         ✅ /admin/trips/** /admin/trips/pricing
│   ├── AdminDashboardController.java    ✅ /admin/dashboard Day/Week/Month
│   ├── AdminRequestController.java      ✅ /admin/requests approve/reject
│   ├── AdminUserController.java         ✅ /admin/users view/block/verify/delete
│   └── AdminLogsController.java         ✅ /admin/logs date filter
├── model/
│   ├── User.java                        ✅ phone email_verified verification_token is_blocked
│   ├── Trip.java                        ✅ end_time nullable
│   ├── TripRequest.java                 ✅ open slot trip requests
│   ├── Booking.java                     ✅
│   ├── Payment.java                     ✅
│   ├── PricingConfig.java               ✅
│   └── RegisterRequest.java             ✅ phoneNumber + SA validation
├── repository/
│   ├── UserRepository.java              ✅ findByVerificationToken findAllByOrderByCreatedAtDesc
│   ├── TripRepository.java              ✅
│   ├── TripRequestRepository.java       ✅
│   ├── BookingRepository.java           ✅
│   ├── PaymentRepository.java           ✅
│   └── PricingConfigRepository.java     ✅
└── service/
    ├── UserService.java                 ✅ email verification
    ├── CustomUserDetailsService.java    ✅ blocks unverified + blocked
    ├── TripService.java                 ✅ CRUD + Google Maps enrichment
    ├── TripRequestService.java          ✅ create/approve/reject requests
    ├── BookingService.java              ✅
    ├── BusinessHoursService.java        ✅ open hours definition
    └── GoogleMapsService.java           ✅ Distance Matrix API

src/main/resources/
├── templates/
│   ├── index.html                       ✅
│   ├── about.html                       ✅
│   ├── contact.html                     ✅
│   ├── auth/
│   │   ├── login.html                   ✅
│   │   └── register.html                ✅ phone number field
│   ├── user/
│   │   ├── bookings.html                ✅ booking + request modals
│   │   └── dashboard.html               ✅ bookings + trip requests + stat cards
│   └── admin/
│       ├── dashboard.html               ✅ Day/Week/Month + block/cancel
│       ├── trips-list.html              ✅
│       ├── trips-new.html               ✅ Google Maps mode indicator
│       ├── pricing.html                 ✅ live fee preview
│       ├── requests.html                ✅ trip request inbox
│       ├── users.html                   ✅ user management
│       └── logs.html                    ✅ trip log date filter
├── static/
│   ├── css/
│   │   ├── style.css                    ✅ fully rewritten — broken CSS fixed
│   │   └── bookings.css                 ✅
│   └── js/
│       ├── main.js                      ✅
│       └── calendar.js                  ✅ green+amber+red slots + both modals
├── application.properties               ✅ app.base-url included
└── application-local.properties         GITIGNORED
```

---

## 🗄️ Database Tables (Complete Current State)

### `users`
| Column | Type | Notes |
|---|---|---|
| id | UUID | PK |
| email | VARCHAR | Unique |
| username | VARCHAR | Display name |
| phone_number | VARCHAR | Optional, SA format |
| password | VARCHAR | BCrypt |
| role | VARCHAR | USER / ADMIN |
| email_verified | BOOLEAN | Must be true to log in |
| verification_token | VARCHAR | Cleared after verification |
| is_blocked | BOOLEAN | Admin can block users |
| created_at | TIMESTAMP | Auto |

### `trips`
| Column | Type | Notes |
|---|---|---|
| id | UUID | PK |
| date | DATE | |
| start_time | TIME | |
| end_time | TIME | Nullable |
| pickup_address | VARCHAR | |
| dropoff_address | VARCHAR | |
| distance_km | DECIMAL | From Google Maps |
| google_eta_minutes | INT | |
| buffered_duration_minutes | INT | ETA + 15 |
| fee | DECIMAL | MAX(km × R8, R50) |
| status | VARCHAR | AVAILABLE / BOOKED / BLOCKED |
| blocked_reason | VARCHAR | Internal |
| label | VARCHAR | Shown on calendar |
| created_at | TIMESTAMP | |

### `trip_requests`
| Column | Type | Notes |
|---|---|---|
| id | UUID | PK |
| user_id | UUID | FK → users |
| requested_date | DATE | |
| requested_start_time | TIME | |
| pickup_address | VARCHAR | |
| dropoff_address | VARCHAR | |
| additional_notes | VARCHAR(500) | Optional |
| status | VARCHAR | PENDING / APPROVED / REJECTED |
| trip_id | UUID | FK → trips (set on approval) |
| admin_notes | VARCHAR(500) | Admin message to user |
| created_at | TIMESTAMP | |

### `bookings`
| Column | Type | Notes |
|---|---|---|
| id | UUID | PK |
| user_id | UUID | FK → users |
| trip_id | UUID | FK → trips |
| status | VARCHAR | PENDING / CONFIRMED / CANCELLED |
| payment_status | VARCHAR | UNPAID / PAID |
| created_at | TIMESTAMP | |

### `payments`
| Column | Type | Notes |
|---|---|---|
| id | UUID | PK |
| booking_id | UUID | FK → bookings |
| amount | DECIMAL | |
| ozow_reference | VARCHAR | Ozow transaction ID |
| payment_method | VARCHAR | OZOW or CASH (add in Phase 9) |
| status | VARCHAR | PENDING / SUCCESS / FAILED |
| created_at | TIMESTAMP | |

### `pricing_config`
| Column | Type | Notes |
|---|---|---|
| id | INT | Always = 1 |
| rate_per_km | DECIMAL | R8.00/km |
| minimum_fare | DECIMAL | R50.00 |
| updated_at | TIMESTAMP | |

### `notifications` — ⬜ NOT YET CREATED
| Column | Type | Notes |
|---|---|---|
| id | UUID | PK |
| message | VARCHAR | e.g. "John booked Trip 2026-04-01 08:00" |
| is_read | BOOLEAN | Default false |
| created_at | TIMESTAMP | |

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

## ⚙️ application-local.properties template (GITIGNORED)

```properties
spring.datasource.url=jdbc:postgresql://[POOLER_HOST]:6543/postgres
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

---

## 🗺️ URL Routing (Complete)

| URL | Status | Auth |
|---|---|---|
| `/` | ✅ | Public |
| `/about` | ✅ | Public |
| `/contact` GET + POST | ✅ | Public |
| `/bookings` | ✅ | Public |
| `/bookings/book` | ✅ | Logged in |
| `/bookings/request` | ✅ | Logged in |
| `/bookings/cancel/{id}` | ✅ | Logged in |
| `/login` | ✅ | Public |
| `/register` | ✅ | Public |
| `/verify-email?token=` | ✅ | Public |
| `/dashboard` | ✅ | Logged in |
| `/logout` | ✅ | Logged in |
| `/admin/dashboard` | ✅ | ADMIN |
| `/admin/dashboard/block/{id}` | ✅ | ADMIN |
| `/admin/dashboard/unblock/{id}` | ✅ | ADMIN |
| `/admin/dashboard/cancel-by-trip/{id}` | ✅ | ADMIN |
| `/admin/trips` | ✅ | ADMIN |
| `/admin/trips/new` | ✅ | ADMIN |
| `/admin/trips/pricing` | ✅ | ADMIN |
| `/admin/trips/block/{id}` | ✅ | ADMIN |
| `/admin/trips/unblock/{id}` | ✅ | ADMIN |
| `/admin/trips/delete/{id}` | ✅ | ADMIN |
| `/admin/requests` | ✅ | ADMIN |
| `/admin/requests/approve/{id}` | ✅ | ADMIN |
| `/admin/requests/reject/{id}` | ✅ | ADMIN |
| `/admin/users` | ✅ | ADMIN |
| `/admin/users/block/{id}` | ✅ | ADMIN |
| `/admin/users/unblock/{id}` | ✅ | ADMIN |
| `/admin/users/verify/{id}` | ✅ | ADMIN |
| `/admin/users/delete/{id}` | ✅ | ADMIN |
| `/admin/logs` | ✅ | ADMIN |
| `/admin/revenue` | ⬜ Phase 7 remaining | ADMIN |
| `/admin/notifications` | ⬜ Phase 7 remaining | ADMIN |

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

Get your bcrypt hash from [bcrypt-generator.com](https://bcrypt-generator.com).

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
| Admin dashboard user navbar | `trips-list.html` corrected to admin navbar |

---

## ⚠️ What Comes Next — Phase 7 Remaining (Do In Order, Block By Block)

**Always: one block at a time, confirm before proceeding.**

### Block A — Admin Private Booking
Add ability for admin to manually book a trip on behalf of a specific registered user.
- New form on existing admin pages
- New endpoint `POST /admin/bookings/private`
- Dropdown to select user + select available trip → creates Booking record

### Block B — Block Whole Day  
Admin can block all slots on a specific date at once.
- New endpoint `POST /admin/dashboard/block-day`
- Takes a date + reason → blocks all AVAILABLE trips on that date

### Block C — Revenue Dashboard
- New page `/admin/revenue`
- Shows bookings count + total revenue for a date range
- Needs: `ALTER TABLE payments ADD COLUMN IF NOT EXISTS payment_method VARCHAR DEFAULT 'OZOW';`

### Block D — In-App Notifications
- Create `notifications` table in Supabase
- Trigger notification when user makes a booking
- Admin navbar badge with unread count
- `/admin/notifications` page to view and mark as read

### Then proceed to Phase 9 (Ozow) — only after Phase 11 security hardening

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
