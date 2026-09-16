# DhatchinaMart Phase 0 — Audit Checklist

Companion to `PHASE_0_AUDIT.md`. Nothing here changes code — it is a verification
checklist for the Phase 0 audit.

## Project & Build
- [x] Project name: DhatchinaMart (`com.dhatchina:dhatchinamart:1.0.0`, WAR)
- [x] Java target: 17 (`maven.compiler.release=17`); docs wrongly say Java 11
- [x] Maven 3 required; **not installed locally**, no `mvnw`, Docker daemon down
  → tests NOT re-run; recorded Surefire result: **87/87 passing**
- [x] Servlet 4.0 / JSP 2.3 / JSTL 1.2, `javax.*`, Tomcat 9 target
- [x] DB: H2 file mode + HikariCP 5.1.0; pool size env-configurable
- [x] Gson, jBCrypt, SLF4J/Logback, JUnit 5 + Mockito confirmed in pom

## Architecture
- [x] Browser → JSP → Filters → Servlet → Service → DAO → DB verified
- [x] DAO interface + impl, DataSource injected
- [x] Service layer present (Auth, Otp, Product, Cart, Order, Seller, Admin, Sms)
- [x] DTOs (RegisterRequest, LoginRequest, CartView, CartLine, AdminStats)
- [x] Patterns: Singleton (ServiceRegistry), Factory + Strategy (SmsProvider*)
- [ ] Front Controller: partial (per-resource servlets)
- [ ] Builder: absent

## Database
- [x] users, otp_verifications, products, cart_items, orders, order_items
- [x] DECIMAL(10,2) for all money; created_at everywhere; FKs + indexes present
- [x] Seed: 1 admin + 40 products (5 categories × 8), admin-seller owned
- [x] Idempotent migration `001_otp_mobile_number.sql`
- [ ] **`reviews`/`ratings` table: MISSING**
- [ ] `users.active` flag: MISSING (admin deactivate)

## Requirements F1–F8
- [x] F1 Registration/Login: COMPLETE (OTP primary, bcrypt, session regen, roles)
- [x] F2 Create product: COMPLETE
- [ ] F2 Edit product: MISSING
- [ ] F2 Delete product: MISSING
- [x] F3 Browse/search/filter: COMPLETE
- [x] F4 Cart (add/update/remove/total, stock clamp): COMPLETE
- [x] F5 Checkout (mock payment, tx, stock decrement, cart clear): COMPLETE
- [x] F6 Buyer order history/status: COMPLETE
- [ ] F6 Seller incoming orders: MISSING
- [x] F7 Admin dashboard stats: COMPLETE
- [ ] F7 Admin users list / activate-deactivate: MISSING
- [ ] F7 Admin orders list: MISSING
- [ ] F7 Admin listing moderation: MISSING
- [ ] F8 Reviews / star ratings: MISSING (no table/model/DAO/service/UI/gate)

## Optional
- [ ] O1 Wishlist: MISSING
- [ ] O2 Status workflow (Pending→Confirmed→Shipped→Delivered): MISSING
- [ ] O3 Seller sales dashboard: PARTIAL (product list/count only)
- [ ] O4 AI chatbot: MISSING

## Security
- [x] PreparedStatements everywhere; LIKE escaping handled
- [x] BCrypt (cost 10); SHA-256 OTP storage; constant-time compare
- [x] `<c:out>` on all dynamic output (no raw XSS found)
- [x] Session ID regeneration; 30-min timeout; role checks in AuthFilter
- [ ] **CSRF: MISSING** (docs claim it exists)
- [ ] Login/OTP rate limiting: weak (email login unlimited)
- [ ] OTP hash plain SHA-256 (consider HMAC/bcrypt)
- [ ] Hardcoded secrets: none (env-driven); `.env.example` committed, `.env` ignored
- [ ] System.out / printStackTrace: none

## Deployment & Git
- [ ] `mvn test` re-run: impossible locally (Maven/Docker absent); rely on Surefire
- [x] Dockerfile + render.yaml exist
- [ ] Production OTP_SMS env defaults: NOT set (dev mode would ship OTPs)
- [ ] Ephemeral H2 in container: data loss on redeploy (needs volume/DB)
- [x] Git: branch main, 8 commits, remote origin, no CI (.github absent)
- [ ] OTP feature (untracked files + modified sources) NOT committed — top risk
- [ ] `target/` + `logs/` tracked despite .gitignore
- [ ] `e2e-buyer-flow.ps1` referenced by README: file does not exist

## Documentation
- [x] README + MVP_REVIEW present, `.env.example` accurate
- [ ] Fix false claims: Java 11 vs 17, edit/delete products, RoleFilter/Security util,
      CSRF tokens, admin user management, test counts (27/67 vs 87), E2E script,
      port 8080 vs 9090, demo buyer creds that don't exist
- [ ] Add: problem statement, API docs, screenshots, deployed URL, known limitations

## Deliverables (this phase)
- [x] `PHASE_0_AUDIT.md` created (complete audit)
- [x] `PHASE_0_CHECKLIST.md` created
- [x] No production/source/db/UI files modified