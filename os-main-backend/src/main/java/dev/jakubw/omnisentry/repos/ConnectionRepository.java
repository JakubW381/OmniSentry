package dev.jakubw.omnisentry.repos;

import dev.jakubw.omnisentry.models.ConnectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConnectionRepository extends JpaRepository<ConnectionEntity, UUID> {
}
