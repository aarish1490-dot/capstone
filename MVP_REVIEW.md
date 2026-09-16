# DhatchinaMart — MVP Review

Status: **MVP complete** — all core flows implemented, verified end-to-end, and documented.

## Verification Summary (all green)

| Layer                    | Result |
| ------------------------ | ------ |
| Unit + integration tests | **67/67 passed** (`mvn clean verify`) |
| Black-box E2E checks     | Verified against the deployed WAR on Tomcat (buyer, seller, admin journeys, OTP login, RBAC) |
| Build                    | Single `dhatchinamart.war`, `BUILD SUCCESS` |

## Mobile Number + OTP Authentication

### Registration

**Name + Email + Mobile + Password** (+ account type):

- Mobile number is mandatory and validated as a 10-digit Indian number
  (`6-9` prefix; rejects `123`, `0000000000`, `abcdefghij` …).
- `users.mobile_number` is unique — duplicate mobile or duplicate email is rejected
  with a friendly message.
- Passwords stay bcrypt-hashed (`password_hash` kept). New columns are added via
  the idempotent migration `db/migrations/001_otp_mobile_number.sql` — existing
  users are never deleted (they get a placeholder mobile on upgrade).

### Login

```
Mobile Number
  → Send OTP
  → Checked against users table ("Mobile number is not registered." otherwise)
  → Secure 6-digit OTP (SecureRandom, SHA-256-hashed in otp_verifications)
  → SMS provider (mock in dev, real in production)
  → User enters OTP
  → Verified → HTTP session created (userId + role, AuthFilter-compatible)
  → Role-based redirect: BUYER → home, SELLER → /seller, ADMIN → /admin
```

- OTP expiry: **5 minutes** · one-time use · max **5 attempts** · resend cooldown
  **60 s** (with live countdown on the page) · new OTP invalidates the previous one.
- OTPs are never stored in plain text, never logged in production, and the full
  mobile number is never shown back (masked `******3210`). In development mode
  (`OTP_MODE=development`, default) the OTP is printed to the log **and shown on
  the login page** in a "DEVELOPMENT ONLY" box; with `OTP_MODE=production` the
  app fails fast at startup unless a real SMS provider is configured, so the
  mock provider can never run in production.
- Email + password login remains available as a secondary flow (demo accounts).

## Who Can Log In

Only `admin@dhatchinamart.com / Admin@123` (mobile `9876500001`) is pre-seeded.
Every other account is self-registered on the sign-up page as a Buyer or Seller —
credentials are stored (bcrypt-hashed) in the H2 database and work on future
logins via mobile + OTP. Roles are fixed at registration.

## What's In (MVP scope)

- Full buyer journey: self-register → login → browse → search → category filter →
  product details → add to cart → quantity update (AJAX JSON) → mock checkout →
  order created (PENDING) → order history → order details.
- Seller journey: self-register as Seller → login → dashboard (own products,
  counts) → create / edit / delete products with server-side validation.
- Admin journey: login → dashboard (total users, sellers, products, orders, revenue)
  → activate / deactivate users → recent orders.
- Stock integrity: quantity checked + decremented atomically at order time
  (`UPDATE ... WHERE stock_qty >= ?`), over-buying blocked, cart clamped to stock.
- Security: session + role filters (302 login redirect, 403 on wrong role),
  BCrypt password hashing, CSRF tokens on all state-changing forms, escaping on
  all user output, PreparedStatements everywhere, custom 404s.
- Mobile OTP authentication: `SecureRandom` 6-digit OTPs, SHA-256-hashed storage,
  5-minute expiry, one-time use, max 5 attempts, 60 s resend cooldown with
  countdown UI, rate limiting per OTP, mock SMS provider in dev (`[DEV OTP]`
  log lines) and env-configured real provider in production.
- Reusable embedded H2 DB (file-based, auto schema + seed on first boot,
  idempotent migrations on every boot).
- Catalog: 40 seed products across 5 categories (Accessories, Books, Clothing,
  Electronics, Home — 8 each), owned by the platform admin; registered sellers
  add their own.

## Notable Bug Found & Fixed During Verification

- **Case-sensitive search** — `LOWER(name) LIKE ?` compared against an
  un-lowercased pattern, so any query with uppercase letters returned no results.
  Fixed by lowercasing the escaped pattern in `ProductDAOImpl.escapeLike`.
- **JSP money formatting** — `pattern="#,##0.00"` used the server default locale
  (`en_IN`), producing inconsistent grouping. Replaced with
  `type="number" minFractionDigits="2" maxFractionDigits="2"` plus a pinned
  `en_US` locale and `pageEncoding="UTF-8"` so ₹ prices render identically on
  every page. Verified in the E2E output (`1,299.00`, `2,598.00`, `5,196.00`).

## What's Out (post-MVP / next iterations)

- Real payment integration (currently mock confirm).
- Order status transitions by seller (mark DELIVERED / CANCELLED) and order
  cancellation by buyer — statuses exist in the schema and render in the UI.
- Search pagination (large catalogs), product images are remote placeholders.
- Password reset / "forgot password"; profile page to edit the mobile number.
- Email notifications, wishlists, reviews/ratings, coupon codes.
- Image upload / storage (currently `imageUrl` text field).
- Dashboard charts; today's numbers are counts only.
- H2 → MySQL/PostgreSQL swap (DAO layer isolates the SQL; the app runs on
  H2-in-file mode for demo portability).

## Run Instructions

1. `mvn clean package`
2. Copy `target/dhatchinamart.war` into Tomcat 9's `webapps/` and start Tomcat.
3. Open `http://localhost:9090/dhatchinamart` (admin is seeded with mobile
   `9876500001`; everyone else self-registers). In dev mode the OTP is printed to
   `logs/dhatchinamart.log` as `[DEV OTP] OTP: xxxxxx`.
