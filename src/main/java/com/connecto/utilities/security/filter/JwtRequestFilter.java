package com.connecto.utilities.security.filter;

import com.connecto.services.implementation.AuthServiceImplementation;
import com.connecto.utilities.CustomUserDetails;
import com.connecto.utilities.security.JwtUtil;
import com.connecto.utilities.security.SessionCookieService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    JwtUtil jwtUtil;

    @Autowired
    AuthServiceImplementation authServiceImplementation;

    @Autowired
    SessionCookieService sessionCookieService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = null;
        String userId = null;
        String authorizationHeader = request.getHeader("Authorization");
        token = sessionCookieService.accessToken(request);

        if ((token == null || token.isBlank()) && authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            token = authorizationHeader.substring(7);
        }
        if (token!=null) {
            userId = jwtUtil.extractUserId(token);
        }
        if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            CustomUserDetails userDetails;
            try {
                userDetails = authServiceImplementation.loadUserByUserId(userId);
            } catch (Exception e) {
                throw new RuntimeException();
            }
            if (jwtUtil.isAccessToken(token) && jwtUtil.validateToken(token, userDetails.getUser().getId())) {
                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                request.setAttribute("user",userDetails.getUser());
            }
        }
        filterChain.doFilter(request, response);
    }
}
