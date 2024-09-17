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
        reqObj.replace("password", passwordEncoder.encode(reqObj.get("password").toString()));

        // Email check logic...
        QuerySnapshot emailCheck = userRepository.findUserByEmail((String) reqObj.get("email"));
        if (!emailCheck.getDocuments().isEmpty()) {
            User user = emailCheck.getDocuments().get(0).toObject(User.class);

            if (user.isVerified()) {
                // Email is already in use and verified
                return new HashMap<>() {{
                    put("status", false);
                    put("message", "Email already in use. Please login.");
                }};
            } else {
                // Email exists but is not verified, update the existing user
                user.updateFields(reqObj); // Assume this method updates the user's fields with the new data
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
            // Generate and send OTP
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
                put("status", false);
                put("message", "There is no user with given email address");
            }};
        }
        String userId = userSnapshot.getDocuments().get(0).getId();
        String resetToken = PasswordResetTokenGenerator.generateRandomToken();
        userRepository.updateUser(userId, new HashMap<>() {{
            put("passwordResetToken", resetToken);
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
        QuerySnapshot userSnapshot = userRepository.getUsersRef().whereEqualTo("passwordResetToken", resetToken)
                .whereGreaterThan("passwordResetExpires", Timestamp.now()).get().get();
        if (userSnapshot.isEmpty()) {
            return new HashMap<>() {{
                put("status", false);
                put("message", "Token is invalid or expired");
            }};
        }
        User user = userSnapshot.getDocuments().get(0).toObject(User.class);
        userRepository.updateUser(user.getId(), new HashMap<>() {{
            put("passwordResetToken", null);
            put("passwordResetExpires", null);
            ;
            put("passwordChangedAt", new Date());
            put("password", passwordEncoder.encode(password));
        }});

//        EmailService.sendResetConfirmation(user.getEmail().toString());
        return new HashMap<>() {{
            put("status", true);
            put("message", "Password reset successfully");
            put("token", jwtUtil.generateToken(user.getId(), new HashMap<>()));
            put("user_id", user.getId());
        }};
    }

    public Object updateProfile(String userId, Object object) throws ExecutionException, InterruptedException {
        UserResponseDTO updatedUser = userRepository.updateUser(userId, (Map<String, Object>) object);

        return new HashMap<>() {{
            put("status", true);
            put("message", "Profile updated successfully");
            put("user", updatedUser);
        }};
    }
}
//TODO Add status for new messages,
// Start timer when OTP sent(Add to mail body the validity of OTP)
// Add max number of incorrect retries
//