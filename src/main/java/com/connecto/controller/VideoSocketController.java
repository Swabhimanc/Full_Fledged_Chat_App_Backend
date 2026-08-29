package com.connecto.controller;

import com.connecto.enums.Status;
import com.connecto.enums.Verdict;
import com.connecto.model.User;
import com.connecto.services.VideoCallService;
import com.connecto.socketIO.SocketIOService;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class VideoSocketController {
    private final VideoCallService videoCallService;
    private final SocketIOServer server;
    private final SocketIOService socketIOService;

    public VideoSocketController(VideoCallService videoCallService, SocketIOServer server, SocketIOService socketIOService) {
        this.videoCallService = videoCallService;
        this.server = server;
        this.socketIOService = socketIOService;
    }

    @PostConstruct
    public void setupEventListeners() {
        server.addEventListener("start_video_call", Map.class, (client, payload, ack) -> {
            String from = user(client).getId();
            String to = value(payload, "to");
            Map<String, Object> response = videoCallService.startVideoCall(from, to, value(payload, "roomID"));
            socketIOService.sendToUser(to, "video_call_notification", response);
        });
        server.addEventListener("video_call_not_picked", Map.class, (client, payload, ack) -> {
            String from = user(client).getId();
            String to = value(payload, "to");
            videoCallService.updateCallRecord(to, from, Verdict.MISSED, Status.ENDED);
            socketIOService.sendToUser(to, "video_call_missed", payload);
        });
        callResponse("video_call_accepted", "video_call_accepted", Verdict.ACCEPTED, Status.ONGOING);
        callResponse("end_video_call", "end_video_call", Verdict.ACCEPTED, Status.ENDED);
        callResponse("video_call_denied", "video_call_denied", Verdict.DENIED, Status.ENDED);
        callResponse("user_is_busy_video_call", "on_another_video_call", Verdict.BUSY, Status.ENDED);
    }

    private void callResponse(String incomingEvent, String outgoingEvent, Verdict verdict, Status status) {
        server.addEventListener(incomingEvent, Map.class, (client, payload, ack) -> {
            String currentUser = user(client).getId();
            String otherUser = value(payload, "streamID");
            videoCallService.updateCallRecord(currentUser, otherUser, verdict, status);
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
