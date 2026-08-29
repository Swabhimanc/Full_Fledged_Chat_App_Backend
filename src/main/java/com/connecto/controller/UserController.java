package com.connecto.controller;

import com.connecto.model.User;
import com.connecto.services.MessageService;
import com.connecto.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private MessageService messageService;

    @GetMapping("/get-users")
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

    @GetMapping("/get-me")
    public ResponseEntity<?> getUserProfile(HttpServletRequest request) {
        try {
            User user = (User) request.getAttribute("user");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            Map<String, Object> response = userService.getUserProfile(user.getId());
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
            Map<String, Object> response = userService.getFriends(user);
            return ResponseEntity.status(200).body(response);

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

    @PostMapping("/update-me")
    public ResponseEntity<?> updateUserProfile(@RequestBody Map<String, Object> object, HttpServletRequest request) {
        try {
            User user = (User) request.getAttribute("user");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorised");
            }
            Set<String> editableFields = Set.of("firstName", "lastName", "avatar", "about");
            Map<String, Object> safeUpdates = object.entrySet().stream()
                    .filter(entry -> editableFields.contains(entry.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            if (safeUpdates.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("status", false, "message", "No editable fields provided"));
            }
            Map<String, Object> response = userService.updateUserProfile(user.getId(), safeUpdates);
            if ((boolean) response.get("status")) {
                return ResponseEntity.status(200).body(response);
            }
            return ResponseEntity.status(400).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @GetMapping("/get-call-logs")
    public ResponseEntity<?> getCallLogs(
            HttpServletRequest request,
            @RequestParam(value = "limit", defaultValue = "40") int limit
    ) {
        try {
            User fromUser = (User) request.getAttribute("user");
            if (fromUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            String userId = fromUser.getId();
            Map<String, Object> response = userService.getCallLogs(userId, limit);
            if ((boolean) response.get("status")) {
                return ResponseEntity.status(200).body(response);
            }
            return ResponseEntity.status(400).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @GetMapping("/get-direct-conversations")
    public ResponseEntity<?> getDirectConversations(HttpServletRequest request) {
        try {
            User fromUser = (User) request.getAttribute("user");
            if (fromUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            String userId = fromUser.getId();
            Map<String, Object> response = messageService.allDirectConversations(userId);
            if ((boolean) response.get("status")) {
                return ResponseEntity.status(200).body(response);
            }
            return ResponseEntity.status(400).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/delete-chat")
    public ResponseEntity<?> deleteChat(@RequestBody Map<String, Object> request, HttpServletRequest httpServletRequest) {
        try {
            User user = (User) httpServletRequest.getAttribute("user");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorised");
            }
            String room_id = request.get("room_id").toString();
            CompletableFuture<HashMap<String, Object>> response = messageService.deleteChat(room_id, user.getId());
            if ((boolean) response.get().get("status")) {
                return ResponseEntity.status(200).body(response.get());
            }
            return ResponseEntity.status(400).body(response.get());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
}
