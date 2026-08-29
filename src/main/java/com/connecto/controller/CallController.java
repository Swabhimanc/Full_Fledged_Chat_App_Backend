package com.connecto.controller;

import com.connecto.model.User;
import com.connecto.services.AudioCallService;
import com.connecto.services.VideoCallService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

}

