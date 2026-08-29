package com.connecto.utilities.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.connecto.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class SessionCookieService {
    public static final String ACCESS_COOKIE = "connecto_access";
    public static final String REFRESH_COOKIE = "connecto_refresh";

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final boolean secure;
    private final String sameSite;

    public SessionCookieService(
            JwtUtil jwtUtil,
            UserRepository userRepository,
            @Value("${app.cookie.secure}") boolean secure,
            @Value("${app.cookie.same-site}") String sameSite
    ) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.secure = secure;
        this.sameSite = sameSite;
    }

    public void issue(HttpServletResponse response, String userId) {
        String refreshToken = jwtUtil.generateRefreshToken(userId);
        try {
            userRepository.updateUser(userId, "refreshTokenHash", hash(refreshToken));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to persist session", e);
        }
        addCookie(response, ACCESS_COOKIE, jwtUtil.generateToken(userId, java.util.Map.of("type", "access")), Duration.ofMinutes(15));
        addCookie(response, REFRESH_COOKIE, refreshToken, Duration.ofDays(7));
    }

    public void clear(HttpServletResponse response) {
        addCookie(response, ACCESS_COOKIE, "", Duration.ZERO);
        addCookie(response, REFRESH_COOKIE, "", Duration.ZERO);
    }

    public void revoke(HttpServletResponse response, String userId) {
        try {
            userRepository.updateUser(userId, "refreshTokenHash", null);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to revoke session", e);
        } finally {
            clear(response);
        }
    }

    public boolean isCurrentRefreshToken(String userId, String token) {
        try {
            String storedHash = userRepository.findUserById(userId).getString("refreshTokenHash");
            return storedHash != null && MessageDigest.isEqual(
                    storedHash.getBytes(StandardCharsets.UTF_8),
                    hash(token).getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            return false;
        }
    }

    public String accessToken(HttpServletRequest request) {
        return cookie(request, ACCESS_COOKIE);
    }

    public String refreshToken(HttpServletRequest request) {
        return cookie(request, REFRESH_COOKIE);
    }

    private String cookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return null;
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private void addCookie(HttpServletResponse response, String name, String value, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash token", e);
        }
    }
}
