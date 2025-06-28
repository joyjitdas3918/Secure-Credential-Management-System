package com.joyjit.scms.api.dto;

import lombok.Builder;
import lombok.Value;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for returning secret details to the client.
 * Note: It intentionally does NOT include the plaintext value for security.
 */
@Value
@Builder
public class SecretResponse {
    UUID id;
    String path;
    int version;
    String encryptionMetadata;
    Instant createdAt;
    Instant updatedAt;
}