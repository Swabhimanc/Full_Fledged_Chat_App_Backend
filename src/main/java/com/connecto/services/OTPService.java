package com.connecto.services;

import com.connecto.model.User;
import org.apache.commons.mail.EmailException;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public interface OTPService {
    Map<String, Object> generateOtp(User user) throws EmailException, ExecutionException, InterruptedException, IOException;
}
