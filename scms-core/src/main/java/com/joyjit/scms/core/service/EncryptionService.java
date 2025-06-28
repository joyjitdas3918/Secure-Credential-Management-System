package com.joyjit.scms.core.service;

import com.joyjit.scms.core.exception.EncryptionException;

/**
 * Defines the interface for encryption and decryption operations.
 * Implementations will reside in the scms-infrastructure module.
 */
public interface EncryptionService {

    /**
     * Encrypts plaintext data.
     * @param plaintext The data to encrypt.
     * @return The encrypted data, typically Base64 encoded for storage/transmission.
     * The string may contain metadata about the encryption (e.g., IV) if needed by decrypt.
     * @throws EncryptionException If an error occurs during encryption.
     */
    String encrypt(String plaintext) throws EncryptionException;

    /**
     * Decrypts encrypted data.
     * @param encryptedText The encrypted data (e.g., Base64 encoded).
     * @return The original plaintext data.
     * @throws EncryptionException If an error occurs during decryption (e.g., invalid key, corrupted data).
     */
    String decrypt(String encryptedText) throws EncryptionException;
}