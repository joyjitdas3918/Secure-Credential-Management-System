package com.joyjit.scms.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity representing the 'secrets' table in the database.
 */
@Entity // Marks this class as a JPA entity
@Table(name = "secrets") // Maps to a table named 'secrets'
@Getter // Lombok: Generates getters
@Setter // Lombok: Generates setters (useful for JPA and DTO mapping)
@NoArgsConstructor // Lombok: Generates a no-argument constructor (required by JPA)
@AllArgsConstructor // Lombok: Generates a constructor with all fields
public class SecretEntity {

    @Id // Marks this field as the primary key
    @GeneratedValue(strategy = GenerationType.UUID) // Generates UUIDs automatically
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "path", nullable = false, unique = true) // Path must be unique
    private String path;

    @Column(name = "encrypted_value", nullable = false, columnDefinition = "TEXT") // Store as TEXT for Base64 encoded string
    private String encryptedValue;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "encryption_metadata", columnDefinition = "TEXT")
    private String encryptionMetadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

}