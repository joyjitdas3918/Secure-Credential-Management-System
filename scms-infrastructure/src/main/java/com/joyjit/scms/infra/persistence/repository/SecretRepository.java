package com.joyjit.scms.infra.persistence.repository;

import com.joyjit.scms.infra.persistence.entity.SecretEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for managing SecretEntity instances.
 * Provides standard CRUD operations and custom queries.
 */
@Repository // Marks this as a Spring Repository component
public interface SecretRepository extends JpaRepository<SecretEntity, UUID> {

    // Custom query to find a secret by its path
    Optional<SecretEntity> findByPath(String path);

    // Custom query to find secrets by a path prefix (e.g., for listing operations)
    List<SecretEntity> findByPathStartingWith(String pathPrefix);
}