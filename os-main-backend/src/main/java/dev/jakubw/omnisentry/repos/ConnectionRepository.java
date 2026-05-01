package dev.jakubw.omnisentry.repos;

import dev.jakubw.omnisentry.models.ConnectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConnectionRepository extends JpaRepository<ConnectionEntity, UUID> {
    void deleteBySaltEdgeConnectionId(String saltEdgeConnectionId);
    List<ConnectionEntity> findAllByCustomerId(String customerId);
}
