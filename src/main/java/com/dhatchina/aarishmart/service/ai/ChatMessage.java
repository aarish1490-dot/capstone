package com.dhatchina.aarishmart.service.ai;

import java.util.Objects;

/**
 * One server-side chat turn. Only this data (never raw passwords, OTPs or
 * session internals) is sent to an external AI provider.
 */
public class ChatMessage {

    public static final String ROLE_USER = "user";
    public static final String ROLE_MODEL = "model";

    private final String role;
    private final String content;

    public ChatMessage(String role, String content) {
        this.role = Objects.requireNonNull(role, "role");
        this.content = Objects.requireNonNull(content, "content");
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }
}