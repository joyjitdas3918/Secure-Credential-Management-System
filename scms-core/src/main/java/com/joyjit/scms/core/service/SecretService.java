package com.joyjit.scms.core.service;

import com.joyjit.scms.core.model.Secret;
import com.joyjit.scms.core.exception.SecretNotFoundException;
import com.joyjit.scms.core.exception.PermissionDeniedException;
import java.util.List;
import java.util.UUID;

/**
 * Defines the core business operations for managing secrets.
 */
public interface SecretService {

    /**
     * Retrieves a secret by its ID.
     * @param secretId The unique ID of the secret.
     * @param callingUserUsername The username of the user attempting to retrieve the secret (for authorization).
     * @return The Secret object.
     * @throws SecretNotFoundException If the secret does not exist.
     * @throws PermissionDeniedException If the user does not have permission to read the secret.
     */
    Secret getSecretById(UUID secretId, String callingUserUsername) throws SecretNotFoundException, PermissionDeniedException;

    /**
     * Retrieves a secret by its path.
     * @param path The unique path of the secret (e.g., "/databases/prod/api_key").
     * @param callingUserUsername The username of the user attempting to retrieve the secret (for authorization).
     * @return The Secret object.
     * @throws SecretNotFoundException If the secret does not exist.
     * @throws PermissionDeniedException If the user does not have permission to read the secret.
     */
    Secret getSecretByPath(String path, String callingUserUsername) throws SecretNotFoundException, PermissionDeniedException;

    /**
     * Creates a new secret.
     * @param path The path for the new secret.
     * @param plaintextValue The unencrypted value of the secret.
     * @param creatingUserUsername The username of the user creating the secret.
     * @return The newly created Secret object (with encrypted value).
     * @throws PermissionDeniedException If the user does not have permission to create secrets at this path.
     */
    Secret createSecret(String path, String plaintextValue, String creatingUserUsername) throws PermissionDeniedException;

    /**
     * Updates an existing secret.
     * @param secretId The ID of the secret to update.
     * @param newPlaintextValue The new unencrypted value of the secret.
     * @param updatingUserUsername The username of the user updating the secret.
     * @return The updated Secret object (with new encrypted value and version).
     * @throws SecretNotFoundException If the secret does not exist.
     * @throws PermissionDeniedException If the user does not have permission to update the secret.
     */
    Secret updateSecret(UUID secretId, String newPlaintextValue, String updatingUserUsername) throws SecretNotFoundException, PermissionDeniedException;

    /**
     * Deletes a secret by its ID.
     * @param secretId The ID of the secret to delete.
     * @param deletingUserUsername The username of the user deleting the secret.
     * @throws SecretNotFoundException If the secret does not exist.
     * @throws PermissionDeniedException If the user does not have permission to delete the secret.
     */
    void deleteSecret(UUID secretId, String deletingUserUsername) throws SecretNotFoundException, PermissionDeniedException;

    /**
     * Lists secrets accessible by a user under a given path prefix.
     * @param pathPrefix The path prefix to search under (e.g., "/databases/").
     * @param callingUserUsername The username of the user requesting the list.
     * @return A list of Secret objects (potentially with sensitive data redacted for API exposure).
     * @throws PermissionDeniedException If the user does not have permission to list secrets under this path.
     */
    List<Secret> listSecretsByPathPrefix(String pathPrefix, String callingUserUsername) throws PermissionDeniedException;
}