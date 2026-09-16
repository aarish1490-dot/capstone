<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="title" value="Login"/>
<%@ include file="/WEB-INF/jsp/fragments/header.jspf" %>

<div class="auth-card">
    <div class="auth-brand">Dhatchina<span>Mart</span></div>
    <h2 class="auth-title">Welcome Back</h2>

    <c:if test="${param.registered == '1'}">
        <div class="alert alert-success">Registration successful. Please login.</div>
    </c:if>
    <c:if test="${not empty error}">
        <div class="alert alert-error"><c:out value="${error}"/></div>
    </c:if>
    <c:if test="${not empty info}">
        <div class="alert alert-success"><c:out value="${info}"/></div>
    </c:if>
    <c:if test="${devMode}">
        <div class="alert alert-warning">
            <strong>DEVELOPMENT ONLY</strong> - mock SMS provider active, no real
            message was sent. Your OTP: <strong><c:out value="${devOtp}"/></strong>
        </div>
    </c:if>

    <c:choose>
        <c:when test="${otpStep == 'verify'}">
            <p class="otp-note">OTP sent to <strong><c:out value="${maskedMobile}"/></strong></p>
            <form action="${ctx}/otp/verify" method="post" id="otpVerifyForm">
                <input type="hidden" name="_csrf" value="${_csrfToken}"/>
                <input type="hidden" name="mobileNumber" value="<c:out value='${mobileNumber}'/>"/>
                <input type="hidden" name="otpCode" id="otpCode"/>
                <div class="otp-boxes" id="otpBoxes">
                    <input type="tel" maxlength="1" inputmode="numeric" pattern="[0-9]" class="otp-box" aria-label="OTP digit 1"/>
                    <input type="tel" maxlength="1" inputmode="numeric" pattern="[0-9]" class="otp-box" aria-label="OTP digit 2"/>
                    <input type="tel" maxlength="1" inputmode="numeric" pattern="[0-9]" class="otp-box" aria-label="OTP digit 3"/>
                    <input type="tel" maxlength="1" inputmode="numeric" pattern="[0-9]" class="otp-box" aria-label="OTP digit 4"/>
                    <input type="tel" maxlength="1" inputmode="numeric" pattern="[0-9]" class="otp-box" aria-label="OTP digit 5"/>
                    <input type="tel" maxlength="1" inputmode="numeric" pattern="[0-9]" class="otp-box" aria-label="OTP digit 6"/>
                </div>
                <button type="submit" class="btn btn-primary" style="width:100%;">Verify OTP</button>
            </form>
            <div class="resend-row">
                <button type="button" class="btn-link" id="resendBtn" disabled>Resend OTP</button>
                <span class="resend-timer" id="resendTimer">01:00</span>
            </div>
            <p class="auth-alt" style="margin-top:14px;text-align:center;">
                <a href="${ctx}/login">Use a different mobile number</a>
            </p>
        </c:when>
        <c:otherwise>
            <form action="${ctx}/login" method="post">
                <input type="hidden" name="_csrf" value="${_csrfToken}"/>
                <div class="form-group">
                    <label for="mobileNumber">Mobile Number</label>
                    <div class="mobile-input">
                        <span class="mobile-prefix">+91</span>
                        <input type="tel" id="mobileNumber" name="mobileNumber" maxlength="10"
                               pattern="[6-9][0-9]{9}" inputmode="numeric" required autofocus
                               placeholder="9876543210"/>
                    </div>
                    <div class="form-hint">We will send a 6-digit OTP to this number.</div>
                </div>
                <button type="submit" class="btn btn-primary" style="width:100%;">Send OTP</button>
            </form>
        </c:otherwise>
    </c:choose>

    <div class="auth-divider"><span>or</span></div>
    <p class="auth-alt" style="text-align:center;">
        <a href="#" id="pwToggle">Log in with email &amp; password</a>
    </p>
    <div id="pwForm" style="display:none;">
        <form action="${ctx}/login" method="post">
            <input type="hidden" name="_csrf" value="${_csrfToken}"/>
            <div class="form-group">
                <label for="email">Email</label>
                <input type="email" id="email" name="email" required/>
            </div>
            <div class="form-group">
                <label for="password">Password</label>
                <input type="password" id="password" name="password" required/>
            </div>
            <button type="submit" class="btn btn-secondary" style="width:100%;">Login</button>
        </form>
        <p class="auth-alt" style="margin-top:10px;text-align:center;">
            Demo: buyer@dhatchinamart.com / Buyer@123
        </p>
    </div>

    <p class="auth-alt" style="margin-top:16px;text-align:center;">
        New here? <a href="${ctx}/register">Create an account</a>
    </p>
</div>

<script>
(function () {
    var RESEND_COOLDOWN = 60; // seconds; the server enforces the same rule

    function setupOtpBoxes() {
        var boxes = document.querySelectorAll('#otpBoxes input');
        if (boxes.length === 0) {
            return;
        }
        var hidden = document.getElementById('otpCode');
        function sync() {
            var value = '';
            for (var i = 0; i < boxes.length; i++) {
                value += boxes[i].value;
            }
            hidden.value = value;
        }
        for (var i = 0; i < boxes.length; i++) {
            (function (index) {
                boxes[index].addEventListener('input', function () {
                    this.value = this.value.replace(/[^0-9]/g, '').slice(0, 1);
                    sync();
                    if (this.value && index < boxes.length - 1) {
                        boxes[index + 1].focus();
                    }
                });
                boxes[index].addEventListener('keydown', function (e) {
                    if (e.key === 'Backspace' && !this.value && index > 0) {
                        boxes[index - 1].focus();
                    }
                });
                boxes[index].addEventListener('paste', function (e) {
                    e.preventDefault();
                    var pasted = (e.clipboardData || window.clipboardData).getData('text').replace(/[^0-9]/g, '');
                    for (var j = 0; j < boxes.length; j++) {
                        boxes[j].value = pasted[j] || '';
                    }
                    sync();
                    boxes[Math.min(pasted.length, boxes.length - 1)].focus();
                });
            })(i);
        }
        sync();
    }

    function setupResendCountdown() {
        var btn = document.getElementById('resendBtn');
        var timer = document.getElementById('resendTimer');
        if (!btn) {
            return;
        }
        var remaining = RESEND_COOLDOWN;
        var display = setInterval(function () {
            remaining--;
            if (remaining <= 0) {
                clearInterval(display);
                btn.disabled = false;
                if (timer) {
                    timer.textContent = '';
                }
                return;
            }
            var mm = String(Math.floor(remaining / 60)).padStart(2, '0');
            var ss = String(remaining % 60).padStart(2, '0');
            if (timer) {
                timer.textContent = mm + ':' + ss;
            }
        }, 1000);
        btn.addEventListener('click', function () {
            btn.disabled = true;
            var form = document.createElement('form');
            form.method = 'post';
            form.action = window.location.origin + window.location.pathname.replace(/\/login.*/, '') + '/otp/resend';
            var mobile = document.querySelector('#otpVerifyForm input[name="mobileNumber"]');
            var input = document.createElement('input');
            input.type = 'hidden';
            input.name = 'mobileNumber';
            input.value = mobile ? mobile.value : '';
            form.appendChild(input);
            var meta = document.querySelector('meta[name="_csrf"]');
            if (meta) {
                var csrf = document.createElement('input');
                csrf.type = 'hidden';
                csrf.name = '_csrf';
                csrf.value = meta.content;
                form.appendChild(csrf);
            }
            document.body.appendChild(form);
            form.submit();
        });
    }

    function setupPasswordToggle() {
        var toggle = document.getElementById('pwToggle');
        var form = document.getElementById('pwForm');
        if (!toggle || !form) {
            return;
        }
        toggle.addEventListener('click', function (e) {
            e.preventDefault();
            var hidden = form.style.display === 'none';
            form.style.display = hidden ? 'block' : 'none';
            toggle.textContent = hidden ? 'Hide email & password login' : 'Log in with email & password';
        });
    }

    setupOtpBoxes();
    setupResendCountdown();
    setupPasswordToggle();
})();
</script>

<%@ include file="/WEB-INF/jsp/fragments/footer.jspf" %>
