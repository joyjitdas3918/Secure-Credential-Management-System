package com.joyjit.scms.core.model;

import lombok.Builder;
import lombok.Value;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a stored secret within the SCMS.
 * This model contains the secret's metadata and its encrypted value.
 */
@Value // Lombok: Generates constructor, getters, equals, hashCode, toString
@Builder // Lombok: Generates a builder pattern for easy object creation
public class Secret {
    // Unique identifier for the secret
    UUID id;

    // Path or name of the secret (e.g., "/databases/prod/api_key")
    String path;

    // The encrypted value of the secret (e.g., AES-encrypted bytes, Base64 encoded)
    String encryptedValue;

    // The version of the secret (useful for auditing and rollback)
    int version;

    // Metadata about the encryption (e.g., encryption algorithm, IV used, key ID)
    String encryptionMetadata;

    // Timestamp when the secret was created
    Instant createdAt;

    // Timestamp when the secret was last updated
    Instant updatedAt;
}