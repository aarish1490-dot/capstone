package com.dhatchina.dhatchinamart.service.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AiProviderFactoryTest {

    @Test
    void mockIsTheDefault() {
        AiProvider provider = AiProviderFactory.create("mock", "", "gemini-1.5-flash", 20_000L);

        assertTrue(provider instanceof MockAiProvider);
    }

    @Test
    void geminiRequiresApiKey() {
        AiProvider provider = AiProviderFactory.create("gemini", "long-key-abc", "gemini-1.5-flash", 20_000L);

        assertTrue(provider instanceof GeminiAiProvider);
    }

    @Test
    void geminiWithoutKeyFallsBackToMock() {
        AiProvider provider = AiProviderFactory.create("gemini", "", "gemini-1.5-flash", 20_000L);

        assertTrue(provider instanceof MockAiProvider,
                "missing API key must not break the app - fall back to mock");
    }

    @Test
    void unknownProviderDefaultsToMock() {
        AiProvider provider = AiProviderFactory.create("wat", "", "gemini-1.5-flash", 20_000L);

        assertTrue(provider instanceof MockAiProvider);
    }
}