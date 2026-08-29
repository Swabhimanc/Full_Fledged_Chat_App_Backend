package com.connecto.configs;

import com.connecto.utilities.security.filter.JwtRequestFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.DefaultCsrfToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class SecurityConfigTests {
    @Test
    void acceptsRawSpaCsrfHeaderValue() {
        SecurityConfig config = new SecurityConfig(mock(JwtRequestFilter.class));
        CsrfTokenRequestHandler handler = config.csrfTokenRequestHandler();
        HttpServletRequest request = new MockHttpServletRequest();
        ((MockHttpServletRequest) request).addHeader("X-XSRF-TOKEN", "raw-token");
        CsrfToken token = new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "raw-token");

        assertEquals("raw-token", handler.resolveCsrfTokenValue(request, token));
    }
}
