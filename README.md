# DhatchinaMart

A full-stack Java EE marketplace MVP with three roles: **Buyer**, **Seller**, and **Admin**. Built with JSP/Servlets (Java 11+), JSTL, HikariCP + H2, Tomcat 9, and hand-rolled CSS. No front-end frameworks, no Spring — pure Java EE, all SQL written by hand.

## Authentication

### Mobile Number + OTP (primary login)

Sign-up asks for **name, email, mobile number, password + account type**. Login is
**mobile-number based**:

```
User → Mobile Number → Send OTP → Secure 6-digit OTP → SMS Provider
     → User enters OTP → Verified → Session → Role-based dashboard
```

- OTP is generated with `SecureRandom` (never `Math.random()`), exactly 6 digits.
- OTP expires after **5 minutes** and can be used **once** only.
- Max **5 verification attempts** per OTP; resend is blocked for **60 seconds**.
- A new OTP invalidates the previous one.
- OTPs are stored **hashed (SHA-256)** in the `otp_verifications` table — never in plain text.
- The full mobile number is never shown back to the user (masked as `******3210`).
- The email + password login remains available as a secondary/recovery flow.

### SMS delivery

Delivery goes through an abstraction (`SmsProvider` → `MockSmsProvider` /
`RealSmsProvider`) configured by environment variables — **no credentials are
hard-coded** (`.env` is git-ignored, never commit API keys).

**Development / MVP mode (default):** with `OTP_MODE=development` /
`SMS_PROVIDER=mock`, no SMS is sent. The OTP is written to the application log
**and shown on the login page** in a clearly-labelled "DEVELOPMENT ONLY" box:

```
[DEV OTP] Mobile: ******3210
[DEV OTP] OTP: 483921
```

**DEVELOPMENT ONLY** — in production set `OTP_MODE=production` and
`SMS_PROVIDER=real` and configure the provider (see table below); the app then
**fails fast at startup** if `SMS_API_URL` / `SMS_API_KEY` are missing, and the
mock provider (and on-page OTP) can never be active in production.

## Demo Accounts

Only the platform admin is pre-created. Everyone else **self-registers** on the
sign-up page — as a **Buyer** ("Shop for products") or a **Seller** ("Sell my
products"). Registered accounts are stored in the H2 database and persist across
restarts; log in again any time with the same mobile number + OTP.

| Role  | Email                     | Mobile        | Password    | Notes                              |
| ----- | ------------------------- | ------------- | ----------- | ---------------------------------- |
| Admin | `admin@dhatchinamart.com` | `9876500001`  | `Admin@123` | Only pre-seeded account            |

> **Existing databases:** on first boot after this upgrade, existing users keep
> their accounts and are assigned a placeholder mobile number
> (`90000` + zero-padded id) by the migration script. They can log in with
> email + password until their mobile is updated.

## Features

**Buyer**
- Register / login / logout — sign up as a Buyer or Seller with name, email, mobile number + password; **mobile number + OTP login** (email/password kept as fallback); accounts persist in the database (sessions, `BCrypt` password hashing)
- Browse, keyword search, and category filter on the product catalog
- Product details, add-to-cart, quantity updates (AJAX JSON + progressive-enhancement forms)
- Checkout with mock payment, order history, order details (PENDING / DELIVERED / CANCELLED statuses)
- Stock is reserved and decremented atomically at order time; over-buying is blocked

**Seller**
- Dashboard with per-seller product counts
- Create / edit / delete own products (cannot touch other sellers' products)

**Admin**
- Dashboard with global metrics (total users, sellers, products, orders, revenue)
- User management (activate / deactivate), recent orders list

**Security**
- Role-based access control via a filter chain: `AuthFilter` (session + redirect to login) and `RoleFilter` (403 for wrong role)
- CSRF tokens on state-changing forms, HTML/JS escaping on all user output (`<c:out>`)
- Server-side validation everywhere; cart quantities clamped to stock; `404` for unknown products
- SQL injection safe by construction (PreparedStatements only)
- OTP login: `SecureRandom` 6-digit OTPs, SHA-256-hashed storage, 5-minute expiry,
  one-time use, max 5 attempts, 60s resend cooldown, constant-time comparison

## Tech Stack

- **Java 11** (compiled with `--release 11`), **Jakarta-era `javax.*`** servlets
- **Tomcat 9.0.x**, **JSP 2.3 / JSTL 1.2**
- **H2** embedded file database (auto-create schema + seed on first run)
- **HikariCP** connection pool
- **JUnit 5 + Mockito** unit/integration tests
- **Maven** build → single deployable WAR

## Project Layout

```
src/main/java/com/dhatchina/dhatchinamart/
  controller/    Servlets (Auth, Otp, Product, Cart, Checkout, Order, Seller, Admin)
  dao/           JDBC DAOs (users, products, cart, orders, otp_verifications)
  model/         POJOs (User, OtpVerification, ...)
  service/       Business logic (auth, otp, sms, products, cart, orders)
  service/sms/   SmsProvider abstraction (MockSmsProvider, RealSmsProvider)
  util/          DbUtil (Hikari pool), OtpUtil (hashing/masking), Security (BCrypt + CSRF)
  filter/        AuthFilter, RoleFilter, EncodingFilter
src/main/webapp/WEB-INF/jsp/   Views (login/OTP, register, products, cart, seller, admin, ...)
db/schema.sql, db/seed.sql     DDL + demo data (loaded on first boot)
db/migrations/                 Idempotent migrations (run on every boot)
```

## Build & Run

```bash
# 1. Build (runs 27 unit/integration tests)
mvn clean package

# 2. Deploy to Tomcat 9
cp target/dhatchinamart.war <TOMCAT_HOME>/webapps/

# 3. Open
http://localhost:8080/dhatchinamart
```

### Configuration (env vars or a `.env` file, all optional)

Values can be provided as environment variables or in a `.env` file placed in
the working directory, `${catalina.base}` (Tomcat) or the user home
(`.env.example` documents every key). Real environment variables always win;
`.env` is git-ignored.

| Variable                      | Default                              |
| ----------------------------- | ------------------------------------ |
| `DHAT_DB_URL`                 | `jdbc:h2:file:~/dhatchinamart;AUTO_SERVER=TRUE` |
| `DHAT_DB_USER`                | `sa`                                 |
| `DHAT_DB_PASSWORD`            | _(empty)_                            |
| `DHAT_DB_POOL_MAX`            | `10`                                 |
| `OTP_MODE`                    | `development`                        |
| `SMS_PROVIDER`                | `mock` (`mock` \| `real`)            |
| `SMS_API_URL`                 | _(empty)_ — real provider endpoint  |
| `SMS_API_KEY`                 | _(empty)_ — NEVER commit to GitHub  |
| `SMS_API_SECRET`              | _(empty)_ — NEVER commit to GitHub  |
| `SMS_SENDER_ID`               | _(empty)_                            |
| `SMS_PAYLOAD_TEMPLATE`        | `{"to":"{to}","message":"{message}","senderId":"{senderId}"}` |
| `SMS_AUTH_HEADER`             | `Authorization`                      |
| `SMS_AUTH_VALUE_TEMPLATE`     | `Bearer {apiKey}`                    |
| `OTP_EXPIRY_MINUTES`          | `5`                                  |
| `OTP_MAX_ATTEMPTS`            | `5`                                  |
| `OTP_RESEND_COOLDOWN_SECONDS` | `60`                                 |

**Production SMS setup example** (`.env` in the Tomcat dir):

```bash
OTP_MODE=production
SMS_PROVIDER=real
SMS_API_URL=https://sms-provider.example.com/v1/send
SMS_API_KEY=<your-key>
SMS_API_SECRET=<your-secret>
SMS_SENDER_ID=DHATCHA
```

The real provider posts the `SMS_PAYLOAD_TEMPLATE` JSON body (placeholders
`{to}`, `{message}`, `{senderId}`, `{apiKey}`, `{apiSecret}` are substituted
with JSON-escaped values) with the `SMS_AUTH_HEADER` header. Most gateways can
be matched with these three vars, e.g. **Fast2SMS**
(`SMS_API_URL`=`https://www.fast2sms.com/dev/bulkV2`,
`SMS_AUTH_VALUE_TEMPLATE`=`{apiKey}`,
`SMS_PAYLOAD_TEMPLATE`=`{"route":"otp","variables_values":"{message}","numbers":"{to}"}`)
or **Textlocal** (`SMS_API_URL`=`https://api.textlocal.in/send`,
`SMS_AUTH_VALUE_TEMPLATE`=`{apiKey}`,
`SMS_PAYLOAD_TEMPLATE`=`{"apikey":"{apiKey}","sender":"{senderId}","message":"{message}","numbers":"{to}"}`).
If your gateway needs a different contract, adjust the template — credentials
are never logged.

On first boot the app creates all tables (from `db/schema.sql`) and seeds the
admin account plus a starter catalog of **40 products across 5 categories**
(Accessories, Books, Clothing, Electronics, Home — 8 each, from `db/seed.sql`).
On every boot the idempotent migration `db/migrations/001_otp_mobile_number.sql`
runs to keep existing databases in sync (adds `users.mobile_number` and the
`otp_verifications` table). Remove the DB file to reset all registered accounts
and data.

## Testing

```bash
mvn test                # 67 unit + integration tests (in-memory H2)
```

A black-box end-to-end script (`e2e-buyer-flow.ps1`) exercises the full buyer /
seller / admin journeys against a running deployment — **30 checks**, all passing:
self-registration (buyer + seller) → login → browse → search → filter → details →
cart (add + AJAX update) → checkout → order created → history → stock decremented →
cart cleared → RBAC (403 / redirect) → seller create → admin dashboard.
