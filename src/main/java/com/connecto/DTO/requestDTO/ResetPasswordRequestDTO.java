package com.connecto.DTO.requestDTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequestDTO(
        @NotBlank String token,
        @NotBlank @Size(min = 8, max = 128) String newPassword,
        @NotBlank @Size(min = 8, max = 128) String confirmPassword
) {
}
