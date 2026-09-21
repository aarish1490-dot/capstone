# AarishMart Phase 10 — Testing & Performance (Completion Report)

Phase: 10 — Testing + Performance
Date: 2026-09-18
Commit: `f771c19 test: improve testing`
Status: COMPLETE — build green, all tests passing

This report records what Phase 10 changed, the measured results, the bugs found
and fixed, and the work that remains outstanding. It is written to be honest:
anything that could not be verified in this environment is called out explicitly.

---

## 1. Objective & scope

Phase 10 was strictly a testing/quality phase. No new business features were added
and Phase 11 was not started. The goals were to:

1. Inspect and run the existing test suite (baseline).
2. Fill genuine coverage gaps: unit, DAO, servlet, security/edge-case, transaction,
   concurrency, full end-to-end journey, AI reliability and performance/load.
3. Inspect database query performance and justify any index changes.
4. Fix any real bugs the new tests exposed.
5. Run a full clean build and produce this report.
6. Commit the work in a single commit.

---

## 2. Results at a glance

| Metric | Baseline | After Phase 10 |
|---|---|---|
| `mvn -B clean verify` | BUILD SUCCESS | **BUILD SUCCESS** |
| Tests run | 430 | **490** |
| Failures / Errors / Skipped | 0 / 0 / 0 | **0 / 0 / 0** |
| New test classes | — | 8 |
| New / extended tests | — | +60 |
| WAR artifact | yes | `target/aarishmart.war` |
| Production bugs found & fixed | — | 1 |

---

## 3. What was added

### 3.1 New unit & servlet tests

| Test class | Tests | Coverage added |
|---|---:|---|
| `util/ValidationUtilTest` | 13 | Name/email/Indian-mobile normalization, product name, optional text/url, positive & non-negative int, quantity bounds, price scale; includes `parsePrice(null)` regression |
| `util/SessionUtilTest` | 8 | `getUser`, `isLoggedIn`, `clientIp` including X-Forwarded-For first-hop and blank-header fallback |
| `service/SmsServiceTest` | 3 | OTP message build + default expiry, configured expiry, provider-failure propagation |
| `controller/AuthServletTest` | 18 | Login/register/logout GET+POST, session rotation on login, locked-account path, OTP step + dev-OTP exposure, register validation repopulation |
| `controller/OtpServletTest` | 10 | Send/resend/verify, rate limits, unknown mobile, role-based redirects (`/`, `/seller`, `/admin`), unknown path → 404 |

### 3.2 New integration tests

| Test class | Tests | Coverage added |
|---|---:|---|
| `service/EndToEndJourneyIntegrationTest` | 1 | Full happy-path journey on in-memory H2: register buyer + seller, password & OTP login, browse/search/filter, product details, cart add/update, checkout, cart cleared + stock reduced, order history/detail, seller creates product, lifecycle PENDING→CONFIRMED→SHIPPED→DELIVERED, buyer review visible, review eligibility exhausted |
| `service/OrderConcurrencyIntegrationTest` | 3 | Real executor threads: 8 buyers / 1 unit → exactly 1 winner; 6 buyers / 3 units → exactly 3; mixed-quantity conservation invariant (final stock = initial − ordered, never negative). Confirms no oversell |

### 3.3 Extended existing suites

| Test class | Added | What it proves |
|---|---:|---|
| `service/OrderServiceIntegrationTest` | +1 | `failureAfterOrderAndFirstDecrementRollsBackEverything`: a mid-transaction failure rolls back the order, both stock decrements and leaves the cart intact |
| `service/ai/GeminiAiProviderTest` | +2 | Time-boxed provider: a 200 ms timeout against a 3 s slow server returns `Optional.empty()` and never blocks; timeout-header sanity |

### 3.4 Performance / load

| Test class | Tests | Duration |
|---|---:|---|
| `perf/LoadTest` | 1 | ~60 s |

10 concurrent simulated users exercising login, browse, search, category filter,
product details, cart read, order history and AI chat (mock provider, rate-limited)
against services + DAOs + in-memory H2.

---

## 4. Load test results

Service-layer synthetic benchmark (see caveat below).

| Operation | Requests | Unexpected errors | AI-limited | Avg (ms) | Slowest (ms) |
|---|---:|---:|---:|---:|---:|
| login | 927 | 0 | 0 | 641.49 | 1365.57 |
| browse | 927 | 0 | 0 | 3.62 | 146.11 |
| search | 927 | 0 | 0 | 2.90 | 135.95 |
| filter | 927 | 0 | 0 | 1.23 | 101.52 |
| details | 927 | 0 | 0 | 0.20 | 102.79 |
| cartRead | 927 | 0 | 0 | 0.09 | 14.31 |
| orderHistory | 927 | 0 | 0 | 0.26 | 103.75 |
| categories | 927 | 0 | 0 | 0.15 | 87.28 |
| aiChat | 40 | 0 | 30 | 2.35 | 19.84 |
| **TOTAL** | **7456** | **0** | **0** | **80.82** | **1365.57** |

- Error rate: **0.0000%** over 7,456 operations in ~60 s.
- `login` dominates latency (avg 641 ms) — this is the **intentional bcrypt cost**,
  not a defect. All read paths are sub-millisecond to a few milliseconds.
- AI chat was rate-limited 30/40 times by design (mock provider), which is expected
  behaviour, not an error.

Raw summary written to `target/load-test/load-summary.txt`.

> **Honest caveat.** This is a service-layer benchmark and excludes servlet dispatch,
> JSP rendering and HTTP/Tomcat overhead. A true HTTP load test against a deployed WAR
> (10 concurrent requests for 60 s) is recorded as **REMAINING** — it is not runnable
> in this environment because no Tomcat is installed. No HTTP-level numbers are claimed.

---

## 5. Database performance inspection

No index changes were made, because the existing schema already indexes the hot paths:

- `users.email`, `users.mobile_number` (unique)
- `products.seller_id`, `products.category`
- `cart_items.user_id`
- `orders.buyer_id`
- `order_items.order_id`
- `reviews` unique on `(order_id, product_id)`

Findings recorded for later:

- Search uses `LIKE '%term%'` (leading wildcard), so a b-tree index cannot help it.
  Acceptable at MVP scale; a full-text or trigram index is a Phase 11 consideration.
- One N+1: `OrderService.ordersForSeller` issues a per-order items query. Acceptable
  at this scale; replaceable with a JOIN or batch fetch later.
- Seed data confirmed: 1 admin + 40 products (5 categories × 8).

---

## 6. Bugs found and fixed

| Bug | Impact | Fix |
|---|---|---|
| `ValidationUtil.parsePrice(null)` threw `NullPointerException` | A missing price field produced an unhandled 500-class failure instead of a clean validation message | Guard null/blank and throw `ValidationException`; regression test added in `ValidationUtilTest` |

This was the only production bug the expanded suite exposed. No other behaviour
changes were made.

---

## 7. Verification

- `mvn -B clean verify`: **490 tests, 0 failures, 0 errors, 0 skipped**, WAR packaged.
- Automated end-to-end journey covers: register, login, OTP login, marketplace,
  search/filter, product details, cart, checkout, order placement, order-status
  lifecycle, reviews, seller product creation, seller/admin flows and AI timeout
  resilience.
- Full test report: `target/test-report/phase10-test-report.md`.

---

## 8. Git

- Single commit added for this phase: `f771c19 test: improve testing` (12 files changed,
  1,648 insertions, 3 deletions).
- `target/`, `logs/` and `.env` remain untracked/ignored — no build output or secrets committed.
- Working tree clean after the commit.

---

## 9. Outstanding / deferred (not part of Phase 10)

These are known and deliberately left for later phases:

- [ ] True HTTP/Tomcat load test against the deployed WAR (no Tomcat available here).
- [ ] Search `LIKE '%term%'` — consider full-text/trigram indexing.
- [ ] `OrderService.ordersForSeller` N+1 — consider JOIN/batch fetch.
- [ ] Phase 11 work — not started, per instructions.

---

## 10. Phase 10 step checklist

- [x] Inspect existing suite and establish baseline (430 passing)
- [x] Add missing unit/edge-case tests
- [x] Add DAO / servlet / security-path tests
- [x] Add transaction rollback and concurrency tests
- [x] Add full end-to-end journey test
- [x] Add AI reliability / timeout tests
- [x] Add performance/load test (service layer)
- [x] Inspect DB performance; document index justification
- [x] Fix discovered bug(s) at root cause
- [x] Run full clean build (green, WAR built)
- [x] Produce this report
- [x] Commit once with `test: improve testing`
- [x] Do not start Phase 11
