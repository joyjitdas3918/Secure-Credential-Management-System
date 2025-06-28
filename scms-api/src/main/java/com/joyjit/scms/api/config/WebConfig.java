package com.joyjit.scms.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Apply CORS to all endpoints
                // Replace with the actual URL(s) where your React frontend will be hosted.
                // For local development: http://localhost:3000
                // For Render deployment: https://your-frontend-service-name.onrender.com
                .allowedOrigins("http://localhost:3000","http://localhost:3000", "https://scms.onrender.com", "https://your-frontend-domain.onrender.com","https://splendid-bonbon-0eddb4.netlify.app")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true); // If you're using cookies/sessions
    }
}