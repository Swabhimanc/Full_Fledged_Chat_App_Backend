package com.connecto.controller;

import com.connecto.DTO.responseDTO.UserResponseDTO;
import com.connecto.DTO.requestDTO.ForgotPasswordRequestDTO;
import com.connecto.DTO.requestDTO.LoginRequestDTO;
import com.connecto.DTO.requestDTO.RegisterRequestDTO;
import com.connecto.DTO.requestDTO.ResetPasswordRequestDTO;
import com.connecto.DTO.requestDTO.VerifyOtpRequestDTO;
import com.connecto.model.Avatar;
import com.connecto.model.User;
import com.connecto.services.AuthService;
import com.connecto.services.GoogleAuthService;
import com.connecto.utilities.security.JwtUtil;
import com.connecto.utilities.security.SessionCookieService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.apache.commons.mail.EmailException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
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

    @Autowired
    private SessionCookieService sessionCookieService;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO loginRequest, HttpServletResponse httpResponse) throws RuntimeException {
        try {
            Map<String, Object> request = loginRequest.toMap();
            Map<String, Object> response = null;
            if (request.get("auth_type").equals("REGULAR")) {
                response = authService.login(request);
            } else if (request.get("auth_type").equals("GOOGLE")) {
                response = googleAuthService.login(request);
            }
            if (response != null && (boolean) response.get("status")) {
                establishSession(response, httpResponse);
                return ResponseEntity.status(200).body(response);
            }
            return ResponseEntity.status(400).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDTO registerRequest, HttpServletResponse httpResponse) {
        try {
            Map<String, Object> request = registerRequest.toMap();
            Map<String, Object> response = null;
            if (request.get("auth_type").equals("REGULAR")) {
                response = authService.register(request);
            } else if (request.get("auth_type").equals("GOOGLE")) {
                response = googleAuthService.register(request);
            }
            if ((boolean) response.get("status")) {
                if (response.get("user_id") != null) {
                    establishSession(response, httpResponse);
                }
                return ResponseEntity.status(200).body(response);
            }
            return ResponseEntity.status(400).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            User user = (User) request.getAttribute("user");
            if (user == null) {
                return ResponseEntity.status(401).build();
            }
            authService.logout(user.getId());
            sessionCookieService.revoke(response, user.getId());
            return ResponseEntity.status(200).body(new HashMap<>() {{
                put("status", true);
                put("message", "User logged out successfully");
            }});
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO request, HttpServletRequest httpServletRequest) {
        try {
            Map<String, Object> response = authService.forgotPassword(Map.of("email", request.email()), frontendUrl);
            if ((boolean) response.get("status")) {
                return ResponseEntity.status(200).body(response);
            }
            return ResponseEntity.status(400).body(response);
        } catch (ExecutionException | InterruptedException | EmailException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO request, HttpServletResponse httpResponse) {
        try {
            Map<String, Object> response = authService.resetPassword(Map.of(
                    "token", request.token(),
                    "newPassword", request.newPassword(),
                    "confirmPassword", request.confirmPassword()
            ));
            if ((boolean) response.get("status")) {
                establishSession(response, httpResponse);
                return ResponseEntity.status(200).body(response);
            }
            return ResponseEntity.status(400).body(response);
        } catch (ExecutionException | InterruptedException | EmailException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpRequestDTO request, HttpServletResponse httpResponse) {
        try {
            Map<String, Object> response = authService.verifyOtp(Map.of("email", request.email(), "otp", request.otp()));
            if ((boolean) response.get("status")) {
                establishSession(response, httpResponse);
                return ResponseEntity.status(200).body(response);
            } else {
                return ResponseEntity.status(403).body(response);
            }
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/setAvatar")
    public ResponseEntity<?> setAvatar(@RequestBody Avatar avatarImage, HttpServletRequest request) {
        try {
            User user = (User) request.getAttribute("user");
            if (user == null) {
                return ResponseEntity.status(401).build();
            }
            avatarImage.setId(user.getId());
            Object object = authService.setAvatar(avatarImage);
            return ResponseEntity.status(200).body(object);
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @GetMapping("/allUsers/{id}")
    public ResponseEntity<?> getAllUsers(@PathVariable String id, HttpServletRequest request) {
        try {
            User user = (User) request.getAttribute("user");
            if (user == null) {
                return ResponseEntity.status(401).build();
            }
            List<UserResponseDTO> users = authService.getAllUsers(user.getId());
            return ResponseEntity.ok(users);
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        User user = (User) request.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(new HashMap<>() {{
            put("status", true);
            put("user_id", user.getId());
            put("user", new UserResponseDTO(user));
        }});
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        String token = sessionCookieService.refreshToken(request);
        String userId = token == null ? null : jwtUtil.extractUserId(token);
        if (userId == null || !jwtUtil.isRefreshToken(token) || !jwtUtil.validateToken(token, userId)
                || !sessionCookieService.isCurrentRefreshToken(userId, token)) {
            sessionCookieService.clear(response);
            return ResponseEntity.status(401).body(Map.of("status", false, "message", "Session expired"));
        }
        sessionCookieService.issue(response, userId);
        return ResponseEntity.ok(Map.of("status", true));
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken());
    }

    private void establishSession(Map<String, Object> response, HttpServletResponse httpResponse) {
        String userId = response.get("user_id").toString();
        sessionCookieService.issue(httpResponse, userId);
        response.remove("token");
    }
}
