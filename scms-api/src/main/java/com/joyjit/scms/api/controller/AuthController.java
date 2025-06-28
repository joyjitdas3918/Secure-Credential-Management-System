package com.joyjit.scms.api.controller;

import com.joyjit.scms.api.dto.AuthResponse;
import com.joyjit.scms.api.dto.LoginRequest;
import com.joyjit.scms.api.dto.RegisterRequest; // <-- NEW IMPORT
import com.joyjit.scms.api.security.jwt.JwtTokenProvider;
import com.joyjit.scms.infra.persistence.entity.UserEntity; // <-- NEW IMPORT
import com.joyjit.scms.infra.persistence.repository.UserRepository; // <-- NEW IMPORT
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder; // <-- NEW IMPORT
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant; // <-- NEW IMPORT
import java.util.Collections; // <-- NEW IMPORT for roles
import java.util.UUID; // <-- NEW IMPORT for UUID generation if needed, or rely on JPA @GeneratedValue

/**
 * REST Controller for user authentication and registration.
 * Provides endpoints for users to log in and obtain a JWT, and to register.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository; // <-- NEW DEPENDENCY
    private final PasswordEncoder passwordEncoder; // <-- NEW DEPENDENCY

    public AuthController(AuthenticationManager authenticationManager,
                          JwtTokenProvider jwtTokenProvider,
                          UserRepository userRepository, // <-- Inject
                          PasswordEncoder passwordEncoder) { // <-- Inject
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Handles user login and generates a JWT.
     * @param loginRequest The LoginRequest DTO containing username and password.
     * @return ResponseEntity with AuthResponse containing the JWT.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            log.info("Attempting to authenticate user: {}", loginRequest.getUsername());
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            String jwt = jwtTokenProvider.generateToken(authentication);
            long expiry = jwtTokenProvider.getJwtExpirationInMs();

            log.info("User {} authenticated successfully. JWT generated.", loginRequest.getUsername());
            return ResponseEntity.ok(AuthResponse.builder()
                    .accessToken(jwt)
                    .tokenType("Bearer")
                    .expiresIn(expiry)
                    .build());
        } catch (org.springframework.security.core.AuthenticationException e) {
            log.warn("Authentication failed for user {}: {}", loginRequest.getUsername(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password", e);
        } catch (Exception e) {
            log.error("An unexpected error occurred during authentication for user {}: {}", loginRequest.getUsername(), e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Authentication failed due to internal error", e);
        }
    }

    /**
     * Handles new user registration.
     * @param registerRequest The RegisterRequest DTO containing new user details.
     * @return ResponseEntity indicating success or failure.
     */
    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        // Check if username already exists
        if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            log.warn("Registration failed: Username '{}' is already taken.", registerRequest.getUsername());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is already taken!");
        }

        // Check if email already exists
        if (userRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            log.warn("Registration failed: Email '{}' is already in use.", registerRequest.getEmail());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is already in use!");
        }

        // Create new user's account
        UserEntity user = new UserEntity();
        user.setUsername(registerRequest.getUsername());
        user.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));
        user.setEmail(registerRequest.getEmail());
        user.setEnabled(true); // User is enabled by default
        user.setRoles(Collections.singleton("ROLE_USER")); // Assign default role, e.g., "ROLE_USER"
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);

        log.info("User '{}' registered successfully.", user.getUsername());
        return new ResponseEntity<>("User registered successfully!", HttpStatus.CREATED);
    }
}