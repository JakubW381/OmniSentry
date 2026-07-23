package dev.jakubw.omnisentry.repos;

import dev.jakubw.omnisentry.models.ConnectionEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConnectionRepository extends JpaRepository<ConnectionEntity, String> {


    @EntityGraph(attributePaths = {"user"})
    List<ConnectionEntity> findAllByUserSaltEdgeCustomerId(String customerId);

    boolean existsBySaltEdgeConnectionIdAndUserSaltEdgeCustomerId(
            String saltEdgeConnectionId,
            String saltEdgeCustomerId
    );
}
