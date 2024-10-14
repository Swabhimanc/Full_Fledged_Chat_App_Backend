package com.connecto.socketIO;

import com.connecto.enums.Status;
import com.connecto.model.User;
import com.connecto.services.implementation.AuthServiceImplementation;
import com.connecto.services.implementation.UserServiceImplementation;
import com.connecto.utilities.CustomUserDetails;
import com.connecto.utilities.security.JwtUtil;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

@Configuration
@EnableWebSecurity
public class SocketIOConfig {

    public static ConcurrentHashMap<String, SocketIOClient> clientMap = new ConcurrentHashMap<>();
    private final JwtUtil jwtUtil;
    private final AuthServiceImplementation authServiceImplementation;
    private final UserServiceImplementation userServiceImplementation;
    @Value("${socket-io-host}")
    private String socketHost;
    @Value("${socket-io-port}")
    private Integer socketPort;

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
        config.setOrigin("*");
        config.setContext("/ws");
        SocketIOServer server = new SocketIOServer(config);
        setupConnectionListeners(server);
        return server;
    }

    private void setupConnectionListeners(SocketIOServer server) {
        server.addConnectListener(socketIOClient -> {

            HandshakeData handshakeData = socketIOClient.getHandshakeData();
            Map<String, Object> authToken = (Map<String, Object>) handshakeData.getAuthToken();
            String token = authToken != null && authToken.get("token") != null ? authToken.get("token").toString() : null;

            if (handshakeData.getUrlParams().containsKey("token")) {
                token = handshakeData.getUrlParams().get("token").get(0);
            }
            try {
                if (token != null && token.startsWith("Bearer ")) {
                    token = token.substring(7);

                    String userId = jwtUtil.extractUserId(token);
                    if (jwtUtil.validateToken(token, userId)) {
                        CustomUserDetails userDetails = authServiceImplementation.loadUserByUserId(userId);
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        clientMap.put(userId, socketIOClient);
                        userServiceImplementation.setUserStatus(userId, Status.ONLINE);
                        socketIOClient.set("user", userDetails.getUser());
                        User user = socketIOClient.get("user");
                    } else {
                        System.out.println("Invalid JWT token");
                        throw new IllegalStateException("Invalid JWT token");
                    }
                } else {
                    System.out.println("Missing or Invalid Authorization Header");
                }
            } catch (Exception e) {
                System.out.println("Error processing WebSocket connection: " + e.getMessage());
            }
        });

        server.addDisconnectListener(socketIOClient -> {
            if (clientMap.containsValue(socketIOClient)) {
                User user = socketIOClient.get("user");
                try {
                    userServiceImplementation.setUserStatus(user.getId(), Status.OFFLINE);
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

}