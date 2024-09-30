package com.connecto.controller;

import com.connecto.DTO.responseDTO.UserResponseDTO;
import com.connecto.model.Avatar;
import com.connecto.services.AuthService;
import com.connecto.services.GoogleAuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.mail.EmailException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private GoogleAuthService googleAuthService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, Object> request) throws RuntimeException {
        try {
            Map<String, Object> response = null;
            if (request.get("auth_type").equals("REGULAR")) {
                response = authService.login(request);
            } else if (request.get("auth_type").equals("GOOGLE")) {
                response = googleAuthService.login(request);
            }
            if (response != null && (boolean) response.get("status")) {
                return ResponseEntity.status(200).body(response);
            }
            return ResponseEntity.status(400).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> response = null;
            if (request.get("auth_type").equals("REGULAR")) {
                response = authService.register(request);
            } else if (request.get("auth_type").equals("GOOGLE")) {
                response = googleAuthService.register(request);
            }
            if ((boolean) response.get("status")) {
                return ResponseEntity.status(200).body(response);
            }
            return ResponseEntity.status(400).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/logout/{id}")
    public ResponseEntity<?> logout(@PathVariable String id, @RequestHeader("Authorization") String token) {
        try {
            authService.logout(id);
            return ResponseEntity.status(200).body(new HashMap<>() {{
                put("status", true);
                put("message", "User logged out successfully");
            }});
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, Object> request, HttpServletRequest httpServletRequest) {
        try {
            String URL = httpServletRequest.getHeader("Origin");
            Map<String, Object> response = authService.forgotPassword(request, URL);
            if ((boolean) response.get("status")) {
                return ResponseEntity.status(200).body(response);
            }
            return ResponseEntity.status(400).body(response);
        } catch (ExecutionException | InterruptedException | EmailException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Object object) {
        try {
            Map<String, Object> response = authService.resetPassword(object);
            if ((boolean) response.get("status")) {
                return ResponseEntity.status(200).body(response);
            }
            return ResponseEntity.status(400).body(response);
        } catch (ExecutionException | InterruptedException | EmailException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyOtp(@RequestBody Object object) {
        try {
            Map<String, Object> response = authService.verifyOtp(object);
            if ((boolean) response.get("status")) {
                return ResponseEntity.status(200).body(response);
            } else {
                return ResponseEntity.status(403).body(response);
            }
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/setAvatar")
    public ResponseEntity<?> setAvatar(@RequestBody Avatar avatarImage) {
        try {
            Object object = authService.setAvatar(avatarImage);
            return ResponseEntity.status(200).body(object);
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @GetMapping("/allUsers/{id}")
    public ResponseEntity<?> getAllUsers(@PathVariable String id) {
        try {
            List<UserResponseDTO> users = authService.getAllUsers(id);
            return ResponseEntity.ok(users);
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
}

