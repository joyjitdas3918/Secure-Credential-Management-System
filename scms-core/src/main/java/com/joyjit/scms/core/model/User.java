package com.joyjit.scms.core.model;

import lombok.Builder;
import lombok.Value;
import java.time.Instant;
import java.util.UUID;
import java.util.Set; // Added for potential roles in core model

/**
 * Represents a user in the SCMS who can access or manage secrets.
 * This is a simplified view for the core module, focusing on identity attributes.
 */
@Value
@Builder
public class User {
    UUID id;
    String username;
    String email;
    boolean enabled;
    Instant createdAt;
    Instant updatedAt;
    // You might choose to include roles/permissions as simple strings or enums here,
    // even if the detailed security logic is in infrastructure/api.
    Set<String> roles; // Example: "ADMIN", "SECRET_READER", "SECRET_WRITER"
}