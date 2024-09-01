package com.connecto.utilities.security.filter;

import com.connecto.services.implementation.AuthServiceImplementation;
import com.connecto.utilities.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
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
        System.out.println(accessor.getCommand());
        System.out.println(accessor.getFirstNativeHeader("random"));
//        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
//            String authHeader = accessor.getFirstNativeHeader("Authorization");
//            if (authHeader != null && authHeader.startsWith("Bearer ")) {
//                String token = authHeader.substring(7);
//                String username = jwtUtil.extractUserId(token);
//                if (token != null && jwtUtil.validateToken(token, username)) {
//                    UserDetails userDetails = userServiceImplementation.loadUserByUsername(username);
//                    UsernamePasswordAuthenticationToken authentication =
//                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
//
//                    SecurityContextHolder.getContext().setAuthentication(authentication);
//                }
//            } else {
//                throw new IllegalStateException("Missing or invalid Authorization header");
//            }
//        }
        return message;
    }
}
