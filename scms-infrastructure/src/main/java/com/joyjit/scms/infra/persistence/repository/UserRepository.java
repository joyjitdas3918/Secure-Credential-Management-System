package com.joyjit.scms.infra.persistence.repository;

import com.joyjit.scms.infra.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for managing UserEntity instances.
 */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    // Custom query to find a user by their username (used by Spring Security)
    Optional<UserEntity> findByUsername(String username);
    Optional<UserEntity> findByEmail(String username);

}