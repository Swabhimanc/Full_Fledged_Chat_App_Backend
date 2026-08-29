package com.connecto.services.implementation;

import com.connecto.DTO.responseDTO.UserResponseDTO;
import com.connecto.model.Avatar;
import com.connecto.model.User;
import com.connecto.repositories.UserRepository;
import com.connecto.services.AuthService;
import com.connecto.services.OTPService;
import com.connecto.utilities.CustomUserDetails;
import com.connecto.utilities.EmailService;
import com.connecto.utilities.PasswordResetTokenGenerator;
import com.connecto.utilities.security.JwtUtil;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import org.apache.commons.mail.EmailException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
public class AuthServiceImplementation implements AuthService {
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Autowired
    OTPService otpService;

    @Autowired
    public AuthServiceImplementation(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Map<String, Object> register(Map<String, Object> reqObj) throws ExecutionException, InterruptedException, EmailException, IOException {
        String rawPassword = Objects.toString(reqObj.get("password"), "");
        if (rawPassword.length() < 8) {
            return Map.of("status", false, "message", "Password must be at least 8 characters long");
        }
        reqObj.replace("password", passwordEncoder.encode(rawPassword));

        // Email check logic...
        QuerySnapshot emailCheck = userRepository.findUserByEmail((String) reqObj.get("email"));
        if (!emailCheck.isEmpty()) {
            User user = emailCheck.getDocuments().get(0).toObject(User.class);

            if (user.isVerified()) {
                // Email is already in use and verified
                return new HashMap<>() {{
                    put("status", false);
                    put("message", "Email already in use. Please login");
                }};
            } else {
                // Email exists but is not verified, update the existing user
                user.updateFields(reqObj);
                userRepository.updateUser(user.getId(), reqObj);
                // Generate and send OTP
                return otpService.generateOtp(user);
            }
        } else {
            // Email does not exist, create a new user
            User user = new User(reqObj);
            List<String> errors = User.validate(user);

            if (!errors.isEmpty()) {
                return new HashMap<>() {{
                    put("status", false);
                    put("message", errors);
                }};
            }
            userRepository.saveUser(user);
            return otpService.generateOtp(user);
        }
    }

    @Override
    public Map<String, Object> login(Map<String, Object> reqObject) throws ExecutionException, InterruptedException {
        String requestEmail = reqObject.get("email").toString();
        String requestPassword = reqObject.get("password").toString();

        QuerySnapshot userSnapshot = userRepository.findUserByEmail(requestEmail);

        User user = userSnapshot.isEmpty() ? null : userSnapshot.getDocuments().get(0).toObject(User.class);
        if (user != null && passwordEncoder.matches(requestPassword, user.getPassword())) {
            if (user.isVerified()) {
                UserResponseDTO userResponseDTO = new UserResponseDTO(user);
                return new HashMap<>() {{
                    put("status", true);
                    put("message", "User logged in successfully");
                    put("token", jwtUtil.generateToken(user.getId(), new HashMap<>()));
                    put("user_id", user.getId());
                    put("user", userResponseDTO);
                }};
            } else {
                return new HashMap<>() {{
                    put("status", false);
                    put("message", "User is not Verified. Please Complete verification.");
                }};
            }
        } else {
            return new HashMap<>() {{
                put("status", false);
                put("message", "Invalid Username or Password");
            }};
        }
    }

    @Override
    public void logout(String userId) throws ExecutionException, InterruptedException {
        userRepository.updateUser(userId, "userLoggedIn", false);
    }

    @Override
    public Object setAvatar(Avatar avatar) throws ExecutionException, InterruptedException {
        userRepository.updateUser(avatar.getId(), "avatarImageSet", true);
        userRepository.updateUser(avatar.getId(), "avatarImage", avatar.getImage());
        DocumentSnapshot userSnapshot = userRepository.findUserById(avatar.getId());
        return new HashMap<>() {{
            put("status", userSnapshot.get("avatarImageSet"));
            put("image", userSnapshot.get("avatarImage"));
        }};
    }

    @Override
    public List<UserResponseDTO> getAllUsers(String currentUserId) throws ExecutionException, InterruptedException {
        List<UserResponseDTO> users = new ArrayList<>();
        List<QueryDocumentSnapshot> usersSnapshot = userRepository.getAllUsers();

        usersSnapshot.forEach(doc -> {
            if (!doc.getId().equals(currentUserId)) {
                UserResponseDTO user = doc.toObject(UserResponseDTO.class);
                users.add(user);
            }
        });

        return users;
    }

    public CustomUserDetails loadUserByUserId(String userId) throws UsernameNotFoundException {
        User user = null;
        try {
            user = userRepository.findUserById(userId).toObject(User.class);
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return new CustomUserDetails(user);
    }

    @Override
    public Map<String, Object> forgotPassword(Map<String, Object> request,String URL) throws ExecutionException, InterruptedException {
        String requestEmail = request.get("email").toString();
        QuerySnapshot userSnapshot = userRepository.findUserByEmail(requestEmail);
        if (userSnapshot.isEmpty()) {
            return new HashMap<>() {{
                put("status", true);
                put("message", "If the account exists, a password reset link has been sent.");
            }};
        }
        String userId = userSnapshot.getDocuments().get(0).getId();
        String resetToken = PasswordResetTokenGenerator.generateRandomToken();
        userRepository.updateUser(userId, new HashMap<>() {{
            put("passwordResetToken", hashToken(resetToken));
            put("passwordResetExpires", new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10)));
        }});
        String resetUrl = String.format(URL+"/auth/new-password/?token=%s", resetToken);
        try {
            EmailService.sendResetLink(userSnapshot.getDocuments().get(0).getData().get("firstName").toString(), requestEmail, resetUrl);
        } catch (Exception e) {
            userRepository.updateUser(userId, new HashMap<>() {{
                put("passwordResetToken", null);
                put("passwordResetExpires", null);
            }});
            return new HashMap<>() {{
                put("status", false);
                put("message", "Something went wrong");
            }};
        }

        return new HashMap<>() {{
            put("status", true);
            put("message", "Password reset URL sent to your registered email.");
        }};
    }

    @Override
    public Map<String, Object> resetPassword(Object object) throws ExecutionException, InterruptedException, EmailException {
        Map<String, Object> request = (Map<String, Object>) object;
        String resetToken = request.get("token").toString();
        String password = request.get("newPassword").toString();
        String confirmPassword = request.get("confirmPassword").toString();
        if (!password.equals(confirmPassword)) {
            return new HashMap<>() {{
                put("status", false);
                put("message", "Password and Confirm passwords don't match.");
            }};
        }
        QuerySnapshot userSnapshot = userRepository.getUsersRef().whereEqualTo("passwordResetToken", hashToken(resetToken))
                .whereGreaterThan("passwordResetExpires", Timestamp.now()).get().get();
        if (userSnapshot.isEmpty()) {
            return new HashMap<>() {{
                put("status", false);
                put("message", "Token is invalid or expired");
            }};
        }
        User user = userSnapshot.getDocuments().get(0).toObject(User.class);
        UserResponseDTO userResponseDTO = userRepository.updateUser(user.getId(), new HashMap<>() {{
            put("passwordResetToken", null);
            put("passwordResetExpires", null);
            ;
            put("passwordChangedAt", new Date());
            put("password", passwordEncoder.encode(password));
        }});

        EmailService.sendResetConfirmation(user.getEmail());
        return new HashMap<>() {{
            put("status", true);
            put("message", "Password reset successfully");
            put("token", jwtUtil.generateToken(user.getId(), new HashMap<>()));
            put("user_id", user.getId());
            put("user", userResponseDTO);
        }};
    }

    @Override
    public Map<String, Object> verifyOtp(Object object) throws ExecutionException, InterruptedException {
        Map<String,Object> request = (Map<String, Object>)object;
        String requestOtp = request.get("otp").toString();
        String requestEmail = request.get("email").toString();

        QuerySnapshot usersSnapshot = userRepository.findUserByEmail(requestEmail);

        // Check if the OTP exists
        if (usersSnapshot.isEmpty()) {
            return new HashMap<>() {{
                put("status", false);
                put("message", "Something went wrong");
            }};
        }
        // OTP validation logic...
        DocumentSnapshot userSnapshot = usersSnapshot.getDocuments().get(0);
        Timestamp validTill = userSnapshot.get("otpExpiry", Timestamp.class);
        Timestamp now = Timestamp.now();
        String storedOtp = userSnapshot.getString("otp");
        if (storedOtp == null || validTill == null || validTill.compareTo(now) <= 0) {
            return Map.of("status", false, "message", "OTP is invalid or expired.");
        }
        Number attemptValue = (Number) userSnapshot.get("otpAttempts");
        long attempts = attemptValue == null ? 0 : attemptValue.longValue();
        if (attempts >= 5) {
            return Map.of("status", false, "message", "Too many incorrect attempts. Request a new OTP.");
        }
        if (!passwordEncoder.matches(requestOtp, storedOtp)) {
            userRepository.updateUser(userSnapshot.getId(), "otpAttempts", attempts + 1);
            return new HashMap<>() {{
                put("status", false);
                put("message", "Incorrect OTP entered");
            }};
        } else {
            UserResponseDTO userResponseDTO = userRepository.updateUser(userSnapshot.getId(),new HashMap<>(){{
                put("verified",true);
                put("otp",null);
                put("otpExpiry",null);
                put("otpAttempts", null);
            }});
            return new HashMap<>(){{
                put("status", true);
                put("message", "OTP Verified Successfully.");
                put("token",jwtUtil.generateToken(userSnapshot.getId(),new HashMap<>()));
                put("user_id",userSnapshot.getId());
                put("user",userResponseDTO);
            }};
        }
    }

    private String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash token", e);
        }
    }
}
