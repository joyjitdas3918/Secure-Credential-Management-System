package com.joyjit.scms.api.dto;

import lombok.Builder;
import lombok.Value;

/**
 * DTO for returning authentication tokens after successful login.
 */
@Value
@Builder
public class AuthResponse {
    String accessToken;
    String tokenType;
    long expiresIn; // In milliseconds
}