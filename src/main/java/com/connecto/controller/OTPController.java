package com.connecto.controller;

import com.connecto.services.OTPService;
import org.apache.commons.mail.EmailException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/auth")
public class OTPController {

    @Autowired
    OTPService otpService;

//    @PostMapping("/send-otp/{email}")
//    public ResponseEntity<?> generateOtp(@PathVariable String email) {
//        try {
//            Object object = otpService.generateOtp(email);
//            return ResponseEntity.status(200).body(object);
//        } catch (EmailException | ExecutionException | InterruptedException e) {
//            return ResponseEntity.status(500).body(e.getMessage());
//        }
//    }
    @PostMapping("/verify")
    public ResponseEntity<?> verifyOtp(@RequestBody Object object) {
        try {
            Object response = otpService.verifyOtp(object);
            return ResponseEntity.status(200).body(response);
        } catch (EmailException | ExecutionException | InterruptedException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
}
