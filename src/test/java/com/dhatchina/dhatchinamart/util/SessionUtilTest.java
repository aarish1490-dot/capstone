package com.dhatchina.dhatchinamart.util;

import com.dhatchina.dhatchinamart.model.User;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionUtilTest {

    @Test
    void getUserReturnsNullWhenThereIsNoSession() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession(false)).thenReturn(null);

        assertNull(SessionUtil.getUser(request));
        verify(request).getSession(false);
    }

    @Test
    void getUserReadsUserFromExistingSession() {
        User user = new User();
        user.setId(7L);
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute(SessionUtil.SESSION_USER)).thenReturn(user);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession(false)).thenReturn(session);

        assertEquals(7L, SessionUtil.getUser(request).getId());
    }

    @Test
    void getUserNeverCreatesASession() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession(false)).thenReturn(null);

        SessionUtil.getUser(request);

        verify(request, never()).getSession(true);
    }

    @Test
    void isLoggedInReflectsPresenceOfUser() {
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute(SessionUtil.SESSION_USER)).thenReturn(new User());
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession(false)).thenReturn(session);

        assertTrue(SessionUtil.isLoggedIn(request));

        when(request.getSession(false)).thenReturn(null);
        assertFalse(SessionUtil.isLoggedIn(request));
    }

    @Test
    void clientIpUsesFirstHopOfForwardedForHeader() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.9, 10.0.0.1, 10.0.0.2");

        assertEquals("203.0.113.9", SessionUtil.clientIp(request));
    }

    @Test
    void clientIpFallsBackToRemoteAddrWithoutHeader() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        assertEquals("127.0.0.1", SessionUtil.clientIp(request));
    }

    @Test
    void clientIpIgnoresBlankForwardedHeader() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("   ");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        assertEquals("127.0.0.1", SessionUtil.clientIp(request));
    }

    @Test
    void clientIpHandlesWhitespaceAroundForwardedEntry() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("  203.0.113.99  ,10.0.0.1");

        assertEquals("203.0.113.99", SessionUtil.clientIp(request));
    }
}