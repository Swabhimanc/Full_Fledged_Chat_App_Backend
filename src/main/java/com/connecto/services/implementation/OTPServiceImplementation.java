package com.connecto.services.implementation;

import com.connecto.DTO.responseDTO.UserResponseDTO;
import com.connecto.model.User;
import com.connecto.repositories.OTPRepository;
import com.connecto.repositories.UserRepository;
import com.connecto.services.OTPService;
import com.connecto.utilities.EmailService;
import com.connecto.utilities.security.JwtUtil;
import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import org.apache.commons.mail.EmailException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;


@Service
public class OTPServiceImplementation implements OTPService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    public OTPServiceImplementation(UserRepository userRepository,JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    public Map<String, Object> generateOtp(User user) throws EmailException, ExecutionException, InterruptedException, IOException {

        final String OTP = passwordEncoder.encode(EmailService.sendOTP(user.getFirstName(),user.getEmail()));
        UserResponseDTO updatedUser = userRepository.updateUser(user.getId(),new HashMap<>(){{
             put("otp",OTP);
             put("otpExpiry",new Date(System.currentTimeMillis()+ Duration.ofMinutes(10).toMillis()));
             put("otpAttempts", 0);
         }});
        if (updatedUser != null) {
            return new HashMap<>() {{
                put("status", true);
                put("message", "OTP Sent Successfully");
            }};
        } else {
            return new HashMap<>() {{
                put("status", false);
                put("message", "Something went wrong");
            }};
        }
    }
}
