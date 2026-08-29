package com.connecto.socketIO;

import com.connecto.enums.Status;
import com.connecto.model.User;
import com.connecto.services.implementation.AuthServiceImplementation;
import com.connecto.services.implementation.UserServiceImplementation;
import com.connecto.utilities.CustomUserDetails;
import com.connecto.utilities.security.JwtUtil;
import com.connecto.utilities.security.SessionCookieService;
import com.corundumstudio.socketio.AuthorizationResult;
import com.corundumstudio.socketio.HandshakeData;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.Transport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

@Configuration
@EnableWebSecurity
public class SocketIOConfig {

    public static ConcurrentHashMap<String, Set<SocketIOClient>> clientMap = new ConcurrentHashMap<>();
    private final JwtUtil jwtUtil;
    private final AuthServiceImplementation authServiceImplementation;
    private final UserServiceImplementation userServiceImplementation;
    @Value("${socket-io-host}")
    private String socketHost;
    @Value("${socket-io-port}")
    private Integer socketPort;
    @Value("${app.allowed-origins}")
    private String allowedOrigins;

    public SocketIOConfig(JwtUtil jwtUtil, UserServiceImplementation userServiceImplementation, AuthServiceImplementation authServiceImplementation) {
        this.jwtUtil = jwtUtil;
        this.authServiceImplementation = authServiceImplementation;
        this.userServiceImplementation = userServiceImplementation;
    }

    @Bean
    public SocketIOServer socketIOServer() {
        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname(socketHost);
        config.setPort(socketPort);
        config.setTransports(Transport.WEBSOCKET, Transport.POLLING);
        config.setContext("/ws");
        config.setOrigin(Arrays.stream(allowedOrigins.split(",")).map(String::trim).findFirst().orElse("http://localhost:3000"));
        config.setAuthorizationListener(handshakeData -> {
            String origin = handshakeData.getHttpHeaders().get("Origin");
            Set<String> origins = Set.copyOf(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
            String token = tokenFromCookies(handshakeData.getHttpHeaders().get("Cookie"));
            String userId = token == null ? null : jwtUtil.extractUserId(token);
            boolean authorized = (origin == null || origins.contains(origin))
                    && userId != null
                    && jwtUtil.isAccessToken(token)
                    && jwtUtil.validateToken(token, userId);
            return authorized ? AuthorizationResult.SUCCESSFUL_AUTHORIZATION : AuthorizationResult.FAILED_AUTHORIZATION;
        });
        SocketIOServer server = new SocketIOServer(config);
        setupConnectionListeners(server);
        return server;
    }

    private void setupConnectionListeners(SocketIOServer server) {
        server.addConnectListener(socketIOClient -> {

            HandshakeData handshakeData = socketIOClient.getHandshakeData();
            String token = tokenFromCookies(handshakeData.getHttpHeaders().get("Cookie"));
            try {
                if (token != null) {
                    String userId = jwtUtil.extractUserId(token);
                    if (jwtUtil.isAccessToken(token) && jwtUtil.validateToken(token, userId)) {
                        CustomUserDetails userDetails = authServiceImplementation.loadUserByUserId(userId);
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        clientMap.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(socketIOClient);
                        userServiceImplementation.setUserStatus(userId, Status.ONLINE);
                        socketIOClient.set("user", userDetails.getUser());
                        User user = socketIOClient.get("user");
                    } else {
                        socketIOClient.disconnect();
                        throw new IllegalStateException("Invalid JWT token");
                    }
                } else {
                    socketIOClient.disconnect();
                }
            } catch (Exception e) {
                socketIOClient.disconnect();
            }
        });

        server.addDisconnectListener(socketIOClient -> {
            User user = socketIOClient.get("user");
            if (user != null) {
                Set<SocketIOClient> clients = clientMap.get(user.getId());
                if (clients != null) {
                    clients.remove(socketIOClient);
                    if (clients.isEmpty()) {
                        clientMap.remove(user.getId(), clients);
                    }
                }
                try {
                    if (!clientMap.containsKey(user.getId())) {
                        userServiceImplementation.setUserStatus(user.getId(), Status.OFFLINE);
                    }
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private String tokenFromCookies(String cookieHeader) {
        if (cookieHeader == null) {
            return null;
        }
        return Arrays.stream(cookieHeader.split(";"))
                .map(String::trim)
                .filter(cookie -> cookie.startsWith(SessionCookieService.ACCESS_COOKIE + "="))
                .map(cookie -> cookie.substring(cookie.indexOf('=') + 1))
                .findFirst()
                .orElse(null);
    }

}
