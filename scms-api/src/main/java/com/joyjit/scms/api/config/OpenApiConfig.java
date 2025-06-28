package com.joyjit.scms.api.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Secure Credential Management System API",
                version = "1.0",
                description = "API for managing secure credentials, user authentication, and authorization."
        )
)
@SecurityScheme(
        name = "bearerAuth", // The name of the security scheme, referenced by @SecurityRequirement
        type = SecuritySchemeType.HTTP, // It's an HTTP security scheme
        scheme = "bearer", // The scheme is "bearer"
        bearerFormat = "JWT", // The format is JWT
        description = "JWT authentication using a Bearer token" // Description for the UI
)
public class OpenApiConfig {
    // This class is primarily for annotations; no specific beans are usually needed here.
}