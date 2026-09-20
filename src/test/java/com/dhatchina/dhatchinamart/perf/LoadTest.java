package com.dhatchina.dhatchinamart.perf;

import com.dhatchina.dhatchinamart.dao.CartDAO;
import com.dhatchina.dhatchinamart.dao.OrderDAO;
import com.dhatchina.dhatchinamart.dao.ProductDAO;
import com.dhatchina.dhatchinamart.dao.ReviewDAO;
import com.dhatchina.dhatchinamart.dao.UserDAO;
import com.dhatchina.dhatchinamart.dao.impl.CartDAOImpl;
import com.dhatchina.dhatchinamart.dao.impl.OrderDAOImpl;
import com.dhatchina.dhatchinamart.dao.impl.ProductDAOImpl;
import com.dhatchina.dhatchinamart.dao.impl.ReviewDAOImpl;
import com.dhatchina.dhatchinamart.dao.impl.UserDAOImpl;
import com.dhatchina.dhatchinamart.dto.RegisterRequest;
import com.dhatchina.dhatchinamart.exception.RateLimitException;
import com.dhatchina.dhatchinamart.model.User;
import com.dhatchina.dhatchinamart.service.AiService;
import com.dhatchina.dhatchinamart.service.AuthService;
import com.dhatchina.dhatchinamart.service.CartService;
import com.dhatchina.dhatchinamart.service.OrderService;
import com.dhatchina.dhatchinamart.service.ProductService;
import com.dhatchina.dhatchinamart.service.ReviewService;
import com.dhatchina.dhatchinamart.service.ai.AiProvider;
import com.dhatchina.dhatchinamart.service.ai.MockAiProvider;
import com.dhatchina.dhatchinamart.util.RateLimiter;
import com.dhatchina.dhatchinamart.util.TestDb;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Honest service-layer load test.
 *
 * <p><b>What this measures:</b> 10 simulated concurrent users hammering the
 * real service + DAO + in-memory H2 stack (the exact production stack, minus
 * the web tier) for 60 seconds, covering login, marketplace browsing, product
 * search, category filtering, product details, cart reads, order history and
 * (capped, per-session rate limited) AI assistant turns through the mock
 * provider.
 *
 * <p><b>What this does NOT measure:</b> servlet dispatch, JSP rendering, HTTP
 * transport or Tomcat container overhead. A true wall-clock HTTP load test
 * against a deployed WAR (10 concurrent requests over 60s) could not be run in
 * this environment because no Tomcat/application server is installed.
 *
 * <p>Numbers are written to {@code target/load-test/load-summary.txt} and
 * printed to the build log. The only hard assertion is correctness: every
 * operation must complete without an unexpected error.
 */
class LoadTest {

    static final int CONCURRENT_USERS = 10;
    static final long WINDOW_MILLIS = 60_000L;

    @Test
    void tenConcurrentUsersForSixtySeconds() throws Exception {
        TestHarness harness = new TestHarness();
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_USERS);
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        try {
            for (int i = 0; i < CONCURRENT_USERS; i++) {
                final int workerIndex = i;
                pool.submit(() -> {
                    try {
                        Worker worker = harness.newWorker(workerIndex);
                        start.await();
                        worker.runLoop(System.currentTimeMillis() + WINDOW_MILLIS);
                    } catch (Throwable t) {
                        failure.set(t);
                    }
                });
            }
            start.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(WINDOW_MILLIS * 3, TimeUnit.MILLISECONDS),
                    "load window must complete");

            Summary total = harness.summarize();
            harness.writeReport(total);

            assertTrue(failure.get() == null,
                    "a worker crashed: " + (failure.get() == null ? "none" : failure.get().getMessage()));
            assertTrue(total.unexpectedErrors == 0,
                    "all measured operations must succeed (unexpected errors=" + total.unexpectedErrors + ")");
            assertTrue(total.totalOperations >= 500,
                    "the load window must exercise the stack meaningfully (ops=" + total.totalOperations + ")");
        } finally {
            pool.shutdownNow();
        }
    }

    private static final class TestHarness {

        private final ProductService productService;
        private final CartService cartService;
        private final OrderService orderService;
        private final AuthService authService;
        private final AiService aiService;
        private final Map<String, Counter> counters = new ConcurrentHashMap<>();

        private TestHarness() {
            var dataSource = TestDb.newDataSource("loadtest", 14);
            UserDAO userDAO = new UserDAOImpl(dataSource);
            ProductDAO productDAO = new ProductDAOImpl(dataSource);
            ReviewDAO reviewDAO = new ReviewDAOImpl(dataSource);
            CartDAO cartDAO = new CartDAOImpl(dataSource);
            OrderDAO orderDAO = new OrderDAOImpl(dataSource);

            productService = new ProductService(productDAO);
            cartService = new CartService(cartDAO, productDAO);
            orderService = new OrderService(dataSource, orderDAO, cartDAO, productDAO);
            reviewService = new ReviewService(reviewDAO, orderDAO, productDAO);
            authService = new AuthService(userDAO);

            AiProvider provider = new MockAiProvider();
            AiService aiServiceWithRateLimit = new AiService(provider, productService, reviewService);
            this.aiService = aiServiceWithRateLimit;
        }

        private final ReviewService reviewService;

        private Worker newWorker(int index) throws Exception {
            String email = "perf-buyer-" + index + "@example.com";
            RegisterRequest request = new RegisterRequest();
            request.setName("Perf Buyer " + index);
            request.setEmail(email);
            request.setMobileNumber(String.format("98766%05d", index));
            request.setPassword("PerfPass@123");
            request.setConfirmPassword("PerfPass@123");
            request.setRole("BUYER");
            authService.register(request);
            return new Worker(this, email);
        }

        private void record(String operation, long elapsedNanos, boolean aiRateLimited, boolean unexpectedError) {
            counters.computeIfAbsent(operation, k -> new Counter())
                    .add(elapsedNanos, aiRateLimited, unexpectedError);
        }

        private Summary summarize() {
            long totalOps = 0;
            long totalErrors = 0;
            long totalNanos = 0;
            long slowest = 0;
            Map<String, String> rows = new LinkedHashMap<>();
            for (Map.Entry<String, Counter> entry : new java.util.TreeMap<>(counters).entrySet()) {
                Counter counter = entry.getValue();
                rows.put(entry.getKey(), counter.toRow());
                totalOps += counter.count.get();
                totalErrors += counter.unexpectedErrors.get();
                totalNanos += counter.totalNanos.get();
                slowest = Math.max(slowest, counter.slowestNanos.get());
            }
            long averageNanos = totalOps == 0 ? 0 : totalNanos / totalOps;
            return new Summary(rows, totalOps, totalErrors, nanosToMillis(averageNanos), nanosToMillis(slowest));
        }

        private void writeReport(Summary summary) {
            StringBuilder sb = new StringBuilder();
            sb.append("DhatchinaMart Phase 10 load test (service layer only)\n");
            sb.append("=====================================================\n");
            sb.append("Concurrent simulated users : ").append(CONCURRENT_USERS).append('\n');
            sb.append("Duration (wall clock)      : ").append(WINDOW_MILLIS / 1000).append(" seconds\n");
            sb.append("Stack under test           : services + DAOs + in-memory H2 (no HTTP/Tomcat)\n");
            sb.append("Operations performed       : login, browse, search, category filter,\n");
            sb.append("                              product details, cart read, order history, AI chat (mock)\n");
            sb.append('\n');
            sb.append("Operation        Requests  Unexp.Errors  AI-limited  Avg (ms)  Slowest (ms)\n");
            for (Map.Entry<String, String> row : summary.rows.entrySet()) {
                sb.append(String.format("%-16s %s%n", row.getKey(), row.getValue()));
            }
            sb.append(String.format("%-16s %s%n", "TOTAL", String.format(
                    "%9d %13d %11d %10.2f %13.2f",
                    summary.totalOperations, summary.unexpectedErrors, 0,
                    summary.averageMillis, summary.slowestMillis)));
            double errorRate = summary.totalOperations == 0 ? 0.0
                    : summary.unexpectedErrors * 100.0 / summary.totalOperations;
            sb.append(String.format("Error rate: %.4f%%%n", errorRate));
            sb.append('\n');
            sb.append("NOTE: service-layer synthetic benchmark. Servlet dispatch, JSP rendering and\n");
            sb.append("HTTP/Tomcat overhead are NOT included. A true HTTP load test against a deployed\n");
            sb.append("WAR (10 concurrent requests for 60s) is recorded as REMAINING - it is not runnable\n");
            sb.append("in this environment (no Tomcat installed).\n");

            try {
                Path dir = Path.of("target", "load-test");
                Files.createDirectories(dir);
                Path out = dir.resolve("load-summary.txt");
                try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(out))) {
                    writer.write(sb.toString());
                }
                System.out.println("[LoadTest] report written to " + out.toAbsolutePath());
            } catch (IOException e) {
                System.out.println("[LoadTest] could not write report file: " + e.getMessage());
            }
            System.out.println(sb);
        }

        private static double nanosToMillis(long nanos) {
            return nanos / 1_000_000.0;
        }
    }

    private static final class Worker {

        private final TestHarness harness;
        private final String email;
        private final long buyerId;
        private final HttpSession session;
        private int iteration;

        private Worker(TestHarness harness, String email) {
            this.harness = harness;
            this.email = email;
            this.buyerId = harness.authService.login(email, "PerfPass@123").getId();
            this.session = mockSession();

            // Give the worker a non-trivial order history before the timed run.
            harness.cartService.addToCart(buyerId, 1L, 1);
            harness.orderService.placeOrder(buyerId);
        }

        private static HttpSession mockSession() {
            Map<String, Object> attrs = new ConcurrentHashMap<>();
            HttpSession s = org.mockito.Mockito.mock(HttpSession.class);
            org.mockito.Mockito.when(s.getId()).thenReturn("perf-session");
            org.mockito.Mockito.doAnswer(inv -> {
                attrs.put(inv.getArgument(0), inv.getArgument(1));
                return null;
            }).when(s).setAttribute(org.mockito.ArgumentMatchers.anyString(),
                    org.mockito.ArgumentMatchers.any());
            org.mockito.Mockito.when(s.getAttribute(org.mockito.ArgumentMatchers.anyString()))
                    .thenAnswer(inv -> attrs.get(inv.getArgument(0)));
            return s;
        }

        private void runLoop(long deadlineMillis) {
            while (System.currentTimeMillis() < deadlineMillis) {
                iterate();
            }
        }

        private void iterate() {
            measure("login", () -> harness.authService.login(email, "PerfPass@123"));
            measure("browse", () -> harness.productService.browse(null, null, 1, 12));
            measure("search", () -> harness.productService.browse("kalamkari", null, 1, 12));
            measure("filter", () -> harness.productService.browse(null, "Electronics", 1, 12));
            measure("categories", () -> harness.productService.categories());
            measure("details", () -> harness.productService.getById((iteration % 40) + 1));
            measure("cartRead", () -> harness.cartService.getCart(buyerId));
            measure("orderHistory", () -> harness.orderService.ordersForBuyer(buyerId));

            if (iteration % 25 == 0) {
                measure("aiChat", () ->
                        harness.aiService.chat(session, "recommend a product for listening to music"));
            }
            iteration++;
        }

        private void measure(String operation, Runnable body) {
            long start = System.nanoTime();
            boolean aiRateLimited = false;
            boolean unexpectedError = false;
            try {
                body.run();
            } catch (RateLimitException e) {
                aiRateLimited = true;
            } catch (Exception e) {
                unexpectedError = true;
            }
            harness.record(operation, System.nanoTime() - start, aiRateLimited, unexpectedError);
        }
    }

    private static final class Counter {
        private final AtomicLong count = new AtomicLong();
        private final AtomicLong unexpectedErrors = new AtomicLong();
        private final AtomicLong aiRateLimited = new AtomicLong();
        private final AtomicLong totalNanos = new AtomicLong();
        private final AtomicLong slowestNanos = new AtomicLong();

        private void add(long elapsedNanos, boolean rateLimited, boolean error) {
            count.incrementAndGet();
            totalNanos.addAndGet(elapsedNanos);
            slowestNanos.accumulateAndGet(elapsedNanos, Math::max);
            if (rateLimited) {
                aiRateLimited.incrementAndGet();
            }
            if (error) {
                unexpectedErrors.incrementAndGet();
            }
        }

        private String toRow() {
            long c = count.get();
            long t = totalNanos.get();
            return String.format("%9d %13d %11d %10.2f %13.2f",
                    c, unexpectedErrors.get(), aiRateLimited.get(),
                    c == 0 ? 0 : t / 1_000_000.0 / c, slowestNanos.get() / 1_000_000.0);
        }
    }

    private record Summary(Map<String, String> rows, long totalOperations, long unexpectedErrors,
                           double averageMillis, double slowestMillis) {
    }
}