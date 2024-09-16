package com.connecto.controller;

import com.connecto.model.User;
import com.connecto.services.AudioCallService;
import com.connecto.services.VideoCallService;
import com.connecto.utilities.TokenServerAssistant;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class CallController {
    @Autowired
    VideoCallService videoCallService;

    @Autowired
    AudioCallService audioCallService;

    @PostMapping("/start-video-call")
    public ResponseEntity<?> startVideoCall(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        try {
            User fromUser = (User) request.getAttribute("user");
            String from = fromUser.getId();
            String to = payload.get("to").toString();
            Map<String, Object> response = videoCallService.startVideoCall(from, to);
            if ((boolean) response.get("status")) {
                return ResponseEntity.status(200).body(response);
            }
            return ResponseEntity.status(400).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
    @PostMapping("/start-audio-call")
    public ResponseEntity<?> startAudioCall(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        try {
            User fromUser = (User) request.getAttribute("user");
            String from = fromUser.getId();
            String to = payload.get("to").toString();
            Map<String, Object> response = audioCallService.startAudioCall(from, to);
            if ((boolean) response.get("status")) {
                return ResponseEntity.status(200).body(response);
            }
            return ResponseEntity.status(400).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/generate-zego-token")
    public ResponseEntity<?> generateZegoToken(@RequestBody Map<String, Object> payload, HttpServletRequest request) throws Exception {
        try {
        String userId = payload.get("userId").toString();
        String room_id = payload.get("room_id").toString();
        String payloadObject = "{" +
                room_id + "," +
                "privilege:{" +
                "1: 1," +
                "2: 1," +
                "}," +
                "stream_id_list: null" +
                "}";
        TokenServerAssistant.TokenInfo token = TokenServerAssistant.generateToken04(159164384, userId, "14b5a9278d3c26dba6a6cc6976eb891e", 3600, null);
        return ResponseEntity.status(200).body(new HashMap<>() {{
            put("status", true);
            put("message", "Token Generated Successfully");
            put("token", token);
        }});
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
}
