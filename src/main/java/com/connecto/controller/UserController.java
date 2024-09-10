package com.connecto.controller;

import com.connecto.model.User;
import com.connecto.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

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
        //firstName: user?.firstName,
        //    about: user?.about,
        //    avatar: ``,
        try {
            User user = (User) request.getAttribute("user");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorised");
            }
            Map<String, Object> response = userService.updateUserProfile(user.getId(),object);
            if ((boolean) response.get("status")) {
                return ResponseEntity.status(200).body(response);
            }
            return ResponseEntity.status(400).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
}
