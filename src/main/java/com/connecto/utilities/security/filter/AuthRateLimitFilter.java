package com.connecto.utilities.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AuthRateLimitFilter extends OncePerRequestFilter {
    private static final long WINDOW_MILLIS = Duration.ofMinutes(15).toMillis();
    private static final Map<String, Integer> LIMITS = Map.of(
            "/auth/login", 10,
            "/auth/register", 5,
            "/auth/verify", 10,
            "/auth/forgot-password", 5,
            "/auth/reset-password", 5
    );

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Integer limit = LIMITS.get(request.getRequestURI());
        if (limit == null || !"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        long now = System.currentTimeMillis();
        String key = clientAddress(request) + ':' + request.getRequestURI();
        Window window = windows.compute(key, (ignored, current) ->
                current == null || now - current.startedAt >= WINDOW_MILLIS
                        ? new Window(now, 1)
                        : new Window(current.startedAt, current.count + 1));

        if (windows.size() > 10_000) {
            windows.entrySet().removeIf(entry -> now - entry.getValue().startedAt >= WINDOW_MILLIS);
        }

        if (window.count > limit) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"status\":false,\"message\":\"Too many requests. Try again later.\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String clientAddress(HttpServletRequest request) {
        String realIp = request.getHeader("X-Real-IP");
        return realIp == null || realIp.isBlank() ? request.getRemoteAddr() : realIp;
    }

    private record Window(long startedAt, int count) {
    }
}
