package com.connecto.utilities.security.filter;

import com.connecto.services.implementation.AuthServiceImplementation;
import com.connecto.utilities.CustomUserDetails;
import com.connecto.utilities.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class JwtChannelInterceptor implements ChannelInterceptor {

    @Autowired
    JwtUtil jwtUtil;

    @Autowired
    AuthServiceImplementation authServiceImplementation;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    String userId = jwtUtil.extractUserId(token);
                    if (token != null && jwtUtil.validateToken(token, userId)) {
                        CustomUserDetails userDetails = authServiceImplementation.loadUserByUserId(userId);
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        accessor.getSessionAttributes().put("user", userDetails.getUser());
                        // Log successful connection
//                        System.out.println("WebSocket connected successfully for user: " + userDetails.getUsername());
                    } else {
                        System.out.println("Invalid JWT token");
                        throw new IllegalStateException("Invalid JWT token");
                    }
                } catch (Exception e) {
                    System.out.println("Error processing WebSocket connection: " + e.getMessage());
                    throw new IllegalStateException("WebSocket connection failed due to token validation error", e);
                }
            } else {
                System.out.println("Missing or invalid Authorization header");
                throw new IllegalStateException("Missing or invalid Authorization header");
            }
        }
        return message;
    }

}
