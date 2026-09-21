# AarishMart Phase 0 Audit

Audit-only report. No production/source files were modified. Date: 2026-09-15.

## 1. Executive Summary

AarishMart is a Java EE (Servlet 4.0 / JSP / JSTL) multi-seller e-commerce
marketplace running on H2 + HikariCP behind a classic Servlet → Service → DAO
architecture. The core buyer journey (registration → browse → cart → mock
checkout → order history) is **implemented, clean and fully tested**. 87 tests
are recorded as passing in existing Surefire reports.

However, the project does **not** yet satisfy all capstone requirements F1–F8:

| Requirement | Status |
|---|---|
| F1 Registration / Login | **COMPLETE** (with minor hardening gaps) |
| F2 Seller product management | **PARTIAL** — create only; **no edit, no delete** |
| F3 Product browsing | **COMPLETE** |
| F4 Cart | **COMPLETE** |
| F5 Checkout | **COMPLETE** |
| F6 Orders | **PARTIAL** — buyer view only; **no seller orders** |
| F7 Admin | **PARTIAL** — dashboard stats only; **no user/order/listing management** |
| F8 Reviews / star ratings | **MISSING** — no table, no code, no UI |

Documentation (README.md, MVP_REVIEW.md) **overstates** the implementation:
they claim seller edit/delete, admin user management + recent orders, CSRF
tokens, a `RoleFilter`/`Security` utility, an E2E script, and "Java 11" — most
of which do not exist in the code. The entire OTP feature (7+ Java files,
migration, tests) is currently **untracked in Git**, and `target/` + `logs/`
are committed to the repository. No CI/CD pipeline exists and the deploy config
(Render) runs the app in **development OTP mode** by default.

Overall capstone readiness: **~58/100**.

## 2. Project Overview

- **Name:** AarishMart
- **Description:** Multi-seller e-commerce marketplace MVP with Buyer, Seller and Admin roles
- **Group/artifact:** `com.dhatchina:aarishmart:1.0.0`
- **Packaging:** WAR (`aarishmart.war`)
- **Repo:** https://github.com/dhatchina1028/AarishMart.git
- **Scope:** Pure Java EE — no Spring, no front-end frameworks, hand-written SQL and CSS

## 3. Technology Stack

| Layer | Technology | Evidence |
|---|---|---|
| Language | Java (bytecode release **17**) | `pom.xml` `<maven.compiler.release>17</maven.compiler.release>` |
| Build | Maven 3 (wrapper absent; not installed on this machine) | `pom.xml`, no `mvnw` |
| Web | Servlet 4.0, JSP 2.3, JSTL 1.2 (`javax.*`) | `pom.xml`, `web.xml` (4.0) |
| App server | Tomcat 9.x (Docker image `tomcat:9.0-jre17-temurin`) | `Dockerfile` |
| Database | H2 2.3.232 file mode (`jdbc:h2:file:~/aarishmart;AUTO_SERVER=TRUE`) | `pom.xml`, `DbUtil.java` |
| Pool | HikariCP 5.1.0 | `pom.xml`, `DbUtil.java` |
| JSON | Gson 2.11.0 | `pom.xml`, `CartServlet` |
| Passwords | jBCrypt 0.4 (cost 10) | `AuthUtil.java` |
| LOG4J/SLF4J | SLF4J 2.0.16 + Logback 1.5.12 (console + rolling file) | `pom.xml`, `logback.xml` |
| Logging OTP | SecureRandom 6-digit, SHA-256 hashed storage | `OtpService`, `OtpUtil` |
| SMS | `SmsProvider` strategy: `MockSmsProvider` / `RealSmsProvider` | `service/sms/` |
| Frontend | JSP + JSTL (`<c:out>`, fmt), 1 hand-rolled CSS, 1 vanilla JS | `webapp/` |
| Tests | JUnit 5.11.4 + Mockito 5.14.2 | `pom.xml` |

**Documentation mismatch:** README/MVP_REVIEW say "Java 11 (compiled with
`--release 11`)"; the build actually targets **Java 17**.

## 4. Architecture

```
Browser
  → JSP / HTML / CSS / JS  (views public for CSS/JS, JSPs under WEB-INF)
  → EncodingFilter (/*)   UTF-8
  → AuthFilter (/cart /checkout /orders /order /order-success /seller/* /admin)
        - redirect-to-login if no session
        - 403 for wrong role (seller/admin paths)
  → Serlvet / Controller  (8 servlets via @WebServlet)
  → Service layer         (Auth, Otp, Product, Cart, Order, Seller, Admin, Sms)
  → DAO interface + impl  (JDBC, DataSource injected)
  → HikariCP → H2
```

Verified components:

- **Controllers (8):** `AuthServlet` `/login /register /logout`,
  `OtpServlet` `/otp/send /otp/verify /otp/resend`,
  `ProductServlet` `/products /product`, `CartServlet` `/cart`,
  `CheckoutServlet` `/checkout`, `OrderServlet` `/orders /order /order-success`,
  `SellerServlet` `/seller /seller/product/create`, `AdminServlet` `/admin`.
- **Services (8):** AuthService, OtpService, SmsService, ProductService,
  CartService, OrderService, SellerService, AdminService.
- **DAOs (5 + impl):** UserDAO, ProductDAO, CartDAO, OrderDAO, OtpDAO.
- **Models (6):** User, Product, CartItem, Order, OrderItem, OtpVerification.
- **DTOs (5):** RegisterRequest, LoginRequest, CartView, CartLine, AdminStats.
- **Exceptions (3):** AppException, NotFoundException, ValidationException.
- **Filters (2):** AuthFilter, EncodingFilter.
- **Listener (1):** AppContextListener (bootstraps DB schema/seed/migration + services).
- **Utils (8):** DbUtil, ServiceRegistry, SessionUtil, AuthUtil, ValidationUtil,
  OtpUtil, EnvFileLoader, MoneyFormatter.
- **Views (16 JSP + 2 JSPF fragments):** login, register, products,
  product-details, cart, checkout, order-history, order-details, order-success,
  create-product, seller-dashboard, admin-dashboard, index, error pages.

### Architecture verdict

Clean separation of concerns. DAO abstraction, service layer, DTO usage and
entity/model separation are **all genuinely present and verified**:

- DAO: interfaces + JDBC impls, `DataSource` injected ✓
- Service layer: business rules (stock checks, totals, OTP rules) in services ✓
- Singleton/Registry: `ServiceRegistry` with synchronized lazy init ✓
- Factory: `SmsProviderFactory` ✓
- Strategy: `SmsProvider` interface + Mock/Real implementations ✓
- Front Controller: **partial** — each resource has its own servlet; there is
  no single front controller, but routing is centralized enough via `@WebServlet`.
- Builder pattern: **absent** (acceptable for scope).

Minor violations: some controller logic duplicates service queries
(e.g. `ProductServlet` calls `productService.categories()`), `parseId` and
error-handling blocks repeat across servlets, and `OrderService.placeOrder`
queries each product twice inside the loop.

## 5. Database

Files: `db/schema.sql`, `db/seed.sql`, `db/migrations/001_otp_mobile_number.sql`.

| Table | Key columns | PK / FK / constraints |
|---|---|---|
| `users` | name, email (unique), mobile_number (unique, added by migration), password_hash, role, created_at | PK id; UNIQUE email, mobile_number |
| `otp_verifications` | user_id, otp_hash (VARCHAR 64 = SHA-256 hex), expires_at, attempts, verified, created_at | PK id; FK user_id → users; index user_id |
| `products` | seller_id, name, description (CLOB), price DECIMAL(10,2), stock_qty, category, image_url, created_at | PK id; FK seller_id → users; indexes seller_id, category |
| `cart_items` | user_id, product_id, quantity, created_at | PK id; UNIQUE(user_id, product_id); FK user, FK product; index user_id |
| `orders` | buyer_id, status (default 'PENDING'), total_amount DECIMAL(10,2), created_at | PK id; FK buyer_id → users; index buyer_id |
| `order_items` | order_id, product_id, quantity, unit_price DECIMAL(10,2), created_at | PK id; FK order, FK product; index order_id |

Verified good practices:

- **Money is `DECIMAL(10,2)`** everywhere (`price`, `total_amount`, `unit_price`) — no float money.
- **`created_at` on every table** ✓
- Money math uses `BigDecimal` in services ✓
- Seed data: admin account (`admin@aarishmart.com` / mobile `9876500001`,
  real bcrypt hash) + **40 products** across 5 categories (Electronics,
  Accessories, Books, Clothing, Home — 8 each) owned by the admin seller.
- Migration numbering starts at `001` (only one migration) and is idempotent.
- `isInitialized()` guards schema re-run; migration runs on every boot.

**Capstone support check:**

| Domain | Supported? |
|---|---|
| Users / Buyer / Seller / Admin | ✓ (users + role enum) |
| OTP | ✓ (otp_verifications) |
| Products | ✓ |
| Cart | ✓ (cart_items) |
| Orders + order items | ✓ |
| **Reviews / ratings** | ✗ **NO TABLE EXISTS** |

Missing schema pieces: `reviews`/`ratings` table, `users.active` flag (admin
deactivate), order-status fields beyond a string column (adequate), any
`seller_orders` relation (derivable from products.seller_id → order_items).

## 6. F1–F8 Requirement Audit

### F1 — Registration / Login — STATUS: **COMPLETE**

- Buyer/Seller register with name, email, mobile, password, role
  (`AuthServlet` → `AuthService.register`).
- **Admin has no public signup:** registration only ever produces
  `SELLER` or `BUYER` (`AuthService.register` line 49); admin is seeded only.
- Login: primary mobile-number + OTP; email+password kept as fallback.
- Password hashing: **BCrypt** cost 10 (`AuthUtil`).
- Session: `request.changeSessionId()` on login (`AuthServlet`, `OtpServlet`) —
  fixation mitigated. Session timeout 30 min (`web.xml`).
- Logout: invalidates session (`AuthServlet` `/logout`).
- Role handling: role stored on session user; `AuthFilter` enforces roles.
- **Gaps (P1):** no login brute-force rate limiting (email flow), no per-IP
  throttling on `/otp/send` beyond the 60 s per-user cooldown, no CSRF tokens
  on auth forms.

### F2 — Seller Product Management — STATUS: **PARTIAL** (create only)

- **Create:** ✓ `/seller/product/create` (`SellerServlet` → `SellerService.createProduct`)
  with name, description, price, stock, category, image URL, validated in
  `ValidationUtil`.
- **Edit:** ✗ **MISSING** — no servlet route, no `ProductDAO.update`, no UI.
- **Delete:** ✗ **MISSING** — no servlet route, no `ProductDAO.delete`, no UI.
- `ProductDAO` (read in full) exposes only: insert, findById, find, findCategories,
  findBySeller, countAll, countBySeller, decrementStock. **No update/delete.**
- **Gap (P0):** implement update + delete (seller may only touch own products).

### F3 — Product Browsing — STATUS: **COMPLETE**

- Browse: `/products` (`ProductServlet.handleBrowse` → `ProductService.browse`).
- Search: `q` keyword, case-insensitive `LOWER(name/description) LIKE` with
  `%`/`_`/`\` escaping (`ProductDAOImpl.find` + `escapeLike`), parameterized.
- Filter: `category` param; categories pulled distinct from DB.
- UI: `products.jsp` (search box, category select, product cards), `product-details.jsp`,
  lazy-loaded images, empty/error states present.

### F4 — Cart — STATUS: **COMPLETE**

- Add / update / remove: `/cart` POST `action=add|update|remove`
  (`CartServlet` → `CartService`).
- Persistence: `cart_items` table, unique (user, product).
- Quantity validation: 1–99, clamped to stock (`ValidationUtil.parseQuantity`,
  `CartService.ensureWithinStock`).
- Running total: `CartView`/`CartLine` computed in `CartService.getCart`,
  subtotal/total rendered in `cart.jsp`; AJAX JSON updates via `js/cart.js`
  (`{"success":true,"count","total"}`) with non-JS form fallback.

### F5 — Checkout — STATUS: **COMPLETE**

- `CheckoutServlet` GET → summary page (`checkout.jsp`); POST → `OrderService.placeOrder`.
- Mock payment only (confirm button on checkout page) — no 3rd-party gateway.
- Order + order_items created in a transaction (`conn.setAutoCommit(false)`).
- Total computed from live `product.price` with `BigDecimal`.
- **Stock decremented atomically** (`UPDATE ... WHERE stock_qty >= ?`,
  `ProductDAOImpl.decrementStock`), rollback on failure.
- **Cart cleared** after successful order (`cartDAO.clearForUser`).
- Empty-cart / stock-fail paths redirect back to `/cart` with messages.

### F6 — Orders — STATUS: **PARTIAL**

- Buyer: order history `/orders`, order details `/order`, success `/order-success` ✓
  (ownership enforced via `getOrderForBuyer(orderId, buyerId)`).
- Order status shown in UI (badge).
- **Seller incoming orders:** ✗ **MISSING** — no seller-facing order query,
  no servlet route, no UI. `OrderDAO` has no seller lookup.
- **Status transitions:** only `PENDING` is ever set; no update mechanism.

### F7 — Admin — STATUS: **PARTIAL**

- `AdminServlet` `/admin` → `AdminService.getDashboardStats`:
  counts only (users, buyers, sellers, products, orders).
- **View users:** ✗ MISSING (no user list endpoint/UI).
- **View orders:** ✗ MISSING (only a count; no list).
- **Moderate/remove listings:** ✗ MISSING (no product management actions).
- Authorization: ✓ `AuthFilter` blocks non-ADMIN (`403`).
- The admin dashboard JSP itself states user management and product moderation
  are "planned for the final review."

### F8 — Reviews / Star Ratings — STATUS: **MISSING**

- No `reviews` table (schema.sql), no Review model, DAO, service, servlet,
  JSP, or JS. Grep for `review|rating` returns only unrelated UI copy.
- No completed-order restriction logic (nothing to restrict).
- **Gap (P0):** build the full reviews/ratings stack.

## 7. Optional Requirement Audit

| Feature | Status | Evidence |
|---|---|---|
| O1 Wishlist / save-for-later | **MISSING** | No table/code/UI |
| O2 Order status workflow (Pending→Confirmed→Shipped→Delivered) | **MISSING** | `OrderService` hard-codes `STATUS_PENDING`; no transition endpoint. (README mentions DELIVERED/CANCELLED but no code sets them) |
| O3 Seller sales dashboard | **PARTIAL** | Seller dashboard shows own products + count only; no sales/order/revenue metrics |
| O4 AI chatbot | **MISSING** | No chatbot code/config anywhere |

## 8. Authentication & OTP Audit

| Area | Finding | Evidence |
|---|---|---|
| Registration | Mobile + email + password + role; validated | `AuthService.register`, `ValidationUtil` |
| Login | Email/password fallback + mobile OTP primary | `AuthServlet`, `OtpServlet` |
| Logout | Session invalidate | `AuthServlet` |
| Password hashing | BCrypt cost 10 | `AuthUtil` |
| Session ID regeneration | `request.changeSessionId()` | `AuthServlet`, `OtpServlet` |
| Session timeout | 30 min | `web.xml` |
| Role authorization | `AuthFilter` (redirect 302 / 403) | `AuthFilter` |
| OTP generation | SecureRandom 6-digit | `OtpService.generateOtp` |
| OTP hashing | SHA-256 stored, never plain text | `OtpUtil.hashOtp` |
| OTP expiry | 5 min (env `OTP_EXPIRY_MINUTES`) | `OtpService` |
| OTP attempts | Max 5 (`OTP_MAX_ATTEMPTS`) | `OtpService.verifyOtp` |
| OTP verification | Constant-time compare; one-time use | `OtpUtil.constantTimeEquals` |
| OTP resend | 60 s cooldown (`OTP_RESEND_COOLDOWN_SECONDS`) + countdown UI | `OtpService`, `login.jsp` |
| SMS abstraction | `SmsProvider` strategy | `service/sms/` |
| Mock provider | Logs OTP (`[DEV OTP]`) — **no real SMS** | `MockSmsProvider` |
| Real provider | HTTP JSON gateway via env vars (Fast2SMS/Textlocal templates documented), never logs credentials | `RealSmsProvider` |
| Env / secrets | `.env` loader; real env wins; `.env` git-ignored; `.env.example` committed | `EnvFileLoader`, `.env.example` |
| Fail-fast | `OTP_MODE=production` rejects mock provider / missing creds at startup | `SmsProviderFactory` |

**Does OTP actually send SMS?** Only when `SMS_PROVIDER=real` with a configured
gateway. The default (`development` mode / `mock` provider) only generates,
stores (hashed) and prints the OTP to the log and login page. The production
safeguards are well implemented (factory refuses mock in production, fails fast
without credentials).

**Gaps:** no per-IP login/OTP rate limit or account lockout; no re-verify of
old-OTP invalidation beyond `invalidateByUserId` (present, good); email login
has no attempt limiting.

## 9. Security Audit

| Check | Result | Details |
|---|---|---|
| SQL injection | **SAFE** | All DAOs use `PreparedStatement` / parameterized queries. `DbUtil.runScript` uses `Statement` only for bundled static DDL/DML. `escapeLike` neutralizes `% _ \` |
| Password hashing | **GOOD** | BCrypt cost 10 |
| Auth bypass | **NONE FOUND** | `AuthFilter` gates protected paths; order ownership checked server-side |
| Authorization bypass | **NONE FOUND** | Non-seller → `/seller/*` 403; non-admin → `/admin` 403; buyer can't read others' orders (`getOrderForBuyer` checks buyer id) |
| XSS | **GOOD** | All dynamic output via `<c:out>` (verified across every JSP grep hit); no raw `${...}` output, no scriptlets |
| CSRF | **MISSING** | **No CSRF tokens anywhere.** README/MVP_REVIEW claim them, but grep for `csrf` in Java + JSP returns nothing. State-changing POSTs (cart, checkout, product create, login, register, OTP) are CSRF-exposed |
| Session security | **GOOD** | ID regeneration on login; 30 min timeout. Cookie not explicitly `Secure` (no TLS in play) |
| Brute force | **WEAK** | Email login unlimited; OTP capped at 5 attempts/OTP + 60 s resend |
| Input validation | **GOOD** | Name/email/mobile/password/price/stock/quantity/URL validated; URL must be http(s) |
| File/path security | **N/A-safe** | No file upload or filesystem access from user input |
| Hardcoded credentials | **OK** | Only seeded admin bcrypt hash in `seed.sql` (by design). `login.jsp` shows a demo `buyer@aarishmart.com / Buyer@123` that is **not seeded** (misleading UI, not a secret leak) |
| API keys / secrets | **NONE** in code | All via env / `.env` |
| System.out / printStackTrace | **NONE** | SLF4J used throughout |
| Unsafe redirects | **NONE FOUND** | Redirects use `contextPath +` fixed paths only |
| Logging | **GOOD** | Mobile numbers masked; OTP never logged in production mode |

## 10. API Audit

No `/api/v1/...` structure exists — the app is servlet-MVC. JSON is emitted by
`CartServlet` only (`format=json`). No unified response envelope.

| Endpoint | Method | Purpose | Auth | Role | Response |
|---|---|---|---|---|---|
| `/login` | GET/POST | login page / OTP | public | – | JSP / redirect |
| `/register` | GET/POST | registration | public | – | JSP / redirect |
| `/logout` | GET | logout | – | – | redirect |
| `/otp/send` `/otp/verify` `/otp/resend` | POST | OTP flow | public (by mobile) | – | JSP / session / redirect |
| `/products` | GET | browse/search/filter | public | – | JSP |
| `/product?id=` | GET | details | public | – | JSP / 404 |
| `/cart` | GET/POST | view / add / update / remove | **auth** | BUYER | JSP / JSON |
| `/checkout` | GET/POST | summary / place order | **auth** | BUYER | JSP / redirect |
| `/orders` `/order` `/order-success` | GET | history / details / success | **auth** | BUYER | JSP / 404 |
| `/seller` `/seller/product/create` | GET/POST | dashboard / create product | **auth** | SELLER | JSP |
| `/admin` | GET | stats dashboard | **auth** | ADMIN | JSP |

JSON shape (CartServlet): success case `{"success":true,"count":"…","total":"…"}`
and failure `{"success":false,"error":"…"}` with HTTP 400/500 — internally
consistent but cart-specific; there is no global `success/data/error` contract.
HTTP status codes are otherwise default (redirects, `sendError` for 403/404).

## 11. Frontend / UI/UX Audit

Layout: single shared `style.css` (714 lines), one `header.jspf`/`footer.jspf`,
responsive breakpoint at `@media (max-width:720px)`, CSS variables, semantic
elements. `login.jsp` has a solid OTP UX (6 boxes, paste handling, resend
countdown). Cart and product cards are clean.

Do-not-fix observations:

- **Inline styles everywhere** (`style="width:100%"`, `style="margin-top:14px"`,
  `style="font-weight:600"`, seller dashboard header div, admin panel, index hero)
  make margins/buttons inconsistent.
- Button set is consistent (`btn`, `btn-secondary`, `btn-sm`, `btn-danger`)
  but spacing varies due to inline overrides.
- **Stale demo credentials** under email login: "buyer@aarishmart.com /
  Buyer@123" — no such seed account exists → guaranteed failed login.
- `error-403.jsp` exists but is **not wired** in `web.xml` (only 404/500 +
  Throwable) — authz errors fall back to the generic container page.
- `index.jsp` hard-codes the 5 categories (duplicated with the DB-driven
  category list on `products.jsp`).
- Admin dashboard is a count-only page with a "planned for final review" note.
- Seller dashboard has no edit/delete/product-price actions.
- No reviews UI (none exists).
- Mobile: one media query covers the essentials; tables use `overflow-x:auto`
  wrapper (`table-wrap`) — acceptable; nav is a simple flex row, unlabeled.
- Accessibility: basic labels/`aria-label` on OTP boxes and form labels exist;
  no skip-nav, no focus styles confirmed, no `aria-live` for alerts.
- Empty states: present (cart, seller products). Error states: present (JSP alerts).
  Loading states: JSON cart updates have no spinner, and a **dead session turns
  the AJAX cart call into an HTML login page → `res.json()` parse failure**.
- Duplicate/unused: `error-403.jsp` unused; no JS library bloat (1 file).

## 12. Testing Audit

Executed: **`mvn test` could NOT be re-run** — Maven is not installed on this
machine, there is no `mvnw` wrapper, and the Docker daemon is not running
(`docker ps` → cannot connect to Docker engine; only the CLI client
`29.6.2` is present). No build was executed; nothing was modified.

Existing artifacts in `target/surefire-reports/` (from the last recorded run,
2026-08-11) show **all green**:

| Test class | Tests | Failures |
|---|---|---|
| OtpDaoIntegrationTest | 6 | 0 |
| ProductDaoIntegrationTest | 7 | 0 |
| UserDaoIntegrationTest | 5 | 0 |
| AuthServiceTest | 16 | 0 |
| CartServiceTest | 6 | 0 |
| OrderServiceIntegrationTest | 5 | 0 |
| OtpServiceTest | 17 | 0 |
| ProductServiceTest | 3 | 0 |
| RealSmsProviderTest | 7 | 0 |
| SmsProviderFactoryTest | 7 | 0 |
| EnvFileLoaderTest | 4 | 0 |
| OtpUtilTest | 4 | 0 |
| **Total** | **87** | **0** |

Coverage present: DAO integration (User/Product/Otp), services (Auth, Cart,
Order, Product, Otp), SMS provider + factory, env loader, OTP util.

**Missing test areas:** no servlet/controller tests, no filter/RBAC tests, no
CSRF/security tests, no cart-DAO integration test, no checkout/payment-flow
test, no E2E harness. README claims an `e2e-buyer-flow.ps1` script with "30
checks" — **the file does not exist** in the repository.
**Test-count documentation is wrong:** README says "27" (build) and "67"
(testing); MVP_REVIEW says "67"; the actual recorded run is **87**.

## 13. Code Quality Audit

- Checkstyle / SpotBugs / PMD: **absent** (no plugins in pom.xml).
- Javadoc: present on most classes and public methods (not uniform).
- Logging: consistent SLF4J/Logback, masked identifiers ✓
- Try-with-resources: used consistently for connections/statements ✓
- Exception handling: custom `AppException`/`ValidationException`/
  `NotFoundException` + wrapped SQLExceptions; servlets map exceptions to
  friendly messages/redirects ✓
- Naming: clear and consistent ✓
- Duplication: `homeFor` redirect logic, inline-style margins, cart
  quantity/stock errors duplicated in `CartService`/`OrderService`, repeated
  error-handling blocks in servlets.
- Dead code: `error-403.jsp` (unused), `ProductService.countBySeller` vs
  `SellerService.productCount` duplication, minor.
- Hardcoded values/magic numbers: `99` max qty, `10` bcrypt cost, `5`/`60`
  OTP defaults are env-backed (good); category chips on `index.jsp` duplicated.
- Large methods: none egregious; `OrderService.placeOrder` is the largest (~50
  lines) but cohesive.
- Generated artifacts and logs committed to Git (`target/`, `logs/`) — repo hygiene issue.

**Design patterns verified in code (not just README):** DAO ✓, Singleton
(`ServiceRegistry`) ✓, Factory (`SmsProviderFactory`) ✓, Strategy
(`SmsProvider` Mock/Real) ✓. Front Controller — partial. Builder — none.

## 14. Deployment Audit

| Item | Status |
|---|---|
| War generation | ✓ `mvn-war-plugin` → `aarishmart.war` |
| Dockerfile | ✓ Multi-stage: `maven:3.9-eclipse-temurin-17` → `tomcat:9.0-jre17-temurin`; copies WAR to webapps (build skips tests) |
| render.yaml | ✓ Single Docker web service, healthCheck `/aarishmart/`, autoDeploy |
| Tomcat compatibility | ✓ javax.* Servlet 4.0 on Tomcat 9 |

**Deployment blockers (P0/P1):**

- **Development OTP leaks to production by default.** No env vars are set in
  `render.yaml`; the default `OTP_MODE=development` / `SMS_PROVIDER=mock` means
  a Render deploy would **print every OTP to logs and show it on the login
  page**. The fail-fast guard only triggers if someone sets `OTP_MODE=production`.
- **Database is ephemeral.** H2 file DB lives inside the container filesystem;
  every deploy/new instance **wipes accounts, products and orders**. Needs a
  persistent disk / volume or a managed DB, or seed re-runs lose data between
  scaling events.
- No explicit `DHAT_DB_URL` / credentials configured for any host (defaults OK
  locally, not for prod).
- No health-check / readiness beyond a GET on the app root.

## 15. Git / CI Audit

- Branch: `main` (tracks `origin/main`).
- Remote: `https://github.com/dhatchina1028/AarishMart.git`.
- Commits: **8** (2026-07-30 initial → 2026-08-12 last: "style: redesign
  AarishMart visual theme").
- Commit style: inconsistent — a few conventional prefixes (`style:`) but also
  `Project`, `add target`, `add logs`, `Delete .gitattributes`.
- **Uncommitted work:** the entire OTP feature is **untracked**
  (`OtpServlet`, `OtpDAO/Impl`, `OtpVerification`, `OtpService`, `SmsService`,
  `service/sms/*`, `EnvFileLoader`, `OtpUtil`, tests, migration) and many files
  are modified (schema, seed, login/register JSPs, AuthServlet, AuthService,
  style.css, etc.). The committed HEAD does **not** contain the OTP/auth work
  described in docs. **This is the single biggest submission risk.**
- `target/` and `logs/` are **tracked** despite `.gitignore` — generated files,
  surefire dumps and WAR are in history.
- GitHub Actions: **absent** — no CI, so `mvn -B clean verify` is never run
  automatically. Capstone "Git requirements" (clean history, CI) are only
  partially met.

## 16. Documentation Audit

| Doc | Content | Issues |
|---|---|---|
| README.md | Features, architecture, tech stack, setup, env-var table, SMS provider examples, testing | **Inaccurate:** claims Java 11 (`--release 11`) → actual 17; claims seller "create / edit / delete" → no edit/delete; claims `RoleFilter`, `Security (BCrypt + CSRF)` util → none exist; claims CSRF tokens → none; claims `e2e-buyer-flow.ps1` (30 checks) → file missing; test count "27"/"67" → actual 87; port 8080 in README vs 9090 in MVP_REVIEW |
| MVP_REVIEW.md | Verification summary, OTP details, scope in/out, era "bug found" | Claims **67/67** tests (actual 87), admin "activate / deactivate users → recent orders" (not implemented), seller "create / edit / delete" (edit/delete missing), "CSRF tokens on all state-changing forms" (missing); port 9090 |
| .env.example | Full OTP/SMS/DB config reference | Good, accurate |

Missing in docs: explicit problem statement/objective, API documentation,
screenshots, deployed URL, and a "known limitations" section in README
(MVP_REVIEW does list out-of-scope items). No architecture diagram.

## 17. Capstone Gap Analysis

| Requirement | Status | Evidence | Priority | Required Action |
|---|---|---|---|---|
| F1 Register / Login | COMPLETE | AuthServlet, OtpServlet, AuthService, BCrypt, AuthFilter | P1 | Harden: login rate-limit, CSRF on auth forms |
| F2 Product create | COMPLETE | SellerServlet, SellerService.createProduct | – | – |
| F2 Product edit | **MISSING** | No route/DAO/UI | **P0** | Add update flow (edit own products only) |
| F2 Product delete | **MISSING** | No route/DAO/UI | **P0** | Add delete flow (own products only) |
| F3 Browse/search/filter | COMPLETE | ProductServlet, ProductDAOImpl.find, products.jsp | – | – |
| F4 Cart | COMPLETE | CartServlet, CartService, cart.js | – | – |
| F5 Checkout (mock payment) | COMPLETE | CheckoutServlet, OrderService.placeOrder | – | – |
| F6 Buyer history/status | COMPLETE | OrderServlet, order-history.jsp | – | – |
| F6 Seller incoming orders | **MISSING** | No seller order query/route/UI | **P0** | Seller order list + mark status |
| F7 Admin stats | COMPLETE | AdminServlet, AdminService | – | – |
| F7 Admin users view | **MISSING** | No list endpoint/UI | **P0** | User list + activate/deactivate |
| F7 Admin orders view | **MISSING** | Only count | **P0** | Order list |
| F7 Admin listing moderation | **MISSING** | No actions | **P0** | Remove/unlist product action |
| F8 Reviews / ratings | **MISSING** | No table/model/DAO/service/servlet/UI | **P0** | Full reviews stack + completed-order gate |
| O1 Wishlist | MISSING | – | P2 | Optional |
| O2 Status workflow | MISSING | Only PENDING set | P1 | Pending→Confirmed→Shipped→Delivered |
| O3 Seller sales dashboard | PARTIAL | Dashboard = product list/count only | P1 | Sales/order metrics per seller |
| O4 AI chatbot | MISSING | – | P3 | Optional |

## 18. Security Gaps

| Issue | Severity | Location | Risk | Recommended Fix |
|---|---|---|---|---|
| No CSRF protection on any state-changing POST | **HIGH** | All servlets (cart, checkout, seller, otp, auth) | Cross-site request forgery (place order, add cart, create product) | Synchronizer token on forms + server-side verification in servlets | 
| No login/OTP rate limiting (email login unlimited; OTP only 5 attempts/60s) | **MEDIUM** | AuthServlet, OtpService | Brute force / enumeration | Per-IP and per-account throttling + lockout |
| Dev OTP exposed on login page & logs by default | **HIGH** | login.jsp, MockSmsProvider, render.yaml | OTP leak in production deploy | Force `OTP_MODE=production` in render.yaml; fail-closed default |
| H2 embedded DB, ephemeral in container | **MEDIUM** | Dockerfile/render.yaml | Data loss on redeploy/scale | Persistent volume or managed DB |
| `logs/` and `target/` committed to Git | **LOW** | repo | Repo hygiene, possible accidental secrets in history | Untrack + remove from history |
| OTP hash is plain SHA-256 (unkeyed) | **LOW** | OtpUtil | If DB leaks, 6-digit OTPs trivially brute-forceable | Use HMAC with server secret or bcrypt/argon for OTP |
| `error-403.jsp` unused; generic error page on authz failure | **LOW** | web.xml | Info leak aesthetics | Wire 403 error page |
| Session cookie not marked `Secure`/`SameSite` | **LOW** | web.xml | Not applicable w/o TLS; medium when TLS added | Add secure + SameSite=strict once HTTPS |
| No password reset / account recovery | **LOW** | AuthService | Usability (not a vuln) | Optional later |

## 19. UI/UX Gaps

| Area | Problem | Impact | Direction | Priority |
|---|---|---|---|---|
| Login | Demo creds "buyer@aarishmart.com / Buyer@123" don't exist | Users can't log in with the shown demo | Show real seeded account or remove | P1 |
| Login | Dev OTP box shown whenever dev mode (deploy risk) | Confusing + insecure on prod | Gate by OTP_MODE + hide in prod banner | P1 |
| Register | No role selection explainer UI | Unclear buy/sell choice | Improve role cards | P3 |
| Navigation | No cart count badge; seller/admin links only in nav | Poor orientation | Add cart count + role-aware nav | P2 |
| Home | Hard-coded category chips duplicate DB list | Drift risk | Generate from DB | P2 |
| Products | No pagination; flat list | Scalability | Paginate | P3 |
| Product details | No reviews/ratings area (feature missing) | F8 unmet | Add after F8 | P0 |
| Cart | AJAX breaks when session dies (reload → HTML into res.json) | Error state poor | Detect 302/redirect, reload to login | P2 |
| Checkout | No order summary review clarity / no payment method messaging | Confusing "mock payment" | Label mock payment clearly | P3 |
| Orders | Status badge hard-coded styling; no seller view | F6 unmet | After seller orders | P0 |
| Seller | No edit/delete buttons; no pricing edit; no sales | F2/F6/O3 unmet | Add actions + metrics | P0 |
| Admin | Stats only; explicit "planned" placeholder | F7 unmet | Build user/order/listing panels | P0 |
| Reviews | Entirely absent | F8 unmet | Add | P0 |
| AI assistant | Absent | O4 unmet | Optional | P3 |
| Mobile | Single breakpoint, tabular forms; nav not hamburger | Acceptable but cramped | Improve nav + table responsiveness | P2 |
| Global | Inline styles, inconsistent spacing, errors/empty states duplicated per page | Inconsistent look | Move to CSS classes | P2 |
| Global | `error-403.jsp` unreachable | Wrong page rendered on 403 | Wire in web.xml | P3 |
| Accessibility | No skip-nav, no live regions, focus states unverified | Usability | Add a11y pass | P3 |

## 20. Performance Findings

- **N+1 queries:** `CartService.getCart` queries the product per cart line
  (`productDAO.findById` in a loop); `CartView` count/total loops again.
- **Double product lookup** in `OrderService.placeOrder` (validation pass + item
  insert pass each `findById`).
- Catalog browsing has **no pagination** — one products-page query loads every
  matching row.
- `LOWER(name) LIKE` search cannot use the category index; acceptable at MVP
  scale but note for large catalogs.
- Images: 40 small local JPGs, `loading="lazy"` used ✓.
- Connection handling: HikariCP pool, try-with-resources everywhere —
  **no connection leaks found**.
- Excess JS: single `cart.js` + inline login script — negligible.
- Admin dashboard: 5 trivial `COUNT(*)` queries per load — fine.
- No destructive load testing performed; no performance harness exists.

## 21. Readiness Scores

| Area | Score | Rationale |
|---|---|---|
| Functional | 64/100 | F1, F3, F4, F5 fully working and transactional; F2/F6/F7 partial, F8 missing |
| Security | 62/100 | Strong fundamentals (PreparedStatements, BCrypt, escaping, session regen) offset by no CSRF, weak rate limiting, dev-OTP-by-default |
| UI/UX | 55/100 | Coherent single-stylesheet design + good OTP UX, but inline-style sprawl, dead demo creds, absent seller/admin/review surfaces |
| Testing | 68/100 | 87 recorded green tests w/ good unit+DAO coverage; but no servlet/RBAC/security tests, no E2E, couldn't re-run locally |
| Deployment | 45/100 | Docker + Render exist but prod-unsafe defaults and ephemeral DB make it not deployment-ready |
| Documentation | 55/100 | Rich docs but multiple false claims (edit/delete, CSRF, RoleFilter, Java 11, test counts, E2E script) |
| **Overall capstone readiness** | **58/100** | Solid core buyer MVP; blocking gaps in F2/F6/F7/F8, CSRF and git hygiene |

## 22. Recommended Implementation Order

Follow the supplied phase order, with F2/F6/F7/F8 features prioritized ahead of
cosmetics. Reasoning: F8 (reviews) needs schema + stack (Phase 6), and auth
hardening (Phase 1) must precede anything else because every later phase
depends on principals and authorization.

- **PHASE 1 — Authentication & security:** commit the uncommitted OTP/auth
  work; add CSRF tokens; login/OTP rate limiting; harden prod OTP defaults.
- **PHASE 2 — Product management:** finish F2 — seller edit + delete (own
  products only), DAO + route + UI.
- **PHASE 3 — Buyer browsing:** (already complete) add pagination, DB-driven
  home categories.
- **PHASE 4 — Cart:** (already complete) small UX: session-death handling in
  `cart.js`, cart badge.
- **PHASE 5 — Checkout & orders:** finish F6 — seller incoming orders, order
  status workflow (O2: Pending→Confirmed→Shipped→Delivered).
- **PHASE 6 — Reviews & ratings:** F8 — table, model, DAO, service, servlet,
  JSP, completed-order gate.
- **PHASE 7 — Seller/Admin:** finish F7 — admin user list/activate, order list,
  listing moderation; extend seller dashboard with sales metrics (O3).
- **PHASE 8 — Security hardening:** sweep per Section 18 (403 page, SameSite,
  OTP HMAC, secret hygiene).
- **PHASE 9 — AI assistant:** optional O4 chatbot.
- **PHASE 10 — UI/UX redesign:** Section 19 items.
- **PHASE 11 — Testing & performance:** servlet/filter/RBAC + CSRF tests,
  N+1 fixes, E2E harness, CI wiring.
- **PHASE 12 — Deployment:** persistent DB, prod OTP mode + env vars, health
  checks, GitHub Actions `mvn -B clean verify`.
- **PHASE 13 — Documentation + final submission:** fix README/MVP_REVIEW
  inaccuracies, add API docs/screenshots/limitations, clean Git history.

## 23. Critical Issues To Fix First

1. **F8 Reviews/ratings entirely missing** (capstone requirement).
2. **F2 edit/delete + F6 seller orders + F7 admin management all missing**
   (three core capstone requirements are only partially implemented).
3. **CSRF protection absent** while docs claim it exists.
4. **Whole OTP feature is uncommitted** and `target/`/`logs/` are tracked —
   submission risk and broken Git story; no CI.
5. **Production deploy runs in dev OTP mode with an ephemeral DB** — not
   deployment-ready, and README/MVP_REVIEW overstate the true state of the
   project (seller edit/delete, admin management, tests counts, E2E script).

## 24. Phase 1 Recommendation

Do **not** start Phase 1 yet. When approved, begin with **Authentication &
security**, in this order:
1. `git add` + commit the existing OTP/auth/migration work and untrack
   `target/`/`logs/` (get history matching what the app actually does).
2. Implement a CSRF token filter + embedded tokens on every state-changing form.
3. Add login/OTP rate limiting (per-IP + per-account).
4. Force production-safe OTP defaults in `render.yaml` and document the prod env.
5. Add servlet/filter/RBAC tests for the hardened auth.
Verify with `mvn -B clean verify` after each step.

---

*Phase 0 audit complete. Production source files were NOT modified. No
implementation, refactoring, or fixes were performed.*