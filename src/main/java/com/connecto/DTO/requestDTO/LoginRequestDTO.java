package com.connecto.DTO.requestDTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.AssertTrue;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashMap;
import java.util.Map;

public record LoginRequestDTO(
        @JsonProperty("auth_type")
        @NotBlank
        @Pattern(regexp = "REGULAR|GOOGLE") String authType,
        @Email String email,
        String password,
        String credential
) {
    @AssertTrue(message = "credentials are required for the selected authentication type")
    @JsonIgnore
    public boolean isCredentialsPresent() {
        return "GOOGLE".equals(authType)
                ? credential != null && !credential.isBlank()
                : email != null && !email.isBlank() && password != null && !password.isBlank();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("auth_type", authType);
        if (email != null) result.put("email", email);
        if (password != null) result.put("password", password);
        if (credential != null) result.put("credential", credential);
        return result;
    }
}
