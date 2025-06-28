package com.joyjit.scms.api.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * Utility class for generating, validating, and extracting information from JWTs.
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${scms.jwt.secret}") // Loaded from application.yml
    private String jwtSecret;

    @Getter
    @Value("${scms.jwt.expiration}") // Loaded from application.yml (in milliseconds)
    private long jwtExpirationInMs;

    private SecretKey key;

    // Initialize the SecretKey from the base64 encoded secret string
    // This method is called by Spring after dependency injection
    public void init() {
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            log.error("JWT secret key is not configured! Please set 'scms.jwt.secret' in application.yml.");
            throw new IllegalArgumentException("JWT secret key not configured.");
        }
        // Ensure the secret key is long enough for HS256 (256 bits = 32 bytes)
        // JWT library expects a key of sufficient length for the chosen algorithm (HS256 requires 256 bits)
        // If your secret string is short, it won't be secure.
        // It's best to use a base64 encoded strong random key generated once and stored securely.
        // For example: Base64.getEncoder().encodeToString(Keys.secretKeyFor(SignatureAlgorithm.HS256).getEncoded())
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        log.info("JWT Token Provider initialized.");
    }

    /**
     * Generates a JWT token for the authenticated user.
     * @param authentication The Spring Security Authentication object.
     * @return The generated JWT string.
     */
    public String generateToken(Authentication authentication) {
        if (key == null) {
            init(); // Ensure key is initialized if not already (e.g., if called too early)
        }
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        // Collect roles as a comma-separated string
        String roles = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .subject(userPrincipal.getUsername())
                .claim("roles", roles) // Add roles as a custom claim
                .issuedAt(new Date())
                .expiration(expiryDate)
                .signWith(key, Jwts.SIG.HS256) // Use HS256 algorithm with the secret key
                .compact();
    }

    /**
     * Extracts username from JWT token.
     * @param token The JWT string.
     * @return The username.
     */
    public String getUsernameFromToken(String token) {
        if (key == null) {
            init();
        }
        return Jwts.parser()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    /**
     * Validates a JWT token.
     * @param authToken The JWT string to validate.
     * @return True if the token is valid, false otherwise.
     */
    public boolean validateToken(String authToken) {
        if (key == null) {
            init();
        }
        try {
            Jwts.parser().setSigningKey(key).build().parseClaimsJws(authToken);
            return true;
        } catch (io.jsonwebtoken.security.SignatureException ex) {
            log.error("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty.");
        }
        return false;
    }
}