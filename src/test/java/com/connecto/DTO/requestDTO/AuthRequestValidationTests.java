package com.connecto.DTO.requestDTO;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthRequestValidationTests {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void regularLoginRequiresEmailAndPassword() {
        assertFalse(validator.validate(new LoginRequestDTO("REGULAR", null, null, null)).isEmpty());
        assertTrue(validator.validate(new LoginRequestDTO("REGULAR", "user@example.com", "password", null)).isEmpty());
    }

    @Test
    void googleRegistrationRequiresCredential() {
        assertFalse(validator.validate(new RegisterRequestDTO("GOOGLE", null, null, null, null, null)).isEmpty());
        assertTrue(validator.validate(new RegisterRequestDTO("GOOGLE", null, null, null, null, "credential")).isEmpty());
    }
}
