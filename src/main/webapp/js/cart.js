(function () {
    document.addEventListener("DOMContentLoaded", function () {
        var baseUrl = window.CONTEXT_PATH || "";
        var pending = false;

        document.querySelectorAll(".qty-plus").forEach(function (btn) {
            btn.addEventListener("click", function () {
                changeQuantity(btn, 1);
            });
        });

        document.querySelectorAll(".qty-minus").forEach(function (btn) {
            btn.addEventListener("click", function () {
                changeQuantity(btn, -1);
            });
        });

        document.querySelectorAll(".remove-item").forEach(function (btn) {
            btn.addEventListener("click", function () {
                var productId = btn.getAttribute("data-product-id");
                var name = btn.closest(".cart-row").querySelector(".cart-item-name");
                var label = name ? name.textContent.trim() : "this item";
                if (!window.confirm("Remove " + label + " from your cart?")) {
                    return;
                }
                sendCartAction("remove", productId, "0", btn);
            });
        });

        function changeQuantity(btn, delta) {
            if (pending) {
                return;
            }
            var row = btn.closest(".cart-row");
            var productId = row.getAttribute("data-product-id");
            var input = row.querySelector(".qty-input");
            var stock = parseInt(row.getAttribute("data-stock"), 10);
            var current = parseInt(input.value, 10);
            var next = current + delta;
            if (next < 1 || next > stock) {
                return;
            }
            sendCartAction("update", productId, String(next), btn);
        }

        function sendCartAction(action, productId, quantity, originBtn) {
            if (pending) {
                return;
            }
            pending = true;
            if (originBtn) {
                originBtn.disabled = true;
            }
            hideError();

            var params = new URLSearchParams();
            params.set("action", action);
            params.set("productId", productId);
            params.set("quantity", quantity);
            params.set("format", "json");

            var headers = {"Content-Type": "application/x-www-form-urlencoded"};
            var meta = document.querySelector('meta[name="_csrf"]');
            if (meta) {
                headers["X-CSRF-Token"] = meta.content;
            }

            fetch(baseUrl + "/cart", {
                method: "POST",
                headers: headers,
                body: params.toString()
            })
                .then(function (res) {
                    if (res.redirected) {
                        showError("Your session has expired. Please sign in again.");
                        window.location.href = baseUrl + "/login";
                        return null;
                    }
                    return res.json().then(function (data) {
                        return {status: res.status, data: data};
                    });
                })
                .then(function (result) {
                    if (result === null) {
                        return;
                    }
                    if (!result.data.success) {
                        showError(result.data.error || "Something went wrong. Please try again.");
                        if (result.status === 400) {
                            window.setTimeout(function () {
                                window.location.reload();
                            }, 900);
                        }
                        return;
                    }
                    hideError();
                    var row = document.querySelector('.cart-row[data-product-id="' + productId + '"]');
                    if (row) {
                        if (action === "remove") {
                            row.remove();
                        } else {
                            var input = row.querySelector(".qty-input");
                            if (input) {
                                input.value = quantity;
                            }
                            var minus = row.querySelector(".qty-minus");
                            var plus = row.querySelector(".qty-plus");
                            var stock = parseInt(row.getAttribute("data-stock"), 10);
                            if (minus) { minus.disabled = parseInt(quantity, 10) <= 1; }
                            if (plus) { plus.disabled = parseInt(quantity, 10) >= stock; }
                            var price = parseFloat(row.getAttribute("data-price"));
                            var subtotal = row.querySelector(".subtotal");
                            if (subtotal) {
                                subtotal.textContent = (price * parseInt(quantity, 10)).toFixed(2);
                            }
                        }
                    }
                    updateBadge(result.data.count);
                    var totalEl = document.getElementById("cart-total");
                    if (totalEl && result.data.total) {
                        totalEl.textContent = result.data.total;
                    }
                    if (action === "remove" && document.querySelectorAll(".cart-row").length === 0) {
                        window.location.reload();
                    }
                })
                .catch(function () {
                    showError("Something went wrong. Please try again.");
                })
                .finally(function () {
                    pending = false;
                    if (originBtn) {
                        originBtn.disabled = false;
                    }
                });
        }

        function updateBadge(count) {
            var badge = document.querySelector(".cart-count");
            if (badge) {
                if (count > 0) {
                    badge.textContent = count;
                    badge.style.display = "";
                } else {
                    badge.remove();
                }
            }
            var summaryCount = document.getElementById("summary-count");
            if (summaryCount) {
                summaryCount.textContent = count;
            }
        }

        function showError(message) {
            var banner = document.getElementById("cart-error");
            if (!banner) {
                banner = document.createElement("div");
                banner.id = "cart-error";
                banner.className = "alert alert-error";
                banner.setAttribute("role", "alert");
                var subtitle = document.querySelector(".page-subtitle");
                if (subtitle) {
                    subtitle.parentNode.insertBefore(banner, subtitle.nextSibling);
                } else {
                    document.querySelector(".container").insertBefore(banner, document.querySelector(".container").firstChild);
                }
            }
            banner.textContent = message;
            banner.style.display = "";
        }

        function hideError() {
            var banner = document.getElementById("cart-error");
            if (banner) {
                banner.style.display = "none";
            }
        }
    });
})();