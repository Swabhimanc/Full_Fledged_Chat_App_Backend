package com.connecto.utilities.security;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTests {
    private final JwtUtil jwtUtil = new JwtUtil("0123456789012345678901234567890123456789012345678901234567890123");

    @Test
    void distinguishesAccessAndRefreshTokens() {
        String access = jwtUtil.generateToken("user-1", Map.of("type", "access"));
        String refresh = jwtUtil.generateRefreshToken("user-1");

        assertTrue(jwtUtil.validateToken(access, "user-1"));
        assertTrue(jwtUtil.isAccessToken(access));
        assertFalse(jwtUtil.isRefreshToken(access));
        assertTrue(jwtUtil.isRefreshToken(refresh));
        assertFalse(jwtUtil.isAccessToken(refresh));
    }

    @Test
    void rejectsMalformedAndWrongUserTokens() {
        String access = jwtUtil.generateToken("user-1", Map.of("type", "access"));

        assertFalse(jwtUtil.validateToken(access, "user-2"));
        assertNull(jwtUtil.extractUserId("not-a-token"));
    }
}
