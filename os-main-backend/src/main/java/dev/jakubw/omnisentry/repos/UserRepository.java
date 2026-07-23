package dev.jakubw.omnisentry.repos;

import dev.jakubw.omnisentry.models.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, String> {
    Optional<UserEntity> findByUsername(String username);
    Optional<UserEntity> findBySaltEdgeCustomerId(String saltEdgeCustomerId);
    Optional<UserEntity> findByEmail(String Email);
    Boolean existsByEmailOrUsername(String email, String username);

    boolean existsBySaltEdgeCustomerId(String saltEdgeCustomerId);
}
