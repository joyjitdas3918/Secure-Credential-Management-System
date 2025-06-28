package com.joyjit.scms.infra.service;

import com.joyjit.scms.core.exception.EncryptionException;
import com.joyjit.scms.core.exception.PermissionDeniedException;
import com.joyjit.scms.core.exception.SecretNotFoundException;
import com.joyjit.scms.core.model.Secret;
import com.joyjit.scms.core.service.EncryptionService;
import com.joyjit.scms.core.service.SecretService;
import com.joyjit.scms.infra.persistence.entity.SecretEntity;
import com.joyjit.scms.infra.persistence.repository.SecretRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Concrete implementation of the SecretService interface, residing in the infrastructure layer.
 * This class handles the business logic, interacting with the persistence layer (SecretRepository)
 * and the encryption service (EncryptionService).
 */
@Service // Marks this class as a Spring Service component
public class SecretServiceImpl implements SecretService {

    private static final Logger log = LoggerFactory.getLogger(SecretServiceImpl.class);

    private final SecretRepository secretRepository;
    private final EncryptionService encryptionService;

    // Spring will automatically inject these dependencies
    public SecretServiceImpl(SecretRepository secretRepository, EncryptionService encryptionService) {
        this.secretRepository = secretRepository;
        this.encryptionService = encryptionService;
    }

    // --- Helper methods for mapping between core.Secret and infra.SecretEntity ---
    private Secret toCoreSecret(SecretEntity entity) {
        if (entity == null) {
            return null;
        }
        return Secret.builder()
                .id(entity.getId())
                .path(entity.getPath())
                .encryptedValue(entity.getEncryptedValue())
                .version(entity.getVersion())
                .encryptionMetadata(entity.getEncryptionMetadata())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    // This method is generally not needed as core.Secret is usually built from entities,
    // and entities are created/updated based on DTOs, not directly from core.Secret model.
    // However, including for completeness if a specific use case required it.
    private SecretEntity toSecretEntity(Secret coreSecret) {
        if (coreSecret == null) {
            return null;
        }
        SecretEntity entity = new SecretEntity();
        entity.setId(coreSecret.getId());
        entity.setPath(coreSecret.getPath());
        entity.setEncryptedValue(coreSecret.getEncryptedValue());
        entity.setVersion(coreSecret.getVersion());
        entity.setEncryptionMetadata(coreSecret.getEncryptionMetadata());
        entity.setCreatedAt(coreSecret.getCreatedAt());
        entity.setUpdatedAt(coreSecret.getUpdatedAt());
        return entity;
    }

    // --- SecretService Interface Implementations ---

    @Override
    @Transactional(readOnly = true) // Optimize for read operations
    public Secret getSecretById(UUID secretId, String callingUserUsername) throws SecretNotFoundException, PermissionDeniedException {
        // Placeholder authorization: In a real system, check user roles/permissions against secret access policies.
        if (!hasReadPermission(callingUserUsername, secretId.toString())) {
            throw new PermissionDeniedException("User " + callingUserUsername + " does not have permission to read secret with ID " + secretId);
        }

        log.debug("Attempting to retrieve secret by ID: {}", secretId);
        Optional<SecretEntity> optionalSecretEntity = secretRepository.findById(secretId);
        SecretEntity secretEntity = optionalSecretEntity.orElseThrow(() -> {
            log.warn("Secret with ID {} not found.", secretId);
            return new SecretNotFoundException("Secret with ID " + secretId + " not found.");
        });

        Secret coreSecret = toCoreSecret(secretEntity);
        try {
            // Decrypt the value here if you intend to use the plaintext within the service layer
            // (e.g., for further business logic that needs plaintext).
            // For API responses, the decryption might happen later in the controller or a DTO mapper.
            // String decryptedValue = encryptionService.decrypt(coreSecret.getEncryptedValue());
            // If the core Secret model were mutable or had a builder for plaintext, you could set it here.
            // However, for security, the core.model.Secret often only holds encrypted data.
            log.debug("Secret with ID {} retrieved successfully.", secretId);
            return coreSecret; // Returns the Secret with its encrypted value.
        } catch (EncryptionException e) {
            log.error("Failed to decrypt secret with ID {}: {}", secretId, e.getMessage(), e);
            throw new SecretNotFoundException("Failed to decrypt secret with ID " + secretId, e); // Mask encryption failure
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Secret getSecretByPath(String path, String callingUserUsername) throws SecretNotFoundException, PermissionDeniedException {
        // Placeholder authorization: In a real system, check user roles/permissions against secret access policies.
        if (!hasReadPermission(callingUserUsername, path)) {
            throw new PermissionDeniedException("User " + callingUserUsername + " does not have permission to read secret at path " + path);
        }

        log.debug("Attempting to retrieve secret by path: {}", path);
        Optional<SecretEntity> optionalSecretEntity = secretRepository.findByPath(path);
        SecretEntity secretEntity = optionalSecretEntity.orElseThrow(() -> {
            log.warn("Secret at path {} not found.", path);
            return new SecretNotFoundException("Secret at path " + path + " not found.");
        });

        Secret coreSecret = toCoreSecret(secretEntity);
        try {
            // String decryptedValue = encryptionService.decrypt(coreSecret.getEncryptedValue()); // Decrypt if needed internally
            log.debug("Secret at path {} retrieved successfully.", path);
            return coreSecret; // Returns the Secret with its encrypted value.
        } catch (EncryptionException e) {
            log.error("Failed to decrypt secret at path {}: {}", path, e.getMessage(), e);
            throw new SecretNotFoundException("Failed to decrypt secret at path " + path, e); // Mask encryption failure
        }
    }

    @Override
    @Transactional // Operations that modify data
    public Secret createSecret(String path, String plaintextValue, String creatingUserUsername) throws PermissionDeniedException {
        // Placeholder authorization: In a real system, check user roles/permissions.
        if (!hasWritePermission(creatingUserUsername, path)) {
            throw new PermissionDeniedException("User " + creatingUserUsername + " does not have permission to create secret at path " + path);
        }

        // Check if secret with this path already exists to prevent duplicates via 'create'
        if (secretRepository.findByPath(path).isPresent()) {
            log.warn("Attempted to create secret at path {} but it already exists.", path);
            throw new IllegalArgumentException("Secret at path " + path + " already exists. Use update operation.");
        }

        log.debug("Attempting to create secret at path: {}", path);
        try {
            String encryptedValue = encryptionService.encrypt(plaintextValue);
            Instant now = Instant.now();

            SecretEntity newSecretEntity = new SecretEntity();
            // ID will be generated by JPA due to @GeneratedValue
            newSecretEntity.setPath(path);
            newSecretEntity.setEncryptedValue(encryptedValue);
            newSecretEntity.setVersion(1); // First version of the secret
            newSecretEntity.setEncryptionMetadata("AES/GCM/NoPadding (Demo)"); // Example metadata of encryption
            newSecretEntity.setCreatedAt(now);
            newSecretEntity.setUpdatedAt(now);

            SecretEntity savedEntity = secretRepository.save(newSecretEntity);
            log.info("Secret at path {} created successfully with ID {}.", path, savedEntity.getId());
            return toCoreSecret(savedEntity);
        } catch (EncryptionException e) {
            log.error("Failed to encrypt secret value for path {}: {}", path, e.getMessage(), e);
            throw new EncryptionException("Failed to encrypt secret value", e);
        }
    }

    @Override
    @Transactional
    public Secret updateSecret(UUID secretId, String newPlaintextValue, String updatingUserUsername) throws SecretNotFoundException, PermissionDeniedException {
        // Placeholder authorization: In a real system, check user roles/permissions.
        if (!hasWritePermission(updatingUserUsername, secretId.toString())) {
            throw new PermissionDeniedException("User " + updatingUserUsername + " does not have permission to update secret with ID " + secretId);
        }

        log.debug("Attempting to update secret with ID: {}", secretId);
        SecretEntity existingSecret = secretRepository.findById(secretId)
                .orElseThrow(() -> {
                    log.warn("Secret with ID {} not found for update.", secretId);
                    return new SecretNotFoundException("Secret with ID " + secretId + " not found for update.");
                });

        try {
            String newEncryptedValue = encryptionService.encrypt(newPlaintextValue);
            Instant now = Instant.now();

            existingSecret.setEncryptedValue(newEncryptedValue);
            existingSecret.setVersion(existingSecret.getVersion() + 1); // Increment version on update
            existingSecret.setUpdatedAt(now);

            SecretEntity updatedEntity = secretRepository.save(existingSecret);
            log.info("Secret with ID {} updated successfully to version {}.", secretId, updatedEntity.getVersion());
            return toCoreSecret(updatedEntity);
        } catch (EncryptionException e) {
            log.error("Failed to encrypt new secret value for ID {}: {}", secretId, e.getMessage(), e);
            throw new EncryptionException("Failed to encrypt new secret value", e);
        }
    }

    @Override
    @Transactional
    public void deleteSecret(UUID secretId, String deletingUserUsername) throws SecretNotFoundException, PermissionDeniedException {
        // Placeholder authorization: In a real system, check user roles/permissions.
        if (!hasDeletePermission(deletingUserUsername, secretId.toString())) {
            throw new PermissionDeniedException("User " + deletingUserUsername + " does not have permission to delete secret with ID " + secretId);
        }

        log.debug("Attempting to delete secret with ID: {}", secretId);
        if (!secretRepository.existsById(secretId)) {
            log.warn("Secret with ID {} not found for deletion.", secretId);
            throw new SecretNotFoundException("Secret with ID " + secretId + " not found for deletion.");
        }
        secretRepository.deleteById(secretId);
        log.info("Secret with ID {} deleted successfully.", secretId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Secret> listSecretsByPathPrefix(String pathPrefix, String callingUserUsername) throws PermissionDeniedException {
        // Placeholder authorization: In a real system, check user roles/permissions.
        if (!hasListPermission(callingUserUsername, pathPrefix)) {
            throw new PermissionDeniedException("User " + callingUserUsername + " does not have permission to list secrets under path " + pathPrefix);
        }

        log.debug("Attempting to list secrets by path prefix: {}", pathPrefix);
        List<SecretEntity> entities = secretRepository.findByPathStartingWith(pathPrefix);
        log.info("Found {} secrets under path prefix {}.", entities.size(), pathPrefix);
        return entities.stream()
                .map(this::toCoreSecret)
                .collect(Collectors.toList());
    }

    // --- Placeholder Authorization Methods ---
    // In a real application, these methods would contain your actual Role-Based Access Control (RBAC)
    // or Attribute-Based Access Control (ABAC) logic, checking user roles/permissions against policies
    // associated with secret paths, user attributes, or other metadata.
    // For now, they provide basic true/false based on username 'admin'.
    private boolean hasReadPermission(String username, String resource) {
        return "admin".equals(username) || true; // Allow 'admin' or everyone for reading during dev
    }

    private boolean hasWritePermission(String username, String resource) {
        return "admin".equals(username); // Only allow 'admin' for writing during dev
    }

    private boolean hasDeletePermission(String username, String resource) {
        return "admin".equals(username); // Only allow 'admin' for deleting during dev
    }

    private boolean hasListPermission(String username, String resource) {
        return "admin".equals(username) || true; // Allow 'admin' or everyone for listing during dev
    }
}