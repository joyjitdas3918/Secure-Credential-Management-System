package com.joyjit.scms.api;

import com.joyjit.scms.infra.persistence.entity.UserEntity;
import com.joyjit.scms.infra.persistence.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Set;

@SpringBootApplication
@ComponentScan(basePackages = "com.joyjit.scms") // Ensures all modules' components are scanned
@EnableJpaRepositories(basePackages = "com.joyjit.scms.infra.persistence.repository")
@EntityScan(basePackages = "com.joyjit.scms.infra.persistence.entity")
public class ScmsApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ScmsApiApplication.class, args);
	}

	/**
	 * CommandLineRunner to create a default admin user if one doesn't exist.
	 * This is for development/demonstration purposes. In production, users should be managed securely.
	 */
	@Bean
	public CommandLineRunner initDefaultUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			if (userRepository.findByUsername("admin").isEmpty()) {
				UserEntity adminUser = new UserEntity();
				adminUser.setUsername("admin");
				adminUser.setPasswordHash(passwordEncoder.encode("adminpass")); // Encode password
				adminUser.setEmail("admin@scms.com");
				adminUser.setEnabled(true);
				adminUser.setRoles(Set.of("ROLE_ADMIN", "ROLE_USER")); // Grant both admin and user roles
				adminUser.setCreatedAt(Instant.now());
				adminUser.setUpdatedAt(Instant.now());

				userRepository.save(adminUser);
				System.out.println("Default admin user created: username='admin', password='adminpass'");
			} else {
				System.out.println("Default admin user already exists.");
			}
		};
	}
}