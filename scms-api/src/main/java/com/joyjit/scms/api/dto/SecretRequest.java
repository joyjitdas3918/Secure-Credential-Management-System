package com.joyjit.scms.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Value;

/**
 * DTO for creating or updating a secret.
 */
@Value // Lombok: Creates immutable class with getters, equals, hashCode, toString
public class SecretRequest {
    @NotBlank(message = "Secret path cannot be empty")
    @Size(max = 255, message = "Secret path cannot exceed 255 characters")
    String path;

    @NotBlank(message = "Secret value cannot be empty")
    String value; // This will be the plaintext value sent by the client
}