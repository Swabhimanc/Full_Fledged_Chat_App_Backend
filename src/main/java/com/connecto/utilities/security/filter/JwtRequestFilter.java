package com.connecto.utilities.security.filter;

import com.connecto.model.User;
import com.connecto.services.implementation.AuthServiceImplementation;
import com.connecto.services.implementation.UserServiceImplementation;
import com.connecto.utilities.CustomUserDetails;
import com.connecto.utilities.security.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    JwtUtil jwtUtil;

    @Autowired
    AuthServiceImplementation userServiceImplementation;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = null;
        String userId = null;
        String authorizationHeader = request.getHeader("Authorization");

        if(authorizationHeader==null){
            String query = request.getQueryString();
            if (query != null && query.contains("token=")) {
                token = query.split("token=")[1].split("&")[0];
            }
        }

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            token = authorizationHeader.substring(7);
        }
        if (token!=null) {
            userId = jwtUtil.extractUserId(token);
        }
        if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            CustomUserDetails userDetails;
            try {
                userDetails = userServiceImplementation.loadUserByUserId(userId);
            } catch (Exception e) {
                throw new RuntimeException();
            }
            if (jwtUtil.validateToken(token, userDetails.getUser().getId())) {
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
