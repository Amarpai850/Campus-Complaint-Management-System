# 🏫 Campus Complaint & Maintenance Management System

An enterprise-ready, full-stack campus operations system designed for engineering colleges and universities. It streamlines reporting, assignment, progress tracking, audit history, and resolution of campus infrastructure and maintenance complaints.

---

## 📑 Table of Contents
1. [Project Overview](#1-project-overview)
2. [Key Features](#2-key-features)
3. [Technology Stack](#3-technology-stack)
4. [System Architecture](#4-system-architecture)
5. [Database Schema & Entity Relationships](#5-database-schema--entity-relationships)
6. [User Roles & Permissions](#6-user-roles--permissions)
7. [End-to-End Workflow](#7-end-to-end-workflow)
8. [Prerequisites & System Requirements](#8-prerequisites--system-requirements)
9. [Database Setup & Configuration](#9-database-setup--configuration)
10. [How to Run the Application](#10-how-to-run-the-application)
11. [Default Test Credentials](#11-default-test-credentials)
12. [User Interface Previews](#12-user-interface-previews)
13. [REST API Documentation](#13-rest-api-documentation)
14. [Postman Setup & Usage](#14-postman-setup--usage)
15. [Automated Testing](#15-automated-testing)
16. [Cloud Deployment Architecture & Guide](#16-cloud-deployment-architecture--guide)
17. [Troubleshooting Guide](#17-troubleshooting-guide)
18. [Team Contribution & Module Breakdown](#18-team-contribution--module-breakdown)

---

## 1. Project Overview

In large academic institutions, students and faculty often encounter facilities issues—such as leaking plumbing, electrical faults, damaged classroom furniture, network downtime, or broken laboratory equipment. Without a centralized tracking mechanism, complaints are misplaced, staff cannot be efficiently dispatched, and students have no visibility into issue resolution.

This **Campus Complaint Management System** solves this problem by providing:
- A responsive web portal for students to submit complaints with precise locations, categories, and priorities.
- An Administrative Control Center for assigning complaints to specialized maintenance staff, monitoring institutional resolution rates, and analyzing complaint statistics.
- A Maintenance Staff Portal to update live work progress, attach notes, and mark tasks as resolved.
- A permanent audit timeline for every status change to guarantee institutional accountability.
- A secured, documented REST API with a companion Postman collection.

---

## 2. Key Features

- **Robust Role-Based Authentication:**
  - Roles: `STUDENT`, `ADMIN`, `MAINTENANCE_STAFF`.
  - Secure BCrypt password hashing (never stored in plaintext).
  - Dual security architecture: HTTP Basic Authentication for REST clients (Postman/cURL) and session/form login for browser users.
- **Controlled Complaint Lifecycle:**
  - Status progression: `PENDING` ➔ `ASSIGNED` ➔ `IN_PROGRESS` ➔ `RESOLVED` ➔ `CLOSED`.
  - Strict validation preventing invalid state transitions (e.g., cannot resolve an unassigned complaint or reopen a closed complaint arbitrarily).
- **Automated Historical Auditing:**
  - Every assignment and status transition automatically logs the timestamp, previous state, next state, action author, and remarks in `complaint_history`.
- **Administrative Command Center:**
  - Live aggregate metric cards (Total, Pending, In Progress, Resolved, Staff Count).
  - Category breakdown analytics.
  - Staff assignment and user management controls.
- **Interactive Web Interface:**
  - Fully responsive, clean CSS3 styling with no heavy third-party UI dependencies.
  - Real-time badges, status indicators, and collapsible audit timelines.

---

## 3. Technology Stack

### Backend
- **Java 17 / 27**
- **Spring Boot 3.4.1**
  - Spring MVC (Web & REST API controllers)
  - Spring Data JPA (Data persistence & repository abstraction)
  - Spring Security 6 (Authentication, Authorization, BCrypt)
  - Jakarta Bean Validation (Hibernate Validator)
  - Spring Boot Starter Thymeleaf (Server-side UI rendering)
  - Thymeleaf Spring Security 6 dialect

### Database
- **MySQL 8.0+** (Primary database)
- **H2 In-Memory Database** (Isolated unit & integration testing)
- **Hibernate / JPA** (Object-Relational Mapping with automatic DDL updates)

### Frontend
- **HTML5 & CSS3** (Custom responsive design system)
- **Thymeleaf Template Engine**
- **Vanilla JavaScript**

### Build & Tooling
- **Apache Maven 3.9+**
- **Postman** (Complete automated API collection provided)
- **Git** version control

---

## 4. System Architecture

The application strictly adheres to a layered Spring Boot architecture:

```
src/main/java/com/campus/complaint/
│
├── config/              # Security configuration, UserDetailsService, Data initialization
│   ├── SecurityConfig.java
│   ├── CustomUserDetailsService.java
│   └── DataInitializer.java
│
├── controller/          # HTTP request handlers (REST API & Web Views)
│   ├── AuthController.java        # Public registration API
│   ├── ComplaintController.java   # Complaint submission & management API
│   ├── UserController.java        # User directory & admin API
│   ├── CategoryController.java    # Category configuration API
│   ├── MaintenanceController.java # Staff assignment API
│   ├── DashboardController.java   # Analytics & reporting API
│   └── WebController.java         # Thymeleaf UI navigation & form actions
│
├── service/             # Pure transactional business logic
│   ├── UserService.java
│   ├── ComplaintService.java
│   └── CategoryService.java
│
├── repository/          # Spring Data JPA repositories
│   ├── UserRepository.java
│   ├── ComplaintRepository.java
│   ├── CategoryRepository.java
│   └── ComplaintHistoryRepository.java
│
├── entity/              # Relational JPA database models
│   ├── User.java
│   ├── Role.java (Enum: STUDENT, ADMIN, MAINTENANCE_STAFF)
│   ├── Complaint.java
│   ├── ComplaintStatus.java (Enum: PENDING, ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED)
│   ├── Category.java
│   └── ComplaintHistory.java
│
├── dto/                 # Request & Response Data Transfer Objects
│   ├── RegisterRequest.java
│   ├── LoginRequest.java
│   ├── UserResponse.java
│   ├── ComplaintRequest.java
│   ├── ComplaintResponse.java
│   ├── ComplaintHistoryResponse.java
│   ├── AssignRequest.java
│   ├── StatusUpdateRequest.java
│   ├── DashboardStats.java
│   └── ApiResponse.java
│
└── exception/           # Centralized exception handling
    ├── GlobalExceptionHandler.java
    ├── ResourceNotFoundException.java
    ├── DuplicateResourceException.java
    └── InvalidOperationException.java
```

---

## 5. Database Schema & Entity Relationships

```
+--------------------+         1:N         +-----------------------+
|       users        |<--------------------+      complaints       |
+--------------------+  (user_id)          +-----------------------+
| id (PK)            |                     | id (PK)               |
| username (UNIQUE)  |         1:N         | title                 |
| password (HASH)    |<--------------------+ description           |
| email (UNIQUE)     |  (assigned_staff_id)| location              |
| full_name          |                     | status (ENUM)         |
| role (ENUM)        |                     | priority              |
| department         |                     | user_id (FK)          |
| enabled            |                     | category_id (FK)      |
| created_at         |                     | assigned_staff_id (FK)|
+--------------------+                     | created_at            |
                                           | resolved_at           |
+--------------------+         1:N         +-----------------------+
|     categories     |<--------------------+           |
+--------------------+  (category_id)                  | 1:N
| id (PK)            |                                 v
| name (UNIQUE)      |                     +-----------------------+
| description        |                     |   complaint_history   |
| active             |                     +-----------------------+
+--------------------+                     | id (PK)               |
                                           | complaint_id (FK)     |
                                           | previous_status       |
                                           | new_status            |
                                           | remarks               |
                                           | updated_by            |
                                           | created_at            |
                                           +-----------------------+
```

### Relationship Summary:
- **User ➔ Complaint:** One-to-Many (`user_id`). A student can lodge multiple complaints.
- **Maintenance Staff ➔ Complaint:** One-to-Many (`assigned_staff_id`). A staff member can resolve multiple complaints.
- **Category ➔ Complaint:** One-to-Many (`category_id`). Complaints belong to a specific department/trade.
- **Complaint ➔ ComplaintHistory:** One-to-Many (`complaint_id`, cascade delete/orphan removal). Each status change appends a new audit record.

---

## 6. User Roles & Permissions

| Feature / Endpoint | Public / Anonymous | STUDENT | MAINTENANCE_STAFF | ADMIN |
|---|:---:|:---:|:---:|:---:|
| User Registration (`/register`, `/api/auth/register`) | ✅ | ❌ | ❌ | ❌ |
| Login / Logout | ✅ | ✅ | ✅ | ✅ |
| Submit New Complaint | ❌ | ✅ | ❌ | ✅ |
| View Own Complaints (`/student/dashboard`) | ❌ | ✅ | ❌ | ✅ |
| View Assigned Tasks (`/staff/dashboard`) | ❌ | ❌ | ✅ | ✅ |
| Update Task Work / Progress | ❌ | ❌ | ✅ | ✅ |
| View All Complaints Across Campus | ❌ | ❌ | ❌ | ✅ |
| Assign Complaint to Staff | ❌ | ❌ | ❌ | ✅ |
| User Directory & Role Management | ❌ | ❌ | ❌ | ✅ |
| Category Management | ❌ | ❌ | ❌ | ✅ |
| System Statistics & Metrics | ❌ | ❌ | ❌ | ✅ |

---

## 7. End-to-End Workflow

```
[Student] Register / Login
    │
    ▼
[Student] Fills complaint form (Title, Category, Location, Description, Priority)
    │
    ▼
Database stores Complaint in PENDING state ➔ Logs History ("Complaint created")
    │
    ▼
[Admin] Views Dashboard ➔ Inspects PENDING complaints
    │
    ▼
[Admin] Selects Maintenance Staff member ➔ Submits Assignment
    │
    ▼
Status transitions to ASSIGNED ➔ Logs History ("Assigned to Staff X")
    │
    ▼
[Staff] Logs into Staff Portal ➔ Views assigned tasks
    │
    ▼
[Staff] Starts repair ➔ Updates status to IN_PROGRESS with notes
    │
    ▼
[Staff] Completes repair ➔ Updates status to RESOLVED with resolution report
    │
    ▼
[Student & Admin] Observe updated state and view full timeline in real-time
    │
    ▼
[Admin] Closes verified ticket ➔ Status transitions to CLOSED
```

---

## 8. Prerequisites & System Requirements

- **Java Development Kit (JDK):** Version 17 or higher (tested with JDK 27).
- **Build Tool:** Apache Maven 3.8+ (bundled wrapper `mvnw` or Maven CLI).
- **Database:** MySQL Server 8.0 or higher.
- **Port Availability:** Port `8080` (HTTP) and Port `3306` (MySQL).

---

## 9. Database Setup & Configuration

### A. Create the Database (Optional / Automatic)
The application is pre-configured with `createDatabaseIfNotExist=true`. However, you can manually create the database in MySQL:

```sql
CREATE DATABASE IF NOT EXISTS campus_complaints CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### B. Configure Credentials
Credentials are decoupled from source code using environment variables. Set them in your environment before running:

#### On Windows (PowerShell):
```powershell
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your_mysql_password_here"
```

#### On Linux / macOS:
```bash
export DB_USERNAME="root"
export DB_PASSWORD="your_mysql_password_here"
```

Alternatively, pass them directly when launching the application:
```bash
java -jar target/complaint-management-1.0.0.jar --spring.datasource.username=root --spring.datasource.password=your_password
```

---

## 10. How to Run the Application

### Option 1: Using Maven Command Line
```powershell
# Navigate to project root
cd d:\antigravity\Campus-Complaint-Management-system

# Build and run directly
mvn spring-boot:run
```

### Option 2: Build Executable JAR and Run
```powershell
# Clean and package
mvn clean package -DskipTests

# Run the packaged fat JAR
java -jar target/complaint-management-1.0.0.jar
```

### Option 3: In Antigravity IDE / VS Code / IntelliJ IDEA
1. Open the folder `Campus-Complaint-Management-system`.
2. Locate `src/main/java/com/campus/complaint/CampusComplaintApplication.java`.
3. Right click and select **Run** or **Debug**.

Once running, access the web UI at: **`http://localhost:8080`**

---

## 11. Default Test Credentials

On initial startup, `DataInitializer` automatically seeds default test accounts and categories if the database is fresh:

| Role | Username | Password | Purpose |
|---|---|---|---|
| **Administrator** | `admin` | `admin123` | System administration & assigning complaints |
| **Maintenance Staff** | `staff1` | `staff123` | Performing repairs & updating status |
| **New Student** | *(Self-register)* | *(Your choice)* | Register via `/register` or `/api/auth/register` |

> ⚠️ **SECURITY WARNING:**  
> The default credentials above (`admin123`, `staff123`) are provided strictly for local development, academic demonstration, and evaluation testing. In any real-world or production deployment, these default passwords **MUST** be changed immediately, and administrative access should be restricted behind multi-factor authentication and strong secret policies.

---

## 12. User Interface Previews

| Screen | Description | Preview |
|---|---|---|
| **Student Portal** | Submit new complaints, choose categories, track real-time resolution badges. | `[Student Dashboard Screenshot Placeholder: /docs/screenshots/student-dashboard.png]` |
| **Admin Command Center** | Comprehensive metrics (Pending, In Progress, Resolved), department distribution, staff assignment. | `[Admin Dashboard Screenshot Placeholder: /docs/screenshots/admin-dashboard.png]` |
| **Staff Task Board** | Specialized maintenance workspace to update work progress and append repair notes. | `[Staff Workspace Screenshot Placeholder: /docs/screenshots/staff-dashboard.png]` |
| **Audit Timeline** | Full chronological transparency showing who changed what, when, and with what remarks. | `[Audit Timeline Screenshot Placeholder: /docs/screenshots/audit-timeline.png]` |

---

## 13. REST API Documentation

All REST APIs are under `/api/` and support JSON request/response bodies with HTTP Basic Authentication.

### 🔐 Authentication
- **`POST /api/auth/register`** (Public)
  - Registers a new student account.
  - Body: `{ "username": "...", "password": "...", "email": "...", "fullName": "...", "department": "..." }`
  - Status: `201 CREATED`

### 📋 Complaints
- **`POST /api/complaints`** (Role: Any authenticated user)
  - Create a new complaint.
  - Body: `{ "title": "...", "description": "...", "location": "...", "categoryId": 1, "priority": "HIGH" }`
  - Status: `201 CREATED`
- **`GET /api/complaints/my`** (Role: Any authenticated user)
  - Returns complaints submitted by the logged-in user.
- **`GET /api/complaints/{id}`** (Role: Any authenticated user)
  - Returns detailed complaint record.
- **`GET /api/complaints`** (Role: `ADMIN`)
  - Lists all complaints institution-wide.
- **`PUT /api/complaints/{id}/assign`** (Role: `ADMIN`)
  - Assigns a staff member.
  - Body: `{ "staffId": 2, "remarks": "Urgent attention required" }`
- **`PUT /api/complaints/{id}/status`** (Role: `MAINTENANCE_STAFF`, `ADMIN`)
  - Updates progress status.
  - Body: `{ "status": "IN_PROGRESS", "remarks": "Replacing pipe" }`
- **`GET /api/complaints/{id}/history`** (Role: Any authenticated user)
  - Returns chronological audit log for the complaint.

### 🏷️ Categories
- **`GET /api/categories`** (Public/Authenticated) — Returns all active categories.
- **`POST /api/categories?name=...&description=...`** (Role: `ADMIN`) — Adds a new category.

### 🔧 Maintenance Staff
- **`GET /api/maintenance/my-assignments`** (Role: `MAINTENANCE_STAFF`, `ADMIN`)
  - Returns complaints assigned to current staff member.
- **`PUT /api/maintenance/complaints/{id}/status`** (Role: `MAINTENANCE_STAFF`, `ADMIN`)
  - Updates status and attaches resolution notes.

### 📊 Dashboard & Reporting
- **`GET /api/dashboard/stats`** (Role: `ADMIN`)
  - Returns real-time metrics: counts by status, user breakdown, and category distribution.

---

## 14. Postman Setup & Usage

A fully configured Postman collection and environment are included in the repository:

1. Open Postman.
2. Click **Import** (top left).
3. Select `Campus_Complaint_Management_API.postman_collection.json` (or import from `postman/collections/`).
4. Select `postman/environments/Campus_Management_Local.postman_environment.json`.
5. Select the **Campus Management Local** environment in Postman.
6. The environment comes pre-configured with:
   - `{{baseUrl}}` ➔ `http://localhost:8080`
   - `{{adminUsername}}` / `{{adminPassword}}` ➔ `admin` / `admin123`
   - `{{staffUsername}}` / `{{staffPassword}}` ➔ `staff1` / `staff123`
   - `{{studentUsername}}` / `{{studentPassword}}` ➔ `student1` / `student123`
7. Execute requests in sequence (e.g. Register ➔ Submit Complaint ➔ Assign Staff ➔ Update Status ➔ View History).

---

## 15. Automated Testing

The project includes unit and integration tests using JUnit 5, Mockito, and Spring Boot Test:

To execute all tests:
```powershell
mvn test
```

### Test Scope:
- **`UserServiceTest`**: Validates registration, duplicate detection, BCrypt password encoding, and role modifications.
- **`ComplaintServiceTest`**: Validates end-to-end complaint lifecycle, staff role verification during assignment, strict lifecycle state machines, and statistics calculations.
- **`ComplaintControllerTest`**: Uses `MockMvc` to verify REST security policies, unauthorized request rejection (401/403), and valid payload processing.

---

## 16. Cloud Deployment Architecture & Guide

The system is configured for seamless deployment to modern containerized cloud platforms (Render, Railway, Fly.io, AWS ECS) with a managed cloud MySQL database.

```
┌─────────────────────────────────┐
│     Cloud Web Service           │
│   (Render / Railway / Docker)   │
│  - Spring Boot 3.4.1 (Java 17)  │
│  - Dynamic PORT Binding ($PORT) │
│  - Health Check: /api/health    │
│  - Profile: prod                │
└────────────────┬────────────────┘
                 │ Secure TLS/SSL (JDBC)
                 ▼
┌─────────────────────────────────┐
│    Cloud MySQL Database         │
│  (TiDB Cloud Serverless /       │
│   Railway Managed MySQL)        │
│  - Automatic schema update      │
│  - Auto-seeded categories       │
│  - 24/7 Persistent Storage     │
└─────────────────────────────────┘
```

### 16.1. Environment Variables Reference

Configure these environment variables in your cloud hosting control panel. **Never check real passwords into source control.**

| Variable Name | Required | Description | Example / Recommended Value |
|---|---|---|---|
| `PORT` | Auto | Application port | `8080` (Automatically injected by Render/Railway) |
| `SPRING_PROFILES_ACTIVE` | Yes | Active Spring profile | `prod` |
| `DB_URL` | Yes | Full JDBC Connection URL with TLS/SSL | `jdbc:mysql://<host>:<port>/campus_complaints?useSSL=true&requireSSL=true` |
| `DB_USERNAME` | Yes | Database user | `<cloud_db_username>` |
| `DB_PASSWORD` | Yes | Database password | `<cloud_db_password>` |
| `ADMIN_INITIAL_PASSWORD` | Optional | Secure initial password for admin | `<strong_random_password>` (Defaults to `admin123` with warning) |
| `STAFF_INITIAL_PASSWORD` | Optional | Secure initial password for staff1 | `<strong_random_password>` (Defaults to `staff123`) |

### 16.2. Production vs Local Profile Differences

- **`application.properties` (Base):** Configured with variable substitution `${DB_URL:...}` and dynamic `${PORT:8080}`.
- **`application-prod.properties` (Active in Cloud):**
  - `spring.thymeleaf.cache=true` for lightning-fast template rendering.
  - `server.error.include-stacktrace=never` to prevent sensitive internal stack trace exposure.
  - `spring.jpa.hibernate.ddl-auto=update` for safe, non-destructive schema evolution.
  - Log levels set to `INFO` for application and `WARN` for Hibernate/Security to conserve memory.

### 16.3. Step-by-Step Free Cloud Deployment (Render + TiDB Cloud)

#### Step 1: Create Free Cloud MySQL Database (TiDB Cloud Serverless)
1. Sign up for free at [TiDB Cloud](https://tidbcloud.com) (No credit card required).
2. Click **Create Cluster** ➔ Select **Serverless (Free Forever, 5 GiB)**.
3. Click **Connect** ➔ Choose **JDBC (Java)**.
4. Note down the Host, Port, Database name, Username, and Password.

#### Step 2: Deploy Web Service on Render
1. Sign up for free at [Render](https://render.com) using your GitHub account.
2. Click **New +** ➔ **Web Service**.
3. Select your repository: `Amarpai850/Campus-Complaint-Management-System`.
4. Choose **Docker** runtime (Render automatically detects the multi-stage `Dockerfile`).
5. Under **Environment Variables**, add:
   - `SPRING_PROFILES_ACTIVE` = `prod`
   - `DB_URL` = `jdbc:mysql://<tidb-host>:4000/campus_complaints?useSSL=true&requireSSL=true`
   - `DB_USERNAME` = `<tidb-user>`
   - `DB_PASSWORD` = `<tidb-password>`
   - `ADMIN_INITIAL_PASSWORD` = `<your-admin-password>`
6. Click **Create Web Service**.
7. Render will build the container, start the Spring Boot app, verify `/api/health`, and issue a live HTTPS URL (e.g. `https://campus-complaint-management.onrender.com`).

---

## 17. Troubleshooting Guide

### 1. `Access denied for user 'root'@'localhost'`
- **Cause:** Incorrect MySQL root password.
- **Fix:** Set the environment variable `$env:DB_PASSWORD = "your_actual_password"` in PowerShell before launching.

### 2. `Can't connect to MySQL server on 'localhost:3306'`
- **Cause:** MySQL service is not running.
- **Fix:** Open Windows Services (`services.msc`) and ensure the MySQL service is started, or run `net start MySQL80`.

### 3. `Port 8080 is already in use`
- **Cause:** Another process is running on port 8080.
- **Fix:** Terminate the conflicting process or change the port in `application.properties`: `server.port=8081`.

---

## 18. Team Contribution & Module Breakdown

*Developed by a 5-member Computer Science & Engineering team:*

- **Member 1 (Auth & Security Lead):** User entity, Spring Security configuration, BCrypt hashing, role-based filters, login/registration controllers and views.
- **Member 2 (Complaint Management Lead):** Complaint entity, category subsystem, complaint creation, validation rules, student dashboard and complaint submission forms.
- **Member 3 (Maintenance & Workflow Lead):** Staff dispatching logic, maintenance portal, assignment services, work status update actions, and staff dashboard views.
- **Member 4 (Audit & Data Analytics Lead):** ComplaintHistory entity, state change listeners, audit timeline rendering, admin dashboard statistics and reporting APIs.
- **Member 5 (QA & API Integration Lead):** Postman test suite collection, JUnit 5/Mockito test suites, MockMvc security integration testing, and documentation.
