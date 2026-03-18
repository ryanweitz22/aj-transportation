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
15. Admin account created directly in Supabase using bcrypt hash from bcrypt-generator.com

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

### 7. 👤 Admin User Management
- Admin can view all registered users in a table
- Admin can block/unblock users (blocked users cannot log in)
- Admin can remove/delete users entirely from the system
- Admin can edit any user's username, email, password, and role
- Blocked users see a clear message on login explaining their account is suspended

### 8. 📅 Admin Schedule View (4am–12pm)
- Admin schedule always shows 4:00am–12:00pm time window
- Admin can select any date from a date picker to view that day's schedule
- Admin can alter or cancel any existing booking from this view
- All slots, bookings, and blocks visible at a glance

### 9. 💵 Revenue Dashboard (Real-Time)
- Admin dashboard shows total revenue for any selected day
- Revenue split shown: EFT (Ozow) vs cash payments
- Revenue updates in real time — every new booking/payment reflects immediately
- `payments` table will store payment_method field: `OZOW` or `CASH`

### 10. 📲 SMS Notifications
- User receives SMS on booking confirmation
- Admin receives SMS when a new booking is made
- SMS provider to be decided (suggested: Twilio or BulkSMS for South Africa)
- SMS sending logic added alongside email in Phase 10

### 11. 🔔 Admin In-App Notifications
- When a user makes a booking, admin sees a notification inside the app (badge/alert)
- Notification shows user name, trip date/time, and route
- Admin can mark notifications as read
- Notifications stored in a `notifications` table in Supabase

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
| `base.html` | `src/main/resources/templates/layout/` |
| `style.css` | `src/main/resources/static/css/` |
| `bookings.css` | `src/main/resources/static/css/` |
| `main.js` | `src/main/resources/static/js/` |
| `calendar.js` | `src/main/resources/static/js/` |
| `SecurityConfig.java` | `src/main/java/com/ajtransportation/app/config/` |
| `PageController.java` | `src/main/java/com/ajtransportation/app/controller/` |

**Pages built:**
- Homepage (`/`) — hero, features, how-it-works, CTA, footer
- Bookings (`/bookings`) — weekly slot calendar with dummy JS data
- About (`/about`) — company info, values, social media links
- Contact (`/contact`) — contact form, details, social links

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
| `UserService.java` | `src/main/java/com/ajtransportation/app/service/` |
| `CustomUserDetailsService.java` | `src/main/java/com/ajtransportation/app/service/` |
| `RegisterRequest.java` | `src/main/java/com/ajtransportation/app/model/` |
| `SecurityConfig.java` | `src/main/java/com/ajtransportation/app/config/` (updated) |
| `login.html` | `src/main/resources/templates/auth/` |
| `register.html` | `src/main/resources/templates/auth/` |
| `dashboard.html` | `src/main/resources/templates/user/` |

**What works after Phase 5:**
- `/register` — form with email, username, password, confirm password
- `/login` — Spring Security form login with email + password
- `/dashboard` — logged-in users see their account details (bookings empty for now)
- `/logout` — clears session, redirects to homepage
- Passwords stored as BCrypt hashes (never plain text)
- Duplicate email/username validation on registration
- Role-based access: USER vs ADMIN routes protected
- Navbar shows correct buttons based on login state (sec:authorize)
- Admin user created directly in Supabase SQL Editor using bcrypt-generator.com

**Important — dev endpoint cleaned up:**
- `/dev/hashgen` endpoint was added temporarily during debugging and has been **removed**
- `SecurityConfig.java` no longer references `/dev/hashgen`
- `AuthController.java` no longer contains the `generateHash` method

### Phase 6 — Booking Calendar Backend: 🔄 NEXT
### Phase 7 — Admin Dashboard + Slot Blocking + User Management + Revenue: ⬜ TODO
### Phase 8 — Google Maps + Price-Per-Km Algorithm: ⬜ TODO
### Phase 9 — Ozow Payment Integration: ⬜ TODO
### Phase 10 — Email + SMS Notifications: ⬜ TODO
### Phase 11 — Security Hardening + Ozow Account Link: ⬜ TODO
### Phase 12 — Mobile Responsiveness & Testing: ⬜ TODO
### Phase 13 — Deployment: ⬜ TODO

---

## 📌 Phase 6 Plan — Booking Calendar Backend

**Goal:** Replace dummy JS trip data with real trips from the database, and allow users to create real bookings.

**Agreed approach — partial Phase 6 + Phase 7 combined:**
Because the calendar needs real trips to display, we build a simple admin trip creation form FIRST, then connect the calendar to show those trips, then wire up user booking logic.

### Step 1 — TripService.java (new file in `service/`)
- `getTripsForWeek(LocalDate weekStart)` — fetch trips from DB for Mon–Sun range
- `getTripById(UUID id)` — fetch single trip
- `createTrip(Trip trip)` — save new trip (admin only)
- `updateTripStatus(UUID id, String status)` — mark AVAILABLE / BOOKED / BLOCKED

### Step 2 — BookingService.java (new file in `service/`)
- `createBooking(User user, UUID tripId)` — create booking + mark trip BOOKED
- `getUserBookings(User user)` — get all bookings for a user

### Step 3 — AdminTripController.java (new file in `controller/`)
- GET `/admin/trips/new` — show form to create a trip slot
- POST `/admin/trips/new` — save trip to DB, redirect back
- GET `/admin/trips` — list all trips

### Step 4 — BookingsController.java (new file in `controller/`)
- GET `/bookings` — pass real trip data as JSON to Thymeleaf + calendar.js
- POST `/bookings/book` — receive tripId, create booking, redirect to payment placeholder

### Step 5 — Update `bookings.html` + `calendar.js`
- Remove SAMPLE_TRIPS dummy data from calendar.js
- Pass real trips from Thymeleaf model as a JSON variable to calendar.js
- Calendar renders real trips from DB

### Step 6 — Update `dashboard.html`
- Show real booking count stats
- Show user's actual bookings in the table

### Step 7 — Admin trip creation page (`admin/trips-new.html`)
- Simple form: date, start time, label, fee fields
- No Google Maps yet (Phase 8) — manual entry for now

---

## 📌 Phase 7 Plan — Admin Dashboard, User Management & Revenue

**Goal:** Full admin control over the system — schedule, users, bookings, and revenue.

### Step 1 — Admin Schedule View
- Admin can pick any date via date picker
- Schedule renders all slots for that day from **4:00am to 12:00pm**
- Each slot shows: time, user name (if booked), route, status, fee
- Admin can cancel or alter any existing booking from this view
- Slots colour-coded: AVAILABLE (green), BOOKED (blue), BLOCKED (red)

### Step 2 — Admin Slot + Booking Management
- Block/unblock individual time slots (existing feature, now wired fully)
- Block entire days (sick day, Friday prayers, public holidays)
- Cancel a booking on behalf of a user
- Alter booking details (change date/time, reassign trip)
- All changes reflected immediately on the user-facing calendar

### Step 3 — Admin User Management (`/admin/users`)
- Table of all registered users: name, email, role, status, joined date
- Block a user — sets `is_blocked = true` on `users` table; blocked users cannot log in
- Unblock a user
- Delete a user (with confirmation prompt)
- Edit a user's username, email, password (re-hashed), or role
- Add `is_blocked` (BOOLEAN, default false) column to `users` table in Supabase

### Step 4 — Revenue Dashboard (Real-Time)
- Admin dashboard shows revenue panel for selected date
- Total revenue = sum of all PAID bookings for that day
- Split by payment method: **EFT (Ozow)** and **Cash**
- Add `payment_method` column to `payments` table: `OZOW` or `CASH`
- Revenue figures reload automatically (polling or on page refresh)
- Running monthly total also shown for context

### Step 5 — In-App Admin Notifications
- New `notifications` table in Supabase: id, message, is_read, created_at
- When a user books a trip, a notification row is inserted
- Admin navbar shows unread notification count (badge)
- `/admin/notifications` page lists all notifications, newest first
- Admin can mark all as read

### New Supabase columns needed for Phase 7:
- `users.is_blocked` — BOOLEAN, default false
- `payments.payment_method` — VARCHAR, values: `OZOW` or `CASH`
- New table: `notifications` — id (UUID), message (VARCHAR), is_read (BOOLEAN default false), created_at (TIMESTAMP)

---

## 📁 Full Project Folder Structure (Current State)

```
aj-transportation/
│
├── src/main/java/com/ajtransportation/app/
│   ├── config/
│   │   └── SecurityConfig.java          ✅ Phase 5 — login/register/admin routes
│   ├── controller/
│   │   ├── PageController.java          ✅ Maps / /about /contact /bookings
│   │   └── AuthController.java          ✅ /register /login /dashboard (cleaned up)
│   ├── model/
│   │   ├── User.java                    ✅ Phase 4
│   │   ├── Trip.java                    ✅ Phase 4
│   │   ├── Booking.java                 ✅ Phase 4
│   │   ├── Payment.java                 ✅ Phase 4
│   │   ├── PricingConfig.java           ✅ Phase 4
│   │   └── RegisterRequest.java         ✅ Phase 5
│   ├── repository/
│   │   ├── UserRepository.java          ✅ Phase 4
│   │   ├── TripRepository.java          ✅ Phase 4
│   │   ├── BookingRepository.java       ✅ Phase 4
│   │   ├── PaymentRepository.java       ✅ Phase 4
│   │   └── PricingConfigRepository.java ✅ Phase 4
│   ├── service/
│   │   ├── UserService.java             ✅ Phase 5
│   │   └── CustomUserDetailsService.java ✅ Phase 5
│   └── AppApplication.java              ✅ Working
│
├── src/main/resources/
│   ├── templates/
│   │   ├── index.html                   ✅ Homepage
│   │   ├── about.html                   ✅ About page
│   │   ├── contact.html                 ✅ Contact page
│   │   ├── auth/
│   │   │   ├── login.html               ✅ Phase 5
│   │   │   └── register.html            ✅ Phase 5
│   │   ├── user/
│   │   │   ├── bookings.html            ✅ Calendar (dummy data — Phase 6 will connect DB)
│   │   │   └── dashboard.html           ✅ Phase 5 (static zeros — Phase 6 will connect DB)
│   │   ├── admin/                       ⬜ Phase 6/7 — trip creation form goes here
│   │   └── layout/
│   │       └── base.html                ✅ Shared layout
│   ├── static/
│   │   ├── css/
│   │   │   ├── style.css                ✅ Main styles (includes .form-input-error)
│   │   │   └── bookings.css             ✅ Calendar styles
│   │   ├── js/
│   │   │   ├── main.js                  ✅ Navbar + animations
│   │   │   └── calendar.js              ✅ Weekly calendar (dummy data — Phase 6 replaces)
│   │   └── images/                      ⬜ Awaiting assets from Uncle Ajmal
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
| is_blocked | BOOLEAN | Default false — blocked users cannot log in (add in Phase 7) |
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
| payment_method | VARCHAR | `OZOW` or `CASH` — add in Phase 7 for revenue split |
| status | VARCHAR | PENDING / SUCCESS / FAILED |
| created_at | TIMESTAMP | Auto-set |

### `notifications` *(add in Phase 7)*
| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| message | VARCHAR | e.g. "John Doe booked Trip on 2026-04-01 at 08:00" |
| is_read | BOOLEAN | Default false |
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
| `/contact` | ✅ Built | ❌ Public |
| `/bookings` | ✅ Built (dummy data) | ❌ Public |
| `/login` | ✅ Phase 5 | ❌ Public |
| `/register` | ✅ Phase 5 | ❌ Public |
| `/dashboard` | ✅ Phase 5 | ✅ Logged in |
| `/logout` | ✅ Phase 5 | ✅ Logged in |
| `/bookings/book` | ⬜ Phase 6 | ✅ Logged in |
| `/admin/trips` | ⬜ Phase 6 | ✅ ADMIN only |
| `/admin/trips/new` | ⬜ Phase 6 | ✅ ADMIN only |
| `/admin/dashboard` | ⬜ Phase 7 | ✅ ADMIN only |
| `/admin/slots/block` | ⬜ Phase 7 | ✅ ADMIN only |
| `/admin/bookings/edit/{id}` | ⬜ Phase 7 | ✅ ADMIN only |
| `/admin/bookings/cancel/{id}` | ⬜ Phase 7 | ✅ ADMIN only |
| `/admin/users` | ⬜ Phase 7 | ✅ ADMIN only |
| `/admin/users/edit/{id}` | ⬜ Phase 7 | ✅ ADMIN only |
| `/admin/users/block/{id}` | ⬜ Phase 7 | ✅ ADMIN only |
| `/admin/users/delete/{id}` | ⬜ Phase 7 | ✅ ADMIN only |
| `/admin/revenue` | ⬜ Phase 7 | ✅ ADMIN only |
| `/admin/notifications` | ⬜ Phase 7 | ✅ ADMIN only |
| `/admin/pricing` | ⬜ Phase 8 | ✅ ADMIN only |
| `/api/price-estimate` | ⬜ Phase 8 | ❌ Public API |

---

## 🔐 SecurityConfig.java (Current — Phase 5)

```java
package com.ajtransportation.app.config;

import com.ajtransportation.app.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/images/**", "/fonts/**").permitAll()
                .requestMatchers("/", "/about", "/contact", "/bookings").permitAll()
                .requestMatchers("/login", "/register").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .userDetailsService(userDetailsService);

        return http.build();
    }
}
```

---

## 🔐 AuthController.java (Current — cleaned up)

```java
package com.ajtransportation.app.controller;

import com.ajtransportation.app.model.RegisterRequest;
import com.ajtransportation.app.model.User;
import com.ajtransportation.app.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Incorrect email or password. Please try again.");
        }
        if (logout != null) {
            model.addAttribute("successMessage", "You have been logged out successfully.");
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerSubmit(
            @Valid @ModelAttribute("registerRequest") RegisterRequest request,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        String errorMessage = userService.register(request);
        if (errorMessage != null) {
            model.addAttribute("errorMessage", errorMessage);
            return "auth/register";
        }
        return "redirect:/login?registered=true";
    }

    @GetMapping("/dashboard")
    public String dashboard(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        User user = userService.findByEmail(userDetails.getUsername());
        model.addAttribute("user", user);
        return "user/dashboard";
    }
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

## 🔑 Admin Login Details

- **URL:** `http://localhost:8080/login`
- **Email:** `admin@ajtransportation.co.za`
- **Password:** whatever was entered into bcrypt-generator.com when creating the hash
- Admin account was inserted directly into Supabase using SQL — role is `ADMIN`
- To create additional admin accounts, repeat the same SQL INSERT process with a new bcrypt hash