(function () {
    document.addEventListener("DOMContentLoaded", function () {
        var baseUrl = window.CONTEXT_PATH || "";

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
                sendCartAction("remove", productId, "0");
            });
        });

        function changeQuantity(btn, delta) {
            var row = btn.closest("tr");
            var productId = row.getAttribute("data-product-id");
            var input = row.querySelector(".qty-input");
            var stock = parseInt(row.getAttribute("data-stock"), 10);
            var current = parseInt(input.value, 10);
            var next = current + delta;
            if (next < 1 || next > stock) {
                return;
            }
            sendCartAction("update", productId, String(next));
        }

        function sendCartAction(action, productId, quantity) {
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
                .then(function (res) { return res.json(); })
                .then(function (data) {
                    if (!data.success) {
                        alert(data.error || "Something went wrong.");
                        window.location.reload();
                        return;
                    }
                    var row = document.querySelector('tr[data-product-id="' + productId + '"]');
                    if (row) {
                        var input = row.querySelector(".qty-input");
                        if (input) {
                            input.value = quantity;
                            var stock = parseInt(row.getAttribute("data-stock"), 10);
                            var minus = row.querySelector(".qty-minus");
                            var plus = row.querySelector(".qty-plus");
                            if (minus) { minus.disabled = parseInt(quantity, 10) <= 1; }
                            if (plus) { plus.disabled = parseInt(quantity, 10) >= stock; }
                        }
                        var price = parseFloat(row.getAttribute("data-price"));
                        var subtotal = row.querySelector(".subtotal");
                        if (subtotal) {
                            subtotal.textContent = "₹ " + (price * parseInt(quantity, 10)).toFixed(2);
                        }
                        if (action === "remove") {
                            row.remove();
                        }
                    }
                    var totalEl = document.getElementById("cart-total");
                    if (totalEl) {
                        totalEl.textContent = data.total;
                    }
                    if (action === "remove") {
                        window.location.reload();
                    }
                })
                .catch(function () {
                    alert("Something went wrong. Please try again.");
                });
        }
    });
})();
