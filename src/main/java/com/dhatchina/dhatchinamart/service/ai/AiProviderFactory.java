package com.dhatchina.dhatchinamart.service.ai;

import com.dhatchina.dhatchinamart.util.DbUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds the configured AI provider from environment variables.
 *
 * <pre>
 *   AI_PROVIDER=mock (default) -&gt; MockAiProvider  (DEVELOPMENT ONLY, no network)
 *   AI_PROVIDER=gemini         -&gt; GeminiAiProvider; requires GEMINI_API_KEY.
 *                                If the key is missing/empty the app continues
 *                                with the mock provider rather than failing.
 * </pre>
 *
 * Optional knobs:
 *   GEMINI_MODEL          default "gemini-1.5-flash"
 *   AI_TIMEOUT_SECONDS    default 20
 */
public final class AiProviderFactory {

    private static final Logger log = LoggerFactory.getLogger(AiProviderFactory.class);

    private AiProviderFactory() {
    }

    public static AiProvider create() {
        String provider = DbUtil.getEnv("AI_PROVIDER", "mock").trim().toLowerCase();
        String apiKey = DbUtil.getEnv("GEMINI_API_KEY", "").trim();
        return create(provider, apiKey,
                DbUtil.getEnv("GEMINI_MODEL", "gemini-1.5-flash").trim(),
                envSeconds(DbUtil.getEnv("AI_TIMEOUT_SECONDS", "20")));
    }

    /**
     * Package-visible, env-free factory used by tests.
     */
    static AiProvider create(String provider, String apiKey, String model, long timeoutMillis) {
        if ("gemini".equalsIgnoreCase(provider)) {
            if (apiKey.isBlank()) {
                log.warn("AI_PROVIDER=gemini is set but GEMINI_API_KEY is missing. "
                        + "Falling back to MockAiProvider so the assistant keeps working.");
                return new MockAiProvider();
            }
            log.info("Using GeminiAiProvider (model={}, timeout={}ms)", model, timeoutMillis);
            return new GeminiAiProvider(apiKey, model, timeoutMillis);
        }
        log.info("Using MockAiProvider (DEVELOPMENT ONLY) - no external AI calls. "
                + "Set AI_PROVIDER=gemini and GEMINI_API_KEY for the real assistant.");
        return new MockAiProvider();
    }

    private static long envSeconds(String raw) {
        try {
            return Long.parseLong(raw.trim()) * 1000L;
        } catch (NumberFormatException e) {
            return 20_000L;
        }
    }
}