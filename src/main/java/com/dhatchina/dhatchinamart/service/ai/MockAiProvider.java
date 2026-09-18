package com.dhatchina.dhatchinamart.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * DEVELOPMENT-ONLY AI provider. Used when no real model is configured
 * (college MVP / local demo / missing API key). It answers with deterministic,
 * keyword-matched replies based on the FAQ so the assistant stays useful
 * without any external calls.
 *
 * <p>It never talks to a network. Tests can construct a failing instance to
 * exercise the provider-error and fallback paths.
 */
public class MockAiProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(MockAiProvider.class);

    private static final String HELP_LINE =
            "You can browse and search products, add them to your cart, "
                    + "check out with a mock payment, and track your orders. What would you like to do?";

    private final boolean failOnCall;

    public MockAiProvider() {
        this(false);
    }

    /**
     * @param failOnCall when true, every call returns empty to simulate a
     *                   provider outage (used to test fallback behaviour).
     */
    MockAiProvider(boolean failOnCall) {
        this.failOnCall = failOnCall;
    }

    @Override
    public Optional<String> complete(String systemPrompt, List<ChatMessage> conversation) {
        if (failOnCall) {
            log.warn("MockAiProvider simulated provider failure");
            return Optional.empty();
        }
        String lastUser = lastUserMessage(conversation);
        return Optional.of(replyFor(lastUser == null ? "" : lastUser));
    }

    private String lastUserMessage(List<ChatMessage> conversation) {
        for (int i = conversation.size() - 1; i >= 0; i--) {
            if (ChatMessage.ROLE_USER.equals(conversation.get(i).getRole())) {
                return conversation.get(i).getContent();
            }
        }
        return null;
    }

    private String replyFor(String message) {
        String text = message.toLowerCase();

        if (text.contains("review") || text.contains("rate") || text.contains("stars")) {
            return "You can review a product once your order for it is delivered. "
                    + "Open the order and use the 'Write a review' link to rate it 1-5 stars and leave a few words.";
        }
        if (text.contains("cart") || text.contains("add to") || text.contains("buy")) {
            return "Open the product you like and press 'Add to Cart'. You can adjust quantities in the cart "
                    + "before placing your order (the quantity is capped at the available stock).";
        }
        if (text.contains("pay") || text.contains("checkout") || text.contains("payment")) {
            return "Checkout uses a mock payment - a confirmation simulates the gateway, so no real money moves. "
                    + "Go to your cart and press 'Checkout', then confirm the demo payment.";
        }
        if (text.contains("order") || text.contains("track") || text.contains("status")) {
            return "Orders progress PENDING -> CONFIRMED -> SHIPPED -> DELIVERED. "
                    + "You can see the current status for every order on your Orders page.";
        }
        if (text.contains("seller") || text.contains("sell")) {
            return "Sellers get their own dashboard where they can add, edit and deactivate products and view "
                    + "their orders and sales stats. Register as a Seller to access it.";
        }
        if (text.contains("search") || text.contains("find") || text.contains("filter")
                || text.contains("electronics") || text.contains("books")
                || text.contains("clothing") || text.contains("home")
                || text.contains("accessories") || text.contains("product")) {
            return "Visit the Products page to search by keyword and filter by category. "
                    + "DhatchinaMart stocks Electronics, Accessories, Books, Clothing and Home items.";
        }
        if (text.contains("register") || text.contains("sign up") || text.contains("signup")
                || text.contains("login") || text.contains("account")) {
            return "Register as a Buyer or Seller from the 'Register' link, then sign in on the login page. "
                    + "You can also log in with your mobile number using a one-time password.";
        }
        if (text.contains("what is") || text.contains("about") || text.contains("help")
                || text.contains("where")) {
            return "DhatchinaMart is a multi-seller e-commerce marketplace. You can browse products from multiple "
                    + "sellers, place orders with a mock payment, and track them until delivery. " + HELP_LINE;
        }
        log.info("MockAiProvider: no FAQ keyword matched for: {}", message);
        return "I'm the DhatchinaMart assistant, focused on helping with shopping here. " + HELP_LINE;
    }
}