package com.connecto.controller;

import com.connecto.enums.Status;
import com.connecto.enums.Verdict;
import com.connecto.services.AudioCallService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.concurrent.ExecutionException;

@Controller
public class AudioSocketController {
    private final SimpMessagingTemplate template;
    @Autowired
    AudioCallService audioCallService;

    public AudioSocketController(SimpMessagingTemplate template) {
        this.template = template;
    }

    @MessageMapping("/start_audio_call")
    public void startAudioCall(@Payload Map<String,Object> payload, SimpMessageHeaderAccessor headerAccessor) throws ExecutionException, InterruptedException {
        if(payload!=null) {
            String from = payload.get("from").toString();
            String to = payload.get("to").toString();
            String roomID = payload.get("roomID").toString();
            Map<String, Object> response = audioCallService.startAudioCall(from, to, roomID);
            template.convertAndSendToUser(to, "/topic/audio_call_notification", response);
        }
    }

    @MessageMapping("/audio_call_not_picked")
    public void handleAudioCallNotPicked(@Payload Map<String, Object> payload) throws ExecutionException, InterruptedException {
        if(!payload.isEmpty()){
            String from = payload.get("from").toString();
            String to = payload.get("to").toString();
            audioCallService.updateCallRecord(to, from, Verdict.MISSED, Status.ENDED);
            template.convertAndSendToUser(to, "/topic/audio_call_missed", payload);
        }
    }

    @MessageMapping("/audio_call_accepted")
    public void handleAudioCallAccepted(@Payload Map<String, Object> payload) throws ExecutionException, InterruptedException {
        if(!payload.isEmpty()) {
            String from = payload.get("streamID").toString();
            String to = payload.get("userID").toString();
            audioCallService.updateCallRecord(to, from, Verdict.ACCEPTED, null);
            template.convertAndSendToUser(from, "/topic/audio_call_accepted", payload);
        }
    }

    @MessageMapping("/end_audio_call")
    public void handleAudioCallEnd(@Payload Map<String, Object> payload) throws ExecutionException, InterruptedException {
        if(!payload.isEmpty()) {
            String from = payload.get("streamID").toString();
            String to = payload.get("userID").toString();
            audioCallService.updateCallRecord(to, from, Verdict.ACCEPTED, null);
            template.convertAndSendToUser(from, "/topic/end_audio_call", payload);
        }
    }

    @MessageMapping("/audio_call_denied")
    public void handleAudioCallDenied( @Payload Map<String, Object> payload) throws ExecutionException, InterruptedException {
        if(!payload.isEmpty()) {
            String from = payload.get("streamID").toString();
            String to = payload.get("userID").toString();
            audioCallService.updateCallRecord(to, from, Verdict.DENIED, Status.ENDED);
            template.convertAndSendToUser(from, "/topic/audio_call_denied", payload);
        }
    }

    @MessageMapping("/user_is_busy_audio_call")
    public void handleUserIsBusyAudioCall(@Payload Map<String, Object> payload) throws ExecutionException, InterruptedException {
        if(!payload.isEmpty()){
            String from = payload.get("streamID").toString();
            String to = payload.get("userID").toString();
            audioCallService.updateCallRecord(to, from, Verdict.BUSY, Status.ENDED);
            template.convertAndSendToUser(from, "/topic/on_another_audio_call", payload);
        }
    }
}
