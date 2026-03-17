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

## 🆕 New Features to Build (Extracted from Owner Brief)

### 1. 💰 Price-Per-Km Algorithm
- Build an algorithm that calculates trip price based on:
  - Distance in km (from Google Maps Distance Matrix API)
  - Uncle Ajmal's rate per km (set by admin in dashboard)
  - Any markup or minimum fare he wants to apply
- Admin sets the rate/km via a simple input field in the admin dashboard
- Price is calculated automatically when a trip is created or when a user enters pickup/dropoff
- Price shown to user before they confirm booking

### 2. 🗺️ Google Maps Integration
- Use **Google Maps Distance Matrix API** to calculate:
  - Distance between pickup and dropoff points
  - Estimated travel time
- Use estimated time + **15-minute automatic buffer** for slot duration
  - e.g. Google says 35 min → system allocates 50 min for that slot
- "Next available trip" shown to user is calculated using this buffered time
- Google Maps API key will be stored in `application-local.properties` (never committed)

### 3. ⏱️ 15-Minute Buffer System
- Every trip slot automatically gets Google Maps ETA + 15 minutes added
- This prevents double-bookings due to traffic, stops, or delays
- Buffer is automatic — admin does not need to set it manually
- Admin can override the buffer on a per-trip basis if needed (future feature)

### 4. 🚫 Admin Slot Blocking (Owner-Only Interface)
- Admin has a dedicated interface (separate from customer view) to:
  - **Block** any time slot as "Busy" (breaks, personal, religious reasons)
  - **Unblock** any slot back to "Available"
  - Block entire days (e.g. Fridays for Jumu'ah prayers, sick days, public holidays)
  - This is a simple button toggle — no complex form needed
- Blocked slots appear as grey/unavailable on the customer calendar
- Customers cannot see the reason for a block — just that it's unavailable

### 5. 📸 Branding Assets (Pending from Owner)
- Uncle Ajmal still needs to provide:
  - [ ] Company logo
  - [ ] A few photos of himself (for About page / trust-building)
  - [ ] A few photos of clients / trips (for homepage hero or gallery)
  - [ ] Exact rate per km he wants to charge
- These will be added once received — placeholders used in the meantime

### 6. 💳 Ozow Payment Account Linking
- Ozow integration is already planned (Phase 8)
- Uncle Ajmal's actual Ozow merchant account will only be linked once:
  - Security hardening (Phase 10) is complete
  - App has been tested end-to-end
  - Team confirms the app is secure

---

## ✅ Phase Completion Status

### Phase 1 — Setup: ✅ COMPLETE
- Java 25, Maven 3.9.14, VS Code, GitHub Desktop all installed
- Supabase project `aj-transportation` created
- GitHub repo created (private), both teammates added

### Phase 2 — Spring Boot Project: ✅ COMPLETE
- Project generated (Spring Boot 4.0.3, Java 21, Maven)
- All 8 dependencies added
- Full folder structure created
- Supabase database connected (`HikariPool-1 - Start completed`)
- README.md and PROJECT_CONTEXT.md created

### Phase 3 — Frontend Pages: ✅ COMPLETE

**Files created:**

| File | Location |
|---|---|
| `index.html` | `src/main/resources/templates/` |
| `about.html` | `src/main/resources/templates/` |
| `contact.html` | `src/main/resources/templates/` |
| `bookings.html` | `src/main/resources/templates/user/` |
| `base.html` | `src/main/resources/templates/layout/` |
| `style.css` | `src/main/resources/static/css/` |
| `bookings.css` | `src/main/resources/static/css/` |
| `main.js` | `src/main/resources/static/js/` |
| `calendar.js` | `src/main/resources/static/js/` |
| `SecurityConfig.java` | `src/main/java/com/ajtransportation/app/config/` |
| `PageController.java` | `src/main/java/com/ajtransportation/app/controller/` |

**Pages built:**
- ✅ Homepage (`/`) — hero, features, how-it-works, CTA, footer
- ✅ Bookings (`/bookings`) — weekly slot calendar with navigation
- ✅ About (`/about`) — company info, values, social media links
- ✅ Contact (`/contact`) — contact form, details, social links

**Calendar features (frontend only — dummy data):**
- Weekly grid Mon–Sun, hourly slots 06:00–19:00
- Green = available, Red = booked, Yellow = pending
- Week navigation (prev/next + today button)
- Month/Year jump dropdown
- Booking modal popup with trip details and price
- Upcoming trips list below calendar

**Security/routing:**
- `SecurityConfig.java` — `permitAll()` + CSRF disabled for development
- `PageController.java` — maps all public URLs to templates
- App runs at `http://localhost:8080` without login

### Phase 4 — Database Tables + Java Models: 🔄 UP NEXT
### Phase 5 — User Registration & Login: ⬜ TODO
### Phase 6 — Booking Calendar Backend: ⬜ TODO
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
│   │   └── SecurityConfig.java          ✅ permitAll for dev
│   ├── controller/
│   │   └── PageController.java          ✅ maps / /about /contact /bookings
│   ├── model/                           ⬜ Empty — Phase 4
│   ├── repository/                      ⬜ Empty — Phase 4
│   ├── service/                         ⬜ Empty — Phase 5
│   └── AppApplication.java              ✅ Working
│
├── src/main/resources/
│   ├── templates/
│   │   ├── index.html                   ✅ Homepage
│   │   ├── about.html                   ✅ About page
│   │   ├── contact.html                 ✅ Contact page
│   │   ├── auth/                        ⬜ Phase 5 (login.html, register.html)
│   │   ├── user/
│   │   │   └── bookings.html            ✅ Booking calendar
│   │   ├── admin/                       ⬜ Phase 7
│   │   └── layout/
│   │       └── base.html                ✅ Shared layout
│   ├── static/
│   │   ├── css/
│   │   │   ├── style.css                ✅ Main styles
│   │   │   └── bookings.css             ✅ Calendar styles
│   │   ├── js/
│   │   │   ├── main.js                  ✅ Navbar + animations
│   │   │   └── calendar.js              ✅ Weekly calendar logic
│   │   └── images/                      ⬜ Awaiting logo + photos from Uncle Ajmal
│   ├── application.properties           ✅ Committed to GitHub
│   └── application-local.properties     ✅ Local only — GITIGNORED
│
├── pom.xml                              ✅ All dependencies
├── .gitignore                           ✅ Protects secrets
└── README.md                            ✅ Professional readme
```

---

## ⚙️ Current application.properties

```properties
spring.application.name=aj-transportation
spring.config.import=optional:classpath:application-local.properties
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
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

## 🗄️ Planned Database Tables (NOT YET BUILT — Phase 4)

### `users`
| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
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
| id | INT | Primary key (single row) |
| rate_per_km | DECIMAL | Set by admin e.g. R8.50/km |
| minimum_fare | DECIMAL | Minimum charge regardless of distance |
| updated_at | TIMESTAMP | Last updated |

---

## 🗺️ URL Routing (Current + Planned)

| URL | Status | Auth |
|---|---|---|
| `/` | ✅ Built | ❌ Public |
| `/about` | ✅ Built | ❌ Public |
| `/contact` | ✅ Built | ❌ Public |
| `/bookings` | ✅ Built (frontend only) | ❌ Public |
| `/login` | ⬜ Phase 5 | ❌ Public |
| `/register` | ⬜ Phase 5 | ❌ Public |
| `/dashboard` | ⬜ Phase 5 | ✅ Logged in |
| `/admin/dashboard` | ⬜ Phase 7 | ✅ ADMIN only |
| `/admin/trips` | ⬜ Phase 7 | ✅ ADMIN only |
| `/admin/slots/block` | ⬜ Phase 7 | ✅ ADMIN only |
| `/admin/pricing` | ⬜ Phase 8 | ✅ ADMIN only |
| `/api/price-estimate` | ⬜ Phase 8 | ❌ Public API |

---

## 🔐 Current SecurityConfig.java (Dev Mode)

```java
package com.ajtransportation.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            )
            .csrf(csrf -> csrf.disable())
            .formLogin(form -> form.disable())
            .logout(logout -> logout
                .logoutSuccessUrl("/")
                .permitAll()
            );
        return http.build();
    }
}
```

> ⚠️ Intentionally open for development. Lock down in Phase 11.

---

## 🔐 Current PageController.java

```java
package com.ajtransportation.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String home() { return "index"; }

    @GetMapping("/about")
    public String about() { return "about"; }

    @GetMapping("/contact")
    public String contact() { return "contact"; }

    @GetMapping("/bookings")
    public String bookings() { return "user/bookings"; }
}
```

---

## 📋 Assets Still Needed From Uncle Ajmal

- [ ] Company logo (PNG or SVG preferred)
- [ ] Photos of Uncle Ajmal (for About page)
- [ ] Photos of clients / trips (for homepage)
- [ ] Exact rate per km he charges (e.g. R8.50/km)
- [ ] Minimum fare amount (if any)
- [ ] Confirmation of which days/times to block by default (e.g. every Friday 12:00–14:00)
- [ ] Any other features he wants added

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

## 📌 What To Do Next — Phase 4

**Goal:** Create database tables in Supabase and matching Java model classes.

**Step 1 — Supabase SQL Editor:**
Create tables: `users`, `trips`, `bookings`, `payments`, `pricing_config`

**Step 2 — Java model classes in `model/` folder:**
- `User.java`
- `Trip.java` (includes distance_km, google_eta_minutes, buffered_duration_minutes)
- `Booking.java`
- `Payment.java`
- `PricingConfig.java`

**Step 3 — Repository interfaces in `repository/` folder:**
- `UserRepository.java`
- `TripRepository.java`
- `BookingRepository.java`
- `PaymentRepository.java`
- `PricingConfigRepository.java`

**Step 4 — Restart app and confirm tables appear in Supabase Table Editor**

**Step 5 — Move to Phase 5: Login, register pages and AuthController**
