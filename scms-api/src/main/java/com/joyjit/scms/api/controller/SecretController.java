package com.joyjit.scms.api.controller;

import com.joyjit.scms.api.dto.SecretRequest;
import com.joyjit.scms.api.dto.SecretResponse;
import com.joyjit.scms.core.exception.PermissionDeniedException;
import com.joyjit.scms.core.exception.SecretNotFoundException;
import com.joyjit.scms.core.model.Secret;
import com.joyjit.scms.core.service.EncryptionService; // Need this to decrypt for client
import jakarta.validation.constraints.NotBlank;
import com.joyjit.scms.core.service.SecretService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for managing secrets.
 * Exposes API endpoints for CRUD operations on secrets.
 */
@RestController // Marks this as a Spring REST Controller
@RequestMapping("/api/secrets") // Base path for all endpoints in this controller
public class SecretController {

    private static final Logger log = LoggerFactory.getLogger(SecretController.class);

    private final SecretService secretService;
    private final EncryptionService encryptionService; // To decrypt for client response

    public SecretController(SecretService secretService, EncryptionService encryptionService) {
        this.secretService = secretService;
        this.encryptionService = encryptionService;
    }

    // Helper method to convert core.Secret to api.dto.SecretResponse
    private SecretResponse toSecretResponse(Secret secret) {
        if (secret == null) {
            return null;
        }
        return SecretResponse.builder()
                .id(secret.getId())
                .path(secret.getPath())
                .value(encryptionService.decrypt(secret.getEncryptedValue()))
                .version(secret.getVersion())
                .encryptionMetadata(secret.getEncryptionMetadata())
                .createdAt(secret.getCreatedAt())
                .updatedAt(secret.getUpdatedAt())
                .build();
    }

    /**
     * Endpoint to create a new secret.
     * @param request The SecretRequest DTO containing path and value.
     * @param currentUser The authenticated user details.
     * @return ResponseEntity with the created SecretResponse and location header.
     */
    @PostMapping
    public ResponseEntity<SecretResponse> createSecret(
            @Valid @RequestBody SecretRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        try {
            log.info("User {} attempting to create secret at path: {}", currentUser.getUsername(), request.getPath());
            Secret createdSecret = secretService.createSecret(request.getPath(), request.getValue(), currentUser.getUsername());
            SecretResponse response = toSecretResponse(createdSecret);
            return ResponseEntity.created(URI.create("/api/secrets/" + createdSecret.getId())).body(response);
        } catch (PermissionDeniedException e) {
            log.warn("Permission denied for user {} to create secret: {}", currentUser.getUsername(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            log.warn("Bad request for creating secret: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error creating secret: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create secret", e);
        }
    }

    /**
     * Endpoint to retrieve a secret by its ID.
     * @param id The UUID of the secret.
     * @param currentUser The authenticated user details.
     * @return ResponseEntity with the SecretResponse, including the decrypted value.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SecretResponse> getSecretById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails currentUser) {
        try {
            log.info("User {} attempting to retrieve secret by ID: {}", currentUser.getUsername(), id);
            Secret secret = secretService.getSecretById(id, currentUser.getUsername());

            // Decrypt the value here for the client, if the API design allows plaintext return.
            // For highly sensitive apps, you might return only metadata or require a separate "reveal" endpoint.
            String decryptedValue = encryptionService.decrypt(secret.getEncryptedValue());

            // For now, we'll return a SecretResponse that *does not* include the decrypted value.
            // If the API needs to provide the plaintext to the client, you'd need a different DTO
            // or modify SecretResponse to include a 'plaintextValue' field.
            // For now, we only return metadata.
            SecretResponse response = toSecretResponse(secret);

            // If you wanted to return the plaintext (be very careful with this!):
            // return ResponseEntity.ok(SecretResponseWithPlaintext.builder()...build());

            log.info("Secret with ID {} retrieved successfully by user {}.", id, currentUser.getUsername());
            return ResponseEntity.ok(response);
        } catch (SecretNotFoundException e) {
            log.warn("Secret not found for ID {}: {}", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (PermissionDeniedException e) {
            log.warn("Permission denied for user {} to get secret {}: {}", currentUser.getUsername(), id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error retrieving secret by ID {}: {}", id, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve secret", e);
        }
    }

    /**
     * Endpoint to retrieve a secret by its path and return its decrypted value.
     * Note: This is a sensitive operation and should be heavily secured.
     * We'll return the SecretResponse without plaintext for security,
     * but the decryption happened internally. If plaintext is needed, design a specific DTO.
     */
    @GetMapping("/by-path")
    public ResponseEntity<SecretResponse> getSecretByPath(
            @RequestParam @NotBlank String path,
            @AuthenticationPrincipal UserDetails currentUser) {
        try {
            log.info("User {} attempting to retrieve secret by path: {}", currentUser.getUsername(), path);
            Secret secret = secretService.getSecretByPath(path, currentUser.getUsername());

            // Decryption happens here, but for security, we're not including plaintext in SecretResponse.
            String decryptedValue = encryptionService.decrypt(secret.getEncryptedValue());

            SecretResponse response = toSecretResponse(secret);
            log.info("Secret at path {} retrieved successfully by user {}.", path, currentUser.getUsername());
            return ResponseEntity.ok(response);
        } catch (SecretNotFoundException e) {
            log.warn("Secret not found for path {}: {}", path, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (PermissionDeniedException e) {
            log.warn("Permission denied for user {} to get secret at path {}: {}", currentUser.getUsername(), path, e.getMessage());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error retrieving secret by path {}: {}", path, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve secret by path", e);
        }
    }


    /**
     * Endpoint to update an existing secret by its ID.
     * @param id The UUID of the secret to update.
     * @param request The SecretRequest DTO containing the new value.
     * @param currentUser The authenticated user details.
     * @return ResponseEntity with the updated SecretResponse.
     */
    @PutMapping("/{id}")
    public ResponseEntity<SecretResponse> updateSecret(
            @PathVariable UUID id,
            @Valid @RequestBody SecretRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        try {
            log.info("User {} attempting to update secret with ID {}.", currentUser.getUsername(), id);
            // Note: path in request is ignored for update by ID, but good for DTO reuse
            Secret updatedSecret = secretService.updateSecret(id, request.getValue(), currentUser.getUsername());
            SecretResponse response = toSecretResponse(updatedSecret);
            log.info("Secret with ID {} updated successfully by user {}.", id, currentUser.getUsername());
            return ResponseEntity.ok(response);
        } catch (SecretNotFoundException e) {
            log.warn("Secret not found for ID {}: {}", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (PermissionDeniedException e) {
            log.warn("Permission denied for user {} to update secret {}: {}", currentUser.getUsername(), id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error updating secret with ID {}: {}", id, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update secret", e);
        }
    }

    /**
     * Endpoint to delete a secret by its ID.
     * @param id The UUID of the secret to delete.
     * @param currentUser The authenticated user details.
     * @return ResponseEntity indicating success.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSecret(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails currentUser) {
        try {
            log.info("User {} attempting to delete secret with ID {}.", currentUser.getUsername(), id);
            secretService.deleteSecret(id, currentUser.getUsername());
            log.info("Secret with ID {} deleted successfully by user {}.", id, currentUser.getUsername());
            return ResponseEntity.noContent().build(); // 204 No Content
        } catch (SecretNotFoundException e) {
            log.warn("Secret not found for ID {}: {}", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (PermissionDeniedException e) {
            log.warn("Permission denied for user {} to delete secret {}: {}", currentUser.getUsername(), id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error deleting secret with ID {}: {}", id, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete secret", e);
        }
    }

    /**
     * Endpoint to list secrets by a path prefix.
     * @param pathPrefix The path prefix to search under (default to root '/').
     * @param currentUser The authenticated user details.
     * @return ResponseEntity with a list of SecretResponse DTOs.
     */
    @GetMapping
    public ResponseEntity<List<SecretResponse>> listSecrets(
            @RequestParam(defaultValue = "/") String pathPrefix,
            @AuthenticationPrincipal UserDetails currentUser) {
        try {
            log.info("User {} attempting to list secrets under path prefix: {}", currentUser.getUsername(), pathPrefix);
            List<Secret> secrets = secretService.listSecretsByPathPrefix(pathPrefix, currentUser.getUsername());
            List<SecretResponse> responses = secrets.stream()
                    .map(this::toSecretResponse)
                    .collect(Collectors.toList());
            log.info("User {} listed {} secrets under path prefix {}.", currentUser.getUsername(), responses.size(), pathPrefix);
            return ResponseEntity.ok(responses);
        } catch (PermissionDeniedException e) {
            log.warn("Permission denied for user {} to list secrets: {}", currentUser.getUsername(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error listing secrets: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to list secrets", e);
        }
    }
}