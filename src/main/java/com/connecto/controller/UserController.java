package com.connecto.controller;

import com.connecto.model.Message;
import com.connecto.model.User;
import com.connecto.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.HashMap;
import java.util.Map;

@Controller
@CrossOrigin(origins = {"http://localhost:3000"})
public class UserController {

    private final SimpMessagingTemplate template;

    @Autowired
    private UserService userService;

    public UserController(SimpMessagingTemplate template) {
        this.template = template;
    }

    @MessageMapping("/send-message")
//    @SendTo("/topic/message-receive")
    public void sendMessage(@Payload Message message, SimpMessageHeaderAccessor headerAccessor) {
        template.convertAndSendToUser("", "/topic/message-receive/" + message.getTo(), message);
    }

    @MessageMapping("/add-user")//app/add-user
    @SendTo("/topic/public")
    public String addUser(@Payload String userId, SimpMessageHeaderAccessor headerAccessor) {
        return "...";
    }

    @MessageMapping("/friend_request")
//    @SendTo("/topic/message-receive")
    public void friendRequest(@Payload String userId, SimpMessageHeaderAccessor headerAccessor) {
        System.out.println("This is the userId from which message was sent " + userId);
        template.convertAndSendToUser(userId, "/topic/friend_request", "This is sent from server");
    }

    @GetMapping("/get-all-users")
    public ResponseEntity<?> getAllUsers(HttpServletRequest request) {
        try {
            User user = (User) request.getAttribute("user");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            Map<String, Object> response = userService.getAllUsers(user);
            if ((boolean) response.get("status")) {
                return ResponseEntity.status(200).body(response);
            }
            return ResponseEntity.status(400).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @GetMapping("/get-friends")
    public ResponseEntity<?> getFriends(HttpServletRequest request) {
        try {
            User user = (User) request.getAttribute("user");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorised");
            }
            return ResponseEntity.status(200).body(new HashMap<>() {{
                put("status", true);
                put("message", "Friends fetched Successfully");
                put("data", user.getFriends());
            }});

        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @GetMapping("/get-friend-requests")
    public ResponseEntity<?> getFriendRequests(HttpServletRequest request) {
        try {
            User user = (User) request.getAttribute("user");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorised");
            }
            Map<String, Object> response = userService.getFriendRequests(user);
            if ((boolean) response.get("status")) {
                return ResponseEntity.status(200).body(response);
            }
            return ResponseEntity.status(400).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
}
