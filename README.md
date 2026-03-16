# 🚌 AJ Transportation

> A full-stack web application for managing transportation bookings, built with Spring Boot and Supabase.

---

## 📋 Table of Contents

- [About](#about)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
  - [Configuration](#configuration)
  - [Running the App](#running-the-app)
- [Usage](#usage)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [Security](#security)
- [License](#license)

---

## About

AJ Transportation is a responsive web booking platform that allows customers to register, view available trip slots, and make transport bookings. Admins can manage availability, set trip fees, and handle all bookings through a dedicated dashboard.

---

## Features

### Customer / User
- 📅 View a monthly calendar of available trips
- 🕐 See full daily schedules with available and booked slots
- 🔒 Register and log in securely with email and password
- 💳 Pay for bookings via Ozow (South African EFT)
- 📧 Receive booking confirmation emails

### Admin / Owner
- ➕ Add and manage trip slots and availability
- 💰 Set and update trip fees per booking
- 📊 View all bookings and customer details
- ✅ Confirm, update, or cancel bookings
- 🔔 Get notified when a new booking is made

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Backend** | Java 21, Spring Boot 4.0.3 |
| **Frontend** | Thymeleaf, HTML5, CSS3, JavaScript |
| **Database** | Supabase (PostgreSQL 17.6) |
| **Security** | Spring Security |
| **Payments** | Ozow (South African EFT) |
| **Email** | Spring Mail (Gmail SMTP) |
| **Build Tool** | Apache Maven 3.9.14 |
| **Version Control** | Git + GitHub Desktop |

---

## Project Structure

```
aj-transportation/
│
├── src/main/java/com/ajtransportation/app/
│   ├── config/           # Security and web configuration
│   ├── controller/       # Web request handlers
│   ├── model/            # Database entity classes
│   ├── repository/       # Database query interfaces
│   ├── service/          # Business logic
│   └── AppApplication.java
│
├── src/main/resources/
│   ├── templates/
│   │   ├── auth/         # Login and register pages
│   │   ├── user/         # Customer-facing pages
│   │   ├── admin/        # Admin dashboard pages
│   │   └── layout/       # Shared header and footer
│   ├── static/
│   │   ├── css/
│   │   ├── js/
│   │   └── images/
│   ├── application.properties           # Main config (safe to commit)
│   └── application-local.properties     # Secrets (gitignored)
│
└── pom.xml
```

---

## Getting Started

### Prerequisites

Make sure you have the following installed:

- [Java JDK 21+](https://adoptium.net) — verify with `java -version`
- [Apache Maven 3.9+](https://maven.apache.org/download.cgi) — verify with `mvn -version`
- [VS Code](https://code.visualstudio.com) with the Java Extension Pack
- [GitHub Desktop](https://desktop.github.com)
- A [Supabase](https://supabase.com) account (one teammate only)

---

### Installation

**1. Clone the repository**

Using GitHub Desktop:
- File → Clone Repository → select `aj-transportation`

Or via terminal:
```bash
git clone https://github.com/YOUR_USERNAME/aj-transportation.git
cd aj-transportation
```

**2. Open in VS Code**
```
File → Open Folder → select the aj-transportation folder
```

---

### Configuration

Create a file called `application-local.properties` inside `src/main/resources/`:

```properties
# ============================================
# AJ TRANSPORTATION - LOCAL SECRETS
# This file is GITIGNORED — never commit this
# ============================================

# Supabase Database Connection
spring.datasource.url=jdbc:postgresql://YOUR_SUPABASE_HOST:5432/postgres
spring.datasource.username=postgres.YOUR_PROJECT_ID
spring.datasource.password=YOUR_DATABASE_PASSWORD
spring.datasource.driver-class-name=org.postgresql.Driver

# Supabase API Keys
supabase.url=https://YOUR_PROJECT_ID.supabase.co
supabase.anon-key=YOUR_ANON_KEY
supabase.service-role-key=YOUR_SERVICE_ROLE_KEY

# Email
spring.mail.username=YOUR_GMAIL@gmail.com
spring.mail.password=YOUR_GMAIL_APP_PASSWORD
```

> ⚠️ Get your Supabase credentials from: **Supabase → Settings → Database → Connection parameters**
> 
> ⚠️ This file is listed in `.gitignore` and will **never** be pushed to GitHub. Share credentials privately with teammates via a secure channel.

---

### Running the App

In the VS Code terminal (`Ctrl + ``):

```bash
mvn spring-boot:run
```

Then open your browser and go to:
```
http://localhost:8080
```

To stop the app:
```
Ctrl + C
```

---

## Usage

### As a Customer
1. Register with your email and password
2. Browse the booking calendar
3. Select an available time slot
4. Confirm your booking and pay via Ozow

### As an Admin
1. Log in with admin credentials
2. Add trip slots and set availability
3. Set pricing for each trip
4. Manage and confirm incoming bookings

---

## Roadmap

- [x] Project setup and folder structure
- [x] Supabase database connection
- [ ] Database schema (users, trips, bookings, payments)
- [ ] User registration and login
- [ ] Booking calendar UI
- [ ] Admin dashboard
- [ ] Ozow payment integration
- [ ] Email confirmation notifications
- [ ] Mobile responsiveness
- [ ] Security hardening
- [ ] Production deployment

---

## Contributing

This project is developed by a 2-person team.

**Workflow:**
1. Open GitHub Desktop → click **Fetch origin** before starting work
2. Make your changes in VS Code
3. In GitHub Desktop, write a commit message and click **Commit to main**
4. Click **Push origin** to share your changes

> ⚠️ Always pull before you start coding to avoid conflicts.

---

## Security

- All secrets are stored in `application-local.properties` which is gitignored
- Spring Security handles authentication and role-based access
- Passwords are stored as bcrypt hashes — never plain text
- HTTPS will be enforced on production deployment
- Input validation is applied on all forms

**Found a vulnerability?** Please report it privately to the project owners — do not open a public issue.

---

## License

This project is private and proprietary to **AJ Transportation**.  
All rights reserved © 2026 AJ Transportation.
