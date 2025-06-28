package com.joyjit.scms.infra.security.encryption;

import com.joyjit.scms.core.exception.EncryptionException;
import com.joyjit.scms.core.service.EncryptionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct; // Correct import for @PostConstruct in Spring Boot 3+

// Use jakarta.crypto imports for Spring Boot 3+
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.logging.Logger;
import java.security.MessageDigest; // For deterministic hashing if not using Base64 directly
import java.nio.charset.StandardCharsets;

@Service
public class AesEncryptionService implements EncryptionService {

    private static final Logger LOGGER = Logger.getLogger(AesEncryptionService.class.getName());
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int KEY_LENGTH_BITS = 256; // AES-256 (requires 32-byte key)

    @Value("${scms.encryption.secret-key}")
    private String secretKeyString; // This should now be a Base64 encoded 32-byte key

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        if (secretKeyString == null || secretKeyString.isEmpty()) {
            LOGGER.severe("SCMS encryption key is not configured! Please set 'scms.encryption.secret-key' in application.yml.");
            throw new IllegalArgumentException("Encryption key not configured.");
        }

        try {
            // Assume secretKeyString is a Base64 encoded 32-byte key (256 bits)
            byte[] decodedKeyBytes = Base64.getDecoder().decode(secretKeyString);

            if (decodedKeyBytes.length * 8 != KEY_LENGTH_BITS) {
                // If the provided Base64 string does not decode to 32 bytes, it's a configuration error.
                // For a more robust solution, you could hash the string to 32 bytes (SHA-256)
                // or use a proper KDF (PBKDF2) here.
                // For now, let's enforce correct length.
                LOGGER.severe("Configured 'scms.encryption.secret-key' is not a valid Base64-encoded 256-bit (32-byte) key. Its decoded length is " + decodedKeyBytes.length + " bytes.");
                throw new IllegalArgumentException("Invalid encryption key length. Expected Base64-encoded 32-byte key.");
                /*
                // ALTERNATIVE: Deterministically hash the string to 32 bytes if not correct length.
                // This is better than SecureRandom(seed) but less secure than PBKDF2 for password-like strings.
                LOGGER.warning("Provided 'scms.encryption.secret-key' length is not 256 bits. Hashing it to derive key.");
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(secretKeyString.getBytes(StandardCharsets.UTF_8));
                this.secretKey = new SecretKeySpec(hash, ALGORITHM); // SHA-256 produces 32 bytes
                */
            } else {
                this.secretKey = new SecretKeySpec(decodedKeyBytes, ALGORITHM);
            }
            LOGGER.info("AES Encryption Service initialized with provided key.");
        } catch (Exception e) {
            LOGGER.severe("Failed to initialize encryption key: " + e.getMessage());
            throw new IllegalArgumentException("Invalid encryption key configuration.", e);
        }
    }

    @Override
    public String encrypt(String plaintext) throws EncryptionException {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        if (secretKey == null) {
            throw new EncryptionException("Encryption key not initialized.");
        }

        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

            byte[] cipherTextWithTag = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] encryptedData = new byte[iv.length + cipherTextWithTag.length];
            System.arraycopy(iv, 0, encryptedData, 0, iv.length);
            System.arraycopy(cipherTextWithTag, 0, encryptedData, iv.length, cipherTextWithTag.length);

            return Base64.getEncoder().encodeToString(encryptedData);
        } catch (Exception e) {
            LOGGER.severe("Encryption failed: " + e.getMessage());
            throw new EncryptionException("Failed to encrypt data", e);
        }
    }

    @Override
    public String decrypt(String encryptedText) throws EncryptionException {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }
        if (secretKey == null) {
            throw new EncryptionException("Encryption key not initialized.");
        }

        try {
            byte[] decodedData = Base64.getDecoder().decode(encryptedText);

            if (decodedData.length < GCM_IV_LENGTH + (GCM_TAG_LENGTH_BITS / 8)) {
                throw new IllegalArgumentException("Encrypted data too short or invalid format.");
            }

            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(decodedData, 0, iv, 0, GCM_IV_LENGTH);

            byte[] cipherTextWithTag = new byte[decodedData.length - GCM_IV_LENGTH];
            System.arraycopy(decodedData, GCM_IV_LENGTH, cipherTextWithTag, 0, cipherTextWithTag.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

            byte[] plaintext = cipher.doFinal(cipherTextWithTag);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Logging detailed exception for debugging BadPaddingException / AEADBadTagException
            LOGGER.severe("Decryption failed: " + e.getClass().getName() + ": " + e.getMessage() +
                    ". Make sure the same key used for encryption is used for decryption and data is not corrupted.");
            throw new EncryptionException("Failed to decrypt data", e);
        }
    }
}