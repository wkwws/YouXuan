package com.example.secondhand_project.util;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IpUtilTest {

    @Test
    void shouldUseFirstAddressFromXForwardedFor() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 10.0.0.1");

        assertEquals("203.0.113.1", IpUtil.getClientIp(request));
    }

    @Test
    void shouldFallbackToXRealIpWhenForwardedForMissing() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Real-IP")).thenReturn("198.51.100.7");

        assertEquals("198.51.100.7", IpUtil.getClientIp(request));
    }

    @Test
    void shouldFallbackToRemoteAddr() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        assertEquals("127.0.0.1", IpUtil.getClientIp(request));
    }

    @Test
    void shouldReturnUnknownWhenRequestIsNull() {
        assertEquals("unknown", IpUtil.getClientIp(null));
    }

    @Test
    void shouldSkipUnknownHeaderValue() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("unknown");
        when(request.getHeader("X-Real-IP")).thenReturn("192.0.2.8");

        assertEquals("192.0.2.8", IpUtil.getClientIp(request));
    }
}
