(function () {
    'use strict';

    var form = document.getElementById('checkoutForm');
    if (!form) {
        return;
    }

    form.addEventListener('submit', function () {
        var btn = document.getElementById('checkoutBtn');
        if (!btn || btn.disabled) {
            return;
        }
        btn.disabled = true;
        btn.textContent = 'Placing order...';
    });
})();