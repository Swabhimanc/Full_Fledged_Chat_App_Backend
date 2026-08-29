package com.connecto.controller;

import com.connecto.model.User;
import com.connecto.socketIO.SocketIOService;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class WebRTCSocketController {
    private final SocketIOServer server;
    private final SocketIOService socketIOService;

    public WebRTCSocketController(SocketIOServer server, SocketIOService socketIOService) {
        this.server = server;
        this.socketIOService = socketIOService;
    }

    @PostConstruct
    public void setupEventListeners() {
        relayEvent("webrtc_offer");
        relayEvent("webrtc_answer");
        relayEvent("webrtc_ice_candidate");
    }

    private void relayEvent(String event) {
        server.addEventListener(event, Map.class, (client, payload, ack) -> {
            User user = client.get("user");
            if (user == null) throw new IllegalStateException("Unauthenticated socket");
            String to = payload.get("to") == null ? null : payload.get("to").toString();
            if (to == null) throw new IllegalArgumentException("to is required");
            payload.put("from", user.getId());
            socketIOService.sendToUser(to, event, payload);
        });
    }
}
