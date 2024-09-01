package com.connecto.utilities;

import java.util.UUID;

public class PasswordResetTokenGenerator {
    public static String generateRandomToken() {
        return UUID.randomUUID().toString();
    }
}
