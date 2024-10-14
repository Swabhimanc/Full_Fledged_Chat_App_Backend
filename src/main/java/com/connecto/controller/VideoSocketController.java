package com.connecto.controller;

import com.connecto.enums.Status;
import com.connecto.enums.Verdict;
import com.connecto.services.VideoCallService;
import com.connecto.socketIO.SocketIOConfig;
import com.connecto.socketIO.SocketIOService;
import com.corundumstudio.socketio.SocketIOServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Controller
public class VideoSocketController {
    private final SimpMessagingTemplate template;

    @Autowired
    VideoCallService videoCallService;
    @Autowired
    SocketIOService socketIOService;
    @Autowired
    private SocketIOServer server;

    public VideoSocketController(SimpMessagingTemplate template) {
        this.template = template;
    }

    @MessageMapping("/start_video_call")
    public void startVideoCall(@Payload Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor) throws ExecutionException, InterruptedException {
        if (payload != null) {
            String from = payload.get("from").toString();
            String to = payload.get("to").toString();
            String roomID = payload.get("roomID").toString();
            Map<String, Object> response = videoCallService.startVideoCall(from, to, roomID);
            template.convertAndSendToUser(to, "/topic/video_call_notification", response);
            SocketIOConfig.clientMap.get(to).sendEvent("video_call_notification", response);
        }
    }

    @MessageMapping("/video_call_not_picked")
    public void handleVideoCallNotPicked(@Payload Map<String, Object> payload) throws ExecutionException, InterruptedException {
        if (!payload.isEmpty()) {
            String from = payload.get("from").toString();
            String to = payload.get("to").toString();
            videoCallService.updateCallRecord(to, from, Verdict.MISSED, Status.ENDED);
            template.convertAndSendToUser(to, "/topic/video_call_missed", payload);
            SocketIOConfig.clientMap.get(to).sendEvent("video_call_missed", payload);
        }
    }

    @MessageMapping("/video_call_accepted")
    public void handleVideoCallAccepted(@Payload Map<String, Object> payload) throws ExecutionException, InterruptedException {
        if (!payload.isEmpty()) {
            String from = payload.get("streamID").toString();
            String to = payload.get("userID").toString();
            videoCallService.updateCallRecord(to, from, Verdict.ACCEPTED, Status.ONGOING);
            template.convertAndSendToUser(from, "/topic/video_call_accepted", payload);
            SocketIOConfig.clientMap.get(from).sendEvent("video_call_accepted", payload);
        }
    }

    @MessageMapping("/end_video_call")
    public void handleAudioCallEnd(@Payload Map<String, Object> payload) throws ExecutionException, InterruptedException {
        if (!payload.isEmpty()) {
            String from = payload.get("streamID").toString();
            String to = payload.get("userID").toString();
            videoCallService.updateCallRecord(to, from, Verdict.ACCEPTED, Status.ENDED);
            template.convertAndSendToUser(from, "/topic/end_video_call", payload);
            SocketIOConfig.clientMap.get(from).sendEvent("end_video_call", payload);
        }
    }

    @MessageMapping("/video_call_denied")
    public void handleVideoCallDenied(@Payload Map<String, Object> payload) throws ExecutionException, InterruptedException {
        if (!payload.isEmpty()) {
            String from = payload.get("streamID").toString();
            String to = payload.get("userID").toString();
            videoCallService.updateCallRecord(to, from, Verdict.DENIED, Status.ENDED);
            template.convertAndSendToUser(from, "/topic/video_call_denied", payload);
            SocketIOConfig.clientMap.get(from).sendEvent("video_call_denied", payload);
        }
    }

    @MessageMapping("/user_is_busy_video_call")
    public void handleUserIsBusyVideoCall(@Payload Map<String, Object> payload) throws ExecutionException, InterruptedException {
        if (!payload.isEmpty()) {
            String from = payload.get("streamID").toString();
            String to = payload.get("userID").toString();
            videoCallService.updateCallRecord(to, from, Verdict.BUSY, Status.ENDED);
            template.convertAndSendToUser(from, "/topic/on_another_video_call", payload);
            SocketIOConfig.clientMap.get(from).sendEvent("on_another_video_call", payload);
        }
    }

    @PostConstruct
    public void setupEventListeners() {
        server.addEventListener("start_video_call", Map.class, ((client, payload, ackRequest) -> {
            if (payload != null) {
                String from = payload.get("from").toString();
                String to = payload.get("to").toString();
                String roomID = payload.get("roomID").toString();
                Map<String, Object> response = videoCallService.startVideoCall(from, to, roomID);
                socketIOService.sendToUser(to, "video_call_notification", response);
                template.convertAndSendToUser(to, "/topic/video_call_notification", response);
            }
        }));

        server.addEventListener("video_call_not_picked", Map.class, ((client, payload, ackRequest) -> {
            if (!payload.isEmpty()) {
                String from = payload.get("from").toString();
                String to = payload.get("to").toString();
                videoCallService.updateCallRecord(to, from, Verdict.MISSED, Status.ENDED);
                socketIOService.sendToUser(to, "video_call_missed", payload);
                template.convertAndSendToUser(to, "/topic/video_call_missed", payload);
            }
        }));

        server.addEventListener("video_call_accepted", Map.class, ((client, payload, ackRequest) -> {
            if (!payload.isEmpty()) {
                String from = payload.get("streamID").toString();
                String to = payload.get("userID").toString();
                videoCallService.updateCallRecord(to, from, Verdict.ACCEPTED, Status.ONGOING);
                socketIOService.sendToUser(from, "video_call_accepted", payload);
                template.convertAndSendToUser(from, "/topic/video_call_accepted", payload);
            }
        }));

        server.addEventListener("end_video_call", Map.class, ((client, payload, ackRequest) -> {
            if (!payload.isEmpty()) {
                String from = payload.get("streamID").toString();
                String to = payload.get("userID").toString();
                videoCallService.updateCallRecord(to, from, Verdict.ACCEPTED, Status.ENDED);
                socketIOService.sendToUser(from, "end_video_call", payload);
                template.convertAndSendToUser(from, "/topic/end_video_call", payload);
            }
        }));

        server.addEventListener("video_call_denied", Map.class, ((client, payload, ackRequest) -> {
            if (!payload.isEmpty()) {
                String from = payload.get("streamID").toString();
                String to = payload.get("userID").toString();
                videoCallService.updateCallRecord(to, from, Verdict.DENIED, Status.ENDED);
                socketIOService.sendToUser(from, "video_call_denied", payload);
                template.convertAndSendToUser(from, "/topic/video_call_denied", payload);
            }
        }));

        server.addEventListener("user_is_busy_video_call", Map.class, ((client, payload, ackRequest) -> {
            if (!payload.isEmpty()) {
                String from = payload.get("streamID").toString();
                String to = payload.get("userID").toString();
                videoCallService.updateCallRecord(to, from, Verdict.BUSY, Status.ENDED);
                socketIOService.sendToUser(from, "on_another_video_call", payload);
                template.convertAndSendToUser(from, "/topic/on_another_video_call", payload);
            }
        }));
    }
}
