package com.joyjit.scms.infra.security.encryption;

import com.joyjit.scms.core.exception.EncryptionException;
import com.joyjit.scms.core.service.EncryptionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.logging.Logger; // Using java.util.logging, can switch to SLF4J/Logback later

// For Java 17+ with Jakarta EE (Spring Boot 3+), you might need:
// import jakarta.crypto.Cipher;
// import jakarta.crypto.KeyGenerator;
// import jakarta.crypto.SecretKey;
// import jakarta.crypto.spec.IvParameterSpec;
// import jakarta.crypto.spec.SecretKeySpec;

@Service // Mark this as a Spring Service component
public class AesEncryptionService implements EncryptionService {

    private static final Logger LOGGER = Logger.getLogger(AesEncryptionService.class.getName());
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding"; // AES in Galois/Counter Mode
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 16; // 128 bits
    private static final int KEY_LENGTH_BITS = 256; // AES-256

    // This key should ideally come from a secure KMS or environment variable, NOT hardcoded.
    // For demonstration, we'll use a key derived from a secret string.
    // In a real application, ensure this key is securely managed.
    @Value("${scms.encryption.secret-key}") // Load from application.yml/properties
    private String secretKeyString;

    private SecretKey secretKey;

    /**
     * Initializes the AES key using the provided secret key string.
     * This method is called by Spring after dependency injection.
     */
    // @PostConstruct // Consider using @PostConstruct if more complex setup is needed immediately after construction
    public void init() {
        if (secretKeyString == null || secretKeyString.isEmpty()) {
            LOGGER.severe("SCMS encryption key is not configured! Please set 'scms.encryption.secret-key' in application.yml.");
            throw new IllegalArgumentException("Encryption key not configured.");
        }
        // For demonstration, directly convert string to key bytes.
        // In reality, use PBKDF2 or similar KDF with a strong salt.
        byte[] keyBytes = secretKeyString.getBytes();
        if (keyBytes.length * 8 != KEY_LENGTH_BITS) { // Check if key is correct length (e.g., 32 bytes for AES-256)
            try {
                // Attempt to generate a proper key from the string if it's not the right length,
                // or throw an error. For simplicity, we'll just resize/pad here or use a KDF.
                // A better approach would be to use a KDF like PBKDF2 to derive the key from the string.
                KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
                keyGen.init(KEY_LENGTH_BITS, new SecureRandom(keyBytes)); // Use SecureRandom for key derivation
                this.secretKey = keyGen.generateKey();
                LOGGER.info("Generated AES key from provided string due to incorrect length.");
            } catch (Exception e) {
                LOGGER.severe("Failed to initialize encryption key: " + e.getMessage());
                throw new IllegalArgumentException("Invalid encryption key configuration.", e);
            }
        } else {
            this.secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
        }
        LOGGER.info("AES Encryption Service initialized with provided key.");
    }

    @Override
    public String encrypt(String plaintext) throws EncryptionException {
        if (secretKey == null) {
            init(); // Ensure key is initialized if not already (e.g., if @PostConstruct is not used)
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv); // Generate a random IV for each encryption

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));

            byte[] cipherText = cipher.doFinal(plaintext.getBytes());

            // Combine IV and cipherText for storage
            byte[] encryptedData = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, encryptedData, 0, iv.length);
            System.arraycopy(cipherText, 0, encryptedData, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(encryptedData);
        } catch (Exception e) {
            LOGGER.severe("Encryption failed: " + e.getMessage());
            throw new EncryptionException("Failed to encrypt data", e);
        }
    }

    @Override
    public String decrypt(String encryptedText) throws EncryptionException {
        if (secretKey == null) {
            init(); // Ensure key is initialized
        }
        try {
            byte[] decodedData = Base64.getDecoder().decode(encryptedText);

            if (decodedData.length < GCM_IV_LENGTH + GCM_TAG_LENGTH) { // IV + minimum tag length
                throw new IllegalArgumentException("Encrypted data too short.");
            }

            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(decodedData, 0, iv, 0, GCM_IV_LENGTH);

            byte[] cipherText = new byte[decodedData.length - GCM_IV_LENGTH];
            System.arraycopy(decodedData, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));

            byte[] plaintext = cipher.doFinal(cipherText);
            return new String(plaintext);
        } catch (Exception e) {
            LOGGER.severe("Decryption failed: " + e.getClass().getName() + ": " + e.getMessage());
            throw new EncryptionException("Failed to decrypt data", e);
        }
    }
}