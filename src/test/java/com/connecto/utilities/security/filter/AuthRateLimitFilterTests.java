package com.connecto.utilities.security.filter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthRateLimitFilterTests {
    @Test
    void rejectsLoginRequestsAboveWindowLimit() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter();

        for (int attempt = 1; attempt <= 11; attempt++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
            request.setRemoteAddr("192.0.2.1");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, new MockFilterChain());

            assertEquals(attempt <= 10 ? 200 : 429, response.getStatus());
        }
    }
}
