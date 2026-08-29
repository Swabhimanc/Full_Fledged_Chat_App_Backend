package com.connecto.DTO.requestDTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashMap;
import java.util.Map;

public record RegisterRequestDTO(
        @JsonProperty("auth_type")
        @NotBlank
        @Pattern(regexp = "REGULAR|GOOGLE") String authType,
        @Size(max = 50) String firstName,
        @Size(max = 50) String lastName,
        @Email String email,
        String password,
        String credential
) {
    @AssertTrue(message = "registration details are required for the selected authentication type")
    @JsonIgnore
    public boolean isRegistrationDataPresent() {
        return "GOOGLE".equals(authType)
                ? credential != null && !credential.isBlank()
                : firstName != null && !firstName.isBlank()
                    && email != null && !email.isBlank()
                    && password != null && password.length() >= 8;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("auth_type", authType);
        if (firstName != null) result.put("firstName", firstName);
        if (lastName != null) result.put("lastName", lastName);
        if (email != null) result.put("email", email);
        if (password != null) result.put("password", password);
        if (credential != null) result.put("credential", credential);
        return result;
    }
}
