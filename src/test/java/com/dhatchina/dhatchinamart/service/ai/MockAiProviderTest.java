package com.dhatchina.dhatchinamart.service.ai;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockAiProviderTest {

    private final MockAiProvider provider = new MockAiProvider();

    @Test
    void answersShoppingQuestions() {
        List<ChatMessage> conversation = List.of(
                new ChatMessage(ChatMessage.ROLE_USER, "How do I add something to my cart?"));

        Optional<String> reply = provider.complete("system prompt", conversation);

        assertTrue(reply.isPresent());
        assertTrue(reply.get().toLowerCase().contains("cart"));
    }

    @Test
    void answersCheckoutQuestions() {
        List<ChatMessage> conversation = List.of(
                new ChatMessage(ChatMessage.ROLE_USER, "how does checkout work"));

        Optional<String> reply = provider.complete("system prompt", conversation);

        assertTrue(reply.isPresent());
        assertTrue(reply.get().toLowerCase().contains("mock payment"));
    }

    @Test
    void answersUnrelatedTopicsWithStoreHelp() {
        List<ChatMessage> conversation = List.of(
                new ChatMessage(ChatMessage.ROLE_USER, "what is the weather"));

        Optional<String> reply = provider.complete("system prompt", conversation);

        assertTrue(reply.isPresent());
        assertTrue(reply.get().toLowerCase().contains("dhatchinamart"));
    }

    @Test
    void keepsPreviousModelTurnsInMind() {
        List<ChatMessage> conversation = List.of(
                new ChatMessage(ChatMessage.ROLE_USER, "hello"),
                new ChatMessage(ChatMessage.ROLE_MODEL, "Hi! How can I help with DhatchinaMart?"),
                new ChatMessage(ChatMessage.ROLE_USER, "Show me some books"));

        Optional<String> reply = provider.complete("system prompt", conversation);

        assertTrue(reply.isPresent());
        assertTrue(reply.get().toLowerCase().contains("books"));
    }

    @Test
    void failingMockReturnsEmpty() {
        AiProvider failing = new MockAiProvider(true);

        Optional<String> reply = failing.complete("system", List.of(
                new ChatMessage(ChatMessage.ROLE_USER, "hi")));

        assertTrue(reply.isEmpty(), "a failed provider call must yield empty for fallback handling");
    }

    @Test
    void predictableReplyForSameQuestion() {
        List<ChatMessage> conversation = List.of(
                new ChatMessage(ChatMessage.ROLE_USER, "when can I review a product?"));

        assertEquals(provider.complete("p", conversation), provider.complete("p", conversation));
    }
}