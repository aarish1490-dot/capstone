package com.dhatchina.aarishmart.service.ai;

import java.util.List;
import java.util.Optional;

/**
 * Abstraction over the AI model backend. The assistant logic talks to this
 * interface only, so the whole feature keeps working when the external
 * provider is unavailable (the implementation is expected to return an empty
 * Optional instead of throwing).
 */
public interface AiProvider {

    /**
     * Generates an assistant reply for the given conversation.
     *
     * @param systemPrompt fixed server-side system prompt (never user-supplied,
     *                     never sent back to the client)
     * @param conversation  ordered chat turns (roles {@code user}/{@code model})
     * @return the reply text, or empty when the provider failed, timed out or
     *         returned no usable text
     */
    Optional<String> complete(String systemPrompt, List<ChatMessage> conversation);
}