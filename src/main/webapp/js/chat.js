(function () {
    'use strict';

    document.addEventListener("DOMContentLoaded", function () {
        var launcher = document.getElementById("chatLauncher");
        var panel = document.getElementById("chatPanel");
        var messages = document.getElementById("chatMessages");
        var form = document.getElementById("chatForm");
        var input = document.getElementById("chatInput");
        var sendBtn = document.getElementById("chatSend");
        var minimizer = document.getElementById("chatMinimize");

        if (!launcher || !panel || !form || !input || !messages) {
            return;
        }

        var baseUrl = window.CONTEXT_PATH || "";
        var pending = false;
        var open = false;

        launcher.addEventListener("click", function () {
            open = !open;
            renderState();
        });
        minimizer.addEventListener("click", function () {
            open = false;
            renderState();
        });
        form.addEventListener("submit", function (event) {
            event.preventDefault();
            var text = input.value.trim();
            if (text === "" || pending) {
                return;
            }
            sendMessage(text);
        });

        function renderState() {
            panel.hidden = !open;
            launcher.classList.toggle("chat-open", open);
            launcher.setAttribute("aria-expanded", String(open));
            if (open) {
                input.focus();
                scrollToBottom();
            } else {
                input.blur();
            }
        }

        function sendMessage(text) {
            pending = true;
            sendBtn.disabled = true;
            input.disabled = true;

            addMessage("user", text);
            input.value = "";
            addTyping();

            var csrf = "";
            var meta = document.querySelector('meta[name="_csrf"]');
            if (meta) {
                csrf = meta.content;
            }

            var headers = {"Content-Type": "application/json"};
            if (csrf) {
                headers["X-CSRF-Token"] = csrf;
            }

            fetch(baseUrl + "/api/chat", {
                method: "POST",
                headers: headers,
                body: JSON.stringify({message: text})
            })
                .then(function (res) {
                    return res.json().then(function (data) {
                        return {status: res.status, data: data};
                    });
                })
                .then(function (result) {
                    removeTyping();
                    if (!result.data.success) {
                        addMessage("bot", result.data.error || "Something went wrong. Please try again.");
                        return;
                    }
                    addMessage("bot", result.data.data.reply);
                })
                .catch(function () {
                    removeTyping();
                    addMessage("bot", "Something went wrong. Please try again.");
                })
                .finally(function () {
                    pending = false;
                    sendBtn.disabled = false;
                    input.disabled = false;
                    input.focus();
                    scrollToBottom();
                });
        }

        function addMessage(who, text) {
            var wrap = document.createElement("div");
            wrap.className = "chat-msg " + (who === "user" ? "chat-msg-user" : "chat-msg-bot");
            var bubble = document.createElement("div");
            bubble.className = "chat-bubble";
            bubble.textContent = text;
            wrap.appendChild(bubble);
            messages.appendChild(wrap);
            scrollToBottom();
        }

        function addTyping() {
            var wrap = document.createElement("div");
            wrap.className = "chat-msg chat-msg-bot chat-typing";
            wrap.textContent = "AarishMart is typing...";
            wrap.id = "chatTyping";
            messages.appendChild(wrap);
            scrollToBottom();
        }

        function removeTyping() {
            var typing = document.getElementById("chatTyping");
            if (typing) {
                typing.remove();
            }
        }

        function scrollToBottom() {
            messages.scrollTop = messages.scrollHeight;
        }
    });
})();