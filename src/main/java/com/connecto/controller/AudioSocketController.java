package com.connecto.controller;

import com.connecto.enums.Status;
import com.connecto.enums.Verdict;
import com.connecto.model.User;
import com.connecto.services.AudioCallService;
import com.connecto.socketIO.SocketIOService;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class AudioSocketController {
    private final AudioCallService audioCallService;
    private final SocketIOServer server;
    private final SocketIOService socketIOService;

    public AudioSocketController(AudioCallService audioCallService, SocketIOServer server, SocketIOService socketIOService) {
        this.audioCallService = audioCallService;
        this.server = server;
        this.socketIOService = socketIOService;
    }

    @PostConstruct
    public void setupEventListeners() {
        server.addEventListener("start_audio_call", Map.class, (client, payload, ack) -> {
            String from = user(client).getId();
            String to = value(payload, "to");
            Map<String, Object> response = audioCallService.startAudioCall(from, to, value(payload, "roomID"));
            socketIOService.sendToUser(to, "audio_call_notification", response);
        });
        server.addEventListener("audio_call_not_picked", Map.class, (client, payload, ack) -> {
            String from = user(client).getId();
            String to = value(payload, "to");
            audioCallService.updateCallRecord(to, from, Verdict.MISSED, Status.ENDED);
            socketIOService.sendToUser(to, "audio_call_missed", payload);
        });
        callResponse("audio_call_accepted", "audio_call_accepted", Verdict.ACCEPTED, Status.ONGOING);
        callResponse("end_audio_call", "end_audio_call", Verdict.ACCEPTED, Status.ENDED);
        callResponse("audio_call_denied", "audio_call_denied", Verdict.DENIED, Status.ENDED);
        callResponse("user_is_busy_audio_call", "on_another_audio_call", Verdict.BUSY, Status.ENDED);
    }

    private void callResponse(String incomingEvent, String outgoingEvent, Verdict verdict, Status status) {
        server.addEventListener(incomingEvent, Map.class, (client, payload, ack) -> {
            String currentUser = user(client).getId();
            String otherUser = value(payload, "streamID");
            audioCallService.updateCallRecord(currentUser, otherUser, verdict, status);
            socketIOService.sendToUser(otherUser, outgoingEvent, payload);
        });
    }

    private User user(SocketIOClient client) {
        User user = client.get("user");
        if (user == null) throw new IllegalStateException("Unauthenticated socket");
        return user;
    }

    private String value(Map<String, Object> payload, String field) {
        if (payload.get(field) == null) throw new IllegalArgumentException(field + " is required");
        return payload.get(field).toString();
    }
}
