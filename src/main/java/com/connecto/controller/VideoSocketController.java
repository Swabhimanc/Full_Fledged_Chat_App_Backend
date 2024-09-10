package com.connecto.controller;

import com.connecto.enums.Status;
import com.connecto.enums.Verdict;
import com.connecto.services.VideoCallService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.concurrent.ExecutionException;

@Controller
public class VideoSocketController {
    private final SimpMessagingTemplate template;
    @Autowired
    VideoCallService videoCallService;

    public VideoSocketController(SimpMessagingTemplate template) {
        this.template = template;
    }

    @MessageMapping("/start_video_call")
    public void startVideoCall(@Payload Map<String,Object> payload, SimpMessageHeaderAccessor headerAccessor) throws ExecutionException, InterruptedException {
        if(payload!=null) {
            String from = payload.get("from").toString();
            String to = payload.get("to").toString();
            String roomID = payload.get("roomID").toString();
            Map<String, Object> response = videoCallService.startVideoCall(from, to, roomID);
            template.convertAndSendToUser(to, "/topic/video_call_notification", response);
        }
    }

    @MessageMapping("/video_call_not_picked")
    public void handleVideoCallNotPicked(@Payload Map<String, Object> payload) throws ExecutionException, InterruptedException {
        if(!payload.isEmpty()){
            String from = payload.get("from").toString();
            String to = payload.get("to").toString();
            videoCallService.updateCallRecord(to, from, Verdict.MISSED, Status.ENDED);
            template.convertAndSendToUser(to, "/topic/video_call_missed", payload);
        }
    }

    @MessageMapping("/video_call_accepted")
    public void handleVideoCallAccepted(@Payload Map<String, Object> payload) throws ExecutionException, InterruptedException {
        if(!payload.isEmpty()) {
            String from = payload.get("from").toString();
            String to = payload.get("to").toString();
            videoCallService.updateCallRecord(to, from, Verdict.ACCEPTED, null);
            template.convertAndSendToUser(from, "/topic/video_call_accepted", payload);
        }
    }

    @MessageMapping("/video_call_denied")
    public void handleVideoCallDenied(@Payload Map<String, Object> payload) throws ExecutionException, InterruptedException {
        if(!payload.isEmpty()) {
            String from = payload.get("from").toString();
            String to = payload.get("to").toString();
            videoCallService.updateCallRecord(to, from, Verdict.DENIED, Status.ENDED);
            template.convertAndSendToUser(from, "/topic/video_call_denied", payload);
        }
    }

    @MessageMapping("/user_is_busy_video_call")
    public void handleUserIsBusyVideoCall(@Payload Map<String, Object> payload) throws ExecutionException, InterruptedException {
        if(!payload.isEmpty()){
            String from = payload.get("from").toString();
            String to = payload.get("to").toString();
            videoCallService.updateCallRecord(to, from, Verdict.BUSY, Status.ENDED);
            template.convertAndSendToUser(from, "/topic/on_another_video_call", payload);
        }
    }
}
