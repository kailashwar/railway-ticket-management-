# RailBook – Railway Ticket Management System

> **"Book Your Journey. Travel Securely."**  
> *A Modern, Responsive Railway Ticket Reservation & Verification Platform*  
> **College Project / Production Demo System**

---

## 📌 Project Overview
**RailBook** is a complete, full-stack, responsive railway reservation web platform built for passenger ticket bookings, administrative capacity and decision management, and onboard TTR (Travelling Ticket Examiner) ticket validation using cryptographic QR codes and privacy-preserving identity checks.

---

## 👥 Three Specialized Role Portals

| Portal | Access URL | Default Credentials | Key Features |
| :--- | :--- | :--- | :--- |
| **Passenger Portal** | [http://localhost:3000](http://localhost:3000) | `demo@railbook.com`<br>`Pass@123` | Train Search (General/Tatkal), Dynamic Passenger Booking, **Age > 60 Lower Berth Auto-Allocation**, Demo Aadhaar Upload, PNR Generation, Digital Ticket & QR Slip. |
| **Admin Portal** | [http://localhost:3000/admin-login.html](http://localhost:3000/admin-login.html) | `kailashwar24@gmail.com`<br>`Kailash@24` | 6 Metric Cards, Passenger Table with Masked Aadhaar (`XXXX XXXX 1234`), **3 Decision Buttons** (`CONFIRM TICKET`, `WAITING LIST`, `NOT SELECTED`), Train Inventory Management. |
| **TTR Terminal** | [http://localhost:3000/ttr-login.html](http://localhost:3000/ttr-login.html) | `ttr_officer` (or `TTR-2026`)<br>`TTR@123` | Mobile-Optimized **QR Camera Scanner**, Manual Token Input, **Ticket Verification Engine**, Controlled Identity Document Inspection with Audit Logging. |

---

## 🏗️ Architecture & Technology Stack

### 1. Presentation Layer (Frontend)
- **HTML5 & CSS3**: Modern design system using Plus Jakarta Sans typography, sleek card glassmorphism, micro-animations, and IRCTC-inspired digital ticket layouts.
- **Bootstrap 5**: Mobile-first responsive grids and components.
- **Font Awesome 6**: Railway, security, and verification iconography.
- **JavaScript (ES6+)**: Dynamic passenger forms, age monitoring, real-time fare calculator, WebRTC camera scanner, and REST API handlers.

### 2. Business Layer (Java Jakarta Servlets & Node REST Runner)
- **MVC Architecture**: Strict separation between Presentation, Business Controllers/Servlets, and Data Access Layers.
- **Java Jakarta Servlets Codebase** (`src/main/java/com/railbook/`):
  - `model/`: `User.java`, `Train.java`, `Station.java`, `Booking.java`, `Passenger.java`, `IdentityDocument.java`, `QRVerification.java`, `TTRUser.java`, `AuditLog.java`
  - `dao/`: `DBConnection.java`, `UserDAO.java`, `TrainDAO.java`, `BookingDAO.java`, `TTRDAO.java`
  - `service/`: `BerthAllocationService.java` (Senior citizen age > 60 logic), `QRService.java` (ZXing QR token generator)
  - `controller/`: `AuthServlet.java`, `TrainServlet.java`, `BookingServlet.java`, `AdminServlet.java`, `TTRServlet.java`, `PNRServlet.java`, `IdentityDocumentServlet.java`
  - `filter/`: `AuthFilter.java`, `SecurityHeadersFilter.java`
  - `util/`: `PasswordUtil.java` (BCrypt), `TokenUtil.java`, `ValidationUtil.java`
  - `pom.xml`: Jakarta Servlet 6.0, MySQL Connector/J, jBCrypt, ZXing, Gson.
- **Active Application Server** (`server.js`):
  - High-performance Express server connected to MySQL 8.0, executing the exact same REST API contracts for browser testing.

### 3. Data Layer (MySQL Database)
- **Database Name**: `railbook_db`
- **Tables**: `users`, `trains`, `stations`, `bookings`, `passengers`, `identity_documents`, `qr_verifications`, `ttr_users`, `verification_audit_log`
- **Location**: `database/schema.sql`

---

## 🔒 Security & Privacy Implementation

1. **Senior Citizen Lower Berth Logic (Prompt Section 11)**:
   - When a passenger's age is entered as `> 60`, the system automatically assigns **Lower Berth / Down Seat** and displays:
     > *"Passenger is above 60 years. Lower berth has been automatically preferred."*
   - Applied individually for each passenger in a multi-passenger booking.

2. **Aadhaar / Identity Document Privacy (Prompt Section 12 & 13)**:
   - Raw Aadhaar numbers are never displayed openly. Stored and presented strictly in masked form: `XXXX XXXX 1234`.
   - **Zero PII in QR Code**: The QR code contains **only** a cryptographic random token (`RB-SECURE-<pnr>-<hex>`). It contains NO Aadhaar number, image, or passwords.
   - Uploaded test documents are stored securely and can only be inspected by authorized TTR/Admin officers through a controlled modal that appends an entry to the `verification_audit_log`.

3. **Status Badges & Decision Flow (Prompt Section 26 & 36)**:
   - `CONFIRMED` → Green (`badge-confirmed`)
   - `WAITING LIST` → Orange (`badge-waiting`)
   - `NOT SELECTED` → Red (`badge-not-selected`)
   - `CANCELLED` → Gray (`badge-cancelled`)
   - `VERIFIED` → Green (`badge-verified`)
   - `INVALID` → Red (`badge-invalid`)

---

## 🚀 How to Run the Project

### Prerequisites
- Node.js installed (`v18+`)
- MySQL Server 8.0 running on `localhost:3306` with user `root` (configured database: `railbook_db`)

### Starting the Server
```bash
cd "railbook"
npm start
```
The application will launch on:
👉 **`http://localhost:3000`**
