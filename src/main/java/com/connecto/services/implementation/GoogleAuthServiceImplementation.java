package com.connecto.services.implementation;

import com.connecto.DTO.responseDTO.UserResponseDTO;
import com.connecto.enums.UserType;
import com.connecto.model.User;
import com.connecto.repositories.UserRepository;
import com.connecto.services.GoogleAuthService;
import com.connecto.utilities.security.JwtUtil;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.cloud.firestore.QuerySnapshot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class GoogleAuthServiceImplementation implements GoogleAuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final String clientId;

    @Autowired
    public GoogleAuthServiceImplementation(UserRepository userRepository, JwtUtil jwtUtil, @Value("${google.client.id}") String clientId) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.clientId = clientId;
    }

    private GoogleIdToken.Payload verifyGoogleToken(String idTokenString) throws Exception {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier
                .Builder(new NetHttpTransport(), new JacksonFactory())
                .setAudience(Collections.singletonList(clientId))
                .build();

        GoogleIdToken idToken = verifier.verify(idTokenString);
        if (idToken != null) {
            return idToken.getPayload();
        } else {
            throw new Exception("Invalid ID token.");
        }
    }

    @Override
    public Map<String, Object> login(Map<String, Object> request) throws Exception {
        GoogleIdToken.Payload payload = verifyGoogleToken(request.get("credential").toString());

        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String googleUserId = payload.getSubject();
        QuerySnapshot querySnapshot = userRepository.findUserByEmail(email);
        if (querySnapshot.isEmpty()) {
            return new HashMap<>() {{
                put("status", false);
                put("message", "User not found. Please Sign Up to continue");
            }};
        }

        UserResponseDTO userResponseDTO = querySnapshot.getDocuments().get(0).toObject(UserResponseDTO.class);
        return new HashMap<>() {{
            put("status", true);
            put("message", "User logged in successfully");
            put("token", jwtUtil.generateToken(userResponseDTO.getId(), new HashMap<>()));
            put("user_id", userResponseDTO.getId());
            put("user", userResponseDTO);
        }};
    }

    @Override
    public Map<String, Object> register(Map<String, Object> request) throws Exception {
        GoogleIdToken.Payload payload = verifyGoogleToken(request.get("credential").toString());

        String firstName = Objects.toString(payload.get("given_name"), "Google");
        String lastName = Objects.toString(payload.get("family_name"), "User");
        String email = payload.getEmail();
        String avatar = payload.get("picture")!=null ? payload.get("picture").toString():"";
        String googleUserId = payload.getSubject();
        boolean isEmailVerified = payload.getEmailVerified();
        QuerySnapshot querySnapshot = userRepository.findUserByEmail(email);
        if (!querySnapshot.isEmpty()) {
            return new HashMap<>() {{
                put("status", false);
                put("message", "Email already in use. Please login.");
            }};
        }
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setAvatar(avatar);
        user.setGoogleUserId(googleUserId);
        user.setVerified(isEmailVerified);
        user.setUserType(UserType.GOOGLE);
        userRepository.saveUser(user);

        UserResponseDTO userResponseDTO = userRepository
                .findUserByEmail(email)
                .getDocuments()
                .get(0).toObject(UserResponseDTO.class);
        if (userResponseDTO != null) {
            return new HashMap<>() {{
                put("status", true);
                put("message", "User Registered successfully");
                put("auth_type",userResponseDTO.getUserType());
                put("token", jwtUtil.generateToken(userResponseDTO.getId(), new HashMap<>()));
                put("user_id", userResponseDTO.getId());
                put("user", userResponseDTO);
            }};
        } else {
            return new HashMap<>() {{
                put("status", false);
                put("message", "Failed to Register");
            }};
        }
    }
}
