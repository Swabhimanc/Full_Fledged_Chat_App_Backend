package com.connecto.services.implementation;

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
//        System.out.println(OTP);
        User updatedUser = userRepository.updateUser(user.getId(),new HashMap<>(){{
            put("otp",OTP);
            put("otpExpiry",new Date(System.currentTimeMillis()+ Duration.ofMinutes(10).toMillis()));
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
        if (!passwordEncoder.matches(requestOtp, (String)userSnapshot.get("otp"))) {
            return new HashMap<>() {{
                put("status", false);
                put("message", "Incorrect OTP entered");
            }};
        } else if (validTill!=null && validTill.compareTo(now) <= 0) {
            return new HashMap<>() {{
                put("status", false);
                put("message", "OTP Expired.");
            }};
        }
        else {
            userRepository.updateUser(userSnapshot.getId(),new HashMap<>(){{
                put("verified",true);
                put("otp",null);
                put("otpExpiry",null);
            }});
            return new HashMap<>(){{
                put("status", true);
                put("message", "OTP Verified Successfully.");
                put("token",jwtUtil.generateToken(userSnapshot.getId(),new HashMap<>()));
                put("user_id",userSnapshot.getId());

            }};
        }
    }
}
