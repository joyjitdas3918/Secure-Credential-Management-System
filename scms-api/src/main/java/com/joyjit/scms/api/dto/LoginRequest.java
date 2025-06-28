package com.joyjit.scms.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Value;

/**
 * DTO for user login requests.
 */
@Value
public class LoginRequest {
    @NotBlank(message = "Username cannot be empty")
    String username;

    @NotBlank(message = "Password cannot be empty")
    String password;
}