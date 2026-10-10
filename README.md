# StockGuard — Smart Inventory Management System

> **Tagline:** Smart Inventory | Track Expiry | Grow Your Business  
> Tailored specifically for small grocery shops, dairy stores, convenience stores, and general merchants.

---

## 🌟 Overview

**StockGuard** is an enterprise-grade full-stack smart inventory management web application. It prevents stock spoilage, tracks multi-batch expiration dates, provides low-stock reorder alerts, logs daily point-of-sale transactions, and automates notifications via the **WhatsApp Business Platform Cloud API** (with a built-in Dev Mock Mode for local development).

---

## 🛠️ Technology Stack

- **Frontend:**
  - HTML5 & CSS3 with Vanilla CSS (CSS Grid, Flexbox, Glassmorphism, animations)
  - Color System: Deep Navy (`#0B1530`), Primary Blue (`#1677FF`), Emerald Green (`#28D978`), Warm Amber (`#F59E0B`), Alert Red (`#EF4444`)
  - Vanilla JavaScript (modular ES6 architecture: `api.js`, `auth.js`, `dashboard.js`, `inventory.js`, `expiry.js`, `sales.js`, `reports.js`, `notifications.js`, `app.js`)
  - [Chart.js](https://www.chartjs.org/) for interactive sales growth and category distribution charts
  - [Three.js](https://threejs.org/) for interactive 3D shield & floating grocery stock cubes hero visual with graceful fallback

- **Backend:**
  - Java 21 / 26
  - Spring Boot 3.2.5
  - Spring Web (RESTful APIs with unified JSON error handling)
  - Spring Data JPA & Hibernate
  - Spring Security 6 with stateless JWT authentication & BCrypt password hashing
  - Bean Validation (`jakarta.validation`)
  - Apache Maven for dependency management

- **Database:**
  - MySQL 8.0 (primary relational database)
  - Auto-fallback to local persistent file engine (`stockguard_data.mv.db`) if local MySQL credentials are not yet configured

- **External Integrations:**
  - WhatsApp Business Platform Cloud API (`v18.0`)
  - Development Mock Simulator with instant visual log viewer

---

## 🚀 Quick Start Guide

### Prerequisites
- Java JDK 17+ (JDK 21 or JDK 26 recommended)
- Maven 3.8+ (Portable Maven included at `$env:LOCALAPPDATA\Programs\apache-maven-3.9.6\bin`)
- MySQL 8.0 (optional for live production; embedded persistent engine runs out of the box)

### 1. Configure Environment Variables (Optional)
Copy `.env.example` to `.env` or set environment variables:
```bash
# MySQL Connection
DB_URL=jdbc:mysql://localhost:3306/stockguard_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=root
DB_PASSWORD=your_mysql_password

# Security
JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970

# WhatsApp Cloud API (Optional - Dev Mock Mode enabled by default)
WHATSAPP_MOCK_MODE=true
WHATSAPP_API_TOKEN=your_meta_system_user_token
WHATSAPP_PHONE_NUMBER_ID=your_phone_number_id
```

### 2. Build and Run the Application
In your terminal, run:
```powershell
mvn spring-boot:run
```
Or with portable Maven:
```powershell
& "$env:LOCALAPPDATA\Programs\apache-maven-3.9.6\bin\mvn.cmd" spring-boot:run
```

### 3. Access the Application
Open your browser and navigate to:
- **Landing Page:** [http://localhost:8080/](http://localhost:8080/)
- **Dashboard Application:** [http://localhost:8080/app.html](http://localhost:8080/app.html)

### 4. Demo Login Credentials
The system automatically seeds realistic grocery data on first run:
- **Email:** `demo@stockguard.com`
- **Password:** `demo123`

---

## 📦 Features & Pages

| Module | Description |
|---|---|
| **Landing Page** | Modern SaaS landing page with 3D Three.js interactive shield, product pills, features grid, store workflow, and grocery showcase. |
| **Authentication** | Secure registration and login with shop isolation and stateless JWT authentication tokens. |
| **Main Dashboard** | Real-time KPI summary cards (Products, Stock units, Low Stock, Expiring Soon, Expired, Today's Sales, Monthly Revenue), urgent alert banner, Chart.js monthly trend & category donut, and recent movements audit log. |
| **Inventory Management** | Product catalog table with live search, category filtering, Add/Edit modals, and manual stock adjustment modal with audit reasoning. |
| **Expiry Radar** | Multi-batch expiry tracking with configurable advance warning (e.g. 7 days). Color-coded statuses: Safe, Expiring Soon, Expired. Never allows expired products to be marked safe. Includes 1-click quarantine/scrap action. |
| **Sales & POS Checkout** | High-speed POS counter layout: live catalog selector, active bill cart with stock ceiling limits, discount input, payment method picker (Cash, UPI, Card), transactional stock deduction, and thermal printable receipt modal. |
| **Analytics & Wastage** | Best-selling products leaderboard, units sold bar chart, expired stock loss valuation in ₹ INR, and sales CSV export. |
| **WhatsApp Cloud** | Configurable notification preferences, template selector (Expiry Warning, Expired Action, Low Stock, Daily Digest), and interactive test simulator with live logs. |
| **Shop Settings** | Configure store name, owner contact details, currency, default low-stock threshold, and expiry warning days. |

---

## 🧪 Automated Testing
Run the test suite:
```powershell
mvn test
```
The test suite covers:
- Spring Boot Application Context Load
- Transactional Inventory Adjustments & Negative Stock Prevention
- Point-of-Sale Real-Time Stock Validation & Automatic Deductions
- Multi-Batch Strict Expiry Status Computation & Scrap Audit Movements

---

## 🔒 Security & Data Isolation
- Every query is scoped to the authenticated user's `shop_id`.
- Passwords are encrypted with BCrypt (10 rounds).
- API tokens and database credentials are fully externalized to environment variables.
- Zero client-side exposure of production Meta Cloud API tokens.
