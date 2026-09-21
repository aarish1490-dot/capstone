package com.dhatchina.aarishmart.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CsrfUtilTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpSession session;

    @Test
    void generateTokenIs64CharHexAndNeverRepeats() {
        String first = CsrfUtil.generateToken();
        String second = CsrfUtil.generateToken();

        assertEquals(64, first.length());
        assertTrue(first.matches("[0-9a-f]{64}"));
        assertNotEquals(first, second);
    }

    @Test
    void isValidComparesInConstantTime() {
        assertTrue(CsrfUtil.isValid("token-123", "token-123"));
        assertFalse(CsrfUtil.isValid("token-123", "token-124"));
        assertFalse(CsrfUtil.isValid(null, "token-123"));
        assertFalse(CsrfUtil.isValid("token-123", null));
        assertFalse(CsrfUtil.isValid(null, null));
    }

    @Test
    void getTokenGeneratesAndStoresWhenMissing() {
        when(request.getSession(true)).thenReturn(session);

        String token = CsrfUtil.getToken(request);

        assertEquals(64, token.length());
        verify(session).setAttribute(CsrfUtil.SESSION_TOKEN_KEY, token);
    }

    @Test
    void getTokenReusesStoredToken() {
        when(request.getSession(true)).thenReturn(session);
        when(session.getAttribute(CsrfUtil.SESSION_TOKEN_KEY)).thenReturn("existing");

        assertEquals("existing", CsrfUtil.getToken(request));
    }

    @Test
    void peekTokenReturnsNullWithoutSession() {
        when(request.getSession(false)).thenReturn(null);

        assertNull(CsrfUtil.peekToken(request));
    }

    @Test
    void isValidForAcceptsHeader() {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(CsrfUtil.SESSION_TOKEN_KEY)).thenReturn("abc");
        when(request.getHeader(CsrfUtil.HEADER_NAME)).thenReturn("abc");

        assertTrue(CsrfUtil.isValidFor(request));
    }

    @Test
    void isValidForAcceptsFormFieldAsFallback() {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(CsrfUtil.SESSION_TOKEN_KEY)).thenReturn("abc");
        when(request.getParameter(CsrfUtil.FORM_FIELD)).thenReturn("abc");

        assertTrue(CsrfUtil.isValidFor(request));
    }

    @Test
    void isValidForRejectsMismatch() {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(CsrfUtil.SESSION_TOKEN_KEY)).thenReturn("abc");
        when(request.getHeader(CsrfUtil.HEADER_NAME)).thenReturn("xyz");

        assertFalse(CsrfUtil.isValidFor(request));
    }

    @Test
    void isValidForRejectsWhenNoTokenStored() {
        when(request.getSession(false)).thenReturn(session);

        assertFalse(CsrfUtil.isValidFor(request));
        verify(request, never()).getParameter(CsrfUtil.FORM_FIELD);
    }
}